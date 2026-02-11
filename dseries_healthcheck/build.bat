@echo off
REM ============================================================================
REM ESP dSeries Health Check Tool - Build Script (Windows)
REM Version: 1.0.0
REM Date: 2026-02-11
REM ============================================================================

echo ===================================================================
echo   ESP dSeries Health Check Tool - Build Script
echo ===================================================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed
    echo    Please install Maven 3.6 or higher
    echo.
    echo    Download from: https://maven.apache.org/download.cgi
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed
    echo    Please install Java 8 or higher
    echo.
    echo    Download from: https://www.oracle.com/java/technologies/downloads/
    exit /b 1
)

echo Maven version:
call mvn -version | findstr "Apache Maven"
echo.

echo Java version:
java -version 2>&1 | findstr "version"
echo.

echo ===================================================================
echo   Building Project
echo ===================================================================
echo.

REM Clean and build
call mvn clean package

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo   BUILD SUCCESSFUL
    echo ===================================================================
    echo.
    echo   JAR file created:
    echo     target\dseries-healthcheck-1.0.0.jar
    echo.
    echo   To run:
    echo     java -jar target\dseries-healthcheck-1.0.0.jar --quick
    echo.
    echo ===================================================================
) else (
    echo.
    echo ===================================================================
    echo   BUILD FAILED
    echo ===================================================================
    echo.
    echo   Please check the error messages above
    echo.
    exit /b 1
)
