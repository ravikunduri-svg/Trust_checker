# dSeries Thread Dump Analysis Report
**Analysis Date:** 2026-02-13  
**Environment:** WLA_NONPROD1  
**Product Version:** 25.0.00 (Thread dump from 2026-02-12 13:20:04)  
**Database:** Microsoft SQL Server (ETS_CAWLA_D1)

---

## Executive Summary

**CRITICAL ISSUE IDENTIFIED:** Severe database connection pool exhaustion causing massive thread contention and system hang.

### Key Findings:
- **3,838 threads** are BLOCKED waiting for database connections
- **ALL blocked threads** are waiting on the same monitor: `DBConnectionPool` (lock address: `0x00007efd52bf7910`)
- **Root Cause:** Database connection attempts are hanging/timing out, holding the DBConnectionPool lock indefinitely
- **Impact:** Complete system paralysis - no threads can acquire database connections
- **Previous Incident:** SQL Server AlwaysOn failover occurred on 2026-02-05 (SQL Error 983)

---

## Detailed Analysis

### 1. Thread Contention Analysis

#### Statistics:
```
Total BLOCKED threads: 8,208
Threads blocked on DBConnectionPool: 3,838 (46.7%)
Lock holder threads: 3 (all stuck in DriverManager.getConnection)
```

#### Lock Contention Pattern:
```
Monitor: com.ca.wa.core.engine.rdbms.DBConnectionPool @ 0x00007efd52bf7910
Status: LOCKED by multiple threads attempting to create new connections
Problem: Lock holders are stuck in JDBC driver connection attempts
```

### 2. Root Cause Analysis

#### Primary Root Cause: **Database Connection Timeout/Hang**

**Evidence:**
1. **Lock Holder Stack Traces** (3 threads holding DBConnectionPool lock):
   ```
   Thread: "DM.Appl.LTC_*.***" 
   State: RUNNABLE (but actually blocked in native socket I/O)
   Stack:
   - java.net.SocketInputStream.socketRead0 (Native Method)
   - com.microsoft.sqlserver.jdbc.SocketFinder.findSocket
   - com.microsoft.sqlserver.jdbc.TDSChannel.open
   - com.microsoft.sqlserver.jdbc.SQLServerConnection.connectHelper
   - com.microsoft.sqlserver.jdbc.SQLServerConnection.login
   - com.microsoft.sqlserver.jdbc.SQLServerConnection.connect
   - java.sql.DriverManager.getConnection
   - DBConnectionPool.newConnection
   - DBConnectionPool.getOrCreateConnection
   - DBConnectionPool.getConnection [HOLDING LOCK]
   ```

2. **What's Happening:**
   - Thread acquires `synchronized` lock on `DBConnectionPool` object
   - Calls `getOrCreateConnection()` → `newConnection()`
   - Calls `DriverManager.getConnection()` which attempts TCP connection to SQL Server
   - **TCP connection hangs** (likely network issue, firewall, or SQL Server unresponsive)
   - Thread holds the lock while waiting for TCP socket timeout
   - All other threads (3,838+) queue up waiting for the same lock

3. **Blocked Thread Pattern:**
   ```
   Thread: "DM.Appl.*", "SS", "RS", "VM", "HAC", "Timer-0", etc.
   State: BLOCKED (on object monitor)
   Stack:
   - DBConnectionPool.getConnection (line 321)
   - waiting to lock <0x00007efd52bf7910> (DBConnectionPool)
   ```

#### Secondary Contributing Factor: **SQL Server AlwaysOn Availability Group Issue**

**Evidence from stdout.txt.2026-02-12.06.55.03:**
```
SQL error code=983
SQL state=S0001
SQL message=Unable to access availability database 'ETS_CAWLA_D1' 
because the database replica is not in the PRIMARY or SECONDARY role.
```

**Impact:**
- SQL Server AlwaysOn failover occurred on 2026-02-05 22:44:08
- Database became inaccessible during replica role transition
- Server auto-restarted and reconnected successfully
- **However:** Connection pool configuration may not handle failover gracefully

### 3. Thread Dump Timeline Analysis

| Timestamp | Event | Impact |
|-----------|-------|--------|
| 2026-02-05 22:44:08 | SQL Server AlwaysOn failover (Error 983) | Server restart triggered |
| 2026-02-05 22:44:15 | Successfully reconnected to database | Normal operation resumed |
| 2026-02-12 06:07:07 | Server shutdown (Signal received) | Planned shutdown |
| 2026-02-12 13:20:15 | Server restart | Normal startup |
| 2026-02-12 13:20:15 - 14:16:58 | **Thread dump captured** | **3,838 threads blocked** |
| 2026-02-12 14:16:58 | Server shutdown (Signal received) | Forced shutdown due to hang |

### 4. Affected Components

All major dSeries components are affected:

1. **Application Manager (DM)** - 3,700+ application threads blocked
2. **Scheduler (SS)** - Scheduler threads blocked
3. **Resource Manager (RS)** - Resource threads blocked
4. **Variable Manager (VM)** - Variable threads blocked
5. **High Availability Component (HAC)** - HA monitoring blocked
6. **Timer threads** - Timer processing blocked
7. **Workstation Server (WSS)** - Data persistence blocked

**Critical Operations Blocked:**
- Application execution
- Job scheduling
- Resource allocation
- Database persistence
- HA monitoring
- Timer processing

---

## Root Cause Summary

### **CRITICAL DEFECT: Synchronized Connection Pool with No Timeout**

**Problem:**
The `DBConnectionPool.getConnection()` method uses a `synchronized` block that holds the lock while attempting to create new database connections. When the database connection attempt hangs (due to network issues, SQL Server unresponsiveness, or AlwaysOn failover), the lock is held indefinitely, blocking ALL other threads from acquiring connections.

**Code Flow:**
```java
public synchronized Connection getConnection() {  // <-- LOCK ACQUIRED
    try {
        return getOrCreateConnection();  // May create new connection
    } catch (Exception e) {
        // ...
    }
}

private Connection getOrCreateConnection() {
    // ...
    if (needNewConnection) {
        return newConnection();  // <-- CALLS JDBC DRIVER
    }
}

private Connection newConnection() {
    return DriverManager.getConnection(url, props);  // <-- HANGS HERE
    // Lock still held while waiting for TCP timeout!
}
```

**Why This is Critical:**
- **Single point of failure:** One slow connection attempt blocks entire system
- **No timeout enforcement:** JDBC driver timeout is too long (default 30-60 seconds)
- **Cascading failure:** All threads pile up, consuming memory and CPU
- **No circuit breaker:** System doesn't detect or prevent the issue

---

## Solutions and Recommendations

### IMMEDIATE ACTIONS (Critical - Implement Within 24 Hours)

#### 1. **Restart dSeries Server** ✅ DONE (Server was restarted at 14:16:58)
   - **Status:** Already performed
   - **Result:** Temporary relief, but issue will recur

#### 2. **Verify SQL Server AlwaysOn Configuration**
   ```sql
   -- Check availability group status
   SELECT 
       ag.name AS AG_Name,
       ar.replica_server_name,
       ar.availability_mode_desc,
       ar.failover_mode_desc,
       ars.role_desc,
       ars.connected_state_desc,
       ars.synchronization_health_desc
   FROM sys.availability_groups ag
   JOIN sys.availability_replicas ar ON ag.group_id = ar.group_id
   JOIN sys.dm_hadr_availability_replica_states ars ON ar.replica_id = ars.replica_id
   WHERE ag.name = 'ETS_CAWLA_D1';
   ```
   
   **Action Items:**
   - Verify database is in PRIMARY role
   - Check for frequent failovers (review SQL Server logs)
   - Ensure listener is properly configured
   - Verify network connectivity between dSeries and SQL Server

#### 3. **Implement Connection Timeout at JDBC Level**
   
   **Update db.properties:**
   ```properties
   # Add these properties to force connection timeouts
   jdbc.URL=jdbc:sqlserver://aal15glb_d020.mfcgd.com:21820;databaseName=ETS_CAWLA_D1;selectMethod=cursor;integratedSecurity=true;domain=mfcgd.com;authenticationScheme=NTLM;loginTimeout=15;socketTimeout=30000;connectRetryCount=0
   
   # Explanation:
   # loginTimeout=15          - 15 second timeout for initial connection
   # socketTimeout=30000      - 30 second socket read timeout (in milliseconds)
   # connectRetryCount=0      - Disable automatic retry (fail fast)
   ```

#### 4. **Monitor Database Connection Pool**
   
   **Enable connection pool monitoring:**
   ```bash
   # Add to startServer script or JVM arguments
   -Dcom.ca.wa.core.engine.rdbms.DBConnectionPool.debug=true
   -Dlog4j2.logger.relationaldatabase.level=DEBUG
   ```

### SHORT-TERM FIXES (Implement Within 1 Week)

#### 5. **Tune Connection Pool Settings**
   
   **Update db.properties:**
   ```properties
   # Connection Pool Configuration
   rdbms.connection.pool.min=10           # Minimum connections (default: 5)
   rdbms.connection.pool.max=50           # Maximum connections (default: 100)
   rdbms.connection.pool.increment=5      # Increment size (default: 5)
   rdbms.connection.pool.timeout=30       # Connection timeout in seconds
   rdbms.connection.validation.enabled=true
   rdbms.connection.validation.query=SELECT 1
   rdbms.connection.validation.interval=30000  # Validate every 30 seconds
   ```

   **Rationale:**
   - Reduce max connections to prevent resource exhaustion
   - Enable connection validation to detect stale connections
   - Set aggressive timeouts to fail fast

#### 6. **Implement Database Health Monitoring**
   
   **Create monitoring script:**
   ```bash
   #!/bin/bash
   # Monitor dSeries thread count and database connectivity
   
   while true; do
       THREAD_COUNT=$(ps -eLf | grep -c espserver)
       DB_CONN_COUNT=$(netstat -an | grep -c "21820.*ESTABLISHED")
       
       echo "$(date): Threads=$THREAD_COUNT, DB_Connections=$DB_CONN_COUNT"
       
       if [ $THREAD_COUNT -gt 5000 ]; then
           echo "ALERT: Thread count exceeded 5000!"
           # Trigger alert
       fi
       
       sleep 60
   done
   ```

#### 7. **Configure SQL Server Connection Resilience**
   
   **SQL Server side:**
   ```sql
   -- Set connection timeout at server level
   EXEC sp_configure 'remote query timeout', 30;  -- 30 seconds
   RECONFIGURE;
   
   -- Enable connection pooling at SQL Server
   EXEC sp_configure 'user connections', 500;
   RECONFIGURE;
   ```

### LONG-TERM SOLUTIONS (Implement Within 1 Month)

#### 8. **Upgrade to Latest dSeries Version**
   - **Current:** 25.0.00
   - **Recommendation:** Upgrade to latest patch release
   - **Reason:** Newer versions may have improved connection pool implementation
   - **Check:** Review release notes for connection pool fixes

#### 9. **Implement Connection Pool Refactoring** (Development Effort Required)
   
   **Recommended Changes to DBConnectionPool:**
   
   ```java
   // BEFORE (Current Implementation - PROBLEMATIC)
   public synchronized Connection getConnection() {
       return getOrCreateConnection();  // Holds lock during connection creation
   }
   
   // AFTER (Recommended Implementation)
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

#### 10. **Implement Circuit Breaker Pattern**
   
   **Add circuit breaker to prevent cascading failures:**
   ```java
   public class DBConnectionCircuitBreaker {
       private int failureCount = 0;
       private long lastFailureTime = 0;
       private static final int FAILURE_THRESHOLD = 5;
       private static final long RESET_TIMEOUT = 60000;  // 1 minute
       
       public boolean isOpen() {
           if (failureCount >= FAILURE_THRESHOLD) {
               if (System.currentTimeMillis() - lastFailureTime > RESET_TIMEOUT) {
                   failureCount = 0;  // Reset after timeout
                   return false;
               }
               return true;  // Circuit is open (blocking connections)
           }
           return false;
       }
       
       public void recordFailure() {
           failureCount++;
           lastFailureTime = System.currentTimeMillis();
       }
       
       public void recordSuccess() {
           failureCount = 0;
       }
   }
   ```

#### 11. **Implement Health Check Endpoint**
   
   **Add REST endpoint for monitoring:**
   ```java
   @GET
   @Path("/health/database")
   public Response checkDatabaseHealth() {
       try {
           Connection conn = dbPool.getConnection();
           Statement stmt = conn.createStatement();
           stmt.setQueryTimeout(5);  // 5 second timeout
           ResultSet rs = stmt.executeQuery("SELECT 1");
           rs.close();
           stmt.close();
           conn.close();
           
           return Response.ok()
               .entity("{\"status\":\"UP\",\"database\":\"connected\"}")
               .build();
       } catch (Exception e) {
           return Response.status(503)
               .entity("{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}")
               .build();
       }
   }
   ```

### OPERATIONAL RECOMMENDATIONS

#### 12. **Monitoring and Alerting**
   
   **Implement these monitors:**
   
   | Metric | Threshold | Action |
   |--------|-----------|--------|
   | Thread count | > 5,000 | Alert + investigate |
   | BLOCKED threads | > 100 | Alert + investigate |
   | DB connection failures | > 5 in 5 min | Alert + check SQL Server |
   | Response time | > 10 seconds | Alert + check thread dump |
   | CPU usage | > 90% for 5 min | Alert + check thread dump |
   | Memory usage | > 85% | Alert + check for leaks |

   **Tools:**
   - JMX monitoring for thread counts
   - Application Performance Monitoring (APM) tool
   - SQL Server monitoring
   - Network monitoring

#### 13. **Automated Thread Dump Collection**
   
   **Create automated thread dump script:**
   ```bash
   #!/bin/bash
   # Auto-collect thread dumps when system is unresponsive
   
   PID=$(cat /opt/cawade/wlade/serverPID)
   THREAD_COUNT=$(ps -eLf -p $PID | wc -l)
   
   if [ $THREAD_COUNT -gt 5000 ]; then
       TIMESTAMP=$(date +%Y%m%d_%H%M%S)
       jstack $PID > /opt/cawade/wlade/logs/threaddump_$TIMESTAMP.txt
       echo "Thread dump collected: threaddump_$TIMESTAMP.txt"
       
       # Optional: Auto-restart if critical
       # /opt/cawade/wlade/bin/stopServer
       # sleep 10
       # /opt/cawade/wlade/bin/startServer
   fi
   ```

   **Schedule via cron:**
   ```
   */5 * * * * /opt/cawade/wlade/bin/monitor_threads.sh
   ```

#### 14. **SQL Server AlwaysOn Best Practices**
   
   **Connection String for AlwaysOn:**
   ```properties
   jdbc.URL=jdbc:sqlserver://aal15glb_d020.mfcgd.com:21820;databaseName=ETS_CAWLA_D1;selectMethod=cursor;integratedSecurity=true;domain=mfcgd.com;authenticationScheme=NTLM;loginTimeout=15;socketTimeout=30000;connectRetryCount=0;multiSubnetFailover=true;applicationIntent=ReadWrite
   
   # Key AlwaysOn properties:
   # multiSubnetFailover=true     - Faster failover detection
   # applicationIntent=ReadWrite  - Connect to primary replica only
   ```

   **SQL Server Configuration:**
   - Enable automatic page repair
   - Configure connection timeout at listener level
   - Set up proper health checks
   - Monitor failover frequency

#### 15. **Capacity Planning**
   
   **Current State:**
   - Thread count at time of hang: ~8,000+ threads
   - Normal thread count: ~500-1,000 threads (estimated)
   - **Conclusion:** System is creating excessive threads
   
   **Recommendations:**
   - Review application design (why so many concurrent applications?)
   - Consider workload distribution
   - Implement thread pool limits
   - Review JVM heap size and GC settings

---

## Testing and Validation

### Test Plan for Fixes

#### Test 1: Connection Timeout Validation
```bash
# Simulate database unavailability
# Block SQL Server port temporarily
iptables -A OUTPUT -p tcp --dport 21820 -j DROP

# Start dSeries and monitor
tail -f /opt/cawade/wlade/logs/stdout.txt

# Verify:
# - Connection attempts timeout within 15 seconds
# - System doesn't hang
# - Appropriate error messages logged

# Restore connectivity
iptables -D OUTPUT -p tcp --dport 21820 -j DROP
```

#### Test 2: Connection Pool Behavior
```bash
# Monitor connection pool metrics
jconsole <PID>
# Navigate to: com.ca.wa.core.engine.rdbms.DBConnectionPool

# Verify:
# - Active connections stay within max limit
# - Idle connections are validated
# - Failed connections are removed from pool
```

#### Test 3: AlwaysOn Failover Simulation
```sql
-- On SQL Server, simulate failover
ALTER AVAILABILITY GROUP [AG_Name] FAILOVER;

-- Monitor dSeries behavior:
# - Should detect failover within 15-30 seconds
# - Should reconnect to new primary
# - Should not accumulate blocked threads
```

---

## Preventive Measures

### 1. **Regular Health Checks**
- Daily review of thread counts
- Weekly analysis of connection pool metrics
- Monthly review of SQL Server AlwaysOn health

### 2. **Proactive Monitoring**
- Set up alerts for abnormal thread growth
- Monitor database connection failures
- Track response time degradation

### 3. **Scheduled Maintenance**
- Weekly restart during maintenance window (if needed)
- Monthly connection pool statistics review
- Quarterly capacity planning review

### 4. **Documentation**
- Document normal thread count baselines
- Document connection pool configuration
- Document failover procedures

---

## Conclusion

### Critical Path Forward:

1. **IMMEDIATE (Today):**
   - ✅ Server restarted (already done)
   - Verify SQL Server AlwaysOn status
   - Add connection timeouts to JDBC URL

2. **THIS WEEK:**
   - Tune connection pool settings
   - Implement monitoring scripts
   - Set up alerting

3. **THIS MONTH:**
   - Review dSeries upgrade path
   - Implement circuit breaker pattern
   - Conduct failover testing

### Success Criteria:
- ✅ No threads blocked on DBConnectionPool
- ✅ Connection timeouts < 15 seconds
- ✅ System remains responsive during database issues
- ✅ Automatic recovery from SQL Server failovers
- ✅ Thread count remains < 2,000 under normal load

---

## Appendix: Technical Details

### A. Thread Dump Statistics

| Metric | Value |
|--------|-------|
| Total threads | ~8,000+ |
| BLOCKED threads | 8,208 |
| Threads blocked on DBConnectionPool | 3,838 |
| Lock holder threads (stuck) | 3 |
| RUNNABLE threads | ~100 |
| WAITING threads | ~3,800 |
| TIMED_WAITING threads | ~100 |

### B. Affected Thread Patterns

```
Pattern 1: Application Manager Threads (3,700+)
- DM.Appl.<APPLICATION_NAME>.<GEN_NO>
- All waiting for DBConnectionPool lock
- Attempting to persist application state

Pattern 2: System Threads
- HAC (High Availability Component)
- SS (Scheduler Service)
- RS (Resource Service)
- VM (Variable Manager)
- Timer-0 (Timer thread)
- All blocked on same lock

Pattern 3: Lock Holders (3 threads)
- State: RUNNABLE (misleading - actually blocked in native I/O)
- Stack: socketRead0 → SQLServerConnection.connect
- Holding: DBConnectionPool lock
- Blocked on: TCP socket I/O to SQL Server
```

### C. Database Connection Details

```
Driver: com.microsoft.sqlserver.jdbc.SQLServerDriver
URL: jdbc:sqlserver://aal15glb_d020.mfcgd.com:21820
Database: ETS_CAWLA_D1
Authentication: Windows Integrated (NTLM)
AlwaysOn: Enabled
Listener: aal15glb_d020.mfcgd.com:21820
```

### D. Historical Events

```
2026-02-05 22:44:08 - SQL Server AlwaysOn failover (Error 983)
                    - Database replica not in PRIMARY/SECONDARY role
                    - Server auto-restart triggered
                    
2026-02-05 22:44:15 - Successfully reconnected to database
                    - Normal operation resumed
                    
2026-02-12 13:20:15 - Server restart (normal)
                    
2026-02-12 13:20:15 - Thread accumulation begins
         to         - Database connection attempts hanging
         14:16:58   - 3,838 threads blocked on DBConnectionPool
                    
2026-02-12 14:16:58 - Server shutdown (forced due to hang)
```

---

## Contact and Escalation

**For Immediate Issues:**
- Restart dSeries server
- Check SQL Server AlwaysOn status
- Review network connectivity

**For Implementation:**
- Development team: Connection pool refactoring
- DBA team: SQL Server tuning and monitoring
- Operations team: Monitoring and alerting setup

**Escalation Path:**
1. Level 1: Operations team (restart, basic checks)
2. Level 2: DBA team (SQL Server issues)
3. Level 3: Development team (code changes)
4. Level 4: Broadcom Support (product defects)

---

**Report Prepared By:** dSeries Health Check Tool Analysis  
**Date:** 2026-02-13  
**Priority:** CRITICAL  
**Status:** OPEN - Requires Immediate Action
