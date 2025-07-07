batch
@echo off
setlocal

REM --- Configuration ---
set "PID_FILE_NAME=pharmaSmart.pid"
set "LOG_FILE_NAME=pharma_smart_monitor.log" REM Separate log for the monitor
set "START_SCRIPT_NAME=start_up_pharma_stmart.bat"
set "CHECK_INTERVAL_SECONDS=10" REM How often to check if the app is running
set "RESTART_DELAY_SECONDS=5"  REM How long to wait before attempting a restart

echo [%date% %time%] Monitor script started. >> "%~dp0%LOG_FILE_NAME%"

:main_loop
REM --- Read the PID from the file ---
set "APP_PID="
if not exist "%~dp0%PID_FILE_NAME%" (
    echo [%date% %time%] PID file (%PID_FILE_NAME%) not found. Assuming application is not running or not started correctly. >> "%~dp0%LOG_FILE_NAME%"
    goto :attempt_restart
)

set /p APP_PID=<"%~dp0%PID_FILE_NAME%"

if not defined APP_PID (
    echo [%date% %time%] PID file (%PID_FILE_NAME%) is empty or unreadable. >> "%~dp0%LOG_FILE_NAME%"
    goto :attempt_restart
)

REM Remove any surrounding spaces from PID, just in case
for /f "tokens=* delims= " %%i in ("%APP_PID%") do set "APP_PID=%%i"

if "%APP_PID%"=="" (
    echo [%date% %time%] PID read from file is empty. >> "%~dp0%LOG_FILE_NAME%"
    goto :attempt_restart
)

REM --- Check if the process with APP_PID is running ---
tasklist /FI "PID eq %APP_PID%" /NH | find /I "%APP_PID%" >nul
if errorlevel 1 (
    echo [%date% %time%] Application with PID %APP_PID% (from %PID_FILE_NAME%) is no longer running. >> "%~dp0%LOG_FILE_NAME%"
    goto :attempt_restart
) else (
    echo [%date% %time%] Application with PID %APP_PID% is running. Checking again in %CHECK_INTERVAL_SECONDS% seconds. >> "%~dp0%LOG_FILE_NAME%"
    timeout /t %CHECK_INTERVAL_SECONDS% /nobreak >nul
    goto :main_loop
)

:attempt_restart
echo [%date% %time%] Attempting to restart the application... >> "%~dp0%LOG_FILE_NAME%"
REM Optional: Delete the old PID file before attempting to restart
if exist "%~dp0%PID_FILE_NAME%" (
    del "%~dp0%PID_FILE_NAME%"
)

call "%~dp0%START_SCRIPT_NAME%"
if errorlevel 1 (
    echo [%date% %time%] Restart attempt via %START_SCRIPT_NAME% failed or did not report a PID. Check %START_SCRIPT_NAME%'s log. >> "%~dp0%LOG_FILE_NAME%"
) else (
    echo [%date% %time%] Restart attempt via %START_SCRIPT_NAME% initiated. Monitor will re-check PID. >> "%~dp0%LOG_FILE_NAME%"
)

echo [%date% %time%] Waiting %RESTART_DELAY_SECONDS% seconds before next check. >> "%~dp0%LOG_FILE_NAME%"
timeout /t %RESTART_DELAY_SECONDS% /nobreak >nul
goto :main_loop

endlocal
