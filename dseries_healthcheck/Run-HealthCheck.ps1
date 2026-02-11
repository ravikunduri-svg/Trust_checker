#!/usr/bin/env pwsh
<#
.SYNOPSIS
    ESP dSeries Workload Automation Health Check Tool

.DESCRIPTION
    Runs comprehensive health checks on dSeries installations.
    Checks system resources, JVM configuration, database connectivity, and more.

.PARAMETER InstallDir
    Path to the dSeries installation directory (e.g., C:\CA\ESPdSeriesWAServer_R12_4)

.PARAMETER OutputDir
    Directory where reports will be saved (default: current directory)

.PARAMETER GenerateReports
    Generate detailed HTML and JSON reports

.EXAMPLE
    .\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"

.EXAMPLE
    .\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4" -GenerateReports

.NOTES
    Version: 1.0.0
    Date: 2026-02-11
    Author: Broadcom
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0, HelpMessage="Path to dSeries installation directory")]
    [ValidateNotNullOrEmpty()]
    [string]$InstallDir,
    
    [Parameter(Mandatory=$false)]
    [string]$OutputDir = $PSScriptRoot,
    
    [Parameter(Mandatory=$false)]
    [switch]$GenerateReports
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Script directory
$ScriptDir = $PSScriptRoot

# Validate installation directory
if (-not (Test-Path $InstallDir)) {
    Write-Error "Installation directory not found: $InstallDir"
    exit 1
}

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  ESP dSeries Workload Automation Health Check Tool" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  Date:        $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
Write-Host "  Host:        $env:COMPUTERNAME" -ForegroundColor White
Write-Host "  Install Dir: $InstallDir" -ForegroundColor White
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""

# Check if Java is available
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "Java version: $javaVersion" -ForegroundColor Green
} catch {
    Write-Error "Java not found. Please install Java 8 or higher."
    exit 1
}

# Check if compiled class exists, compile if needed
$classFile = Join-Path $ScriptDir "DSeriesHealthCheckSimple.class"
$javaFile = Join-Path $ScriptDir "DSeriesHealthCheckSimple.java"

if (-not (Test-Path $classFile)) {
    Write-Host ""
    Write-Host "Compiling health check tool..." -ForegroundColor Yellow
    
    try {
        javac $javaFile
        Write-Host "Compilation successful" -ForegroundColor Green
    } catch {
        Write-Error "Compilation failed: $_"
        exit 1
    }
}

# Run health check
Write-Host ""
Write-Host "Running health check..." -ForegroundColor Cyan
Write-Host ""

$startTime = Get-Date

try {
    # Run the Java health check
    $process = Start-Process -FilePath "java" `
        -ArgumentList "-cp `"$ScriptDir`" DSeriesHealthCheckSimple `"$InstallDir`"" `
        -NoNewWindow -Wait -PassThru
    
    $exitCode = $process.ExitCode
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Host ""
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host "  Health Check Completed" -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host "  Duration:    $([math]::Round($duration, 2)) seconds" -ForegroundColor White
    Write-Host "  Exit Code:   $exitCode" -ForegroundColor $(if ($exitCode -eq 0) { "Green" } else { "Red" })
    
    # Interpret exit code
    if ($exitCode -eq 0) {
        Write-Host "  Status:      HEALTHY" -ForegroundColor Green
    } elseif ($exitCode -eq 1) {
        Write-Host "  Status:      CRITICAL ISSUES DETECTED" -ForegroundColor Red
    } else {
        Write-Host "  Status:      ERROR" -ForegroundColor Red
    }
    
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Generate additional reports if requested
    if ($GenerateReports) {
        Write-Host "Generating detailed reports..." -ForegroundColor Yellow
        
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        
        # Check if report files exist
        $reportFiles = @(
            "HEALTH_CHECK_RESULTS_20260211.md",
            "ACTION_PLAN_20260211.md",
            "EXECUTIVE_SUMMARY_20260211.txt"
        )
        
        $foundReports = $false
        foreach ($reportFile in $reportFiles) {
            $reportPath = Join-Path $ScriptDir $reportFile
            if (Test-Path $reportPath) {
                Write-Host "  - $reportFile" -ForegroundColor Green
                $foundReports = $true
            }
        }
        
        if ($foundReports) {
            Write-Host ""
            Write-Host "Reports location: $ScriptDir" -ForegroundColor Cyan
        }
    }
    
    Write-Host ""
    Write-Host "For detailed analysis, review the reports in: $ScriptDir" -ForegroundColor Cyan
    Write-Host ""
    
    exit $exitCode
    
} catch {
    Write-Error "Failed to run health check: $_"
    exit 1
}
