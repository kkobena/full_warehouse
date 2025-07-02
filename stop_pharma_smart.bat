@echo off
REM Vérifier que le fichier PID existe
if not exist "%~dp0pharmaSmart.pid" (
    echo Fichier pharmaSmart.pid introuvable. L'application ne semble pas être en cours d'execution.
    exit /b 1
)

REM Lire le PID depuis le fichier
set /p PID=<"%~dp0pharmaSmart.pid"
echo Tentative d'arret du processus avec PID %PID%

REM Tuer le processus
taskkill /PID %PID% /F

if %errorlevel%==0 (
    echo Application arretee avec succes.
    del "%~dp0pharmaSmart.pid"
) else (
    echo Echec de l'arret de l'application. Verifiez si le PID est correct.
)
