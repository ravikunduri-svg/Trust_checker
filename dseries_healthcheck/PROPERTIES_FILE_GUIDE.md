# dSeries Health Check - Properties File Guide

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Document Type:** Configuration Reference

---

## 📋 Overview

This guide explains how the dSeries Health Check Tool reads and uses configuration files from the dSeries installation. Understanding these files is crucial for interpreting health check results.

---

## 📁 Configuration Files Used

### File Locations

```
C:\CA\ESPdSeriesWAServer_R12_4\
├── conf\
│   ├── db.properties                    ← Database configuration
│   ├── windows.service.properties       ← JVM configuration (Windows)
│   ├── server.properties                ← Server settings
│   └── [other config files]
└── config\
    └── healthcheck.properties           ← Optional: Custom thresholds
```

---

## 1️⃣ db.properties (Database Configuration)

### Purpose
Contains database connection parameters used by dSeries to connect to its backend database.

### Location
```
{installDir}/conf/db.properties
```

### Example Content
```properties
# Database Connection Settings
jdbc.URL=jdbc:postgresql://G36R0T3:5432/dseries
rdbms.userid=postgres
rdbms.password=<encrypted_password>
rdbms.driver=org.postgresql.Driver
rdbms.schema=public

# Connection Pool Settings
jdbc.pool.minSize=5
jdbc.pool.maxSize=20
jdbc.pool.timeout=30000
```

### How Health Check Uses This File

#### Reading Process
```java
File dbPropsFile = new File(installDir + "/conf/db.properties");
if (dbPropsFile.exists()) {
    BufferedReader reader = new BufferedReader(new FileReader(dbPropsFile));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("jdbc.URL=")) {
            String url = line.substring("jdbc.URL=".length());
            // Parse URL to extract components
        } else if (line.startsWith("rdbms.userid=")) {
            dbUser = line.substring("rdbms.userid=".length());
        }
    }
    reader.close();
}
```

#### Data Extracted

| Property | Example Value | Used For |
|----------|---------------|----------|
| `jdbc.URL` | `jdbc:postgresql://G36R0T3:5432/dseries` | Extract host, port, database name |
| `rdbms.userid` | `postgres` | Display database username |
| `rdbms.password` | `<encrypted>` | NOT extracted (security) |

#### URL Parsing Example
```
Input:  jdbc:postgresql://G36R0T3:5432/dseries
        ├────┬────────┘  ├──┬───┘├─┬─┘└──┬───┘
        │    │           │  │    │ │    │
        │    │           │  │    │ │    └─ Database name: dseries
        │    │           │  │    │ └────── Port: 5432
        │    │           │  │    └──────── Host: G36R0T3
        │    │           │  └───────────── Protocol: postgresql
        │    └───────────┴──────────────── JDBC prefix
        └────────────────────────────────── jdbc:

Output:
  dbHost = "G36R0T3"
  dbPort = 5432
  dbName = "dseries"
  dbUser = "postgres"
```

#### Health Check Output
```
[DB-001] Checking Database Connectivity...
  Database: G36R0T3:5432/dseries
  User: postgres
  [SKIP] PostgreSQL JDBC driver not found in classpath
         To test database connectivity, add postgresql-*.jar to classpath
```

### Important Notes
- ⚠️ **Password is NOT read** - It's encrypted and not needed for health check
- ℹ️ **Connection test is skipped** - JDBC driver not included in health check tool
- ✅ **Configuration validation** - Confirms file exists and is readable

---

## 2️⃣ windows.service.properties (JVM Configuration)

### Purpose
Contains JVM settings for the dSeries server when running as a Windows service. This is the **MOST CRITICAL** file for health checks.

### Location
```
{installDir}/conf/windows.service.properties
```

### Example Content
```properties
# Windows Service Configuration
service.name=ESP dSeries Workload Automation
service.displayname=ESP dSeries Workload Automation
service.description=ESP dSeries Workload Automation Server

# JVM Properties
jvmproperty_1=-Djava.awt.headless=true
jvmproperty_2=-Xms1024M
jvmproperty_3=-Xmx1024M
jvmproperty_4=-XX:+UseG1GC
jvmproperty_5=-XX:MaxGCPauseMillis=200
jvmproperty_6=-XX:+HeapDumpOnOutOfMemoryError
jvmproperty_7=-XX:HeapDumpPath=C:/CA/ESPdSeriesWAServer_R12_4/logs/
jvmproperty_8=-Dfile.encoding=UTF-8
jvmproperty_9=-Duser.timezone=UTC

# Classpath
classpath=C:/CA/ESPdSeriesWAServer_R12_4/lib/*
```

### How Health Check Uses This File

#### Reading Process
```java
File propsFile = new File(installDir + "/conf/windows.service.properties");
int heapSizeMB = 0;

if (propsFile.exists()) {
    BufferedReader reader = new BufferedReader(new FileReader(propsFile));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.contains("-Xmx")) {
            // Extract heap size from -Xmx parameter
            String[] parts = line.split("=");
            if (parts.length > 1) {
                String xmx = parts[1].trim();
                if (xmx.contains("-Xmx")) {
                    xmx = xmx.substring(xmx.indexOf("-Xmx") + 4);
                    
                    // Parse size with unit
                    if (xmx.toLowerCase().endsWith("g")) {
                        // Gigabytes: convert to MB
                        heapSizeMB = Integer.parseInt(xmx.substring(0, xmx.length() - 1)) * 1024;
                    } else if (xmx.toLowerCase().endsWith("m")) {
                        // Megabytes: use as-is
                        heapSizeMB = Integer.parseInt(xmx.substring(0, xmx.length() - 1));
                    }
                }
            }
        }
    }
    reader.close();
}
```

#### JVM Parameter Parsing Examples

| Line in File | Extracted Value | Parsed Result |
|--------------|-----------------|---------------|
| `jvmproperty_3=-Xmx1024M` | `1024M` | `heapSizeMB = 1024` |
| `jvmproperty_3=-Xmx4096M` | `4096M` | `heapSizeMB = 4096` |
| `jvmproperty_3=-Xmx4g` | `4g` | `heapSizeMB = 4096` (4 × 1024) |
| `jvmproperty_3=-Xmx8G` | `8G` | `heapSizeMB = 8192` (8 × 1024) |

#### Parsing Flow Diagram
```
Line: "jvmproperty_3=-Xmx1024M"
       │
       ▼
Split by '=' 
       │
       ├─ [0]: "jvmproperty_3"
       └─ [1]: "-Xmx1024M"
              │
              ▼
       Find "-Xmx"
              │
              ▼
       Extract after "-Xmx"
              │
              ▼
       Result: "1024M"
              │
              ▼
       Check last character
              │
       ┌──────┴──────┐
       │             │
       ▼ 'M' or 'm' ▼ 'G' or 'g'
    Parse as MB   Parse as GB
       │             │
       │             ▼
       │        Multiply by 1024
       │             │
       └─────────────┘
              │
              ▼
       heapSizeMB = 1024
```

#### Health Check Validation

**Threshold:** Minimum 4096 MB (4 GB) for production dSeries

```java
if (heapSizeMB < jvmHeapMinMB) {  // jvmHeapMinMB = 4096
    status = CheckStatus.FAIL;
    message = "JVM heap is " + heapSizeMB + " MB (minimum: " + jvmHeapMinMB + " MB)";
    recommendation = "CRITICAL - Increase JVM heap to at least 4096 MB for production!";
    failedChecks++;
}
```

#### Health Check Output (FAIL Example)
```
[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...
  Current JVM heap: 1024 MB
  [FAIL] CRITICAL: JVM heap is 1024 MB (minimum: 4096 MB)
         Recommendation: CRITICAL - Increase JVM heap to at least 4096 MB for production!
         Edit: C:\CA\ESPdSeriesWAServer_R12_4/conf/windows.service.properties
         Change: jvmproperty_3=-Xmx1024M
         To:     jvmproperty_3=-Xmx4096M
         Also set: jvmproperty_2=-Xms4096M (pre-allocated memory model)
```

#### Health Check Output (PASS Example)
```
[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...
  Current JVM heap: 4096 MB
  [PASS] JVM heap is 4096 MB (healthy)
```

### Important JVM Parameters

| Parameter | Purpose | Recommended Value |
|-----------|---------|-------------------|
| `-Xms` | Initial heap size | 4096M (match -Xmx) |
| `-Xmx` | Maximum heap size | 4096M (minimum for production) |
| `-XX:+UseG1GC` | Garbage collector | G1GC (recommended) |
| `-XX:MaxGCPauseMillis` | GC pause target | 200 (milliseconds) |
| `-XX:+HeapDumpOnOutOfMemoryError` | Dump on OOM | Enabled (for troubleshooting) |

### Why 4096 MB is Critical

**dSeries Best Practices:**
- Minimum: 4096 MB (4 GB)
- Recommended: 4096-8192 MB (4-8 GB)
- Large environments: 8192+ MB (8+ GB)

**Impact of Insufficient Heap:**
- ❌ Frequent garbage collection
- ❌ Out of Memory errors
- ❌ Poor performance
- ❌ Service crashes
- ❌ Job processing delays

---

## 3️⃣ healthcheck.properties (Optional - Future Enhancement)

### Purpose
Allows customization of health check thresholds without modifying code.

### Location
```
{installDir}/config/healthcheck.properties
```

### Example Content (Future Implementation)
```properties
# ============================================================================
# dSeries Health Check Configuration
# ============================================================================

# System Resource Thresholds
threshold.cpu.warning=70
threshold.cpu.critical=85
threshold.memory.warning=80
threshold.memory.critical=90
threshold.disk.warning=75
threshold.disk.critical=85

# JVM Settings (CRITICAL!)
threshold.jvm.heap.min.mb=4096
threshold.jvm.heap.recommended.mb=4096
threshold.jvm.heap.max.mb=8192

# Server Settings
server.host=localhost
server.port=7599
server.timeout.seconds=5

# Database Settings
db.host=localhost
db.port=5432
db.name=dseries
db.user=postgres
db.connection.timeout.seconds=5

# Workload Sizing (based on industry best practices)
workload.size=medium
workload.daily.jobs=50000

# Thread Pool Configuration (based on workload size)
# Small:  < 15,000 jobs/day  → download=3, db_update=2, selector=4
# Medium: 15,000-75,000       → download=6, db_update=4, selector=8
# Large:  > 75,000            → download=8, db_update=8, selector=12
thread.pool.download=6
thread.pool.db_update=4
thread.pool.selector=8

# Performance Thresholds
performance.max.queue.depth=1000
performance.max.job.duration.minutes=240
performance.max.failed.jobs.percent=5
performance.max.db.query.time.ms=500

# Reporting
report.output.dir=/var/log/dseries/healthcheck
report.retention.days=90
report.enable.email=false
report.email.recipients=admin@company.com

# Historical Tracking
trending.enable=false
trending.database.file=/var/log/dseries/healthcheck/trends.db
```

### How It Would Be Used (Future)
```java
// Load custom configuration
Properties config = new Properties();
File configFile = new File(installDir + "/config/healthcheck.properties");
if (configFile.exists()) {
    config.load(new FileInputStream(configFile));
    
    // Override defaults
    cpuWarning = Integer.parseInt(config.getProperty("threshold.cpu.warning", "70"));
    cpuCritical = Integer.parseInt(config.getProperty("threshold.cpu.critical", "85"));
    jvmHeapMinMB = Integer.parseInt(config.getProperty("threshold.jvm.heap.min.mb", "4096"));
}
```

### Current Status
⚠️ **Not yet implemented** - Currently uses hardcoded defaults in Java code

---

## 📊 Configuration Priority

When multiple configuration sources exist, priority order:

```
┌─────────────────────────────────────┐
│  1. Command-Line Arguments          │  ← Highest Priority
│     (Installation directory)        │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  2. dSeries Configuration Files     │
│     • db.properties                 │
│     • windows.service.properties    │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  3. Health Check Properties         │  ← Future Enhancement
│     (healthcheck.properties)        │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  4. Hardcoded Defaults              │  ← Lowest Priority
│     (In Java code)                  │
└─────────────────────────────────────┘
```

---

## 🔍 Troubleshooting Configuration Issues

### Issue 1: File Not Found

**Error:**
```
[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...
  [SKIP] Could not determine JVM heap size from configuration
```

**Cause:** `windows.service.properties` file not found

**Solution:**
1. Verify installation directory is correct
2. Check file exists: `{installDir}/conf/windows.service.properties`
3. Verify file permissions (readable)

---

### Issue 2: Cannot Parse JVM Heap Size

**Error:**
```
[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...
  [SKIP] Could not determine JVM heap size
```

**Cause:** `-Xmx` parameter not found or malformed

**Solution:**
1. Open `windows.service.properties`
2. Look for line containing `-Xmx`
3. Ensure format is: `jvmproperty_*=-Xmx####M` or `jvmproperty_*=-Xmx#g`
4. Examples:
   - ✅ `jvmproperty_3=-Xmx4096M`
   - ✅ `jvmproperty_3=-Xmx4g`
   - ❌ `jvmproperty_3=Xmx4096M` (missing dash)
   - ❌ `jvmproperty_3=-Xmx4096` (missing unit)

---

### Issue 3: Database Configuration Not Found

**Error:**
```
[DB-001] Checking Database Connectivity...
  [SKIP] Could not read database configuration
```

**Cause:** `db.properties` file not found or unreadable

**Solution:**
1. Verify file exists: `{installDir}/conf/db.properties`
2. Check file permissions
3. Ensure file contains `jdbc.URL=` line

---

## 📝 Example: Complete Configuration Read

### Input Files

**db.properties:**
```properties
jdbc.URL=jdbc:postgresql://G36R0T3:5432/dseries
rdbms.userid=postgres
```

**windows.service.properties:**
```properties
jvmproperty_2=-Xms1024M
jvmproperty_3=-Xmx1024M
```

### Health Check Execution

```
[DB-001] Checking Database Connectivity...
  Database: G36R0T3:5432/dseries
  User: postgres
  [SKIP] PostgreSQL JDBC driver not found in classpath

[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...
  Current JVM heap: 1024 MB
  [FAIL] CRITICAL: JVM heap is 1024 MB (minimum: 4096 MB)
         Recommendation: CRITICAL - Increase JVM heap to at least 4096 MB!
         Edit: C:\CA\ESPdSeriesWAServer_R12_4/conf/windows.service.properties
         Change: jvmproperty_3=-Xmx1024M
         To:     jvmproperty_3=-Xmx4096M
```

### Configuration Data Extracted

| Source | Property | Value | Used In Check |
|--------|----------|-------|---------------|
| `db.properties` | `jdbc.URL` | `jdbc:postgresql://G36R0T3:5432/dseries` | DB-001 |
| `db.properties` | `rdbms.userid` | `postgres` | DB-001 |
| `windows.service.properties` | `jvmproperty_3` | `-Xmx1024M` | SRV-001 |

---

## 🎯 Best Practices

### For dSeries Administrators

1. **Always set JVM heap to 4096 MB minimum**
   ```properties
   jvmproperty_2=-Xms4096M
   jvmproperty_3=-Xmx4096M
   ```

2. **Use matching -Xms and -Xmx values**
   - Prevents heap resizing overhead
   - Implements pre-allocated memory model

3. **Document any custom configurations**
   - Add comments to properties files
   - Track changes in version control

4. **Test after configuration changes**
   - Run health check after modifications
   - Verify service starts successfully

### For Health Check Users

1. **Understand what's being checked**
   - Review this guide
   - Know which files are read

2. **Don't modify health check tool**
   - Configuration is read-only
   - Tool doesn't change dSeries settings

3. **Use reports for remediation**
   - Follow recommendations provided
   - Refer to ACTION_PLAN documents

---

## 📚 Summary

### Files Read by Health Check

| File | Purpose | Critical? | Data Extracted |
|------|---------|-----------|----------------|
| `db.properties` | Database config | No | Host, port, database, username |
| `windows.service.properties` | JVM config | **YES** | Heap size (-Xmx) |
| `healthcheck.properties` | Custom thresholds | No | (Future enhancement) |

### Most Important Configuration

**JVM Heap Size** in `windows.service.properties`:
- ✅ **Minimum:** 4096 MB
- ✅ **Recommended:** 4096-8192 MB
- ❌ **Insufficient:** < 4096 MB (CRITICAL FAILURE)

### Key Takeaways

1. Health check **reads** configuration files (never modifies)
2. JVM heap size check is the **most critical** validation
3. Database connection test is **informational only** (JDBC driver not included)
4. All configuration parsing is **fault-tolerant** (skips on errors)
5. Detailed recommendations provided for **all failures**

---

**Document Version:** 1.0.0  
**Last Updated:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
