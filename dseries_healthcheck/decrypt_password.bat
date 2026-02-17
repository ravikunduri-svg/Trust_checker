@echo off
rem dSeries Password Decryption Script
rem Usage: decrypt_password.bat <dSeries_install_dir> <encrypted_password>

if "%~1"=="" (
    echo Usage: decrypt_password.bat ^<dSeries_install_dir^> ^<encrypted_password^>
    echo Example: decrypt_password.bat C:\CA\ESPdSeriesWAServer_R12_4 rRQcfTTiTZPX3plzpBwmvA==
    exit /b 1
)

if "%~2"=="" (
    echo Usage: decrypt_password.bat ^<dSeries_install_dir^> ^<encrypted_password^>
    echo Example: decrypt_password.bat C:\CA\ESPdSeriesWAServer_R12_4 rRQcfTTiTZPX3plzpBwmvA==
    exit /b 1
)

set CAWA_HOME=%~1
set ENCRYPTED_PASSWORD=%~2

rem Run decryption utility with dSeries libraries
"%CAWA_HOME%\jre\bin\java" -cp "%CAWA_HOME%\lib\*;%~dp0" DecryptPassword "%ENCRYPTED_PASSWORD%"
