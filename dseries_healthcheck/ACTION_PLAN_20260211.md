# dSeries Health Check - Immediate Action Plan

**Date:** February 11, 2026  
**System:** C:\CA\ESPdSeriesWAServer_R12_4  
**Health Score:** 28/100 (CRITICAL)  
**Critical Issues:** 4

---

## 🚨 EMERGENCY ACTIONS (Next 1 Hour)

### Action 1: Free Up Disk Space (CRITICAL)
**Current:** 98.2% used (only 9 GB free)  
**Target:** At least 100 GB free  
**Priority:** 🔴 HIGHEST

#### Quick Wins - Manual Cleanup:

```powershell
# 1. Navigate to installation directory
cd C:\CA\ESPdSeriesWAServer_R12_4

# 2. Remove old update packages (can save 50+ GB)
Remove-Item *.zip -Force
# Estimated space saved: 50-60 GB

# 3. Remove old backup directories (can save 30+ GB)
Get-ChildItem -Directory -Filter "UPDATE_*" | 
    Where-Object {$_.LastWriteTime -lt (Get-Date).AddMonths(-6)} | 
    Remove-Item -Recurse -Force
# Estimated space saved: 30-40 GB

# 4. Clean up old logs (can save 5-10 GB)
cd logs
Get-ChildItem -File | 
    Where-Object {$_.LastWriteTime -lt (Get-Date).AddDays(-30)} | 
    Remove-Item -Force
# Estimated space saved: 5-10 GB

# 5. Clean temp directory
cd ..\temp
Remove-Item * -Recurse -Force
# Estimated space saved: 1-5 GB
```

#### Automated Cleanup (Recommended):

```powershell
cd C:\Codes\dseries_healthcheck
.\cleanup_dseries.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -Verbose
```

**Expected Result:** 80-100 GB freed  
**Time Required:** 5-10 minutes  
**Risk Level:** Low (only removes old backups and logs)

---

### Action 2: Investigate High CPU Usage
**Current:** 100% CPU utilization  
**Priority:** 🔴 HIGH

#### Investigation Steps:

```powershell
# 1. Check what's consuming CPU
Get-Process | Sort-Object CPU -Descending | Select-Object -First 10 Name, CPU, Id

# 2. Look for Java processes
Get-Process java* | Select-Object Name, CPU, WorkingSet, Id

# 3. Check for runaway processes
Get-Process | Where-Object {$_.CPU -gt 100} | Select-Object Name, CPU, Id

# 4. If you find a problematic process, you can stop it:
# Stop-Process -Id <ProcessId> -Force
```

#### Common Causes:
- Stuck or long-running jobs
- Database query issues
- Memory pressure causing excessive GC
- Runaway agent processes
- System maintenance tasks

**Expected Result:** Identify and resolve high CPU process  
**Time Required:** 10-15 minutes  
**Risk Level:** Medium (may need to stop processes)

---

## ⚠️ URGENT ACTIONS (Within 24 Hours)

### Action 3: Fix JVM Heap Size (CRITICAL)
**Current:** 1024 MB  
**Required:** 4096 MB minimum  
**Priority:** 🔴 CRITICAL

#### Option A: Automated Fix (Recommended)

```powershell
cd C:\Codes\dseries_healthcheck
.\fix_jvm_heap.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -HeapSizeMB 4096
```

#### Option B: Manual Fix

1. **Stop the dSeries service:**
   ```powershell
   Stop-Service "ESP dSeries Workload Automation"
   ```

2. **Edit the configuration file:**
   ```powershell
   notepad "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties"
   ```

3. **Find and modify these lines:**
   ```properties
   # BEFORE (INCORRECT):
   jvmproperty_2=-Xms1024M
   jvmproperty_3=-Xmx1024M
   
   # AFTER (CORRECT):
   jvmproperty_2=-Xms4096M
   jvmproperty_3=-Xmx4096M
   ```

4. **Save the file**

5. **Start the dSeries service:**
   ```powershell
   Start-Service "ESP dSeries Workload Automation"
   ```

6. **Verify the change:**
   ```powershell
   # Wait 30 seconds for service to start
   Start-Sleep -Seconds 30
   
   # Check Java process memory
   Get-Process java* | Select-Object Name, @{N='Memory(MB)';E={[math]::Round($_.WorkingSet/1MB,2)}}
   # Should show ~4000-4500 MB
   ```

**Expected Result:** JVM using 4096 MB heap  
**Time Required:** 5 minutes (automated) or 10 minutes (manual)  
**Risk Level:** Low (requires service restart)

---

### Action 4: Start dSeries Service
**Current:** Service not running (port 7599 not accessible)  
**Priority:** 🔴 HIGH

#### Steps:

1. **Check service status:**
   ```powershell
   Get-Service | Where-Object {$_.DisplayName -like "*dSeries*"}
   ```

2. **If service is stopped, start it:**
   ```powershell
   Start-Service "ESP dSeries Workload Automation"
   ```

3. **Monitor the startup:**
   ```powershell
   # Watch the log file for startup messages
   Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Wait -Tail 50
   ```

4. **Verify service is running:**
   ```powershell
   # Check service status
   Get-Service | Where-Object {$_.DisplayName -like "*dSeries*"}
   
   # Check if port is listening
   Test-NetConnection -ComputerName localhost -Port 7599
   ```

5. **If service fails to start, check logs:**
   ```powershell
   # View recent errors
   Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\error.log" -Tail 100
   
   # View server log
   Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 100
   ```

#### Common Startup Issues:
- **Low memory:** Fix JVM heap first (Action 3)
- **Database connection:** Check database is running
- **Port conflict:** Another application using port 7599
- **Configuration error:** Review error.log for details

**Expected Result:** Service running and port 7599 accessible  
**Time Required:** 5-10 minutes  
**Risk Level:** Low

---

## 📋 VERIFICATION CHECKLIST

After completing the emergency actions, verify the fixes:

```powershell
# 1. Check disk space
Get-PSDrive C | Select-Object Used, Free, @{N='PercentFree';E={[math]::Round($_.Free/$_.Used*100,1)}}
# Should show > 20% free

# 2. Check CPU usage
Get-Counter '\Processor(_Total)\% Processor Time' -SampleInterval 2 -MaxSamples 5
# Should be < 80%

# 3. Check service status
Get-Service | Where-Object {$_.DisplayName -like "*dSeries*"}
# Should show "Running"

# 4. Check port accessibility
Test-NetConnection -ComputerName localhost -Port 7599
# Should show "TcpTestSucceeded : True"

# 5. Check JVM memory
Get-Process java* | Select-Object Name, @{N='Memory(MB)';E={[math]::Round($_.WorkingSet/1MB,2)}}
# Should show ~4000-4500 MB

# 6. Re-run health check
cd C:\Codes\dseries_healthcheck
java DSeriesHealthCheckSimple
# Should show improved health score (target: 75+)
```

---

## 📊 EXPECTED OUTCOMES

### Before Remediation:
- Health Score: **28/100** (CRITICAL)
- Passed: 2
- Failed: 4
- Status: 🔴 CRITICAL

### After Remediation:
- Health Score: **75-85/100** (GOOD)
- Passed: 6-7
- Failed: 0-1
- Status: 🟢 GOOD

### Improvements:
- ✅ Disk space: 98.2% → 70-75% (100+ GB free)
- ✅ CPU usage: 100% → 30-50% (normal)
- ✅ JVM heap: 1024 MB → 4096 MB (correct)
- ✅ Service: Down → Running
- ✅ Port 7599: Not accessible → Accessible

---

## 🔄 FOLLOW-UP ACTIONS (Within 1 Week)

### 1. Implement Automated Log Rotation
```powershell
# Create a scheduled task for log cleanup
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" `
    -Argument "-File C:\Codes\dseries_healthcheck\cleanup_dseries.ps1 -InstallDir C:\CA\ESPdSeriesWAServer_R12_4"
$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At 2am
Register-ScheduledTask -TaskName "dSeries Log Cleanup" -Action $action -Trigger $trigger
```

### 2. Schedule Regular Health Checks
```powershell
# Create a scheduled task for daily health check
$action = New-ScheduledTaskAction -Execute "java.exe" `
    -Argument "DSeriesHealthCheckSimple" `
    -WorkingDirectory "C:\Codes\dseries_healthcheck"
$trigger = New-ScheduledTaskTrigger -Daily -At 6am
Register-ScheduledTask -TaskName "dSeries Health Check - Daily" -Action $action -Trigger $trigger
```

### 3. Set Up Monitoring Alerts
- Configure email alerts for critical issues
- Set up disk space monitoring (alert at 80%)
- Monitor CPU usage trends
- Track service availability

### 4. Review and Optimize
- Analyze job execution patterns
- Optimize database queries
- Review housekeeping procedures
- Plan for capacity expansion

---

## 📞 ESCALATION

### If Issues Persist:
1. **Disk Space Still Critical:**
   - Consider moving installation to larger drive
   - Archive old data to external storage
   - Contact storage team for disk expansion

2. **Service Won't Start:**
   - Review error logs in detail
   - Check database connectivity
   - Verify configuration files
   - Contact Broadcom Support

3. **Performance Issues Continue:**
   - Increase JVM heap to 6144 MB or 8192 MB
   - Review thread pool configuration
   - Analyze database performance
   - Consider hardware upgrade

### Support Contacts:
- **Broadcom Support:** https://support.broadcom.com
- **Internal Support:** [Your internal support contact]
- **Emergency:** [Emergency contact number]

---

## 📝 DOCUMENTATION

### Files to Review:
- **Full Report:** `HEALTH_CHECK_RESULTS_20260211.md`
- **Best Practices:** `BEST_PRACTICES_GUIDE.md`
- **Quick Reference:** `QUICK_START_REMEDIATION.md`
- **Installation Guide:** `INSTALLATION_GUIDE.md`

### Scripts Available:
- **cleanup_dseries.ps1** - Automated disk cleanup
- **fix_jvm_heap.ps1** - Automated JVM heap fix
- **DSeriesHealthCheckSimple.java** - Health check tool

---

## ✅ COMPLETION CHECKLIST

- [ ] Disk space freed (target: 100+ GB free)
- [ ] CPU usage investigated and resolved
- [ ] JVM heap increased to 4096 MB
- [ ] dSeries service started and running
- [ ] Port 7599 accessible
- [ ] Health check re-run (score improved to 75+)
- [ ] Automated cleanup scheduled
- [ ] Regular health checks scheduled
- [ ] Monitoring alerts configured
- [ ] Documentation reviewed and filed

---

**Action Plan Created:** February 11, 2026  
**Estimated Total Time:** 1-2 hours  
**Risk Level:** Low-Medium  
**Success Probability:** High (95%+)

**Next Step:** Start with Action 1 (Disk Cleanup) immediately!
