# dSeries Production Issues - Quick Fix Guide
**For Developers and Operations Teams**

---

## 🔴 CRITICAL ISSUE #1: Database Connection Pool Deadlock

### Symptoms
- Server unresponsive
- 3,838+ threads blocked
- High CPU usage
- Cannot execute jobs

### Quick Fix (5 minutes)
```bash
# 1. Restart server
/opt/cawade/wlade/bin/stopServer
sleep 10
/opt/cawade/wlade/bin/startServer

# 2. Add connection timeout to db.properties
vi /opt/cawade/wlade/conf/db.properties

# Add these to jdbc.URL:
;loginTimeout=15;socketTimeout=30000;connectRetryCount=0
```

### Example db.properties Fix
```properties
# BEFORE:
jdbc.URL=jdbc:sqlserver://server:21820;databaseName=ETS_CAWLA_D1;integratedSecurity=true

# AFTER:
jdbc.URL=jdbc:sqlserver://server:21820;databaseName=ETS_CAWLA_D1;integratedSecurity=true;loginTimeout=15;socketTimeout=30000;connectRetryCount=0
```

### Verify Fix
```bash
# Monitor thread count
watch -n 5 'ps -eLf | grep espserver | wc -l'

# Should stay < 2000
```

---

## 🔴 CRITICAL ISSUE #2: Agent Encryption Key Missing

### Symptoms
```
ERROR: Key not available for node CPCWVIGPUTLU01
CryptoException in WAKeyObtainerImpl
```

### Quick Fix (10 minutes)
```bash
# 1. On agent machine (CPCWVIGPUTLU01)
cd /opt/CA/WA_AGENT/bin
./cybAgent stop

# 2. Remove old keys
rm -f ../config/agent.key
rm -f ../config/agent.cert

# 3. Re-register
./cybAgent -r <server_hostname> -p 7599

# 4. Start agent
./cybAgent start
```

### Verify Fix
```bash
# On server - should NOT see errors
tail -f /opt/cawade/wlade/logs/errors.txt | grep CPCWVIGPUTLU01

# Should be silent (no errors)
```

---

## 🟡 WARNING ISSUE #3: TDR Update Failures

### Symptoms
```
ERROR: Unable to Update the query UPDATE ESP_TDR_DATA SET FIRING='Y' WHERE FIRING='P'
DatabaseUpdateException: table key is null
```

### Quick Fix (5 minutes)
```sql
-- Connect to database
psql -d dseries_db  # or sqlcmd for SQL Server

-- Clean up bad TDR records
DELETE FROM ESP_TDR_DATA 
WHERE TDR_ID IS NULL 
   OR EVENT_NAME IS NULL;

-- Reset stuck TDRs
UPDATE ESP_TDR_DATA 
SET FIRING = 'N' 
WHERE FIRING = 'P' 
  AND LAST_UPDATE_TIME < CURRENT_TIMESTAMP - INTERVAL '1 hour';
```

### Verify Fix
```sql
-- Check TDR health
SELECT FIRING, COUNT(*) 
FROM ESP_TDR_DATA 
GROUP BY FIRING;

-- Should see:
-- FIRING | COUNT
-- N      | 1234
-- Y      | 56
-- P      | 0 (or very few)
```

---

## 🟡 WARNING ISSUE #4: Unknown RMI Message

### Symptoms
```
ERROR: Message type Server retry is unknown
```

### Quick Fix (2 minutes)
```properties
# Edit conf/rmi.properties
vi /opt/cawade/wlade/conf/rmi.properties

# Add or change:
rmi.message.validation=lenient
rmi.unknown.message.action=log
```

### Verify Fix
```bash
# Restart server
/opt/cawade/wlade/bin/stopServer
/opt/cawade/wlade/bin/startServer

# Monitor - should see warnings but not errors
tail -f /opt/cawade/wlade/logs/errors.txt | grep "Message type"
```

---

## Emergency Runbook

### When Server Hangs

```bash
# 1. Collect thread dump FIRST (before restart)
PID=$(cat /opt/cawade/wlade/serverPID)
jstack $PID > /tmp/threaddump_$(date +%Y%m%d_%H%M%S).txt

# 2. Check thread count
ps -eLf -p $PID | wc -l
# If > 5000, it's deadlocked

# 3. Restart server
/opt/cawade/wlade/bin/stopServer
sleep 10
/opt/cawade/wlade/bin/startServer

# 4. Monitor startup
tail -f /opt/cawade/wlade/logs/stdout.txt

# 5. Verify components started
# Should see:
# - RMI components are ready
# - Variable Manager Component is ready
# - Resource Manager Component is ready
# - Scheduler Component is ready
# - Application Manager Component is ready
# - Workstation Component is ready
# - Rest Component is ready
```

### Health Check Commands

```bash
# 1. Check server is running
ps -ef | grep espserver

# 2. Check database connectivity
sqlcmd -S server:21820 -d ETS_CAWLA_D1 -Q "SELECT 1"

# 3. Check thread count
ps -eLf | grep espserver | wc -l
# Normal: 500-1500
# Warning: 1500-3000
# Critical: > 3000

# 4. Check database connections
netstat -an | grep 21820 | grep ESTABLISHED | wc -l
# Should be < 100

# 5. Check for errors
tail -100 /opt/cawade/wlade/logs/errors.txt
```

---

## Configuration Quick Reference

### db.properties (Critical Settings)
```properties
# Connection timeouts (CRITICAL)
jdbc.URL=...;loginTimeout=15;socketTimeout=30000;connectRetryCount=0

# Connection pool (tune based on load)
rdbms.connection.pool.min=10
rdbms.connection.pool.max=50
rdbms.connection.pool.timeout=30

# Connection validation
rdbms.connection.validation.enabled=true
rdbms.connection.validation.query=SELECT 1
```

### JVM Settings (conf/windows.service.properties or bin/startServer)
```properties
# Heap size
-Xms4g
-Xmx8g

# GC settings
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Thread stack size
-Xss512k

# Enable JMX monitoring
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

---

## Automated Monitoring Script

```bash
#!/bin/bash
# File: /opt/cawade/wlade/bin/health_monitor.sh

PID=$(cat /opt/cawade/wlade/serverPID 2>/dev/null)

if [ -z "$PID" ]; then
    echo "$(date): ERROR - Server not running"
    exit 1
fi

# Check thread count
THREADS=$(ps -eLf -p $PID | wc -l)
echo "$(date): Threads=$THREADS"

if [ $THREADS -gt 5000 ]; then
    echo "$(date): CRITICAL - Thread count exceeded 5000!"
    jstack $PID > /opt/cawade/wlade/logs/threaddump_critical_$(date +%Y%m%d_%H%M%S).txt
    # Send alert
    mail -s "dSeries CRITICAL: Thread Deadlock" ops@company.com < /dev/null
fi

# Check database connectivity
timeout 5 sqlcmd -S server:21820 -d ETS_CAWLA_D1 -Q "SELECT 1" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "$(date): ERROR - Database not accessible"
    # Send alert
fi

# Check for recent errors
ERROR_COUNT=$(tail -100 /opt/cawade/wlade/logs/errors.txt | grep -c ERROR)
if [ $ERROR_COUNT -gt 10 ]; then
    echo "$(date): WARNING - High error rate: $ERROR_COUNT errors in last 100 lines"
fi
```

**Schedule:**
```cron
*/5 * * * * /opt/cawade/wlade/bin/health_monitor.sh >> /opt/cawade/wlade/logs/health_monitor.log 2>&1
```

---

## Testing Checklist

### After Applying Fixes

- [ ] Server starts successfully
- [ ] All components ready (RMI, VM, RM, SS, AM, WSS, Rest)
- [ ] Thread count < 2,000
- [ ] No BLOCKED threads on DBConnectionPool
- [ ] Agent CPCWVIGPUTLU01 communicating (no crypto errors)
- [ ] No TDR update failures
- [ ] Jobs executing normally
- [ ] Database queries responding < 5 seconds
- [ ] No RMI unknown message errors

### Performance Baseline

| Metric | Normal | Warning | Critical |
|--------|--------|---------|----------|
| Thread Count | 500-1500 | 1500-3000 | > 3000 |
| BLOCKED Threads | < 10 | 10-100 | > 100 |
| DB Connections | 10-50 | 50-80 | > 80 |
| Response Time | < 1s | 1-5s | > 5s |
| Error Rate | < 5/min | 5-20/min | > 20/min |

---

## Quick Commands Reference

```bash
# Restart server
/opt/cawade/wlade/bin/stopServer && sleep 10 && /opt/cawade/wlade/bin/startServer

# Check thread count
ps -eLf | grep espserver | wc -l

# Collect thread dump
jstack $(cat /opt/cawade/wlade/serverPID) > threaddump.txt

# Check database
sqlcmd -S server:21820 -d ETS_CAWLA_D1 -Q "SELECT 1"

# Monitor errors
tail -f /opt/cawade/wlade/logs/errors.txt

# Check agent status
grep "CPCWVIGPUTLU01" /opt/cawade/wlade/logs/stdout.txt | tail -20

# Clean TDR records
psql -d dseries_db -c "DELETE FROM ESP_TDR_DATA WHERE TDR_ID IS NULL"
```

---

**Last Updated:** 2026-02-17  
**Version:** 1.0  
**Status:** READY FOR IMPLEMENTATION
