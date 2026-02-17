@echo off
REM ============================================================================
REM dSeries Health Check Tool - Windows Launcher
REM ============================================================================
REM Version: 2.1.0
REM Date: 2026-02-12
REM
REM Usage: dseries_healthcheck.bat <dSeries_install_directory>
REM Example: dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4
REM ============================================================================

setlocal enabledelayedexpansion

REM ============================================================================
REM CONFIGURATION
REM ============================================================================

set "TOOL_VERSION=2.1.0"
set "SCRIPT_DIR=%~dp0"

REM ============================================================================
REM BANNER
REM ============================================================================

echo.
echo ========================================================================
echo   dSeries Health Check Tool v%TOOL_VERSION%
echo ========================================================================
echo.

REM ============================================================================
REM VALIDATE ARGUMENTS
REM ============================================================================

if "%~1"=="" (
    echo ERROR: dSeries installation directory not provided
    echo.
    echo Usage: %~nx0 ^<dSeries_install_directory^>
    echo.
    echo Example:
    echo   %~nx0 C:\CA\ESPdSeriesWAServer_R12_4
    echo   %~nx0 D:\dSeries\R25_0
    echo.
    goto :error
)

set "DSERIES_HOME=%~1"

REM Remove trailing backslash if present
if "%DSERIES_HOME:~-1%"=="\" set "DSERIES_HOME=%DSERIES_HOME:~0,-1%"

echo [1/6] Validating dSeries installation directory...
if not exist "%DSERIES_HOME%" (
    echo   ERROR: Directory not found: %DSERIES_HOME%
    goto :error
)

if not exist "%DSERIES_HOME%\conf" (
    echo   ERROR: conf directory not found in: %DSERIES_HOME%
    echo   This does not appear to be a valid dSeries installation
    goto :error
)

echo   OK: %DSERIES_HOME%
echo.

REM ============================================================================
REM CHECK JAVA
REM ============================================================================

echo [2/6] Checking Java installation...

REM Try to find Java in dSeries installation first
set "JAVA_CMD=java"

if exist "%DSERIES_HOME%\jre\bin\java.exe" (
    set "JAVA_CMD=%DSERIES_HOME%\jre\bin\java.exe"
    echo   Using dSeries bundled Java: %DSERIES_HOME%\jre
) else if exist "%DSERIES_HOME%\java\bin\java.exe" (
    set "JAVA_CMD=%DSERIES_HOME%\java\bin\java.exe"
    echo   Using dSeries bundled Java: %DSERIES_HOME%\java
) else (
    REM Check if Java is in PATH
    where java >nul 2>&1
    if errorlevel 1 (
        echo   ERROR: Java not found
        echo   Please ensure Java is installed and in PATH
        echo   Or dSeries bundled Java is available in %DSERIES_HOME%\jre
        goto :error
    )
    echo   Using system Java from PATH
)

REM Verify Java works
"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo   ERROR: Java command failed
    goto :error
)

echo   OK: Java is available
echo.

REM ============================================================================
REM BUILD CLASSPATH
REM ============================================================================

echo [3/6] Building classpath...

REM Try to use dSeries native classpath.bat for complete classpath
if exist "%DSERIES_HOME%\bin\classpath.bat" (
    echo   Using dSeries native classpath.bat...
    
    REM Execute classpath.bat to set CLASSPATH (suppress all output)
    pushd "%DSERIES_HOME%\bin"
    call classpath.bat >nul 2>nul
    popd
    
    if defined CLASSPATH (
        echo   OK: dSeries classpath loaded successfully
        echo   ℹ️  Includes all JDBC drivers, encryption libs, and auth DLLs
        
        REM Add our health check JAR to the beginning
        set "CLASSPATH=%SCRIPT_DIR%dseries-healthcheck.jar;!CLASSPATH!"
    ) else (
        echo   WARNING: classpath.bat did not set CLASSPATH
        echo   Falling back to manual classpath building...
        goto :MANUAL_CLASSPATH
    )
) else (
    echo   WARNING: classpath.bat not found
    echo   Falling back to manual classpath building...
    goto :MANUAL_CLASSPATH
)

goto :CLASSPATH_DONE

:MANUAL_CLASSPATH
echo   Building classpath manually...

set "CLASSPATH=%SCRIPT_DIR%dseries-healthcheck.jar"

REM Add dSeries lib directory JARs (for JDBC drivers and encryption)
if exist "%DSERIES_HOME%\lib" (
    echo   Adding dSeries libraries from: %DSERIES_HOME%\lib
    set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\lib\*"
    
    if exist "%DSERIES_HOME%\lib\jdbc" (
        set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\lib\jdbc\*"
    )
    if exist "%DSERIES_HOME%\lib\ext" (
        set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\lib\ext\*"
    )
)

REM Add dSeries third-party libs
if exist "%DSERIES_HOME%\third-party" (
    echo   Adding third-party libraries from: %DSERIES_HOME%\third-party
    set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\third-party\*"
)

REM Add dSeries ext directory
if exist "%DSERIES_HOME%\ext" (
    echo   Adding extension libraries from: %DSERIES_HOME%\ext
    set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\ext\*"
)

REM Add webserver libs
if exist "%DSERIES_HOME%\webserver\lib" (
    echo   Adding webserver libraries from: %DSERIES_HOME%\webserver\lib
    set "CLASSPATH=!CLASSPATH!;%DSERIES_HOME%\webserver\lib\*"
)

echo   OK: Manual classpath built

:CLASSPATH_DONE
echo.

REM ============================================================================
REM CHECK HEALTH CHECK JAR
REM ============================================================================

echo [4/6] Checking health check JAR...

if not exist "%SCRIPT_DIR%dseries-healthcheck.jar" (
    echo   ERROR: dseries-healthcheck.jar not found
    echo   Location: %SCRIPT_DIR%dseries-healthcheck.jar
    echo.
    echo   Please run create_jar.bat first to build the JAR file
    goto :error
)

echo   OK: %SCRIPT_DIR%dseries-healthcheck.jar
echo.

REM ============================================================================
REM CHECK SQL QUERIES FILE
REM ============================================================================

echo [5/6] Checking SQL queries configuration...

set "SQL_CONFIG=%SCRIPT_DIR%config\health_check_queries.sql"

if exist "%SQL_CONFIG%" (
    echo   OK: %SQL_CONFIG%
) else (
    echo   WARNING: SQL queries file not found
    echo   Location: %SQL_CONFIG%
    echo   Database checks will use defaults
)
echo.

REM ============================================================================
REM SETUP NATIVE LIBRARIES (for Windows Authentication)
REM ============================================================================

REM Add dSeries bin directory to PATH for native DLLs (e.g., sqljdbc_auth.dll)
if exist "%DSERIES_HOME%\bin" (
    set "PATH=%DSERIES_HOME%\bin;%PATH%"
)

REM ============================================================================
REM RUN HEALTH CHECK
REM ============================================================================

echo [6/6] Running health check...
echo.
echo ========================================================================
echo.

REM Set Java options (including java.library.path for native DLLs)
set "JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8 -Djava.library.path=%DSERIES_HOME%\bin"

REM Run the health check
if exist "%SQL_CONFIG%" (
    "%JAVA_CMD%" %JAVA_OPTS% -cp "%CLASSPATH%" DSeriesHealthCheck "%DSERIES_HOME%" "%SQL_CONFIG%"
) else (
    "%JAVA_CMD%" %JAVA_OPTS% -cp "%CLASSPATH%" DSeriesHealthCheck "%DSERIES_HOME%"
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

goto :end

REM ============================================================================
REM ERROR HANDLING
REM ============================================================================

:error
echo.
echo ========================================================================
echo   Health Check Failed
echo ========================================================================
echo.
set EXIT_CODE=2
goto :end

:end
endlocal
exit /b %EXIT_CODE%
