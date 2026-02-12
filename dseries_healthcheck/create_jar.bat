@echo off
REM ============================================================================
REM Create dSeries Health Check JAR file
REM ============================================================================

setlocal

echo.
echo ========================================================================
echo   Creating dSeries Health Check JAR
echo ========================================================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM Clean old files
echo [1/4] Cleaning old files...
if exist dseries-healthcheck.jar del dseries-healthcheck.jar
if exist manifest.txt del manifest.txt
echo   OK

REM Compile Java source
echo.
echo [2/4] Compiling Java source...
javac -encoding UTF-8 DSeriesHealthCheck.java
if errorlevel 1 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)
echo   OK

REM Create manifest
echo.
echo [3/4] Creating manifest...
echo Manifest-Version: 1.0> manifest.txt
echo Main-Class: DSeriesHealthCheck>> manifest.txt
echo Implementation-Title: dSeries Health Check Tool>> manifest.txt
echo Implementation-Version: 2.1.0>> manifest.txt
echo Implementation-Vendor: Broadcom>> manifest.txt
echo.>> manifest.txt
echo   OK

REM Create JAR
echo.
echo [4/4] Creating JAR file...
jar cfm dseries-healthcheck.jar manifest.txt DSeriesHealthCheck*.class
if errorlevel 1 (
    echo ERROR: JAR creation failed
    pause
    exit /b 1
)
echo   OK

REM Cleanup
del manifest.txt

echo.
echo ========================================================================
echo   JAR created successfully: dseries-healthcheck.jar
echo ========================================================================
echo.
echo Size: 
dir dseries-healthcheck.jar | findstr dseries-healthcheck.jar
echo.

pause
