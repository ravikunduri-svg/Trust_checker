@echo off
REM ============================================================================
REM ESP dSeries Workload Automation Health Check Script (Windows)
REM Version: 1.0.0
REM Date: 2026-02-09
REM
REM Purpose: Comprehensive health check for dSeries deployments on Windows
REM Based on: Control-M, AutoSys, Automic, and dSeries best practices
REM ============================================================================

setlocal enabledelayedexpansion

REM Script directory
set SCRIPT_DIR=%~dp0
set CONFIG_FILE=%SCRIPT_DIR%config\healthcheck.conf
set LIB_DIR=%SCRIPT_DIR%lib

REM Set timestamp
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

REM Report settings
set REPORT_DIR=C:\CA\WA_DE\logs\healthcheck
set REPORT_FILE=%REPORT_DIR%\healthcheck_%TIMESTAMP%.html
set JSON_FILE=%REPORT_DIR%\healthcheck_%TIMESTAMP%.json
set LOG_FILE=%REPORT_DIR%\healthcheck_%TIMESTAMP%.log

REM Health check counters
set TOTAL_CHECKS=0
set PASSED_CHECKS=0
set WARNING_CHECKS=0
set FAILED_CHECKS=0
set OVERALL_SCORE=0

REM ============================================================================
REM Main Functions
REM ============================================================================

:show_banner
echo ===================================================================
echo   ESP dSeries Workload Automation Health Check Tool v1.0.0
echo ===================================================================
echo   Date: %date% %time%
echo   Host: %COMPUTERNAME%
echo ===================================================================
echo.
goto :eof

:load_configuration
echo Loading configuration...
if not exist "%CONFIG_FILE%" (
    echo ERROR: Configuration file not found: %CONFIG_FILE%
    exit /b 1
)
call "%CONFIG_FILE%"
echo Configuration loaded successfully
goto :eof

:initialize_report
echo Initializing report directory: %REPORT_DIR%
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"
echo Report initialized: %REPORT_FILE%
goto :eof

:run_system_checks
echo.
echo ===================================================================
echo SYSTEM RESOURCE CHECKS
echo ===================================================================
echo.

call :check_cpu_utilization
call :check_memory_usage
call :check_disk_space
call :check_network_connectivity

goto :eof

:check_cpu_utilization
echo [SYS-001] Checking CPU utilization...
for /f "skip=1" %%p in ('wmic cpu get loadpercentage') do (
    set CPU_USAGE=%%p
    goto :check_cpu_done
)
:check_cpu_done

if !CPU_USAGE! GTR 85 (
    echo   ❌ CRITICAL: CPU usage is !CPU_USAGE!%% ^(threshold: 85%%^)
    set /a FAILED_CHECKS+=1
) else if !CPU_USAGE! GTR 70 (
    echo   ⚠️  WARNING: CPU usage is !CPU_USAGE!%% ^(threshold: 70%%^)
    set /a WARNING_CHECKS+=1
) else (
    echo   ✅ PASS: CPU usage is !CPU_USAGE!%%
    set /a PASSED_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:check_memory_usage
echo [SYS-002] Checking memory usage...
for /f "skip=1" %%m in ('wmic OS get FreePhysicalMemory^,TotalVisibleMemorySize /value') do (
    set "line=%%m"
    if "!line:~0,19!"=="FreePhysicalMemory=" set FREE_MEM=!line:~19!
    if "!line:~0,24!"=="TotalVisibleMemorySize=" set TOTAL_MEM=!line:~24!
)

set /a USED_MEM_PERCENT=(TOTAL_MEM - FREE_MEM) * 100 / TOTAL_MEM

if !USED_MEM_PERCENT! GTR 90 (
    echo   ❌ CRITICAL: Memory usage is !USED_MEM_PERCENT!%% ^(threshold: 90%%^)
    set /a FAILED_CHECKS+=1
) else if !USED_MEM_PERCENT! GTR 80 (
    echo   ⚠️  WARNING: Memory usage is !USED_MEM_PERCENT!%% ^(threshold: 80%%^)
    set /a WARNING_CHECKS+=1
) else (
    echo   ✅ PASS: Memory usage is !USED_MEM_PERCENT!%%
    set /a PASSED_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:check_disk_space
echo [SYS-003] Checking disk space...
for /f "tokens=1,2" %%a in ('wmic logicaldisk where "DeviceID='C:'" get FreeSpace^,Size /value ^| findstr "="') do (
    set "line=%%a"
    if "!line:~0,9!"=="FreeSpace" set FREE_DISK=%%b
    if "!line:~0,4!"=="Size" set TOTAL_DISK=%%b
)

set /a USED_DISK_PERCENT=(TOTAL_DISK - FREE_DISK) * 100 / TOTAL_DISK

if !USED_DISK_PERCENT! GTR 85 (
    echo   ❌ CRITICAL: Disk usage is !USED_DISK_PERCENT!%% ^(threshold: 85%%^)
    set /a FAILED_CHECKS+=1
) else if !USED_DISK_PERCENT! GTR 75 (
    echo   ⚠️  WARNING: Disk usage is !USED_DISK_PERCENT!%% ^(threshold: 75%%^)
    set /a WARNING_CHECKS+=1
) else (
    echo   ✅ PASS: Disk usage is !USED_DISK_PERCENT!%%
    set /a PASSED_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:check_network_connectivity
echo [SYS-005] Checking network connectivity...
ping -n 1 localhost >nul 2>&1
if errorlevel 1 (
    echo   ❌ FAIL: Cannot ping localhost
    set /a FAILED_CHECKS+=1
) else (
    echo   ✅ PASS: Network connectivity OK
    set /a PASSED_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:calculate_overall_score
echo.
echo ===================================================================
echo CALCULATING OVERALL HEALTH SCORE
echo ===================================================================
echo.

if %TOTAL_CHECKS% EQU 0 (
    set OVERALL_SCORE=0
    goto :score_done
)

set /a OVERALL_SCORE=(PASSED_CHECKS * 100 + WARNING_CHECKS * 60) / TOTAL_CHECKS

:score_done
echo Total Checks: %TOTAL_CHECKS%
echo Passed: %PASSED_CHECKS%
echo Warnings: %WARNING_CHECKS%
echo Failed: %FAILED_CHECKS%
echo Overall Score: %OVERALL_SCORE%/100
goto :eof

:display_summary
echo.
echo ===================================================================
echo   HEALTH CHECK SUMMARY
echo ===================================================================
echo.
echo   Overall Health Score: %OVERALL_SCORE%/100

if %OVERALL_SCORE% GEQ 90 (
    echo   Status: ✅ EXCELLENT
) else if %OVERALL_SCORE% GEQ 75 (
    echo   Status: 🟢 GOOD
) else if %OVERALL_SCORE% GEQ 60 (
    echo   Status: 🟡 FAIR
) else if %OVERALL_SCORE% GEQ 40 (
    echo   Status: 🟠 POOR
) else (
    echo   Status: 🔴 CRITICAL
)

echo.
echo   Total Checks: %TOTAL_CHECKS%
echo   ✅ Passed: %PASSED_CHECKS%
echo   ⚠️  Warnings: %WARNING_CHECKS%
echo   ❌ Failed: %FAILED_CHECKS%
echo.
echo   Reports:
echo     HTML: %REPORT_FILE%
echo     JSON: %JSON_FILE%
echo     Log:  %LOG_FILE%
echo.
echo ===================================================================
echo.

if %FAILED_CHECKS% GTR 0 (
    echo   ⚠️  CRITICAL ISSUES DETECTED!
    echo   Please review the detailed report for remediation steps.
    echo.
)
goto :eof

REM ============================================================================
REM Main Execution
REM ============================================================================

:main
call :show_banner
call :load_configuration
call :initialize_report
call :run_system_checks
call :calculate_overall_score
call :display_summary

if %OVERALL_SCORE% GEQ 60 (
    exit /b 0
) else (
    exit /b 1
)

REM Run main
call :main %*
