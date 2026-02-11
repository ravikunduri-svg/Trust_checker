# ESP dSeries Server Cleanup Script
# Version: 1.0.0
# Date: 2026-02-11
# Purpose: Automated cleanup of dSeries server to free up disk space

param(
    [switch]$DryRun = $false,
    [switch]$Force = $false,
    [int]$LogRetentionDays = 30,
    [int]$BackupsToKeep = 3,
    [int]$UpdateZipsToKeep = 3
)

# Configuration
$dSeriesHome = "C:\CA\ESPdSeriesWAServer_R12_4"
$backupLocation = "C:\Backup\dSeries"
$logFile = "C:\Temp\dseries_cleanup_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

# Create backup location if it doesn't exist
if (-not (Test-Path $backupLocation)) {
    New-Item -ItemType Directory -Path $backupLocation -Force | Out-Null
}

# Logging function
function Write-Log {
    param($Message, $Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "$timestamp [$Level] $Message"
    Write-Host $logMessage
    Add-Content -Path $logFile -Value $logMessage
}

Write-Log "=========================================="
Write-Log "dSeries Server Cleanup Script Started"
Write-Log "=========================================="
Write-Log "Mode: $(if ($DryRun) {'DRY RUN (no changes will be made)'} else {'LIVE (changes will be made)'})"
Write-Log "dSeries Home: $dSeriesHome"
Write-Log ""

# Check if dSeries directory exists
if (-not (Test-Path $dSeriesHome)) {
    Write-Log "ERROR: dSeries directory not found: $dSeriesHome" "ERROR"
    exit 1
}

# Get initial disk space
$initialDisk = Get-PSDrive C
$initialUsedGB = [math]::Round($initialDisk.Used / 1GB, 2)
$initialFreeGB = [math]::Round($initialDisk.Free / 1GB, 2)
$initialPercentUsed = [math]::Round(($initialDisk.Used / ($initialDisk.Used + $initialDisk.Free)) * 100, 2)

Write-Log "Initial Disk Space:"
Write-Log "  Used: $initialUsedGB GB"
Write-Log "  Free: $initialFreeGB GB"
Write-Log "  Percent Used: $initialPercentUsed%"
Write-Log ""

$totalSpaceFreed = 0

# ============================================================================
# STEP 1: Clean up old log files
# ============================================================================

Write-Log "STEP 1: Cleaning up old log files (older than $LogRetentionDays days)"
Write-Log "----------------------------------------------------------------------"

$logsDir = Join-Path $dSeriesHome "logs"
if (Test-Path $logsDir) {
    $cutoffDate = (Get-Date).AddDays(-$LogRetentionDays)
    $oldLogs = Get-ChildItem -Path $logsDir -Recurse -File | Where-Object {$_.LastWriteTime -lt $cutoffDate}
    
    if ($oldLogs) {
        $logSize = ($oldLogs | Measure-Object -Property Length -Sum).Sum
        $logSizeMB = [math]::Round($logSize / 1MB, 2)
        
        Write-Log "Found $($oldLogs.Count) old log files totaling $logSizeMB MB"
        
        if (-not $DryRun) {
            # Archive logs before deleting
            $archiveName = "dSeries_logs_archive_$(Get-Date -Format 'yyyyMMdd').zip"
            $archivePath = Join-Path $backupLocation $archiveName
            
            Write-Log "Archiving logs to: $archivePath"
            try {
                $oldLogs | Compress-Archive -DestinationPath $archivePath -Update
                Write-Log "Archive created successfully"
                
                # Delete archived logs
                $oldLogs | Remove-Item -Force
                Write-Log "Deleted $($oldLogs.Count) old log files"
                $totalSpaceFreed += $logSize
            } catch {
                Write-Log "ERROR: Failed to archive/delete logs: $_" "ERROR"
            }
        } else {
            Write-Log "[DRY RUN] Would archive and delete $($oldLogs.Count) files ($logSizeMB MB)"
        }
    } else {
        Write-Log "No old log files found"
    }
} else {
    Write-Log "WARNING: Logs directory not found" "WARN"
}
Write-Log ""

# ============================================================================
# STEP 2: Clean up old update backups
# ============================================================================

Write-Log "STEP 2: Cleaning up old update backups (keeping $BackupsToKeep most recent)"
Write-Log "------------------------------------------------------------------------------"

$backupDirs = Get-ChildItem -Path $dSeriesHome -Directory | 
    Where-Object {$_.Name -like "UPDATE_*_BACKUP*"} | 
    Sort-Object LastWriteTime -Descending

if ($backupDirs) {
    Write-Log "Found $($backupDirs.Count) backup directories"
    
    # Calculate size of backups to delete
    $backupsToDelete = $backupDirs | Select-Object -Skip $BackupsToKeep
    
    if ($backupsToDelete) {
        foreach ($backup in $backupsToDelete) {
            $backupSize = (Get-ChildItem -Path $backup.FullName -Recurse -File | 
                Measure-Object -Property Length -Sum).Sum
            $backupSizeMB = [math]::Round($backupSize / 1MB, 2)
            
            Write-Log "  $($backup.Name): $backupSizeMB MB (Last Modified: $($backup.LastWriteTime))"
            
            if (-not $DryRun) {
                if ($Force -or (Read-Host "Delete $($backup.Name)? (y/n)") -eq 'y') {
                    try {
                        Remove-Item -Path $backup.FullName -Recurse -Force
                        Write-Log "    Deleted successfully"
                        $totalSpaceFreed += $backupSize
                    } catch {
                        Write-Log "    ERROR: Failed to delete: $_" "ERROR"
                    }
                } else {
                    Write-Log "    Skipped by user"
                }
            } else {
                Write-Log "    [DRY RUN] Would delete this backup"
            }
        }
    } else {
        Write-Log "All backups are within retention policy ($BackupsToKeep to keep)"
    }
} else {
    Write-Log "No backup directories found"
}
Write-Log ""

# ============================================================================
# STEP 3: Clean up old update ZIP files
# ============================================================================

Write-Log "STEP 3: Cleaning up old update ZIP files (keeping $UpdateZipsToKeep most recent)"
Write-Log "---------------------------------------------------------------------------------"

$updateZips = Get-ChildItem -Path $dSeriesHome -File | 
    Where-Object {$_.Name -like "*_update.zip"} | 
    Sort-Object LastWriteTime -Descending

if ($updateZips) {
    Write-Log "Found $($updateZips.Count) update ZIP files"
    
    $zipsToDelete = $updateZips | Select-Object -Skip $UpdateZipsToKeep
    
    if ($zipsToDelete) {
        foreach ($zip in $zipsToDelete) {
            $zipSizeMB = [math]::Round($zip.Length / 1MB, 2)
            
            Write-Log "  $($zip.Name): $zipSizeMB MB (Last Modified: $($zip.LastWriteTime))"
            
            if (-not $DryRun) {
                if ($Force -or (Read-Host "Delete $($zip.Name)? (y/n)") -eq 'y') {
                    try {
                        Remove-Item -Path $zip.FullName -Force
                        Write-Log "    Deleted successfully"
                        $totalSpaceFreed += $zip.Length
                    } catch {
                        Write-Log "    ERROR: Failed to delete: $_" "ERROR"
                    }
                } else {
                    Write-Log "    Skipped by user"
                }
            } else {
                Write-Log "    [DRY RUN] Would delete this file"
            }
        }
    } else {
        Write-Log "All update ZIPs are within retention policy ($UpdateZipsToKeep to keep)"
    }
} else {
    Write-Log "No update ZIP files found"
}
Write-Log ""

# ============================================================================
# STEP 4: Clean up temp directory
# ============================================================================

Write-Log "STEP 4: Cleaning up temp directory (files older than 7 days)"
Write-Log "-------------------------------------------------------------"

$tempDir = Join-Path $dSeriesHome "temp"
if (Test-Path $tempDir) {
    $cutoffDate = (Get-Date).AddDays(-7)
    $tempFiles = Get-ChildItem -Path $tempDir -Recurse -File | Where-Object {$_.LastWriteTime -lt $cutoffDate}
    
    if ($tempFiles) {
        $tempSize = ($tempFiles | Measure-Object -Property Length -Sum).Sum
        $tempSizeMB = [math]::Round($tempSize / 1MB, 2)
        
        Write-Log "Found $($tempFiles.Count) old temp files totaling $tempSizeMB MB"
        
        if (-not $DryRun) {
            try {
                $tempFiles | Remove-Item -Force
                Write-Log "Deleted $($tempFiles.Count) temp files"
                $totalSpaceFreed += $tempSize
            } catch {
                Write-Log "ERROR: Failed to delete temp files: $_" "ERROR"
            }
        } else {
            Write-Log "[DRY RUN] Would delete $($tempFiles.Count) files ($tempSizeMB MB)"
        }
    } else {
        Write-Log "No old temp files found"
    }
} else {
    Write-Log "WARNING: Temp directory not found" "WARN"
}
Write-Log ""

# ============================================================================
# STEP 5: Analyze large files
# ============================================================================

Write-Log "STEP 5: Analyzing large files (> 100 MB)"
Write-Log "-----------------------------------------"

$largeFiles = Get-ChildItem -Path $dSeriesHome -Recurse -File -ErrorAction SilentlyContinue | 
    Where-Object {$_.Length -gt 100MB} | 
    Sort-Object Length -Descending | 
    Select-Object -First 20

if ($largeFiles) {
    Write-Log "Top 20 largest files:"
    foreach ($file in $largeFiles) {
        $fileSizeMB = [math]::Round($file.Length / 1MB, 2)
        $relativePath = $file.FullName.Replace($dSeriesHome, ".")
        Write-Log "  $fileSizeMB MB - $relativePath"
    }
    Write-Log ""
    Write-Log "Review these files manually to determine if they can be archived or deleted"
} else {
    Write-Log "No files larger than 100 MB found"
}
Write-Log ""

# ============================================================================
# Summary
# ============================================================================

Write-Log "=========================================="
Write-Log "Cleanup Summary"
Write-Log "=========================================="

$spaceFreedGB = [math]::Round($totalSpaceFreed / 1GB, 2)
Write-Log "Total space freed: $spaceFreedGB GB"

if (-not $DryRun) {
    # Get final disk space
    $finalDisk = Get-PSDrive C
    $finalUsedGB = [math]::Round($finalDisk.Used / 1GB, 2)
    $finalFreeGB = [math]::Round($finalDisk.Free / 1GB, 2)
    $finalPercentUsed = [math]::Round(($finalDisk.Used / ($finalDisk.Used + $finalDisk.Free)) * 100, 2)
    
    Write-Log ""
    Write-Log "Final Disk Space:"
    Write-Log "  Used: $finalUsedGB GB (was $initialUsedGB GB)"
    Write-Log "  Free: $finalFreeGB GB (was $initialFreeGB GB)"
    Write-Log "  Percent Used: $finalPercentUsed% (was $initialPercentUsed%)"
    Write-Log ""
    
    $improvement = $initialPercentUsed - $finalPercentUsed
    Write-Log "Improvement: $([math]::Round($improvement, 2))% reduction in disk usage"
} else {
    Write-Log ""
    Write-Log "This was a DRY RUN - no changes were made"
    Write-Log "Run without -DryRun parameter to actually perform cleanup"
}

Write-Log ""
Write-Log "Log file saved to: $logFile"
Write-Log "Backup location: $backupLocation"
Write-Log ""
Write-Log "Cleanup script completed"
Write-Log "=========================================="

# Return exit code based on final disk usage
if (-not $DryRun) {
    if ($finalPercentUsed -gt 85) {
        Write-Log "WARNING: Disk usage still critical (>85%)" "WARN"
        exit 1
    } elseif ($finalPercentUsed -gt 75) {
        Write-Log "WARNING: Disk usage still high (>75%)" "WARN"
        exit 0
    } else {
        Write-Log "SUCCESS: Disk usage is now healthy (<75%)" "INFO"
        exit 0
    }
}
