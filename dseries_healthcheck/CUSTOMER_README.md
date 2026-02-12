# dSeries Health Check Tool - Customer Guide

**Version:** 2.1.0  
**Date:** February 12, 2026  
**Status:** Production Ready

---

## 📋 Quick Start

### Windows

```cmd
dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4
```

### Linux/Unix/AIX

```bash
./dseries_healthcheck.sh /opt/CA/WA_DE
```

**That's it!** The tool handles everything automatically.

---

## 🎯 What This Tool Does

The dSeries Health Check Tool performs comprehensive validation of your dSeries installation:

✅ **System Resources** - CPU, Memory, Disk Space  
✅ **Server Configuration** - JVM Heap, Ports, Logging  
✅ **Database Health** - Table sizes, Data integrity  
✅ **Agent Connectivity** - Connected agents, Versions  
✅ **Client Connections** - Active clients, Failures  
✅ **High Availability** - HA mode, Node status  
✅ **Performance Metrics** - Active jobs, Failed jobs  
✅ **Housekeeping** - Maintenance validation  
✅ **Version Information** - Installed components  

---

## 🔧 Automatic Features

The tool handles everything automatically - no configuration needed:

- ✅ **Finds Java** - Uses dSeries bundled Java or system Java
- ✅ **Builds Classpath** - Includes all dSeries libraries (lib, third-party, ext)
- ✅ **Loads JDBC Drivers** - Automatically detects PostgreSQL, Oracle, SQL Server drivers
- ✅ **Reads Configuration** - Uses existing dSeries config files (db.properties, server.properties)
- ✅ **Decrypts Passwords** - Handles encrypted database passwords (ENC format)
- ✅ **Generates Report** - Creates detailed health check report with recommendations
- ✅ **Masks Sensitive Data** - Protects credentials and hostnames in output

---

## 📦 What's Included

```
dseries_healthcheck/
├── dseries-healthcheck.jar          # Main application (ready to use)
├── dseries_healthcheck.bat          # Windows launcher
├── dseries_healthcheck.sh           # Unix/Linux/AIX launcher
├── config/
│   └── health_check_queries.sql    # SQL queries (customizable)
└── docs/
    ├── CUSTOMER_README.md           # This file
    ├── ENHANCED_README.md           # Detailed documentation
    └── DATABASE_CONFIGURATION_GUIDE.md
```

---

## 🚀 Installation

### Step 1: Extract Files

```bash
# Extract to any location
unzip dseries_healthcheck.zip
cd dseries_healthcheck
```

### Step 2: Set Permissions (Unix/Linux/AIX only)

```bash
chmod +x dseries_healthcheck.sh
```

### Step 3: Run

```bash
# Windows
dseries_healthcheck.bat <your_dseries_install_directory>

# Unix/Linux/AIX
./dseries_healthcheck.sh <your_dseries_install_directory>
```

---

## 💻 Usage Examples

### Example 1: Windows Installation

```cmd
REM Standard installation
dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4

REM Custom installation path
dseries_healthcheck.bat D:\dSeries\Production
```

### Example 2: Linux Installation

```bash
# Standard installation
./dseries_healthcheck.sh /opt/CA/WA_DE

# Custom installation path
./dseries_healthcheck.sh /usr/local/dseries
```

### Example 3: AIX Installation

```bash
# Standard installation
./dseries_healthcheck.sh /opt/CA/WA_DE

# With custom path
./dseries_healthcheck.sh /dseries/prod
```

---

## 📊 Sample Output

```
========================================================================
  dSeries Health Check Tool v2.1.0
========================================================================

[1/6] Validating dSeries installation directory...
  OK: C:\CA\ESPdSeriesWAServer_R12_4

[2/6] Checking Java installation...
  Using dSeries bundled Java: C:\CA\ESPdSeriesWAServer_R12_4\jre
  OK: Java is available

[3/6] Building classpath...
  Adding dSeries libraries from: C:\CA\ESPdSeriesWAServer_R12_4\lib
  OK: Classpath built

[4/6] Checking health check JAR...
  OK: dseries-healthcheck.jar

[5/6] Checking SQL queries configuration...
  OK: config\health_check_queries.sql

[6/6] Running health check...

========================================================================

[SYS-001] Checking CPU Utilization...
  ✅ CPU: 45.2% (Normal) - 8 cores available

[SYS-002] Checking Memory Usage...
  ✅ Memory: 65.3% used (Normal) - 10.4 GB / 16.0 GB

[SRV-001] Checking JVM Heap Size...
  ✅ JVM Heap: 4096 MB (Meets minimum 4096 MB)

[DB-001] ESP_APPLICATION Table Size
  ✅ Table size: 850000 (OK)

========================================================================
  HEALTH CHECK SUMMARY
========================================================================

  Total Checks:    25
  Passed:          22 ✅
  Warnings:        2 ⚠️
  Failed:          1 ❌

  Health Score:    92/100
  Status:          ✅ EXCELLENT

📄 Report generated: health_check_report_20260212_145530.txt

========================================================================
  Health Check Complete
========================================================================
  Exit Code: 0

  Status: SUCCESS - System is healthy
========================================================================
```

---

## 🔧 What the Tool Does Automatically

### 1. Finds Java

The tool automatically locates Java in this order:

1. **dSeries bundled Java** - `<install_dir>/jre/bin/java`
2. **System Java** - From PATH or JAVA_HOME
3. **Fallback** - Clear error message if not found

**You don't need to configure Java!**

### 2. Builds Classpath

The tool automatically includes:

- dSeries libraries (`<install_dir>/lib/*.jar`)
- Third-party libraries (`<install_dir>/third-party/*.jar`)
- Extension libraries (`<install_dir>/ext/*.jar`)
- JDBC drivers (PostgreSQL, Oracle, SQL Server)

**You don't need to configure classpath!**

### 3. Reads Database Configuration

The tool automatically reads:

- Database connection from `<install_dir>/conf/db.properties`
- Supports encrypted passwords
- No separate configuration needed

**You don't need to configure database!**

### 4. Detects Operating System

The tool automatically adapts to:

- Windows (all versions)
- Linux (all distributions)
- AIX
- Solaris
- HP-UX

**You don't need to specify OS!**

---

## 📈 Exit Codes

| Exit Code | Status | Description |
|-----------|--------|-------------|
| 0 | SUCCESS | Health score ≥ 60 (System is healthy) |
| 1 | FAILURE | Health score < 60 (Critical issues found) |
| 2 | ERROR | Invalid input or execution error |

**Use exit codes for automation:**

```bash
# Unix/Linux/AIX
./dseries_healthcheck.sh /opt/CA/WA_DE
if [ $? -eq 0 ]; then
    echo "Health check passed"
else
    echo "Health check failed"
fi

# Windows
dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4
if %errorlevel% equ 0 (
    echo Health check passed
) else (
    echo Health check failed
)
```

---

## 🔍 Troubleshooting

### Issue 1: Java Not Found

**Error:**
```
ERROR: Java not found
```

**Solution:**

1. **Check dSeries bundled Java:**
   ```bash
   # Should exist
   <install_dir>/jre/bin/java
   ```

2. **Install Java if needed:**
   ```bash
   # Linux
   sudo apt-get install openjdk-8-jre  # Debian/Ubuntu
   sudo yum install java-1.8.0-openjdk # RHEL/CentOS
   
   # Windows
   Download from: https://www.java.com/download/
   ```

3. **Set JAVA_HOME:**
   ```bash
   # Unix/Linux
   export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
   export PATH=$JAVA_HOME/bin:$PATH
   
   # Windows
   set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

---

### Issue 2: Permission Denied (Unix/Linux/AIX)

**Error:**
```
Permission denied: ./dseries_healthcheck.sh
```

**Solution:**
```bash
chmod +x dseries_healthcheck.sh
```

---

### Issue 3: JAR Not Found

**Error:**
```
ERROR: dseries-healthcheck.jar not found
```

**Solution:**

1. **Verify files extracted correctly:**
   ```bash
   ls -l dseries-healthcheck.jar
   ```

2. **Run from correct directory:**
   ```bash
   cd /path/to/dseries_healthcheck
   ./dseries_healthcheck.sh /opt/CA/WA_DE
   ```

---

### Issue 4: Invalid dSeries Directory

**Error:**
```
ERROR: conf directory not found
This does not appear to be a valid dSeries installation
```

**Solution:**

1. **Verify dSeries installation path:**
   ```bash
   # Should contain conf, bin, logs directories
   ls <install_dir>/conf
   ```

2. **Use correct path:**
   ```bash
   # Correct
   ./dseries_healthcheck.sh /opt/CA/WA_DE
   
   # Wrong (don't include /bin or /conf)
   ./dseries_healthcheck.sh /opt/CA/WA_DE/bin
   ```

---

### Issue 5: JDBC Driver Not Found

**Error:**
```
⚠️  JDBC driver not found in classpath: org.postgresql.Driver
```

**Solution:**

The tool automatically loads JDBC drivers from the dSeries `lib` directory. If you see this error:

1. **Use the launcher scripts (recommended):**
   ```bash
   # These scripts automatically include all JARs from lib directory
   dseries_healthcheck.bat <install_dir>    # Windows
   ./dseries_healthcheck.sh <install_dir>   # Unix/Linux/AIX
   ```

2. **Check if JDBC driver exists:**
   ```bash
   # PostgreSQL
   ls <install_dir>/lib/postgresql*.jar
   ls <install_dir>/webserver/lib/postgresql*.jar
   
   # Oracle
   ls <install_dir>/lib/ojdbc*.jar
   
   # SQL Server
   ls <install_dir>/lib/mssql*.jar
   ```

3. **Download JDBC driver if missing:**
   
   **PostgreSQL:**
   - Download: https://jdbc.postgresql.org/download/
   - Place in: `<install_dir>/lib/`
   - File: `postgresql-42.5.0.jar` or later
   
   **Oracle:**
   - Download: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
   - Place in: `<install_dir>/lib/`
   - File: `ojdbc8.jar` or later
   
   **SQL Server:**
   - Download: https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
   - Place in: `<install_dir>/lib/`
   - File: `mssql-jdbc-12.2.0.jre8.jar` or later

4. **For detailed JDBC configuration, see:**
   - `docs/JDBC_DRIVER_GUIDE.md` (included in package)

---

### Issue 6: Database Connection Failed

**Error:**
```
⚠️  Could not connect to database
```

**This is OK!** The tool will:
- ✅ Still run all system and server checks
- ⚠️ Skip database-specific checks
- ℹ️ Provide informational message

**To enable database checks:**

1. **Verify database is running:**
   ```bash
   # PostgreSQL
   ps aux | grep postgres
   
   # Check port
   netstat -an | grep 5432
   ```

2. **Check db.properties:**
   ```bash
   cat <install_dir>/conf/db.properties
   ```

---

## 📚 Advanced Usage

### Custom SQL Queries

You can customize the SQL queries:

```bash
# Edit queries
vi config/health_check_queries.sql

# Run with custom queries
./dseries_healthcheck.sh /opt/CA/WA_DE
```

### Scheduled Execution

**Unix/Linux (cron):**
```bash
# Run daily at 2 AM
0 2 * * * /path/to/dseries_healthcheck/dseries_healthcheck.sh /opt/CA/WA_DE >> /var/log/dseries_health.log 2>&1
```

**Windows (Task Scheduler):**
```cmd
# Create scheduled task
schtasks /create /tn "dSeries Health Check" /tr "C:\dseries_healthcheck\dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4" /sc daily /st 02:00
```

---

## 🔒 Security

### What Data is Collected?

The tool collects:
- ✅ System metrics (CPU, memory, disk)
- ✅ Configuration settings (JVM heap, ports)
- ✅ Database table sizes
- ✅ Job statistics

The tool does NOT collect:
- ❌ Passwords (automatically masked)
- ❌ Job definitions or content
- ❌ User data
- ❌ Application data

### Credentials

- Database credentials are read from dSeries configuration
- Passwords are automatically decrypted if encrypted
- All credentials are masked in output
- No credentials are stored or transmitted

---

## 📞 Support

### Documentation

- **CUSTOMER_README.md** - This file (quick start)
- **ENHANCED_README.md** - Detailed documentation
- **DATABASE_CONFIGURATION_GUIDE.md** - Database configuration
- **ENHANCED_HEALTH_CHECK_GUIDE.md** - Complete technical guide

### Broadcom Support

- **Portal:** https://support.broadcom.com
- **Community:** https://community.broadcom.com
- **Documentation:** https://techdocs.broadcom.com

---

## ✅ System Requirements

### Minimum Requirements

| Component | Requirement |
|-----------|-------------|
| **Java** | JRE 8 or higher (included with dSeries) |
| **OS** | Windows, Linux, Unix, AIX, Solaris, HP-UX |
| **Memory** | 512 MB available |
| **Disk** | 50 MB for tool + space for reports |
| **Permissions** | Read access to dSeries installation |

### Supported Platforms

✅ **Windows**
- Windows Server 2012 R2+
- Windows Server 2016, 2019, 2022
- Windows 10, 11

✅ **Linux**
- RHEL 6, 7, 8, 9
- CentOS 6, 7, 8
- Ubuntu 16.04, 18.04, 20.04, 22.04
- SUSE Linux Enterprise 12, 15

✅ **Unix**
- AIX 7.1, 7.2, 7.3
- Solaris 10, 11
- HP-UX 11i v3

---

## 🎯 Best Practices

### 1. Run Regularly

```bash
# Weekly health checks
0 2 * * 0 /path/to/dseries_healthcheck.sh /opt/CA/WA_DE
```

### 2. Review Reports

```bash
# Check generated reports
ls -lt health_check_report_*.txt | head -5
```

### 3. Track Trends

```bash
# Compare health scores over time
grep "Health Score" health_check_report_*.txt
```

### 4. Address Issues

```bash
# Focus on FAILED checks first
# Then WARNING checks
# Review remediation guidance in report
```

---

## 📝 Frequently Asked Questions

### Q: Do I need to install anything?

**A:** No! Just extract the files and run. Java is included with dSeries.

### Q: Will this modify my dSeries installation?

**A:** No! The tool only reads data. It makes no changes.

### Q: Can I run this on a production system?

**A:** Yes! The tool is read-only and safe for production.

### Q: How long does it take?

**A:** 2-5 minutes depending on database size.

### Q: What if database checks fail?

**A:** System and server checks still run. Database checks are optional.

### Q: Can I customize the checks?

**A:** Yes! Edit `config/health_check_queries.sql`

### Q: Does it work with Oracle/SQL Server?

**A:** Yes! Automatically detects database type from dSeries configuration.

---

## 🎉 Summary

### Simple to Use

```bash
# Just one command!
./dseries_healthcheck.sh /opt/CA/WA_DE
```

### Automatic Configuration

✅ Finds Java automatically  
✅ Builds classpath automatically  
✅ Reads database config automatically  
✅ Detects OS automatically  
✅ Handles encryption automatically  

### Comprehensive Checks

✅ 50+ health checks  
✅ System, server, database, agents, clients  
✅ Performance metrics  
✅ Detailed reports  
✅ Actionable remediation  

### Production Ready

✅ No installation required  
✅ No configuration required  
✅ Safe for production  
✅ Works on all platforms  
✅ Fully automated  

---

**Just provide your dSeries installation directory and let the tool do the rest!**

---

**Version:** 2.1.0  
**Last Updated:** February 12, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
