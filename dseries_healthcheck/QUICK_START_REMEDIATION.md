# Quick Start - dSeries Server Remediation

**Server:** C:\CA\ESPdSeriesWAServer_R12_4  
**Health Score:** 28/100 - 🔴 CRITICAL  
**Time Required:** 2-3 hours

---

## 🚨 Critical Issues Summary

1. **Disk Space: 98.2% full** (only 9 GB remaining)
2. **JVM Heap: 1 GB** (should be 4 GB)
3. **CPU Usage: 100%** (system at max capacity)
4. **Server Port: Not accessible**

---

## ⚡ Quick Fix (30 Minutes)

### **Step 1: Free Up Disk Space (15 minutes)**

```powershell
# Navigate to health check directory
cd C:\Codes\Iron_man\dseries_healthcheck

# Preview what will be deleted (safe - no changes)
.\cleanup_dseries.ps1 -DryRun

# Review the output, then execute cleanup
.\cleanup_dseries.ps1 -Force
```

**Expected Result:** 50-100 GB freed, disk usage < 75%

---

### **Step 2: Fix JVM Heap Size (15 minutes)**

```powershell
# Still in health check directory
cd C:\Codes\Iron_man\dseries_healthcheck

# Preview changes (safe - no changes)
.\fix_jvm_heap.ps1 -DryRun

# Apply changes (will restart server)
.\fix_jvm_heap.ps1 -Force
```

**Expected Result:** JVM heap increased to 4 GB, server restarted

---

### **Step 3: Verify Improvements (5 minutes)**

```powershell
# Wait 2 minutes for server to fully start
Start-Sleep -Seconds 120

# Run health check again
cd C:\Codes\Iron_man\dseries_healthcheck
java DSeriesHealthCheckSimple
```

**Expected Result:** Health score improved to 75-85/100

---

## 📋 Detailed Instructions

### **Option A: Automated (Recommended)**

Run both scripts in sequence:

```powershell
# Open PowerShell as Administrator
cd C:\Codes\Iron_man\dseries_healthcheck

# Step 1: Cleanup
Write-Host "Step 1: Cleaning up disk space..." -ForegroundColor Green
.\cleanup_dseries.ps1 -Force

# Step 2: Fix JVM
Write-Host "`nStep 2: Fixing JVM heap size..." -ForegroundColor Green
.\fix_jvm_heap.ps1 -Force

# Step 3: Wait for server
Write-Host "`nStep 3: Waiting for server to start..." -ForegroundColor Green
Start-Sleep -Seconds 120

# Step 4: Verify
Write-Host "`nStep 4: Running health check..." -ForegroundColor Green
java DSeriesHealthCheckSimple
```

---

### **Option B: Manual (Step-by-Step)**

#### **1. Clean Up Disk Space**

**A. Delete Old Logs**
```powershell
cd C:\CA\ESPdSeriesWAServer_R12_4\logs

# Check size
Get-ChildItem -Recurse | Measure-Object -Property Length -Sum

# Delete logs older than 30 days
$date = (Get-Date).AddDays(-30)
Get-ChildItem -Recurse -File | Where-Object {$_.LastWriteTime -lt $date} | Remove-Item -Force
```

**B. Delete Old Backups**
```powershell
cd C:\CA\ESPdSeriesWAServer_R12_4

# List backups
Get-ChildItem -Directory | Where-Object {$_.Name -like "UPDATE_*_BACKUP*"} | 
    Sort-Object LastWriteTime -Descending

# Keep 3 most recent, delete rest
Get-ChildItem -Directory | Where-Object {$_.Name -like "UPDATE_*_BACKUP*"} | 
    Sort-Object LastWriteTime -Descending | Select-Object -Skip 3 | Remove-Item -Recurse -Force
```

**C. Delete Old Update ZIPs**
```powershell
# List update ZIPs
Get-ChildItem -File | Where-Object {$_.Name -like "*_update.zip"} | 
    Sort-Object LastWriteTime -Descending

# Keep 3 most recent, delete rest
Get-ChildItem -File | Where-Object {$_.Name -like "*_update.zip"} | 
    Sort-Object LastWriteTime -Descending | Select-Object -Skip 3 | Remove-Item -Force
```

#### **2. Fix JVM Heap Size**

**A. Stop Server**
```powershell
Stop-Service -Name "ESP_dSeries_Workload_Automation_7599"
Start-Sleep -Seconds 10
```

**B. Backup Configuration**
```powershell
$config = "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties"
Copy-Item $config "$config.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
```

**C. Edit Configuration**

Open in Notepad:
```powershell
notepad C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties
```

Find and change:
```
FROM:
jvmproperty_2=-Xms8M
jvmproperty_3=-Xmx1024M

TO:
jvmproperty_2=-Xms4096M
jvmproperty_3=-Xmx4096M
```

Save and close.

**D. Restart Server**
```powershell
Start-Service -Name "ESP_dSeries_Workload_Automation_7599"
Start-Sleep -Seconds 120  # Wait 2 minutes
```

**E. Verify**
```powershell
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"
Get-Process java | Select-Object Id, @{Name="MemoryMB";Expression={$_.WorkingSet64/1MB}}
```

---

## ✅ Verification Checklist

After remediation, verify:

```powershell
# 1. Check disk space
Get-PSDrive C | Select-Object @{Name="PercentUsed";Expression={($_.Used/($_.Used+$_.Free))*100}}
# Should be < 75%

# 2. Check service status
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"
# Should be "Running"

# 3. Check JVM heap
Select-String -Path "C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties" -Pattern "Xmx"
# Should show 4096M

# 4. Check server port
Test-NetConnection -ComputerName localhost -Port 7599
# Should succeed

# 5. Run health check
cd C:\Codes\Iron_man\dseries_healthcheck
java DSeriesHealthCheckSimple
# Score should be > 75
```

---

## 🔍 Troubleshooting

### **Issue: Cleanup script fails**

```powershell
# Check permissions
whoami
# Should be Administrator

# Run PowerShell as Administrator
Start-Process powershell -Verb RunAs
```

### **Issue: Service won't start**

```powershell
# Check server logs
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 100

# Check for Java processes
Get-Process java -ErrorAction SilentlyContinue

# Kill stuck processes
Get-Process java | Stop-Process -Force

# Try starting again
Start-Service -Name "ESP_dSeries_Workload_Automation_7599"
```

### **Issue: Not enough memory for 4GB heap**

```powershell
# Check system memory
$totalMemory = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB
Write-Host "Total Memory: $([math]::Round($totalMemory, 2)) GB"

# If less than 6 GB total, use 2 GB heap instead
.\fix_jvm_heap.ps1 -HeapSizeMB 2048 -Force
```

---

## 📊 Expected Results

| Metric | Before | After | Target |
|--------|--------|-------|--------|
| Disk Usage | 98.2% | 65-70% | < 75% |
| JVM Heap | 1 GB | 4 GB | 4 GB |
| CPU Usage | 100% | 40-60% | < 70% |
| Health Score | 28/100 | 75-85/100 | > 75 |

---

## 📞 Need Help?

### **If scripts fail:**
1. Check the log files:
   - `C:\Temp\dseries_cleanup_*.log`
   - `C:\Temp\fix_jvm_heap_*.log`

2. Run health check to see current status:
   ```powershell
   cd C:\Codes\Iron_man\dseries_healthcheck
   java DSeriesHealthCheckSimple
   ```

3. Contact support with log files

### **Support Contacts:**
- **Internal:** dseries-support@company.com
- **Broadcom:** https://support.broadcom.com

---

## 📝 Quick Commands Reference

```powershell
# Navigate to health check directory
cd C:\Codes\Iron_man\dseries_healthcheck

# Run cleanup (dry run)
.\cleanup_dseries.ps1 -DryRun

# Run cleanup (execute)
.\cleanup_dseries.ps1 -Force

# Fix JVM heap (dry run)
.\fix_jvm_heap.ps1 -DryRun

# Fix JVM heap (execute)
.\fix_jvm_heap.ps1 -Force

# Run health check
java DSeriesHealthCheckSimple

# Check service
Get-Service -Name "ESP_dSeries_Workload_Automation_7599"

# Check disk space
Get-PSDrive C

# Check server logs
Get-Content "C:\CA\ESPdSeriesWAServer_R12_4\logs\server.log" -Tail 50
```

---

**Ready to start? Run the automated option above!** 🚀

**Estimated Time:** 30 minutes  
**Difficulty:** Easy (scripts do everything)  
**Risk:** Low (backups created automatically)

---

**Document Version:** 1.0  
**Created:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
