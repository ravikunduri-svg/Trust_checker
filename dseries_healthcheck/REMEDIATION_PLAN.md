# ESP dSeries Server Health Check - Remediation Plan

**Server:** C:\CA\ESPdSeriesWAServer_R12_4  
**Health Score:** 28/100 - 🔴 CRITICAL  
**Date:** February 11, 2026  
**Priority:** URGENT - Immediate Action Required

---

## 🚨 Executive Summary

Your dSeries server has **4 critical issues** that require immediate attention:

1. **Disk Space: 98.2% full** (only 9 GB remaining)
2. **JVM Heap: 1 GB** (should be 4 GB minimum)
3. **CPU Usage: 100%** (system at maximum capacity)
4. **Server Port: Not accessible** (connection issues)

**Estimated Time to Fix:** 2-3 hours  
**Risk Level:** HIGH - System may fail or become unstable

---

## 📋 Remediation Steps (Priority Order)

### **STEP 1: Free Up Disk Space (URGENT - 30 minutes)**

**Priority:** 🔴 CRITICAL  
**Impact:** System may crash if disk fills completely  
**Estimated Space to Recover:** 50-100 GB

#### **1.1 Clean Up Old Log Files**

```powershell
# Navigate to logs directory
cd C:\CA\ESPdSeriesWAServer_R12_4\logs

# Check current log size
Get-ChildItem -Recurse | Measure-Object -Property Length -Sum

# Archive logs older than 30 days
$date = (Get-Date).AddDays(-30)
Get-ChildItem -Recurse -File | Where-Object {$_.LastWriteTime -lt $date} | 
    Compress-Archive -DestinationPath "C:\Backup\dSeries_logs_archive_$(Get-Date -Format 'yyyyMMdd').zip"

# Delete archived logs
Get-ChildItem -Recurse -File | Where-Object {$_.LastWriteTime -lt $date} | Remove-Item -Force
```

**Expected Space Recovered:** 10-20 GB

#### **1.2 Clean Up Old Update Backups**

```powershell
cd C:\CA\ESPdSeriesWAServer_R12_4

# List all backup directories
Get-ChildItem -Directory | Where-Object {$_.Name -like "UPDATE_*_BACKUP*"} | 
    Select-Object Name, @{Name="SizeGB";Expression={(Get-ChildItem $_.FullName -Recurse | Measure-Object -Property Length -Sum).Sum / 1GB}}

# Keep only the 3 most recent backups, delete the rest
$backups = Get-ChildItem -Directory | Where-Object {$_.Name -like "UPDATE_*_BACKUP*"} | 
    Sort-Object LastWriteTime -Descending
$backups | Select-Object -Skip 3 | Remove-Item -Recurse -Force
```

**Expected Space Recovered:** 20-40 GB

#### **1.3 Clean Up Old Update ZIP Files**

```powershell
cd C:\CA\ESPdSeriesWAServer_R12_4

# List all update ZIP files
Get-ChildItem -File | Where-Object {$_.Name -like "*_update.zip"} | 
    Select-Object Name, @{Name="SizeMB";Expression={$_.Length / 1MB}}

# Keep only the 3 most recent, delete the rest
Get-ChildItem -File | Where-Object {$_.Name -like "*_update.zip"} | 
    Sort-Object LastWriteTime -Descending | Select-Object -Skip 3 | Remove-Item -Force
```

**Expected Space Recovered:** 10-20 GB

#### **1.4 Clean Up Temp Directory**

```powershell
cd C:\CA\ESPdSeriesWAServer_R12_4\temp

# Delete all files older than 7 days
$date = (Get-Date).AddDays(-7)
Get-ChildItem -Recurse -File | Where-Object {$_.LastWriteTime -lt $date} | Remove-Item -Force
```

**Expected Space Recovered:** 5-10 GB

#### **1.5 Database Housekeeping**

If you have database housekeeping jobs configured, run them manually:

```sql
-- Connect to PostgreSQL database
psql -h G36R0T3 -p 5432 -U postgres -d dseries

-- Check database size
SELECT pg_size_pretty(pg_database_size('dseries'));

-- Vacuum and analyze
VACUUM ANALYZE;

-- Check table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
```

**Verification:**

```powershell
# Check disk space after cleanup
Get-PSDrive C | Select-Object Used,Free,@{Name="PercentUsed";Expression={($_.Used/($_.Used+$_.Free))*100}}
```

**Target:** Reduce disk usage to below 75% (< 353 GB used)

---

### **STEP 2: Increase JVM Heap Size (CRITICAL - 15 minutes)**

**Priority:** 🔴 CRITICAL  
**Impact:** Server performance severely degraded with 1 GB heap  
**Requires:** Server restart

#### **2.1 Stop dSeries Server**

```powershell
# Stop the service
Stop-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Verify it's stopped
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Wait for all Java processes to stop
Get-Process | Where-Object {$_.ProcessName -eq "java"} | Stop-Process -Force
```

#### **2.2 Backup Current Configuration**

```powershell
# Create backup
Copy-Item "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties" `
          "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
```

#### **2.3 Update JVM Heap Settings**

**Option A: Manual Edit**

1. Open: `C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties`
2. Find these lines:
   ```
   jvmproperty_2=-Xms8M
   jvmproperty_3=-Xmx1024M
   ```
3. Change to:
   ```
   jvmproperty_2=-Xms4096M
   jvmproperty_3=-Xmx4096M
   ```
4. Save the file

**Option B: PowerShell Script** (Automated)

```powershell
$configFile = "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties"
$content = Get-Content $configFile

# Replace Xms setting
$content = $content -replace 'jvmproperty_2=-Xms\d+M', 'jvmproperty_2=-Xms4096M'

# Replace Xmx setting
$content = $content -replace 'jvmproperty_3=-Xmx\d+M', 'jvmproperty_3=-Xmx4096M'

# Save changes
$content | Set-Content $configFile

# Verify changes
Select-String -Path $configFile -Pattern "jvmproperty_[23]"
```

#### **2.4 Verify Available System Memory**

```powershell
# Check total system memory
$totalMemory = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB
Write-Host "Total System Memory: $([math]::Round($totalMemory, 2)) GB"

# Check available memory
$availableMemory = (Get-Counter '\Memory\Available MBytes').CounterSamples.CookedValue / 1024
Write-Host "Available Memory: $([math]::Round($availableMemory, 2)) GB"

if ($availableMemory -lt 5) {
    Write-Host "WARNING: Less than 5 GB available. Consider adding more RAM." -ForegroundColor Yellow
}
```

#### **2.5 Restart dSeries Server**

```powershell
# Start the service
Start-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Wait for service to start
Start-Sleep -Seconds 30

# Verify service is running
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Check Java process memory
Get-Process java | Select-Object Id, ProcessName, 
    @{Name="MemoryMB";Expression={$_.WorkingSet64 / 1MB}},
    @{Name="CPUPercent";Expression={$_.CPU}}
```

#### **2.6 Verify Heap Size**

```powershell
# Check server logs for heap size confirmation
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 100 | 
    Select-String -Pattern "heap|memory|Xmx|Xms"
```

**Expected Result:** Server should now be using 4 GB heap

---

### **STEP 3: Investigate High CPU Usage (1 hour)**

**Priority:** 🟠 HIGH  
**Impact:** Performance degradation, slow response times

#### **3.1 Identify CPU-Intensive Processes**

```powershell
# Get top CPU consumers
Get-Process | Sort-Object CPU -Descending | Select-Object -First 10 | 
    Format-Table ProcessName, Id, CPU, 
    @{Name="MemoryMB";Expression={$_.WorkingSet64 / 1MB}} -AutoSize

# Monitor Java processes specifically
Get-Process java | Format-Table Id, 
    @{Name="CPUPercent";Expression={$_.CPU}},
    @{Name="MemoryMB";Expression={$_.WorkingSet64 / 1MB}},
    @{Name="Threads";Expression={$_.Threads.Count}} -AutoSize
```

#### **3.2 Check dSeries Server Logs**

```powershell
# Check for errors in server log
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 500 | 
    Select-String -Pattern "ERROR|SEVERE|Exception|OutOfMemory"

# Check for performance issues
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 500 | 
    Select-String -Pattern "slow|timeout|queue|thread"
```

#### **3.3 Check Active Applications and Jobs**

```sql
-- Connect to database
psql -h G36R0T3 -p 5432 -U postgres -d dseries

-- Check active applications
SELECT COUNT(*) as active_applications 
FROM esp_application 
WHERE status = 'ACTIVE';

-- Check running jobs
SELECT COUNT(*) as running_jobs 
FROM esp_job 
WHERE status IN ('RUNNING', 'STARTING');

-- Check queue depth
SELECT COUNT(*) as queued_jobs 
FROM esp_job 
WHERE status = 'WAITING';
```

#### **3.4 Possible Causes and Solutions**

**Cause 1: Too Many Active Applications**
- **Threshold:** > 5000 active applications
- **Solution:** Review and deactivate unused applications
- **Command:** Check `active.appl.gen.notify` in server.properties

**Cause 2: Disk I/O Bottleneck**
- **Cause:** 98% disk usage causing excessive I/O wait
- **Solution:** Free up disk space (Step 1)
- **Verification:** Monitor disk I/O with Performance Monitor

**Cause 3: Database Performance Issues**
- **Cause:** Slow queries, missing indexes
- **Solution:** Run database maintenance (VACUUM, ANALYZE)
- **Verification:** Check PostgreSQL slow query log

**Cause 4: Thread Pool Exhaustion**
- **Cause:** Not enough threads to handle workload
- **Solution:** Adjust thread pool settings in server.properties
- **Recommended Settings (Medium workload):**
  ```properties
  # Add to server.properties
  download.threads=6
  db.update.threads=4
  selector.threads=8
  ```

#### **3.5 Enable Performance Monitoring**

```powershell
# Create performance monitoring script
$script = @'
while ($true) {
    $cpu = (Get-Counter '\Processor(_Total)\% Processor Time').CounterSamples.CookedValue
    $mem = (Get-Counter '\Memory\Available MBytes').CounterSamples.CookedValue
    $disk = (Get-Counter '\PhysicalDisk(_Total)\% Disk Time').CounterSamples.CookedValue
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp - CPU: $([math]::Round($cpu, 1))% | Memory Available: $([math]::Round($mem, 0)) MB | Disk: $([math]::Round($disk, 1))%"
    
    Start-Sleep -Seconds 60
}
'@

# Save and run in background
$script | Out-File "C:\Temp\monitor_dseries.ps1"
Start-Process powershell -ArgumentList "-File C:\Temp\monitor_dseries.ps1" -WindowStyle Minimized
```

---

### **STEP 4: Fix Server Port Accessibility (30 minutes)**

**Priority:** 🟡 MEDIUM  
**Impact:** Cannot connect to server remotely

#### **4.1 Verify Server is Running**

```powershell
# Check service status
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Check if Java process is running
Get-Process java | Where-Object {
    $_.Modules | Where-Object {$_.ModuleName -like "*esp*"}
}

# Check server logs for startup completion
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 100 | 
    Select-String -Pattern "started|listening|ready"
```

#### **4.2 Check Port Binding**

```powershell
# Check what's listening on port 7599
netstat -ano | findstr ":7599"

# If nothing, check what ports Java is using
netstat -ano | findstr "LISTENING" | findstr "java"
```

#### **4.3 Verify Server Configuration**

```powershell
# Check configured port in server.properties
Select-String -Path "C:\CA\ESPdSeriesWAServer_R12_4\conf\server.properties" -Pattern "port"

# Check if server is bound to specific interface
Select-String -Path "C:\CA\ESPdSeriesWAServer_R12_4\conf\server.properties" -Pattern "host|bind|interface"
```

#### **4.4 Check Firewall Rules**

```powershell
# Check if port 7599 is allowed
Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*7599*" -or $_.DisplayName -like "*dSeries*"}

# Add firewall rule if needed
New-NetFirewallRule -DisplayName "dSeries Server Port 7599" `
    -Direction Inbound -LocalPort 7599 -Protocol TCP -Action Allow
```

#### **4.5 Test Connectivity**

```powershell
# Test from localhost
Test-NetConnection -ComputerName localhost -Port 7599

# Test from server name
Test-NetConnection -ComputerName G36R0T3 -Port 7599

# Test with telnet
telnet localhost 7599
```

---

## 📊 Post-Remediation Verification

### **Run Health Check Again**

```powershell
cd C:\Codes\Iron_man\dseries_healthcheck
java DSeriesHealthCheckSimple
```

**Expected Results:**
- Overall Score: > 75 (GOOD or better)
- Disk Usage: < 75%
- JVM Heap: 4096 MB
- CPU Usage: < 70%
- Server Port: Accessible

### **Verification Checklist**

- [ ] Disk usage reduced to < 75%
- [ ] JVM heap increased to 4096 MB
- [ ] Server restarted successfully
- [ ] Server port 7599 is accessible
- [ ] CPU usage reduced to < 85%
- [ ] No errors in server logs
- [ ] Applications running normally
- [ ] Jobs executing successfully

---

## 📅 Ongoing Maintenance Plan

### **Daily Tasks**

```powershell
# Monitor disk space
Get-PSDrive C | Select-Object @{Name="PercentUsed";Expression={($_.Used/($_.Used+$_.Free))*100}}

# Check service status
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Quick log check
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 50 | 
    Select-String -Pattern "ERROR|SEVERE"
```

### **Weekly Tasks**

```powershell
# Run health check
cd C:\Codes\Iron_man\dseries_healthcheck
java DSeriesHealthCheckSimple

# Archive old logs
# Clean temp files
# Review performance metrics
```

### **Monthly Tasks**

```powershell
# Database maintenance
psql -h G36R0T3 -p 5432 -U postgres -d dseries -c "VACUUM ANALYZE;"

# Review and clean old backups
# Update documentation
# Capacity planning review
```

---

## 🔧 Automated Cleanup Script

See: `cleanup_dseries.ps1` (created separately)

---

## 📞 Support Contacts

**If Issues Persist:**
- Broadcom Support: https://support.broadcom.com
- Internal Support: dseries-support@company.com
- Emergency: 1-800-DSERIES

---

## 📝 Change Log

| Date | Action | Result | By |
|------|--------|--------|-----|
| 2026-02-11 | Initial health check | Score: 28/100 | Health Check Tool |
| 2026-02-11 | Remediation plan created | Pending | System |
| | | | |

---

**Next Review Date:** After remediation completion  
**Estimated Completion:** 2-3 hours  
**Priority:** URGENT

---

**Document Version:** 1.0  
**Created:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
