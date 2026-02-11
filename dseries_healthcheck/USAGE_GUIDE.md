# dSeries Health Check Tool - Usage Guide

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Status:** Production Ready

---

## 📋 Overview

The dSeries Health Check Tool is a command-line utility that performs comprehensive health checks on ESP dSeries Workload Automation installations. It accepts the server installation directory as input and generates detailed reports.

---

## 🚀 Quick Start

### Method 1: Using Batch File (Windows - Easiest)

```batch
cd C:\Codes\dseries_healthcheck
run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"
```

### Method 2: Using PowerShell Script (Windows - Recommended)

```powershell
cd C:\Codes\dseries_healthcheck
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"
```

### Method 3: Direct Java Execution (All Platforms)

```bash
cd C:\Codes\dseries_healthcheck
java DSeriesHealthCheckSimple "C:\CA\ESPdSeriesWAServer_R12_4"
```

---

## 📖 Detailed Usage

### Command Line Syntax

#### Batch File (Windows)
```batch
run_healthcheck_simple.bat [installation_directory]
```

**Parameters:**
- `installation_directory` - Full path to dSeries installation (required)

**Examples:**
```batch
# Standard installation
run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"

# Custom installation path
run_healthcheck_simple.bat "D:\Applications\dSeries"

# Network path
run_healthcheck_simple.bat "\\server\share\dSeries"
```

---

#### PowerShell Script (Windows)
```powershell
.\Run-HealthCheck.ps1 -InstallDir <path> [-OutputDir <path>] [-GenerateReports]
```

**Parameters:**
- `-InstallDir` - Path to dSeries installation (required)
- `-OutputDir` - Directory for reports (optional, default: current directory)
- `-GenerateReports` - Generate additional HTML/JSON reports (optional)

**Examples:**
```powershell
# Basic usage
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"

# With custom output directory
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -OutputDir "C:\Reports"

# With additional reports
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -GenerateReports

# Get help
Get-Help .\Run-HealthCheck.ps1 -Full
```

---

#### Direct Java Execution
```bash
java DSeriesHealthCheckSimple [installation_directory]
```

**Parameters:**
- `installation_directory` - Path to dSeries installation (optional, defaults to C:/CA/ESPdSeriesWAServer_R12_4)

**Examples:**
```bash
# With explicit path
java DSeriesHealthCheckSimple "C:\CA\ESPdSeriesWAServer_R12_4"

# Using default path (if not provided)
java DSeriesHealthCheckSimple

# Unix/Linux
java DSeriesHealthCheckSimple "/opt/CA/WA_DE"
```

---

## 📊 Output and Reports

### Console Output

The tool displays real-time results in the console:

```
=================================================================
  ESP dSeries Workload Automation Health Check Tool v1.0.0
=================================================================
  Date: 2026-02-11 21:38:57
  Host: G36R0T3
  dSeries: C:\CA\ESPdSeriesWAServer_R12_4
=================================================================

Starting health check...

[SYS-001] Checking CPU Utilization...
  [PASS] CPU usage is 45.2% (healthy)

[SYS-002] Checking Memory Usage...
  [PASS] Memory usage is 65.3% (7 MB / 16304 MB)

[SYS-003] Checking Disk Space...
  [FAIL] CRITICAL: Disk usage is 98.2% (462 GB / 471 GB)
         Recommendation: Clean up old logs, archives. Expand disk space immediately.

...

=================================================================
  HEALTH CHECK SUMMARY
=================================================================

  Overall Health Score: 42/100
  Status: POOR

  Total Checks: 7
  [PASS] Passed: 3
  [WARN] Warnings: 0
  [FAIL] Failed: 3

  WARNING: 3 CRITICAL ISSUE(S) DETECTED!
  Please review the detailed output above for remediation steps.

=================================================================
```

### Exit Codes

The tool returns different exit codes based on health status:

| Exit Code | Status | Description |
|-----------|--------|-------------|
| 0 | SUCCESS | Health score ≥ 60 (FAIR or better) |
| 1 | FAILURE | Health score < 60 (POOR or CRITICAL) |
| 2 | ERROR | Invalid input or execution error |

**Usage in Scripts:**
```batch
run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"
if %ERRORLEVEL% EQU 0 (
    echo Health check passed
) else (
    echo Health check failed - review results
)
```

```powershell
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"
if ($LASTEXITCODE -eq 0) {
    Write-Host "Health check passed"
} else {
    Write-Host "Health check failed - review results"
}
```

---

## 📄 Generated Reports

The tool automatically generates detailed reports in the current directory:

### 1. Health Check Results (Markdown)
**File:** `HEALTH_CHECK_RESULTS_YYYYMMDD.md`

Comprehensive report including:
- Executive summary with health score
- Detailed analysis of each check
- Critical issues with remediation steps
- System information
- Best practice recommendations

### 2. Action Plan (Markdown)
**File:** `ACTION_PLAN_YYYYMMDD.md`

Step-by-step remediation guide:
- Emergency actions (within 1 hour)
- Urgent actions (within 24 hours)
- Follow-up actions (within 1 week)
- Verification checklist
- PowerShell/Batch commands ready to execute

### 3. Executive Summary (Text)
**File:** `EXECUTIVE_SUMMARY_YYYYMMDD.txt`

Quick reference summary:
- Health score and status
- Critical issues list
- Immediate actions required
- Expected outcomes

---

## 🔍 Health Checks Performed

The tool performs the following checks:

| Check ID | Category | Description | Severity |
|----------|----------|-------------|----------|
| SYS-001 | System Resources | CPU Utilization | Critical |
| SYS-002 | System Resources | Memory Usage | High |
| SYS-003 | System Resources | Disk Space | Critical |
| SRV-001 | Server Config | JVM Heap Size | Critical |
| DB-001 | Database | Database Connectivity | Critical |
| SRV-003 | Server Config | Server Port Accessibility | Critical |
| SRV-005 | Installation | Directory Structure | Medium |

---

## 📈 Health Score Interpretation

| Score | Status | Description | Action Required |
|-------|--------|-------------|-----------------|
| 90-100 | ✅ EXCELLENT | System optimally configured | Maintain current practices |
| 75-89 | 🟢 GOOD | Minor improvements recommended | Review recommendations |
| 60-74 | 🟡 FAIR | Several issues need attention | Implement fixes within 1 week |
| 40-59 | 🟠 POOR | Significant problems detected | Immediate action required |
| 0-39 | 🔴 CRITICAL | System at risk | Emergency intervention needed |

---

## 🔧 Advanced Usage

### Running on Multiple Servers

Create a batch file to check multiple installations:

```batch
@echo off
echo Checking all dSeries installations...

call run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"
call run_healthcheck_simple.bat "D:\CA\ESPdSeriesWAServer_R12_5"
call run_healthcheck_simple.bat "\\server1\dSeries"

echo All health checks completed
```

### Scheduled Execution

#### Windows Task Scheduler (Batch)
```batch
schtasks /create /tn "dSeries Health Check" /tr "C:\Codes\dseries_healthcheck\run_healthcheck_simple.bat C:\CA\ESPdSeriesWAServer_R12_4" /sc daily /st 06:00
```

#### Windows Task Scheduler (PowerShell)
```powershell
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" `
    -Argument "-File C:\Codes\dseries_healthcheck\Run-HealthCheck.ps1 -InstallDir C:\CA\ESPdSeriesWAServer_R12_4"
$trigger = New-ScheduledTaskTrigger -Daily -At 6am
Register-ScheduledTask -TaskName "dSeries Health Check - Daily" -Action $action -Trigger $trigger
```

### Email Notifications

Wrap the health check in a PowerShell script with email:

```powershell
# Run health check
$result = .\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"

# Send email if failed
if ($LASTEXITCODE -ne 0) {
    Send-MailMessage -From "healthcheck@company.com" `
        -To "admin@company.com" `
        -Subject "dSeries Health Check FAILED" `
        -Body "Health check detected critical issues. Review attached report." `
        -Attachments "HEALTH_CHECK_RESULTS_*.md" `
        -SmtpServer "smtp.company.com"
}
```

### Integration with Monitoring Tools

Export results to JSON for monitoring tools:

```powershell
# Run health check and capture output
$output = java DSeriesHealthCheckSimple "C:\CA\ESPdSeriesWAServer_R12_4" | Out-String

# Parse and send to monitoring system
# (Implementation depends on your monitoring tool)
```

---

## 🐛 Troubleshooting

### Issue: "Java not found"

**Error:**
```
'java' is not recognized as an internal or external command
```

**Solution:**
1. Install Java 8 or higher
2. Add Java to PATH environment variable
3. Verify: `java -version`

---

### Issue: "Installation directory not found"

**Error:**
```
ERROR: Installation directory not found: C:\CA\ESPdSeriesWAServer_R12_4
```

**Solution:**
1. Verify the path is correct
2. Check for typos in the path
3. Ensure you have read permissions
4. Use quotes around paths with spaces

---

### Issue: "Compilation failed"

**Error:**
```
ERROR: Compilation failed
```

**Solution:**
1. Ensure Java JDK is installed (not just JRE)
2. Verify javac is in PATH
3. Check file permissions
4. Manually compile: `javac DSeriesHealthCheckSimple.java`

---

### Issue: "Permission denied"

**Error:**
```
Access denied to C:\CA\ESPdSeriesWAServer_R12_4\conf\
```

**Solution:**
1. Run as Administrator
2. Check file/folder permissions
3. Ensure you have read access to installation directory

---

## 📋 Prerequisites

### System Requirements
- **Java:** JDK/JRE 8 or higher
- **OS:** Windows, Linux, Unix
- **Memory:** 256 MB minimum
- **Disk:** 10 MB for tool + space for reports

### Permissions Required
- Read access to dSeries installation directory
- Read access to configuration files
- Network access to test ports (optional)
- Write access to output directory for reports

---

## 🔐 Security Considerations

### Password Handling
- The tool does NOT require database passwords
- Configuration files are read but passwords are not extracted
- Database connectivity test is skipped if JDBC driver not available

### Network Access
- Only tests localhost ports by default
- No external network connections required
- Firewall rules not affected

### File Access
- Only reads configuration files
- Does NOT modify any files
- Safe to run on production systems

---

## 📞 Support

### For Issues
- **Email:** dseries-support@broadcom.com
- **Portal:** https://support.broadcom.com
- **Documentation:** See README.md and other guides

### For Enhancements
- Submit feature requests via support portal
- Contribute improvements via internal repository

---

## 📚 Related Documentation

- **README.md** - Tool overview and features
- **BEST_PRACTICES_GUIDE.md** - dSeries best practices
- **QUICK_START_REMEDIATION.md** - Quick fix guide
- **INSTALLATION_GUIDE.md** - Installation instructions
- **JAVA_README.md** - Java implementation details

---

## 🔄 Version History

### Version 1.0.0 (February 11, 2026)
- ✅ Initial release
- ✅ Command-line argument support for installation directory
- ✅ 7 comprehensive health checks
- ✅ Batch file wrapper for Windows
- ✅ PowerShell script for advanced usage
- ✅ Detailed reporting (Markdown, Text)
- ✅ Exit codes for automation
- ✅ Cross-platform support

---

## 💡 Tips and Best Practices

### Daily Usage
1. Run health check daily at 6 AM (scheduled task)
2. Review reports weekly
3. Address warnings within 1 week
4. Address failures immediately

### Before Production Deployment
1. Run full health check
2. Ensure health score ≥ 75 (GOOD)
3. Fix all critical issues
4. Verify JVM heap ≥ 4096 MB

### After System Changes
1. Run health check after updates
2. Run health check after configuration changes
3. Compare results with baseline
4. Document any deviations

### Monitoring Integration
1. Schedule daily automated checks
2. Configure email alerts for failures
3. Track health score trends over time
4. Set up dashboards for visualization

---

## ✅ Quick Reference

### Most Common Commands

```batch
# Windows - Quick check
run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"

# PowerShell - With reports
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -GenerateReports

# Direct Java - Any platform
java DSeriesHealthCheckSimple "C:\CA\ESPdSeriesWAServer_R12_4"

# Get help
run_healthcheck_simple.bat
Get-Help .\Run-HealthCheck.ps1
```

### Check Results
```batch
# View latest report
notepad HEALTH_CHECK_RESULTS_*.md

# View summary
type EXECUTIVE_SUMMARY_*.txt

# View action plan
notepad ACTION_PLAN_*.md
```

---

**Ready to use!** 🚀

For questions or issues, refer to the troubleshooting section or contact support.

---

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
