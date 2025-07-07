
@echo off
setlocal

set "PID_FILE=%~dp0pharmaSmart.pid"
if not exist "%PID_FILE%" (
    echo Fichier PID introuvable.
    exit /b 1
)

set /p PID=<"%PID_FILE%"
if "%PID%"=="" (
    echo PID vide dans le fichier.
    exit /b 1
)

REM --- Arrêter le processus ---
taskkill /F /PID %PID%
if %ERRORLEVEL%==0 (
    echo Processus pharmaSmart.jar (PID %PID%) arrêté.
    del "%PID_FILE%"
) else (
    echo Echec de l'arrêt du processus (PID %PID%).
)

endlocal
exit /b