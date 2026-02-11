# ESP dSeries Server - JVM Heap Configuration Fix
# Version: 1.0.0
# Date: 2026-02-11
# Purpose: Automatically fix JVM heap size to recommended 4GB

param(
    [switch]$DryRun = $false,
    [int]$HeapSizeMB = 4096,
    [switch]$Force = $false
)

$dSeriesHome = "C:\CA\ESPdSeriesWAServer_R12_4"
$configFile = Join-Path $dSeriesHome "conf\windows.service.properties"
$serviceName = "ESP_dSeries_Workload_Automation_7599"
$logFile = "C:\Temp\fix_jvm_heap_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

# Logging function
function Write-Log {
    param($Message, $Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "$timestamp [$Level] $Message"
    Write-Host $logMessage
    Add-Content -Path $logFile -Value $logMessage
}

Write-Log "=========================================="
Write-Log "dSeries JVM Heap Configuration Fix"
Write-Log "=========================================="
Write-Log "Mode: $(if ($DryRun) {'DRY RUN (no changes)'} else {'LIVE (will make changes)'})"
Write-Log "Target Heap Size: $HeapSizeMB MB"
Write-Log "Config File: $configFile"
Write-Log ""

# Check if config file exists
if (-not (Test-Path $configFile)) {
    Write-Log "ERROR: Configuration file not found: $configFile" "ERROR"
    exit 1
}

# Check current system memory
$totalMemoryGB = [math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 2)
$availableMemoryGB = [math]::Round((Get-Counter '\Memory\Available MBytes').CounterSamples.CookedValue / 1024, 2)

Write-Log "System Memory Check:"
Write-Log "  Total Memory: $totalMemoryGB GB"
Write-Log "  Available Memory: $availableMemoryGB GB"
Write-Log "  Required for $HeapSizeMB MB heap: $([math]::Round($HeapSizeMB / 1024 + 2, 2)) GB (heap + 2GB for OS)"
Write-Log ""

# Verify sufficient memory
$requiredMemoryGB = [math]::Round($HeapSizeMB / 1024 + 2, 2)
if ($totalMemoryGB -lt $requiredMemoryGB) {
    Write-Log "WARNING: System has only $totalMemoryGB GB total memory" "WARN"
    Write-Log "         Recommended: $requiredMemoryGB GB for $HeapSizeMB MB heap" "WARN"
    Write-Log "         Consider using a smaller heap size or adding more RAM" "WARN"
    
    if (-not $Force) {
        $response = Read-Host "Continue anyway? (y/n)"
        if ($response -ne 'y') {
            Write-Log "Aborted by user"
            exit 1
        }
    }
}

# Read current configuration
Write-Log "Reading current configuration..."
$content = Get-Content $configFile
$currentXms = ""
$currentXmx = ""

foreach ($line in $content) {
    if ($line -match 'jvmproperty_2=-Xms(\d+)M') {
        $currentXms = $matches[1]
    }
    if ($line -match 'jvmproperty_3=-Xmx(\d+)M') {
        $currentXmx = $matches[1]
    }
}

Write-Log "Current Configuration:"
Write-Log "  Initial Heap (-Xms): $currentXms MB"
Write-Log "  Maximum Heap (-Xmx): $currentXmx MB"
Write-Log ""

if ($currentXmx -eq $HeapSizeMB) {
    Write-Log "Configuration already correct! No changes needed."
    exit 0
}

Write-Log "Proposed Changes:"
Write-Log "  Initial Heap (-Xms): $currentXms MB -> $HeapSizeMB MB"
Write-Log "  Maximum Heap (-Xmx): $currentXmx MB -> $HeapSizeMB MB"
Write-Log ""

if ($DryRun) {
    Write-Log "[DRY RUN] Would make the following changes:"
    Write-Log "  1. Stop service: $serviceName"
    Write-Log "  2. Backup config file"
    Write-Log "  3. Update jvmproperty_2=-Xms${HeapSizeMB}M"
    Write-Log "  4. Update jvmproperty_3=-Xmx${HeapSizeMB}M"
    Write-Log "  5. Restart service"
    Write-Log ""
    Write-Log "Run without -DryRun to apply changes"
    exit 0
}

# Confirm with user unless Force is specified
if (-not $Force) {
    Write-Log "This will:"
    Write-Log "  1. Stop the dSeries server service"
    Write-Log "  2. Update JVM heap configuration"
    Write-Log "  3. Restart the service"
    Write-Log ""
    $response = Read-Host "Continue? (y/n)"
    if ($response -ne 'y') {
        Write-Log "Aborted by user"
        exit 1
    }
}

# ============================================================================
# STEP 1: Stop dSeries Service
# ============================================================================

Write-Log "STEP 1: Stopping dSeries service..."
Write-Log "------------------------------------"

try {
    $service = Get-Service -Name $serviceName -ErrorAction Stop
    
    if ($service.Status -eq 'Running') {
        Write-Log "Service is running, stopping..."
        Stop-Service -Name $serviceName -Force
        
        # Wait for service to stop
        $timeout = 60
        $elapsed = 0
        while ((Get-Service -Name $serviceName).Status -ne 'Stopped' -and $elapsed -lt $timeout) {
            Start-Sleep -Seconds 2
            $elapsed += 2
            Write-Log "  Waiting for service to stop... ($elapsed seconds)"
        }
        
        if ((Get-Service -Name $serviceName).Status -eq 'Stopped') {
            Write-Log "Service stopped successfully"
        } else {
            Write-Log "WARNING: Service did not stop within $timeout seconds" "WARN"
        }
    } else {
        Write-Log "Service is already stopped"
    }
    
    # Wait for Java processes to stop
    Start-Sleep -Seconds 5
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        Write-Log "Waiting for Java processes to stop..."
        Start-Sleep -Seconds 10
    }
    
} catch {
    Write-Log "ERROR: Failed to stop service: $_" "ERROR"
    exit 1
}
Write-Log ""

# ============================================================================
# STEP 2: Backup Configuration File
# ============================================================================

Write-Log "STEP 2: Backing up configuration file..."
Write-Log "-----------------------------------------"

$backupFile = "$configFile.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
try {
    Copy-Item -Path $configFile -Destination $backupFile -Force
    Write-Log "Backup created: $backupFile"
} catch {
    Write-Log "ERROR: Failed to create backup: $_" "ERROR"
    Write-Log "Attempting to restart service..."
    Start-Service -Name $serviceName
    exit 1
}
Write-Log ""

# ============================================================================
# STEP 3: Update Configuration
# ============================================================================

Write-Log "STEP 3: Updating JVM heap configuration..."
Write-Log "-------------------------------------------"

try {
    # Read content
    $content = Get-Content $configFile
    
    # Update Xms (initial heap)
    $content = $content -replace 'jvmproperty_2=-Xms\d+M', "jvmproperty_2=-Xms${HeapSizeMB}M"
    
    # Update Xmx (maximum heap)
    $content = $content -replace 'jvmproperty_3=-Xmx\d+M', "jvmproperty_3=-Xmx${HeapSizeMB}M"
    
    # Save changes
    $content | Set-Content $configFile
    
    Write-Log "Configuration updated successfully"
    
    # Verify changes
    $verifyContent = Get-Content $configFile
    $newXms = ""
    $newXmx = ""
    
    foreach ($line in $verifyContent) {
        if ($line -match 'jvmproperty_2=-Xms(\d+)M') {
            $newXms = $matches[1]
        }
        if ($line -match 'jvmproperty_3=-Xmx(\d+)M') {
            $newXmx = $matches[1]
        }
    }
    
    Write-Log "Verification:"
    Write-Log "  New Initial Heap (-Xms): $newXms MB"
    Write-Log "  New Maximum Heap (-Xmx): $newXmx MB"
    
    if ($newXms -eq $HeapSizeMB -and $newXmx -eq $HeapSizeMB) {
        Write-Log "Configuration verified successfully"
    } else {
        Write-Log "ERROR: Configuration verification failed!" "ERROR"
        Write-Log "Restoring backup..."
        Copy-Item -Path $backupFile -Destination $configFile -Force
        Write-Log "Backup restored"
        Start-Service -Name $serviceName
        exit 1
    }
    
} catch {
    Write-Log "ERROR: Failed to update configuration: $_" "ERROR"
    Write-Log "Restoring backup..."
    Copy-Item -Path $backupFile -Destination $configFile -Force
    Write-Log "Backup restored"
    Start-Service -Name $serviceName
    exit 1
}
Write-Log ""

# ============================================================================
# STEP 4: Restart dSeries Service
# ============================================================================

Write-Log "STEP 4: Restarting dSeries service..."
Write-Log "--------------------------------------"

try {
    Start-Service -Name $serviceName
    
    # Wait for service to start
    $timeout = 120
    $elapsed = 0
    while ((Get-Service -Name $serviceName).Status -ne 'Running' -and $elapsed -lt $timeout) {
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Log "  Waiting for service to start... ($elapsed seconds)"
    }
    
    if ((Get-Service -Name $serviceName).Status -eq 'Running') {
        Write-Log "Service started successfully"
    } else {
        Write-Log "WARNING: Service did not start within $timeout seconds" "WARN"
        Write-Log "Check server logs for errors: $dSeriesHome\logs\server.log" "WARN"
    }
    
} catch {
    Write-Log "ERROR: Failed to start service: $_" "ERROR"
    Write-Log "Service may need to be started manually" "ERROR"
    exit 1
}
Write-Log ""

# ============================================================================
# STEP 5: Verify Service and Memory
# ============================================================================

Write-Log "STEP 5: Verifying service and memory..."
Write-Log "----------------------------------------"

# Wait for server to fully initialize
Write-Log "Waiting 30 seconds for server to initialize..."
Start-Sleep -Seconds 30

# Check service status
$service = Get-Service -Name $serviceName
Write-Log "Service Status: $($service.Status)"

# Check Java process memory
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue | 
    Select-Object Id, @{Name="MemoryMB";Expression={[math]::Round($_.WorkingSet64 / 1MB, 0)}}

if ($javaProcesses) {
    Write-Log "Java Processes:"
    foreach ($proc in $javaProcesses) {
        Write-Log "  PID $($proc.Id): $($proc.MemoryMB) MB"
    }
} else {
    Write-Log "WARNING: No Java processes found" "WARN"
}
Write-Log ""

# Check server log for heap size confirmation
$serverLog = Join-Path $dSeriesHome "logs\server.log"
if (Test-Path $serverLog) {
    Write-Log "Checking server log for heap size confirmation..."
    $recentLog = Get-Content $serverLog -Tail 200 | Select-String -Pattern "heap|Xmx|Xms|memory" | Select-Object -First 5
    if ($recentLog) {
        foreach ($line in $recentLog) {
            Write-Log "  $line"
        }
    }
}
Write-Log ""

# ============================================================================
# Summary
# ============================================================================

Write-Log "=========================================="
Write-Log "JVM Heap Configuration Fix - Summary"
Write-Log "=========================================="
Write-Log ""
Write-Log "Changes Applied:"
Write-Log "  Initial Heap: $currentXms MB -> $HeapSizeMB MB"
Write-Log "  Maximum Heap: $currentXmx MB -> $HeapSizeMB MB"
Write-Log ""
Write-Log "Backup Location: $backupFile"
Write-Log "Log File: $logFile"
Write-Log ""

if ($service.Status -eq 'Running') {
    Write-Log "SUCCESS: Service is running with new configuration"
    Write-Log ""
    Write-Log "Next Steps:"
    Write-Log "  1. Monitor server logs for any issues"
    Write-Log "  2. Run health check again to verify: java DSeriesHealthCheckSimple"
    Write-Log "  3. Monitor Java process memory usage"
    Write-Log "  4. Check application performance"
    exit 0
} else {
    Write-Log "WARNING: Service is not running" "WARN"
    Write-Log "Please check server logs: $dSeriesHome\logs\server.log"
    Write-Log "You may need to start the service manually"
    exit 1
}
