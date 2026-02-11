# ESP dSeries Health Check Tool
## Installation & Deployment Guide

**Version:** 1.0.0  
**Date:** February 9, 2026

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation on Linux/Unix](#installation-on-linuxunix)
3. [Installation on Windows](#installation-on-windows)
4. [Configuration](#configuration)
5. [Testing](#testing)
6. [Scheduling](#scheduling)
7. [Troubleshooting](#troubleshooting)

---

## ✅ Prerequisites

### **System Requirements**

```
Operating System:
  - Linux: RHEL 7+, CentOS 7+, Ubuntu 18.04+, SLES 12+
  - Windows: Windows Server 2016+, Windows 10+
  
Python:
  - Version: Python 3.7 or higher
  - Check: python3 --version or python --version
  
Permissions:
  - Read access to dSeries installation directory
  - Read access to dSeries logs
  - Network access to dSeries server and database
  - Write access to report output directory
```

### **Network Requirements**

```
Firewall Rules:
  - dSeries Server Port (default: 7507)
  - Database Port (PostgreSQL: 5432, Oracle: 1521, SQL Server: 1433)
  - SMTP Port (if email alerts enabled): 25 or 587
  
DNS Resolution:
  - dSeries server hostname
  - Database server hostname
  - Agent hostnames
```

### **Database Access**

```
Required Permissions:
  - CONNECT to database
  - SELECT on system tables
  - SELECT on dSeries tables
  
Recommended:
  - Read-only database user for health checks
  - Separate from application user
```

---

## 🐧 Installation on Linux/Unix

### **Step 1: Install Python 3**

```bash
# RHEL/CentOS
sudo yum install python3 python3-pip

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install python3 python3-pip

# SLES
sudo zypper install python3 python3-pip

# Verify installation
python3 --version
pip3 --version
```

### **Step 2: Create Installation Directory**

```bash
# Create directory
sudo mkdir -p /opt/CA/WA_DE/healthcheck
cd /opt/CA/WA_DE/healthcheck

# Set ownership (replace 'waadmin' with your dSeries user)
sudo chown -R waadmin:waadmin /opt/CA/WA_DE/healthcheck
```

### **Step 3: Extract Health Check Package**

```bash
# Copy package to server
scp dseries_healthcheck_v1.0.zip waadmin@server:/tmp/

# Extract
cd /opt/CA/WA_DE/healthcheck
unzip /tmp/dseries_healthcheck_v1.0.zip

# Verify contents
ls -la
# Should see: dseries_healthcheck.py, requirements.txt, config/, etc.
```

### **Step 4: Install Python Dependencies**

```bash
# Install dependencies
pip3 install -r requirements.txt

# For PostgreSQL (most common)
pip3 install psycopg2-binary

# For Oracle (if needed)
# pip3 install cx_Oracle

# For SQL Server (if needed)
# pip3 install pyodbc

# Verify installation
pip3 list | grep psutil
pip3 list | grep psycopg2
```

### **Step 5: Set Permissions**

```bash
# Make scripts executable
chmod +x run_healthcheck.sh
chmod +x dseries_healthcheck.py

# Set appropriate permissions
chmod 600 config/healthcheck.conf
chmod 700 /opt/CA/WA_DE/healthcheck
```

### **Step 6: Create Report Directory**

```bash
# Create report directory
sudo mkdir -p /var/log/dseries/healthcheck
sudo chown waadmin:waadmin /var/log/dseries/healthcheck
sudo chmod 755 /var/log/dseries/healthcheck
```

### **Step 7: Configure Database Password**

```bash
# Create secure password file
echo "your_db_password" > /opt/CA/WA_DE/.dbpass
chmod 600 /opt/CA/WA_DE/.dbpass
chown waadmin:waadmin /opt/CA/WA_DE/.dbpass

# Or use environment variable
export DB_PASSWORD="your_db_password"
```

---

## 🪟 Installation on Windows

### **Step 1: Install Python 3**

```powershell
# Download Python from https://www.python.org/downloads/
# Or use Chocolatey
choco install python3

# Verify installation
python --version
pip --version
```

### **Step 2: Create Installation Directory**

```powershell
# Create directory
New-Item -Path "C:\CA\WA_DE\healthcheck" -ItemType Directory -Force

# Navigate to directory
cd C:\CA\WA_DE\healthcheck
```

### **Step 3: Extract Health Check Package**

```powershell
# Extract ZIP file
Expand-Archive -Path "C:\Downloads\dseries_healthcheck_v1.0.zip" -DestinationPath "C:\CA\WA_DE\healthcheck"

# Verify contents
Get-ChildItem
```

### **Step 4: Install Python Dependencies**

```powershell
# Install dependencies
pip install -r requirements.txt

# For PostgreSQL
pip install psycopg2-binary

# For SQL Server
pip install pyodbc

# Verify installation
pip list | findstr psutil
pip list | findstr psycopg2
```

### **Step 5: Create Report Directory**

```powershell
# Create report directory
New-Item -Path "C:\CA\WA_DE\logs\healthcheck" -ItemType Directory -Force
```

### **Step 6: Configure Database Password**

```powershell
# Create secure password file
"your_db_password" | Out-File -FilePath "C:\CA\WA_DE\.dbpass" -NoNewline

# Set file permissions (restrict access)
$acl = Get-Acl "C:\CA\WA_DE\.dbpass"
$acl.SetAccessRuleProtection($true, $false)
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("Administrators","FullControl","Allow")
$acl.AddAccessRule($rule)
Set-Acl "C:\CA\WA_DE\.dbpass" $acl
```

---

## ⚙️ Configuration

### **Step 1: Edit Configuration File**

**Linux/Unix:**

```bash
cd /opt/CA/WA_DE/healthcheck
vi config/healthcheck.json
```

**Windows:**

```powershell
cd C:\CA\WA_DE\healthcheck
notepad config\healthcheck.json
```

### **Step 2: Configure Settings**

```json
{
  "db_host": "localhost",
  "db_port": 5432,
  "db_name": "WADB",
  "db_user": "wauser",
  "db_type": "postgresql",
  
  "server_host": "localhost",
  "server_port": 7507,
  "server_admin_user": "admin",
  "install_dir": "/opt/CA/WA_DE",
  
  "cpu_warning": 70,
  "cpu_critical": 85,
  "memory_warning": 80,
  "memory_critical": 90,
  "disk_warning": 75,
  "disk_critical": 85,
  
  "jvm_heap_min_mb": 4096,
  "jvm_heap_recommended_mb": 4096,
  "jvm_heap_max_mb": 8192,
  
  "workload_size": "medium",
  "daily_jobs_count": 50000,
  
  "report_output_dir": "/var/log/dseries/healthcheck",
  "enable_email_alerts": false,
  "email_recipients": ["admin@company.com"]
}
```

### **Configuration Parameters Explained**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `db_host` | Database server hostname | `dbserver.company.com` |
| `db_port` | Database port | `5432` (PostgreSQL), `1521` (Oracle) |
| `db_name` | Database name | `WADB` |
| `db_user` | Database username | `wauser` |
| `db_type` | Database type | `postgresql`, `oracle`, `mssql` |
| `server_host` | dSeries server hostname | `dseries.company.com` |
| `server_port` | dSeries server port | `7507` |
| `workload_size` | Environment size | `small`, `medium`, `large` |
| `daily_jobs_count` | Average daily jobs | `50000` |

---

## 🧪 Testing

### **Step 1: Test Basic Execution**

**Linux/Unix:**

```bash
cd /opt/CA/WA_DE/healthcheck

# Test Python script
python3 dseries_healthcheck.py --help

# Test quick check
python3 dseries_healthcheck.py --quick

# Or use shell script
./run_healthcheck.sh --quick
```

**Windows:**

```powershell
cd C:\CA\WA_DE\healthcheck

# Test Python script
python dseries_healthcheck.py --help

# Test quick check
python dseries_healthcheck.py --quick

# Or use batch script
.\run_healthcheck.bat
```

### **Step 2: Verify Output**

```bash
# Check report directory
ls -la /var/log/dseries/healthcheck/

# View latest HTML report
firefox /var/log/dseries/healthcheck/healthcheck_*.html

# View JSON report
cat /var/log/dseries/healthcheck/healthcheck_*.json | jq .
```

### **Step 3: Test Database Connectivity**

```bash
# Test database connection separately
python3 -c "
import psycopg2
conn = psycopg2.connect(
    host='localhost',
    port=5432,
    database='WADB',
    user='wauser'
)
print('Database connection successful')
conn.close()
"
```

### **Step 4: Test Full Health Check**

```bash
# Run full health check
python3 dseries_healthcheck.py --full

# Check exit code
echo $?
# 0 = healthy (score ≥60), 1 = unhealthy (score <60)
```

---

## 📅 Scheduling

### **Linux/Unix - Cron**

```bash
# Edit crontab
crontab -e

# Add daily quick check at 6 AM
0 6 * * * /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --quick --output /var/log/dseries/healthcheck >> /var/log/dseries/healthcheck/cron.log 2>&1

# Add weekly full check on Sunday at 2 AM
0 2 * * 0 /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --full --output /var/log/dseries/healthcheck >> /var/log/dseries/healthcheck/cron.log 2>&1

# Add monthly comprehensive check on 1st at 1 AM
0 1 1 * * /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --full --trending --output /var/log/dseries/healthcheck >> /var/log/dseries/healthcheck/cron.log 2>&1
```

### **Windows - Task Scheduler**

**Create Daily Quick Check:**

```powershell
# Create scheduled task
$action = New-ScheduledTaskAction -Execute "python.exe" -Argument "C:\CA\WA_DE\healthcheck\dseries_healthcheck.py --quick --output C:\CA\WA_DE\logs\healthcheck"
$trigger = New-ScheduledTaskTrigger -Daily -At 6am
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Register-ScheduledTask -TaskName "dSeries Health Check - Daily" -Action $action -Trigger $trigger -Principal $principal -Settings $settings
```

**Create Weekly Full Check:**

```powershell
$action = New-ScheduledTaskAction -Execute "python.exe" -Argument "C:\CA\WA_DE\healthcheck\dseries_healthcheck.py --full --output C:\CA\WA_DE\logs\healthcheck"
$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At 2am
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Register-ScheduledTask -TaskName "dSeries Health Check - Weekly" -Action $action -Trigger $trigger -Principal $principal -Settings $settings
```

---

## 🔧 Troubleshooting

### **Issue: Python not found**

```bash
# Linux: Install Python 3
sudo yum install python3  # RHEL/CentOS
sudo apt-get install python3  # Ubuntu

# Windows: Add Python to PATH
# Control Panel → System → Advanced → Environment Variables
# Add: C:\Python3x to PATH
```

### **Issue: Module not found (psutil, psycopg2, etc.)**

```bash
# Reinstall dependencies
pip3 install -r requirements.txt --force-reinstall

# Check installation
pip3 list | grep psutil
```

### **Issue: Database connection failed**

```bash
# Test database connectivity
telnet dbserver 5432

# Test with psql (PostgreSQL)
psql -h dbserver -p 5432 -U wauser -d WADB

# Check credentials
cat /opt/CA/WA_DE/.dbpass

# Check firewall
sudo firewall-cmd --list-all
```

### **Issue: Permission denied**

```bash
# Fix ownership
sudo chown -R waadmin:waadmin /opt/CA/WA_DE/healthcheck

# Fix permissions
chmod +x dseries_healthcheck.py
chmod 600 config/healthcheck.json
```

### **Issue: Report directory not writable**

```bash
# Create directory
sudo mkdir -p /var/log/dseries/healthcheck

# Set permissions
sudo chown waadmin:waadmin /var/log/dseries/healthcheck
sudo chmod 755 /var/log/dseries/healthcheck
```

### **Issue: SSL/TLS certificate error**

```bash
# For testing, disable SSL verification (NOT for production)
export PYTHONHTTPSVERIFY=0

# Or install certificates
sudo cp your-cert.crt /etc/pki/ca-trust/source/anchors/
sudo update-ca-trust
```

---

## 📦 Package Contents

```
dseries_healthcheck/
├── README.md                    # Main documentation
├── INSTALLATION_GUIDE.md        # This file
├── BEST_PRACTICES_GUIDE.md      # Best practices reference
├── requirements.txt             # Python dependencies
├── dseries_healthcheck.py       # Main Python script
├── run_healthcheck.sh           # Linux/Unix wrapper script
├── run_healthcheck.bat          # Windows wrapper script
├── config/
│   ├── healthcheck.json         # Configuration file
│   └── healthcheck.conf         # Shell script config
├── lib/                         # Library functions (for shell scripts)
│   ├── common_functions.sh
│   ├── system_checks.sh
│   ├── database_checks.sh
│   ├── server_checks.sh
│   ├── agent_checks.sh
│   ├── workload_checks.sh
│   ├── security_checks.sh
│   └── reporting.sh
└── docs/
    ├── API_REFERENCE.md
    └── CHANGELOG.md
```

---

## 🚀 Quick Start Summary

### **Linux/Unix**

```bash
# 1. Install Python 3
sudo yum install python3 python3-pip

# 2. Extract package
cd /opt/CA/WA_DE
unzip dseries_healthcheck_v1.0.zip

# 3. Install dependencies
cd healthcheck
pip3 install -r requirements.txt

# 4. Configure
vi config/healthcheck.json

# 5. Test
python3 dseries_healthcheck.py --quick

# 6. Schedule
crontab -e
# Add: 0 6 * * * /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --quick
```

### **Windows**

```powershell
# 1. Install Python 3
choco install python3

# 2. Extract package
Expand-Archive dseries_healthcheck_v1.0.zip -DestinationPath C:\CA\WA_DE\healthcheck

# 3. Install dependencies
cd C:\CA\WA_DE\healthcheck
pip install -r requirements.txt

# 4. Configure
notepad config\healthcheck.json

# 5. Test
python dseries_healthcheck.py --quick

# 6. Schedule via Task Scheduler (see above)
```

---

## 📞 Support

### **For Installation Issues**
- Email: dseries-support@company.com
- Portal: https://support.company.com/dseries

### **For Product Issues**
- Broadcom Support Portal
- Community Forums

---

## 📝 Next Steps

After installation:

1. ✅ Run initial health check
2. ✅ Review and address any critical issues
3. ✅ Schedule automated checks
4. ✅ Configure email alerts
5. ✅ Document baseline metrics
6. ✅ Train operations team
7. ✅ Establish review process

---

**Installation Complete!** 🎉

See `README.md` for usage instructions and `BEST_PRACTICES_GUIDE.md` for optimization tips.

---

**Version:** 1.0.0  
**Last Updated:** February 9, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
