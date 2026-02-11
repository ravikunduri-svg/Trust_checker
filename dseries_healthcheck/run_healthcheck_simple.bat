@echo off
REM ============================================================================
REM ESP dSeries Health Check - Simple Runner
REM Version: 1.0.0
REM ============================================================================

setlocal

REM Check if installation directory is provided
if "%~1"=="" (
    echo.
    echo ERROR: Installation directory not provided
    echo.
    echo Usage: run_healthcheck_simple.bat [installation_directory]
    echo.
    echo Example:
    echo   run_healthcheck_simple.bat C:\CA\ESPdSeriesWAServer_R12_4
    echo.
    exit /b 1
)

set INSTALL_DIR=%~1

REM Check if directory exists
if not exist "%INSTALL_DIR%" (
    echo.
    echo ERROR: Installation directory not found: %INSTALL_DIR%
    echo.
    exit /b 1
)

REM Get script directory
set SCRIPT_DIR=%~dp0

REM Check if compiled class exists
if not exist "%SCRIPT_DIR%DSeriesHealthCheckSimple.class" (
    echo.
    echo Compiling health check tool...
    javac "%SCRIPT_DIR%DSeriesHealthCheckSimple.java"
    if errorlevel 1 (
        echo ERROR: Compilation failed
        exit /b 1
    )
    echo Compilation successful
    echo.
)

REM Run health check
echo.
echo Running health check on: %INSTALL_DIR%
echo.

cd "%SCRIPT_DIR%"
java DSeriesHealthCheckSimple "%INSTALL_DIR%"

set EXIT_CODE=%ERRORLEVEL%

REM Generate timestamp for report
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

echo.
echo ============================================================================
echo Health check completed
echo Exit code: %EXIT_CODE%
echo.
echo Reports generated:
echo   - HEALTH_CHECK_RESULTS_%TIMESTAMP%.md
echo   - ACTION_PLAN_%TIMESTAMP%.md
echo   - EXECUTIVE_SUMMARY_%TIMESTAMP%.txt
echo.
echo Location: %SCRIPT_DIR%
echo ============================================================================
echo.

exit /b %EXIT_CODE%
