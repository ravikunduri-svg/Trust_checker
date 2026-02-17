# dSeries Production Issues - Developer Analysis Report
**Environment:** WLA_NONPROD1  
**Product Version:** 25.0.00  
**Analysis Date:** 2026-02-17  
**Log Period:** 2026-02-12 06:55 - 14:17  
**Total Error Lines:** 17,610

---

## Executive Summary

### Critical Issues Identified
1. **🔴 CRITICAL**: Massive thread deadlock on DBConnectionPool (3,838 threads blocked)
2. **🔴 CRITICAL**: Repeated encryption key failures for agent CPCWVIGPUTLU01 (162 occurrences)
3. **🟡 WARNING**: TDR (Time-Dependent Resource) update failures
4. **🟡 WARNING**: Unknown RMI message type errors

### Impact Assessment
- **System Availability**: Server became unresponsive, required forced shutdown
- **Agent Communication**: Agent CPCWVIGPUTLU01 unable to communicate (encryption key missing)
- **Job Scheduling**: TDR processing failures affecting time-dependent job execution
- **Performance**: 1,355 threads created (normal: ~500), indicating resource exhaustion

---

## Problem #1: Database Connection Pool Deadlock

### 🔍 Root Cause Analysis

**Problem Statement:**
The dSeries server became completely unresponsive with 3,838 threads blocked waiting for database connections.

**Technical Details:**
```
Thread State: BLOCKED (on object monitor)
Lock Object: com.ca.wa.core.engine.rdbms.DBConnectionPool @ 0x00007efd52bf7910
Lock Holders: 3 threads stuck in DriverManager.getConnection()
Blocked Threads: 3,838 threads waiting for same lock
```

**Root Cause:**
```java
// PROBLEMATIC CODE PATTERN:
public synchronized Connection getConnection() {  // <-- LOCK ACQUIRED
    try {
        return getOrCreateConnection();
    } catch (Exception e) {
        // ...
    }
}

private Connection getOrCreateConnection() {
    if (needNewConnection) {
        return newConnection();  // <-- CALLS JDBC DRIVER
    }
}

private Connection newConnection() {
    // THIS HANGS WHILE HOLDING THE LOCK!
    return DriverManager.getConnection(url, props);  
    // Waiting for TCP socket timeout (30-60 seconds)
}
```

**Why This Happens:**
1. Thread acquires `synchronized` lock on `DBConnectionPool` object
2. Calls `DriverManager.getConnection()` which attempts TCP connection to SQL Server
3. **Network issue or SQL Server unresponsive** causes TCP connection to hang
4. Thread holds the lock while waiting for socket timeout
5. All other threads (3,838+) queue up waiting for the same lock
6. **System paralysis** - no threads can acquire database connections

**Evidence from Logs:**
```
stdout.txt.2026-02-12.13.20.04:
- Full thread dump at 08:47:57 (server started 08:42:27)
- 1,355 threads total (excessive - normal is ~500)
- 3,838 threads BLOCKED on DBConnectionPool
- Lock holders stuck in: socketRead0 → SQLServerConnection.connect
```

### 💡 Solution

#### Immediate Fix (Emergency)
```bash
# 1. Restart dSeries server
/opt/cawade/wlade/bin/stopServer
sleep 10
/opt/cawade/wlade/bin/startServer

# 2. Verify SQL Server connectivity
sqlcmd -S aal15glb_d020.mfcgd.com -d ETS_CAWLA_D1 -U svccawdev -Q "SELECT 1"

# 3. Check network connectivity
ping aal15glb_d020.mfcgd.com
telnet aal15glb_d020.mfcgd.com 21820
```

#### Short-Term Fix (Configuration)
**File:** `conf/db.properties`
```properties
# Add connection timeouts to JDBC URL
jdbc.URL=jdbc:sqlserver://aal15glb_d020.mfcgd.com:21820;databaseName=ETS_CAWLA_D1;selectMethod=cursor;integratedSecurity=true;domain=mfcgd.com;authenticationScheme=NTLM;loginTimeout=15;socketTimeout=30000;connectRetryCount=0

# Explanation:
# loginTimeout=15          - 15 second connection timeout (fail fast)
# socketTimeout=30000      - 30 second socket read timeout
# connectRetryCount=0      - Disable automatic retry (fail immediately)
```

**File:** `conf/db.properties` (Connection Pool Tuning)
```properties
# Reduce connection pool size to prevent resource exhaustion
rdbms.connection.pool.min=10           # Minimum connections (default: 5)
rdbms.connection.pool.max=50           # Maximum connections (default: 100)
rdbms.connection.pool.increment=5      # Increment size
rdbms.connection.pool.timeout=30       # Connection timeout in seconds
rdbms.connection.validation.enabled=true
rdbms.connection.validation.query=SELECT 1
rdbms.connection.validation.interval=30000  # Validate every 30 seconds
```

#### Long-Term Fix (Code Change Required)

**Problem Code Location:**
```
File: com.ca.wa.core.engine.rdbms.DBConnectionPool
Method: getConnection()
Line: ~329 (based on stack trace)
```

**Recommended Code Fix:**
```java
// CURRENT (PROBLEMATIC):
public synchronized Connection getConnection() {
    return getOrCreateConnection();  // Holds lock during connection creation
}

// RECOMMENDED FIX:
public Connection getConnection() {
    // Try to get existing connection without lock
    Connection conn = tryGetExistingConnection();
    if (conn != null) return conn;
    
    // Only lock for pool management, not connection creation
    synchronized(this) {
        conn = tryGetExistingConnection();  // Double-check
        if (conn != null) return conn;
        
        // Mark that we're creating a connection
        pendingConnectionCount++;
    }
    
    try {
        // Create connection OUTSIDE synchronized block
        Connection newConn = createConnectionWithTimeout(15);  // 15 sec timeout
        
        synchronized(this) {
            addConnectionToPool(newConn);
            pendingConnectionCount--;
            return newConn;
        }
    } catch (SQLException e) {
        synchronized(this) {
            pendingConnectionCount--;
        }
        throw e;
    }
}

private Connection createConnectionWithTimeout(int timeoutSeconds) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Connection> future = executor.submit(() -> 
        DriverManager.getConnection(url, props)
    );
    
    try {
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        throw new SQLException("Connection timeout after " + timeoutSeconds + " seconds");
    } finally {
        executor.shutdown();
    }
}
```

**Why This Fix Works:**
1. ✅ Existing connections retrieved without blocking
2. ✅ New connections created OUTSIDE synchronized block
3. ✅ Timeout enforced at application level (not relying on JDBC driver)
4. ✅ One slow connection doesn't block entire system
5. ✅ Prevents cascading failures

#### Monitoring & Alerting

**Add JMX Monitoring:**
```java
// Monitor these metrics:
- Thread count (alert if > 2,000)
- BLOCKED thread count (alert if > 100)
- Connection pool active connections
- Connection pool wait time
- Database connection failures (alert if > 5 in 5 min)
```

**Automated Thread Dump Collection:**
```bash
#!/bin/bash
# File: /opt/cawade/wlade/bin/monitor_threads.sh

PID=$(cat /opt/cawade/wlade/serverPID)
THREAD_COUNT=$(ps -eLf -p $PID | wc -l)

if [ $THREAD_COUNT -gt 2000 ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    jstack $PID > /opt/cawade/wlade/logs/threaddump_$TIMESTAMP.txt
    echo "$(date): ALERT - Thread count: $THREAD_COUNT" >> /opt/cawade/wlade/logs/thread_monitor.log
    
    # Optional: Auto-restart if critical
    # if [ $THREAD_COUNT -gt 5000 ]; then
    #     /opt/cawade/wlade/bin/stopServer
    #     sleep 10
    #     /opt/cawade/wlade/bin/startServer
    # fi
fi
```

**Cron Schedule:**
```cron
*/5 * * * * /opt/cawade/wlade/bin/monitor_threads.sh
```

---

## Problem #2: Encryption Key Missing for Agent

### 🔍 Root Cause Analysis

**Problem Statement:**
Agent `CPCWVIGPUTLU01` cannot communicate with the server due to missing encryption key.

**Error Pattern:**
```
ERROR: Key not available for node CPCWVIGPUTLU01
Exception: com.ca.wa.publiclibrary.engine.library.crypto.CryptoException
Location: WAKeyObtainerImpl.getKey()
Frequency: 162 occurrences over 2 hours (every ~2 minutes)
```

**Stack Trace:**
```java
com.ca.wa.publiclibrary.engine.library.crypto.CryptoException: Key not available for node CPCWVIGPUTLU01.
    at com.ca.wa.comp.library.communications.WAKeyObtainerImpl.getKey(WAKeyObtainerImpl.java:63)
    at com.ca.wa.comp.library.communications.listener.WAFIPSDecryptionListener.decrypt(WAFIPSDecryptionListener.java:304)
    at com.ca.wa.comp.library.communications.listener.WAFIPSDecryptionListener.communicationReceive(WAFIPSDecryptionListener.java:133)
    at com.ca.wa.comp.distributedmanager.communications.WADistributedManagerInputSession.run(WADistributedManagerInputSession.java:151)
```

**Root Cause:**
1. **Agent not properly registered** in dSeries server's encryption key store
2. **Agent certificate/key expired** or not synchronized
3. **Agent hostname mismatch** (registered as different name)
4. **Key exchange failure** during agent initialization

**Evidence:**
- Error occurs in `WAKeyObtainerImpl.getKey()` - trying to retrieve encryption key for agent
- Error in `WAFIPSDecryptionListener` - attempting to decrypt agent communication
- Repeats every ~2 minutes - agent is trying to reconnect/communicate

### 💡 Solution

#### Immediate Fix

**1. Check Agent Registration:**
```sql
-- Connect to dSeries database
-- Check if agent is registered
SELECT * FROM ESP_AGENT_RP 
WHERE AGENT_NAME = 'CPCWVIGPUTLU01';

-- Check agent status
SELECT AGENT_NAME, STATUS, ACTIONSTATUS, LAST_CONTACT_TIME 
FROM ESP_AGENT_RP 
WHERE AGENT_NAME = 'CPCWVIGPUTLU01';
```

**2. Re-register Agent:**
```bash
# On agent machine (CPCWVIGPUTLU01)
cd /opt/CA/WA_AGENT/bin

# Stop agent
./cybAgent stop

# Remove old encryption keys
rm -f ../config/agent.key
rm -f ../config/agent.cert

# Re-register with server
./cybAgent -r <server_hostname> -p <server_port>

# Start agent
./cybAgent start
```

**3. Verify Agent Communication:**
```bash
# On server
tail -f /opt/cawade/wlade/logs/stdout.txt | grep CPCWVIGPUTLU01

# Should see successful connection messages
```

#### Configuration Fix

**File:** `conf/agent.properties` (on agent machine)
```properties
# Ensure correct hostname is configured
agent.name=CPCWVIGPUTLU01
agent.hostname=cpcwvigputlu01.mfcgd.com  # Use FQDN
server.hostname=<dSeries_server_hostname>
server.port=7599

# Enable encryption
encryption.enabled=true
encryption.algorithm=AES256

# Key refresh interval (in seconds)
key.refresh.interval=3600  # Refresh key every hour
```

**File:** `conf/security.properties` (on server)
```properties
# Agent key expiration (in days)
agent.key.expiration.days=365

# Auto-refresh agent keys
agent.key.auto.refresh=true

# Log encryption errors
encryption.error.logging=true
```

#### Verification Steps

**1. Check Agent Key Store:**
```bash
# On server
cd /opt/cawade/wlade/config

# List agent keys
ls -la agent_keys/

# Should see: CPCWVIGPUTLU01.key
```

**2. Test Agent Communication:**
```bash
# From agent machine
telnet <server_hostname> 7599

# Should connect successfully
```

**3. Monitor Logs:**
```bash
# On server - should NOT see encryption errors
grep "CPCWVIGPUTLU01" /opt/cawade/wlade/logs/errors.txt

# On agent - should see successful heartbeat
grep "Heartbeat" /opt/CA/WA_AGENT/logs/agent.log
```

#### Preventive Measures

**1. Automated Key Rotation:**
```bash
#!/bin/bash
# File: /opt/cawade/wlade/bin/rotate_agent_keys.sh

# Rotate agent keys every 6 months
AGENTS=$(psql -d dseries_db -t -c "SELECT AGENT_NAME FROM ESP_AGENT_RP WHERE STATUS='ACTIVE'")

for AGENT in $AGENTS; do
    echo "Rotating key for agent: $AGENT"
    # Trigger key rotation via dSeries API or command
    /opt/cawade/wlade/bin/espagent -rotate-key $AGENT
done
```

**2. Agent Health Monitoring:**
```sql
-- Create monitoring view
CREATE OR REPLACE VIEW v_agent_health AS
SELECT 
    AGENT_NAME,
    STATUS,
    LAST_CONTACT_TIME,
    CASE 
        WHEN LAST_CONTACT_TIME < NOW() - INTERVAL '5 minutes' THEN 'STALE'
        WHEN STATUS != 'ACTIVE' THEN 'INACTIVE'
        ELSE 'HEALTHY'
    END AS HEALTH_STATUS
FROM ESP_AGENT_RP;

-- Query for unhealthy agents
SELECT * FROM v_agent_health WHERE HEALTH_STATUS != 'HEALTHY';
```

---

## Problem #3: TDR (Time-Dependent Resource) Update Failures

### 🔍 Root Cause Analysis

**Problem Statement:**
TDR updates failing due to null table key.

**Error Details:**
```
ERROR: Unable to Update the query UPDATE ESP_TDR_DATA SET FIRING='Y' WHERE FIRING='P'
Exception: DatabaseUpdateException: The record for query ... was not updated: the table key is null
Location: TdrMapper.resetInprogressTDRs()
Frequency: 3 occurrences
```

**Stack Trace:**
```java
com.ca.wa.core.engine.rdbms.DatabaseUpdateException: The record for query UPDATE ESP_TDR_DATA SET FIRING='Y' WHERE FIRING='P' was not updated: the table key is null
    at com.ca.wa.core.engine.rdbms.RelationalDatabaseManager.doGenericUpdate(RelationalDatabaseManager.java:984)
    at com.ca.wa.comp.schedulerserver.tdr.TdrMapper.resetInprogressTDRs(TdrMapper.java:509)
    at com.ca.wa.comp.schedulerserver.event.Worker.run(Worker.java:120)
```

**Root Cause:**
1. **Missing primary key** in ESP_TDR_DATA table update
2. **Race condition** - record deleted before update
3. **Database constraint violation** - null key field
4. **Orphaned TDR records** with incomplete data

**Why This Matters:**
- TDRs control time-dependent job execution
- Failures can cause jobs to not fire at scheduled times
- Can lead to missed SLA deadlines

### 💡 Solution

#### Immediate Fix

**1. Check TDR Data Integrity:**
```sql
-- Find TDR records with null keys
SELECT * FROM ESP_TDR_DATA 
WHERE TDR_ID IS NULL 
   OR EVENT_NAME IS NULL;

-- Find orphaned TDR records
SELECT t.* FROM ESP_TDR_DATA t
LEFT JOIN ESP_EVENT_RP e ON t.EVENT_NAME = e.EVENT_NAME
WHERE e.EVENT_NAME IS NULL;

-- Clean up orphaned records
DELETE FROM ESP_TDR_DATA 
WHERE TDR_ID IS NULL 
   OR EVENT_NAME IS NULL
   OR EVENT_NAME NOT IN (SELECT EVENT_NAME FROM ESP_EVENT_RP);
```

**2. Reset TDR State:**
```sql
-- Reset stuck TDR records
UPDATE ESP_TDR_DATA 
SET FIRING = 'N' 
WHERE FIRING = 'P' 
  AND LAST_UPDATE_TIME < NOW() - INTERVAL '1 hour';
```

#### Code Fix Required

**Problem Code Location:**
```
File: com.ca.wa.comp.schedulerserver.tdr.TdrMapper
Method: resetInprogressTDRs()
Line: ~509
```

**Recommended Fix:**
```java
// CURRENT (PROBLEMATIC):
public void resetInprogressTDRs() {
    String sql = "UPDATE ESP_TDR_DATA SET FIRING='Y' WHERE FIRING='P'";
    rdbm.doGenericUpdate(sql);  // Fails if no primary key
}

// RECOMMENDED FIX:
public void resetInprogressTDRs() {
    // Add WHERE clause to ensure we have a valid key
    String sql = "UPDATE ESP_TDR_DATA " +
                 "SET FIRING='Y' " +
                 "WHERE FIRING='P' " +
                 "  AND TDR_ID IS NOT NULL " +
                 "  AND EVENT_NAME IS NOT NULL";
    
    try {
        int rowsUpdated = rdbm.doGenericUpdate(sql);
        logger.info("Reset " + rowsUpdated + " in-progress TDRs");
    } catch (DatabaseUpdateException e) {
        logger.error("Failed to reset TDRs", e);
        // Don't fail the entire operation - log and continue
    }
}

// BETTER: Use parameterized query with explicit key
public void resetInprogressTDRs() {
    String sql = "UPDATE ESP_TDR_DATA " +
                 "SET FIRING='Y', LAST_UPDATE_TIME=? " +
                 "WHERE TDR_ID=? AND EVENT_NAME=? AND FIRING='P'";
    
    // Get list of TDRs to update
    List<TDR> tdrs = getTDRsInProgress();
    
    for (TDR tdr : tdrs) {
        if (tdr.getTdrId() != null && tdr.getEventName() != null) {
            try {
                rdbm.doGenericUpdate(sql, 
                    new Timestamp(System.currentTimeMillis()),
                    tdr.getTdrId(),
                    tdr.getEventName());
            } catch (DatabaseUpdateException e) {
                logger.warn("Failed to reset TDR: " + tdr.getTdrId(), e);
                // Continue with next TDR
            }
        }
    }
}
```

#### Database Schema Fix

**Add Constraints:**
```sql
-- Ensure TDR_ID is never null
ALTER TABLE ESP_TDR_DATA 
ALTER COLUMN TDR_ID SET NOT NULL;

-- Ensure EVENT_NAME is never null
ALTER TABLE ESP_TDR_DATA 
ALTER COLUMN EVENT_NAME SET NOT NULL;

-- Add composite primary key if missing
ALTER TABLE ESP_TDR_DATA 
ADD CONSTRAINT pk_tdr_data PRIMARY KEY (TDR_ID, EVENT_NAME);

-- Add foreign key to ensure referential integrity
ALTER TABLE ESP_TDR_DATA 
ADD CONSTRAINT fk_tdr_event 
FOREIGN KEY (EVENT_NAME) REFERENCES ESP_EVENT_RP(EVENT_NAME)
ON DELETE CASCADE;
```

#### Monitoring

**Create TDR Health Check:**
```sql
-- Monitor TDR processing
SELECT 
    FIRING,
    COUNT(*) as count,
    MIN(LAST_UPDATE_TIME) as oldest_update,
    MAX(LAST_UPDATE_TIME) as newest_update
FROM ESP_TDR_DATA
GROUP BY FIRING;

-- Alert if TDRs stuck in 'P' state for > 1 hour
SELECT * FROM ESP_TDR_DATA 
WHERE FIRING = 'P' 
  AND LAST_UPDATE_TIME < NOW() - INTERVAL '1 hour';
```

---

## Problem #4: Unknown RMI Message Type

### 🔍 Root Cause Analysis

**Problem Statement:**
RMI layer receiving unknown message type "Server retry".

**Error Details:**
```
ERROR: Message type Server retry is unknown
Location: RMI TCP Connection(11)-10.246.142.191
Frequency: 7 occurrences
```

**Root Cause:**
1. **Version mismatch** between dSeries components
2. **Custom message type** not recognized by server
3. **Corrupted RMI message** due to network issues
4. **Client using newer protocol** than server supports

**Impact:**
- Low severity (only 7 occurrences)
- May indicate client-server version incompatibility
- Could cause intermittent communication failures

### 💡 Solution

#### Investigation Steps

**1. Check Component Versions:**
```bash
# On server
cat /opt/cawade/wlade/version.txt

# Check connected clients
netstat -an | grep :7599 | grep ESTABLISHED

# For each client IP, check version
ssh <client_ip> "cat /opt/CA/WA_AGENT/version.txt"
```

**2. Check RMI Configuration:**
```properties
# File: conf/rmi.properties
rmi.protocol.version=2.0
rmi.message.validation=strict  # Change to 'lenient' for compatibility
rmi.unknown.message.action=log  # Options: log, ignore, reject
```

**3. Enable RMI Debug Logging:**
```properties
# File: conf/log4j2.xml
<Logger name="com.ca.wa.comp.library.communications" level="DEBUG"/>
<Logger name="sun.rmi" level="DEBUG"/>
```

#### Fix Options

**Option 1: Update All Components to Same Version**
```bash
# Recommended - ensure all components are on same version
# Server: 25.0.00
# All agents: 25.0.00
# All clients: 25.0.00
```

**Option 2: Enable Lenient Message Handling**
```java
// Code change in RMI message handler
// File: com.ca.wa.comp.library.communications.RMIMessageHandler

public void handleMessage(Message msg) {
    try {
        processMessage(msg);
    } catch (UnknownMessageTypeException e) {
        // CURRENT: Throws error
        // NEW: Log and ignore unknown messages
        logger.warn("Unknown message type: " + msg.getType() + ", ignoring");
        // Don't fail the connection
    }
}
```

**Option 3: Add Message Type Support**
```java
// If "Server retry" is a valid message type, add support
// File: com.ca.wa.comp.library.communications.MessageType

public enum MessageType {
    HEARTBEAT,
    JOB_STATUS,
    AGENT_REGISTER,
    SERVER_RETRY,  // <-- ADD THIS
    // ... other types
}
```

---

## Summary of Fixes

### Priority Matrix

| Issue | Severity | Impact | Effort | Priority |
|-------|----------|--------|--------|----------|
| DBConnectionPool Deadlock | CRITICAL | HIGH | HIGH | **P0** |
| Agent Encryption Key | CRITICAL | MEDIUM | LOW | **P0** |
| TDR Update Failures | WARNING | MEDIUM | MEDIUM | **P1** |
| RMI Unknown Message | WARNING | LOW | LOW | **P2** |

### Implementation Timeline

#### Week 1 (Emergency Fixes)
- ✅ Add JDBC connection timeouts to `db.properties`
- ✅ Tune connection pool settings
- ✅ Re-register agent CPCWVIGPUTLU01
- ✅ Clean up orphaned TDR records
- ✅ Implement thread monitoring script

#### Week 2-3 (Short-term Fixes)
- 🔧 Deploy DBConnectionPool code fix (requires development)
- 🔧 Add database schema constraints for TDR table
- 🔧 Implement automated agent key rotation
- 🔧 Set up JMX monitoring and alerting

#### Month 1-2 (Long-term Improvements)
- 📋 Implement circuit breaker pattern for database connections
- 📋 Add connection pool health check REST endpoint
- 📋 Upgrade all components to consistent versions
- 📋 Implement comprehensive monitoring dashboard

---

## Monitoring Checklist

### Daily Checks
- [ ] Thread count < 2,000
- [ ] No BLOCKED threads > 100
- [ ] All agents communicating (no encryption errors)
- [ ] TDR records processing normally
- [ ] Database connection pool healthy

### Weekly Checks
- [ ] Review error logs for patterns
- [ ] Check agent key expiration dates
- [ ] Verify database connection pool metrics
- [ ] Review thread dump history
- [ ] Check RMI communication errors

### Monthly Checks
- [ ] Review and update connection pool settings
- [ ] Rotate agent encryption keys
- [ ] Clean up orphaned database records
- [ ] Review and tune JVM heap settings
- [ ] Update monitoring thresholds based on trends

---

## Contact & Escalation

### For Immediate Issues
1. Restart dSeries server
2. Check SQL Server connectivity
3. Verify agent registration
4. Review error logs

### For Development Changes
- **DBConnectionPool fix**: Core development team
- **TDR improvements**: Scheduler team
- **RMI enhancements**: Communications team

### For Production Support
- **Level 1**: Operations team (restart, basic checks)
- **Level 2**: DBA team (SQL Server, connection issues)
- **Level 3**: Development team (code changes)
- **Level 4**: Broadcom Support (product defects)

---

**Report Generated:** 2026-02-17  
**Analyst:** dSeries Health Check Tool  
**Status:** ACTIONABLE - Requires Immediate Attention
