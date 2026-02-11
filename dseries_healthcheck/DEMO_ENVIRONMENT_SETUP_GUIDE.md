# dSeries Demo Environment Setup Guide

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Purpose:** Automated setup for Sales and Services teams

---

## 📋 Overview

This guide provides a complete solution for setting up dSeries demo environments quickly and consistently. Perfect for sales demonstrations, customer proof-of-concepts, training sessions, and development environments.

### Key Features

✅ **Automated Setup** - One command to set up complete environment  
✅ **Multiple Versions** - Support for 12.4 and 25.0  
✅ **Embedded PostgreSQL** - No external database required  
✅ **Pre-configured** - Optimal settings out of the box  
✅ **Sample Data** - Demo applications and jobs included  
✅ **Demo Users** - Pre-created accounts for demonstrations  
✅ **Health Validated** - Automatic health check after setup  
✅ **Quick Start** - Ready to demo in 15-20 minutes  

---

## 🎯 Use Cases

### For Sales Teams
- Quick demo environment setup for customer presentations
- Multiple versions for feature comparison
- Pre-loaded sample applications
- Professional demo scenarios

### For Services Teams
- Consistent environment for customer onboarding
- Training environment setup
- Proof-of-concept deployments
- Customer workshops

### For Development Teams
- Quick development environment setup
- Testing environment for new features
- Integration testing
- Performance testing baseline

---

## 🚀 Quick Start

### Prerequisites

**System Requirements:**
- **OS:** Windows Server 2016+ or Windows 10/11
- **CPU:** 4 cores minimum (8 cores recommended)
- **Memory:** 16 GB minimum (32 GB recommended)
- **Disk:** 100 GB free space
- **Java:** JDK/JRE 8 or higher
- **PowerShell:** 5.1 or higher
- **Permissions:** Administrator access

**Software Requirements:**
- dSeries installation package (12.4 or 25.0)
- PostgreSQL portable (or use embedded version)
- Network access (for downloads if needed)

---

### One-Command Setup

```powershell
# Setup dSeries 12.4 Demo Environment
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices

# Setup dSeries 25.0 Sales Environment
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Sales -ImportSamples -StartServices

# Setup Development Environment
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Development -InstallPath "D:\Dev\dSeries"
```

**Setup Time:** 15-20 minutes (automated)

---

## 📖 Detailed Setup Instructions

### Step 1: Prepare Installation Files

**Directory Structure:**
```
\\fileserver\software\dSeries\
├── 12.4\
│   ├── ESPdSeriesWAServer_R12_4.zip
│   └── PostgreSQL\
│       └── postgresql-12-portable.zip
├── 25.0\
│   ├── ESPdSeriesWAServer_R25_0.zip
│   └── PostgreSQL\
│       └── postgresql-14-portable.zip
└── samples\
    ├── applications\
    │   ├── SampleApp1.xml
    │   ├── SampleApp2.xml
    │   └── SampleApp3.xml
    └── jobs\
        ├── DailyBackup.xml
        ├── DataProcessing.xml
        └── ReportGeneration.xml
```

**Update Configuration:**

Edit `Setup-DSeriesDemoEnvironment.ps1` and update source paths:

```powershell
SourcePaths = @{
    "12.4" = "\\your-fileserver\software\dSeries\12.4\ESPdSeriesWAServer_R12_4.zip"
    "25.0" = "\\your-fileserver\software\dSeries\25.0\ESPdSeriesWAServer_R25_0.zip"
}

SampleAppsPath = "\\your-fileserver\dSeries\samples\applications"
SampleJobsPath = "\\your-fileserver\dSeries\samples\jobs"
```

---

### Step 2: Run Setup Script

**Basic Setup (Demo Environment):**
```powershell
cd C:\Codes\dseries_healthcheck
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo
```

**Full Setup (with samples and auto-start):**
```powershell
.\Setup-DSeriesDemoEnvironment.ps1 `
    -Version 12.4 `
    -Environment Demo `
    -ImportSamples `
    -StartServices
```

**Custom Installation Path:**
```powershell
.\Setup-DSeriesDemoEnvironment.ps1 `
    -Version 25.0 `
    -Environment Sales `
    -InstallPath "D:\Demo\dSeries_Sales" `
    -ServerPort 7600 `
    -ImportSamples `
    -StartServices
```

---

### Step 3: Verify Installation

The script automatically runs a health check. Review the results:

```powershell
# View health check results
notepad "$InstallPath\SETUP_SUMMARY.txt"

# Or run health check manually
cd C:\Codes\dseries_healthcheck
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"
```

**Expected Health Score:** 85-95 (GOOD to EXCELLENT)

---

## 🔧 What Gets Configured

### 1. PostgreSQL Database

**Configuration:**
- Database name: `dseries_demo` (or based on environment)
- Port: 5432 (customizable)
- User: `dseries_user`
- Password: `dSeries2026!`

**Optimized Settings:**
```properties
shared_buffers = 1GB
effective_cache_size = 4GB
work_mem = 50MB
maintenance_work_mem = 512MB
max_connections = 200
```

---

### 2. dSeries Server

**JVM Configuration:**
```properties
jvmproperty_2=-Xms4096M          # Initial heap
jvmproperty_3=-Xmx4096M          # Maximum heap
jvmproperty_4=-XX:+UseG1GC       # G1 garbage collector
jvmproperty_5=-XX:MaxGCPauseMillis=200
```

**Server Settings:**
- Port: 7599 (12.4) or 7600 (25.0)
- Admin user: demo_admin
- Logging: Enhanced for demo purposes

---

### 3. Demo Users

| Username | Password | Role | Purpose |
|----------|----------|------|---------|
| demo_admin | Demo2026! | Administrator | Full access for demos |
| demo_user | Demo2026! | User | Standard user demos |
| sales_demo | Sales2026! | Administrator | Sales demonstrations |

---

### 4. Sample Applications (Optional)

When `-ImportSamples` is used:

**Sample Applications:**
1. **Daily Operations** - Common daily jobs
2. **Data Processing** - ETL workflow example
3. **File Transfer** - File movement automation
4. **Database Maintenance** - DB backup and maintenance
5. **Report Generation** - Scheduled reporting

**Sample Jobs:**
- Daily_Backup (runs at 2:00 AM)
- Data_Processing (runs at 8:00 AM weekdays)
- Report_Generation (runs at 6:00 PM)
- File_Transfer (runs every 30 minutes)
- Database_Maintenance (runs Sunday 3:00 AM)

---

## 📊 Environment Types

### Demo Environment
**Purpose:** Sales demonstrations and customer presentations

**Configuration:**
- Pre-loaded sample applications
- Demo users with simple passwords
- Optimized for quick demos
- Sample data included
- Visual dashboards enabled

**Use Cases:**
- Customer demos
- Trade shows
- Webinars
- Proof of concepts

---

### Sales Environment
**Purpose:** Extended customer evaluations

**Configuration:**
- Full feature set enabled
- Extended trial period
- Customer-specific branding
- Sample integrations
- Performance optimized

**Use Cases:**
- Customer trials
- Extended evaluations
- Competitive comparisons
- Feature showcases

---

### Development Environment
**Purpose:** Development and testing

**Configuration:**
- Debug logging enabled
- Development tools included
- API access enabled
- Test data sets
- Relaxed security (for testing)

**Use Cases:**
- Feature development
- Integration testing
- API development
- Custom plugin testing

---

## 🎬 Demo Scenarios

### Scenario 1: Basic Job Scheduling (5 minutes)

**Objective:** Show how to create and schedule a simple job

**Steps:**
1. Login as `demo_admin`
2. Navigate to **Job Definitions**
3. Click **New Job**
4. Configure:
   - Name: "Demo_Daily_Report"
   - Command: `generate_report.bat`
   - Schedule: Daily at 8:00 AM
5. Save and run manually
6. Show job execution in real-time
7. Review job log and output

**Key Points to Highlight:**
- Intuitive UI
- Simple scheduling
- Real-time monitoring
- Detailed logging

---

### Scenario 2: Workflow Creation (10 minutes)

**Objective:** Demonstrate complex workflow with dependencies

**Steps:**
1. Create workflow: "Data_Processing_Workflow"
2. Add jobs:
   - Job 1: Extract data from source
   - Job 2: Transform data (depends on Job 1)
   - Job 3: Load data to target (depends on Job 2)
   - Job 4: Generate report (depends on Job 3)
3. Define dependencies
4. Show visual workflow diagram
5. Execute workflow
6. Monitor progress in real-time
7. Show success/failure handling

**Key Points to Highlight:**
- Visual workflow designer
- Dependency management
- Parallel execution
- Error handling
- Workflow monitoring

---

### Scenario 3: Agent Management (8 minutes)

**Objective:** Show agent deployment and management

**Steps:**
1. Navigate to **Agent Management**
2. Show agent topology
3. Deploy new agent
4. Configure agent properties
5. Test agent connectivity
6. Show agent health status
7. Demonstrate workload distribution

**Key Points to Highlight:**
- Easy agent deployment
- Health monitoring
- Load balancing
- Centralized management

---

### Scenario 4: Monitoring & Alerting (7 minutes)

**Objective:** Demonstrate monitoring capabilities

**Steps:**
1. Show **Dashboard** with real-time metrics
2. Display active jobs
3. Show job history and statistics
4. Configure alert rules
5. Trigger an alert (simulate failure)
6. Show alert notification
7. Demonstrate remediation

**Key Points to Highlight:**
- Real-time dashboards
- Comprehensive metrics
- Proactive alerting
- Quick remediation

---

### Scenario 5: REST API Integration (10 minutes)

**Objective:** Show API capabilities for integration

**Steps:**
1. Open REST API documentation
2. Show authentication
3. Demonstrate job submission via API
4. Query job status via API
5. Show webhook integration
6. Display API response formats

**Key Points to Highlight:**
- RESTful API
- Easy integration
- Comprehensive endpoints
- Standard formats (JSON)

---

## 🔧 Advanced Configuration

### Multiple Environments on Same Server

```powershell
# Setup multiple versions for comparison
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -InstallPath "C:\CA\dSeries_12_4" -ServerPort 7599
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Demo -InstallPath "C:\CA\dSeries_25_0" -ServerPort 7600
```

**Result:**
- dSeries 12.4 on port 7599
- dSeries 25.0 on port 7600
- Side-by-side comparison ready

---

### Custom Branding

**Customize for Customer:**

```powershell
# After setup, customize branding
$installPath = "C:\CA\dSeries_Demo"

# Update company name
$configFile = "$installPath\conf\branding.properties"
@"
company.name=ACME Corporation
company.logo=acme_logo.png
theme.primary.color=#0066CC
"@ | Set-Content $configFile
```

---

### Performance Tuning for Large Demos

```powershell
# For demos with large datasets
.\Setup-DSeriesDemoEnvironment.ps1 `
    -Version 25.0 `
    -Environment Demo `
    -InstallPath "C:\CA\dSeries_Large" `
    -ImportSamples

# Then manually adjust JVM heap
$propsFile = "C:\CA\dSeries_Large\conf\windows.service.properties"
(Get-Content $propsFile) -replace '-Xmx4096M', '-Xmx8192M' | Set-Content $propsFile
(Get-Content $propsFile) -replace '-Xms4096M', '-Xms8192M' | Set-Content $propsFile
```

---

## 📦 Pre-Packaged Demo Kits

### Demo Kit 1: Standard Demo (Recommended)

**Contents:**
- dSeries 12.4 or 25.0
- Embedded PostgreSQL
- 5 sample applications
- 10 demo jobs
- 3 demo users
- Quick start guide

**Setup Time:** 15 minutes  
**Use Case:** Standard customer demos

**Command:**
```powershell
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices
```

---

### Demo Kit 2: Sales Evaluation

**Contents:**
- dSeries 25.0 (latest)
- Embedded PostgreSQL
- 10 sample applications
- 25 demo jobs
- 5 demo users
- Extended trial license
- Customer branding template

**Setup Time:** 20 minutes  
**Use Case:** Extended customer evaluations

**Command:**
```powershell
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Sales -ImportSamples -StartServices
```

---

### Demo Kit 3: Development Environment

**Contents:**
- dSeries 12.4 or 25.0
- Embedded PostgreSQL
- Development tools
- API documentation
- Test data sets
- Debug logging enabled

**Setup Time:** 15 minutes  
**Use Case:** Development and testing

**Command:**
```powershell
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Development -ImportSamples
```

---

### Demo Kit 4: Version Comparison

**Contents:**
- dSeries 12.4 AND 25.0 (side-by-side)
- Separate databases
- Different ports
- Same sample data in both
- Comparison guide

**Setup Time:** 30 minutes  
**Use Case:** Feature comparison demos

**Commands:**
```powershell
# Setup 12.4
.\Setup-DSeriesDemoEnvironment.ps1 `
    -Version 12.4 `
    -Environment Demo `
    -InstallPath "C:\CA\dSeries_12_4" `
    -ServerPort 7599 `
    -PostgreSQLPort 5432 `
    -ImportSamples `
    -StartServices

# Setup 25.0
.\Setup-DSeriesDemoEnvironment.ps1 `
    -Version 25.0 `
    -Environment Demo `
    -InstallPath "C:\CA\dSeries_25_0" `
    -ServerPort 7600 `
    -PostgreSQLPort 5433 `
    -ImportSamples `
    -StartServices
```

---

## 🎓 Step-by-Step Manual Setup

If you prefer manual setup or need to customize beyond the script:

### Step 1: Install PostgreSQL (15 minutes)

```powershell
# Download PostgreSQL portable
$pgVersion = "14.5"
$pgUrl = "https://get.enterprisedb.com/postgresql/postgresql-$pgVersion-1-windows-x64-binaries.zip"
$pgZip = "C:\Temp\postgresql.zip"
$pgPath = "C:\CA\dSeries_Demo\PostgreSQL"

# Download
Invoke-WebRequest -Uri $pgUrl -OutFile $pgZip

# Extract
Expand-Archive -Path $pgZip -DestinationPath $pgPath

# Initialize database cluster
& "$pgPath\bin\initdb.exe" -D "$pgPath\data" -U postgres -W

# Start PostgreSQL
& "$pgPath\bin\pg_ctl.exe" -D "$pgPath\data" -l "$pgPath\logs\postgresql.log" start

# Create database
& "$pgPath\bin\createdb.exe" -h localhost -p 5432 -U postgres dseries_demo
```

---

### Step 2: Install dSeries Server (10 minutes)

```powershell
# Extract dSeries installation
$installZip = "\\fileserver\software\dSeries\12.4\ESPdSeriesWAServer_R12_4.zip"
$installPath = "C:\CA\dSeries_Demo"

Expand-Archive -Path $installZip -DestinationPath $installPath

# Configure JVM heap
$propsFile = "$installPath\conf\windows.service.properties"
$content = Get-Content $propsFile
$content = $content -replace 'jvmproperty_2=-Xms\d+[mMgG]', 'jvmproperty_2=-Xms4096M'
$content = $content -replace 'jvmproperty_3=-Xmx\d+[mMgG]', 'jvmproperty_3=-Xmx4096M'
$content | Set-Content $propsFile

# Configure database connection
$dbPropsFile = "$installPath\conf\db.properties"
@"
jdbc.URL=jdbc:postgresql://localhost:5432/dseries_demo
rdbms.userid=dseries_user
rdbms.driver=org.postgresql.Driver
"@ | Set-Content $dbPropsFile
```

---

### Step 3: Initialize dSeries Schema (5 minutes)

```powershell
# Run dSeries database initialization
$initScript = "$installPath\bin\dbinit.bat"

if (Test-Path $initScript) {
    & $initScript
    Write-Host "Database schema initialized"
}
```

---

### Step 4: Import Sample Data (10 minutes)

```powershell
# Import sample applications
$importTool = "$installPath\bin\import.bat"
$samplesDir = "\\fileserver\dSeries\samples\applications"

Get-ChildItem $samplesDir -Filter "*.xml" | ForEach-Object {
    Write-Host "Importing: $($_.Name)"
    & $importTool -file $_.FullName
}

Write-Host "Sample applications imported"
```

---

### Step 5: Create Demo Users (5 minutes)

```powershell
# Create demo users using dSeries CLI
$cliTool = "$installPath\bin\cli.bat"

# Admin user
& $cliTool user create -username demo_admin -password Demo2026! -role Administrator

# Standard user
& $cliTool user create -username demo_user -password Demo2026! -role User

# Sales demo user
& $cliTool user create -username sales_demo -password Sales2026! -role Administrator

Write-Host "Demo users created"
```

---

### Step 6: Start Services (2 minutes)

```powershell
# Start PostgreSQL
Start-Service "postgresql-5432"

# Start dSeries server
Start-Service "ESP dSeries Workload Automation"

# Wait for startup
Start-Sleep -Seconds 30

# Verify
Test-NetConnection -ComputerName localhost -Port 7599
```

---

### Step 7: Verify Installation (5 minutes)

```powershell
# Run health check
cd C:\Codes\dseries_healthcheck
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"

# Expected: Health score 85-95 (GOOD to EXCELLENT)
```

---

## 🎯 Best Practices for Demo Environments

### 1. System Configuration

**Optimal Settings:**
```
CPU:      8 cores (dedicated)
Memory:   16 GB (8GB for dSeries, 8GB for OS)
Disk:     SSD recommended (100 GB+)
Network:  1 Gbps
```

**JVM Settings:**
```properties
-Xms4096M -Xmx4096M              # Heap size
-XX:+UseG1GC                     # G1 garbage collector
-XX:MaxGCPauseMillis=200         # GC pause target
-XX:+HeapDumpOnOutOfMemoryError  # Troubleshooting
```

---

### 2. Database Configuration

**PostgreSQL Tuning:**
```properties
shared_buffers = 2GB             # 25% of system RAM
effective_cache_size = 8GB       # 50% of system RAM
work_mem = 50MB
maintenance_work_mem = 1GB
max_connections = 200
checkpoint_completion_target = 0.9
```

**Backup Strategy:**
```powershell
# Daily backup script
$backupDir = "C:\CA\dSeries_Demo\backups"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
& pg_dump -h localhost -p 5432 -U dseries_user -d dseries_demo -f "$backupDir\backup_$timestamp.sql"
```

---

### 3. Security for Demos

**Demo-Friendly Security:**
- Simple passwords (documented)
- No password expiration
- Relaxed password complexity
- Extended session timeout
- Audit logging enabled (for demos)

**Production Security:**
- Complex passwords
- Password expiration (90 days)
- Strong password policy
- Standard session timeout
- Full audit logging

---

### 4. Performance Optimization

**For Smooth Demos:**
```
✅ Pre-load data before demo
✅ Warm up JVM (run test jobs)
✅ Clear old logs
✅ Optimize database (VACUUM, ANALYZE)
✅ Test all demo scenarios beforehand
✅ Have backup environment ready
```

---

## 📋 Pre-Demo Checklist

### 24 Hours Before Demo

- [ ] Run setup script
- [ ] Verify health check passes (score ≥ 85)
- [ ] Import sample applications
- [ ] Create demo users
- [ ] Test all demo scenarios
- [ ] Prepare demo script
- [ ] Backup environment

### 1 Hour Before Demo

- [ ] Start all services
- [ ] Verify server is accessible
- [ ] Test login with demo accounts
- [ ] Clear old job history
- [ ] Run sample jobs to warm up
- [ ] Check system resources (CPU, memory, disk)
- [ ] Prepare screen sharing/projector

### During Demo

- [ ] Have quick start guide open
- [ ] Monitor system resources
- [ ] Have backup scenarios ready
- [ ] Document customer questions
- [ ] Capture customer requirements

### After Demo

- [ ] Export demo configuration (if customer interested)
- [ ] Document customization requests
- [ ] Schedule follow-up
- [ ] Archive demo environment
- [ ] Update demo scenarios based on feedback

---

## 🔄 Environment Lifecycle

### Setup → Use → Maintain → Refresh

```
┌─────────────────────────────────────────────────────────────┐
│  SETUP (15-20 minutes)                                      │
│  • Run automated setup script                               │
│  • Verify health check                                      │
│  • Test demo scenarios                                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  USE (Demo period)                                          │
│  • Conduct demonstrations                                   │
│  • Customer evaluations                                     │
│  • Training sessions                                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  MAINTAIN (Weekly)                                          │
│  • Run health check                                         │
│  • Clear old logs                                           │
│  • Update sample data                                       │
│  • Backup database                                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  REFRESH (Monthly or as needed)                             │
│  • Re-run setup script                                      │
│  • Update to latest version                                 │
│  • Refresh sample data                                      │
│  • Update demo scenarios                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Troubleshooting

### Issue: Setup Script Fails

**Error:** "Installation package not found"

**Solution:**
1. Verify source paths in script configuration
2. Check network connectivity to file server
3. Ensure you have read permissions
4. Update `SourcePaths` in script

---

### Issue: PostgreSQL Won't Start

**Error:** "Could not start PostgreSQL service"

**Solution:**
```powershell
# Check if port is already in use
netstat -ano | findstr :5432

# Check PostgreSQL logs
Get-Content "C:\CA\dSeries_Demo\PostgreSQL\logs\postgresql.log" -Tail 50

# Try different port
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -PostgreSQLPort 5433
```

---

### Issue: dSeries Server Won't Start

**Error:** "Server port not accessible"

**Solution:**
```powershell
# Check service status
Get-Service | Where-Object {$_.DisplayName -like "*dSeries*"}

# Check logs
Get-Content "C:\CA\dSeries_Demo\logs\server.log" -Tail 100

# Verify JVM heap
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"

# Check database connectivity
psql -h localhost -p 5432 -U dseries_user -d dseries_demo
```

---

### Issue: Health Check Fails

**Error:** "Health score below 60"

**Solution:**
1. Review health check report
2. Fix critical issues (JVM heap, disk space)
3. Re-run setup if needed
4. Check HEALTH_CHECK_RESULTS_*.md for details

---

## 📚 Additional Resources

### Documentation
- **QUICK_START.md** - Created in installation directory
- **SETUP_SUMMARY.txt** - Setup details and credentials
- **ARCHITECTURE_AND_FLOW.md** - System architecture
- **BEST_PRACTICES_GUIDE.md** - Configuration best practices

### Scripts
- **Setup-DSeriesDemoEnvironment.ps1** - Main setup script
- **Run-HealthCheck.ps1** - Health check tool
- **cleanup_dseries.ps1** - Cleanup utility
- **fix_jvm_heap.ps1** - JVM heap fix utility

### Support
- **Broadcom Support:** https://support.broadcom.com
- **Documentation:** https://techdocs.broadcom.com
- **Community:** https://community.broadcom.com

---

## 🎉 Success Criteria

### Environment is Ready When:

✅ **Health check score ≥ 85** (GOOD or EXCELLENT)  
✅ **All services running** (PostgreSQL and dSeries)  
✅ **Server accessible** (port responds)  
✅ **Demo users can login** (authentication works)  
✅ **Sample jobs execute** (job processing works)  
✅ **No critical errors in logs** (clean startup)  

### Demo is Ready When:

✅ **All demo scenarios tested** (end-to-end validation)  
✅ **Performance is smooth** (no lag or delays)  
✅ **UI is responsive** (quick page loads)  
✅ **Sample data is loaded** (realistic demos)  
✅ **Backup environment available** (contingency plan)  

---

## 📊 Setup Time Estimates

| Task | Automated | Manual |
|------|-----------|--------|
| Prerequisites check | 1 min | 5 min |
| PostgreSQL install | 3 min | 15 min |
| dSeries install | 5 min | 10 min |
| Database init | 2 min | 5 min |
| Sample data import | 3 min | 10 min |
| User creation | 1 min | 5 min |
| Configuration | 2 min | 10 min |
| Service startup | 2 min | 5 min |
| Health check | 1 min | 5 min |
| **TOTAL** | **20 min** | **70 min** |

**Time Saved with Automation:** 50 minutes per environment!

---

## 🔗 Quick Reference

### Common Commands

```powershell
# Standard demo setup
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices

# Sales evaluation setup
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Sales -ImportSamples -StartServices

# Development environment
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Development

# Health check
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"

# Start services
Start-Service "postgresql-5432"
Start-Service "ESP dSeries Workload Automation"

# Stop services
Stop-Service "ESP dSeries Workload Automation"
Stop-Service "postgresql-5432"
```

---

## 📞 Support

### For Setup Issues
- Review troubleshooting section above
- Check SETUP_SUMMARY.txt in installation directory
- Run health check for diagnostics

### For Demo Questions
- Review demo scenarios in this guide
- Check QUICK_START.md in installation directory
- Consult dSeries documentation

### For Technical Support
- **Portal:** https://support.broadcom.com
- **Email:** dseries-support@broadcom.com
- **Community:** https://community.broadcom.com

---

## ✅ Summary

This automated setup solution provides:

✅ **Fast Setup** - 15-20 minutes vs 70+ minutes manual  
✅ **Consistent** - Same configuration every time  
✅ **Reliable** - Validated with health checks  
✅ **Flexible** - Multiple versions and environments  
✅ **Complete** - Database, server, samples, users  
✅ **Demo-Ready** - Pre-configured scenarios  
✅ **Professional** - Production-quality setup  

**Perfect for sales demos, customer evaluations, and training!**

---

**Version:** 1.0.0  
**Last Updated:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
