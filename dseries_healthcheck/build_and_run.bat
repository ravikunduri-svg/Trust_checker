@echo off
REM ============================================================================
REM dSeries Health Check - Build and Run Script
REM ============================================================================
REM Version: 2.0.0
REM Date: 2026-02-11
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo   dSeries Health Check - Build and Run
echo ========================================================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM Check if Java is available
echo [1/5] Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 8 or higher
    pause
    exit /b 1
)
echo   OK: Java is installed
echo.

REM Check for installation directory argument
if "%~1"=="" (
    echo ERROR: Installation directory not provided
    echo.
    echo Usage: build_and_run.bat ^<install_directory^> [db_config] [sql_config]
    echo.
    echo Examples:
    echo   build_and_run.bat C:\CA\ESPdSeriesWAServer_R12_4
    echo   build_and_run.bat C:\CA\ESPdSeriesWAServer_R12_4 config\db.properties
    echo   build_and_run.bat C:\CA\ESPdSeriesWAServer_R12_4 config\db.properties config\health_check_queries.sql
    echo.
    pause
    exit /b 1
)

set "INSTALL_DIR=%~1"
set "DB_CONFIG=%~2"
set "SQL_CONFIG=%~3"

if "%DB_CONFIG%"=="" set "DB_CONFIG=config\db.properties"
if "%SQL_CONFIG%"=="" set "SQL_CONFIG=config\health_check_queries.sql"

echo [2/5] Validating installation directory...
if not exist "%INSTALL_DIR%" (
    echo ERROR: Installation directory not found: %INSTALL_DIR%
    pause
    exit /b 1
)
echo   OK: Installation directory found
echo.

echo [3/5] Compiling Java source...
if exist DSeriesHealthCheck.class del DSeriesHealthCheck.class
javac DSeriesHealthCheck.java
if errorlevel 1 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)
echo   OK: Compilation successful
echo.

echo [4/5] Checking configuration files...
if not exist "%SQL_CONFIG%" (
    echo WARNING: SQL configuration file not found: %SQL_CONFIG%
    echo Database checks will be limited
    echo.
)

if not exist "%DB_CONFIG%" (
    echo WARNING: Database configuration file not found: %DB_CONFIG%
    echo Database checks will be skipped
    echo.
    echo To enable database checks:
    echo   1. Copy config\db.properties.template to config\db.properties
    echo   2. Update with your database connection details
    echo   3. Add PostgreSQL JDBC driver to classpath
    echo.
)
echo.

echo [5/5] Running health check...
echo.
echo ========================================================================
echo.

REM Run the health check
if "%DB_CONFIG%"=="" (
    java DSeriesHealthCheck "%INSTALL_DIR%"
) else if "%SQL_CONFIG%"=="" (
    java DSeriesHealthCheck "%INSTALL_DIR%" "%DB_CONFIG%"
) else (
    java DSeriesHealthCheck "%INSTALL_DIR%" "%DB_CONFIG%" "%SQL_CONFIG%"
)

set EXIT_CODE=%errorlevel%

echo.
echo ========================================================================
echo   Health Check Complete
echo ========================================================================
echo   Exit Code: %EXIT_CODE%
echo.

if %EXIT_CODE%==0 (
    echo   Status: SUCCESS - System is healthy
) else if %EXIT_CODE%==1 (
    echo   Status: FAILURE - Critical issues found
) else (
    echo   Status: ERROR - Health check encountered errors
)

echo.
echo   Check the generated report for detailed results
echo ========================================================================
echo.

pause
exit /b %EXIT_CODE%
