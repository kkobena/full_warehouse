@echo off
REM --- Variables de connexion DB ---
SET DB_HOST=localhost
SET DB_PORT=3306
SET DB_NAME=warehouse
SET MAGASIN_EMAIL=badoukobena@gmail.com
SET SMTP_HOST=smtp.gmail.com
SET SMTP_PORT=587
SET SERVER_PORT=9080
SET SMTP_MAIL=easyshopws@gmail.com
SET SMTP_APP_PWD=
SET FNE_URL=
SET FNE_KEY=
SET FNE_POS=
SET PORT_COM=

REM --- Chemin vers JRE local ---
set "JAVA_HOME=%~dp0jre"

REM --- Créer un nom de fichier de log quotidien ---
for /f "tokens=1-3 delims=/-." %%a in ("%date:\=/%") do (
    set "MM=%%a"
    set "DD=%%b"
    set "YYYY=%%c"
)
set "LOG_FILE_NAME=pharma_smart_start_up_%YYYY%-%MM%-%DD%.log"

REM --- Lancement de l'application en arrière-plan ---
start "" /b "%JAVA_HOME%\bin\java.exe" ^
 -Xms4g -Xmx4g ^
 -XX:+UseG1GC ^
 -jar "%~dp0pharmaSmart.jar" ^
 --server.port=%SERVER_PORT% ^
 --spring.profiles.active=prod ^
 "--spring.datasource.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useLegacyDatetimeCode=false&serverTimezone=UTC&characterEncoding=UTF-8" ^
 --spring.flyway.enabled=true ^
 --spring.flyway.schemas=%DB_NAME% ^
 --spring.mail.host=%SMTP_HOST% ^
 --spring.mail.port=%SMTP_PORT% ^
 --spring.mail.username=%SMTP_MAIL% ^
 --spring.mail.password=%SMTP_APP_PWD% ^
 --mail.email="%MAGASIN_EMAIL%" ^
 --fne.url="%FNE_URL%" ^
 --fne.api-key="%FNE_KEY%" ^
 --fne.point-of-sale="%FNE_POS%" ^
 --port-com="%PORT_COM%" ^
 >> "%~dp0%LOG_FILE_NAME%" 2>&1

REM --- Attendre 10 secondes pour laisser le temps au process de démarrer ---
timeout /t 10 >nul

REM --- Récupérer le PID du processus Java associé ---
for /f "tokens=2 delims==;" %%a in ('wmic process where "CommandLine like '%%pharmaSmart.jar%%'" get ProcessId /value ^| find "ProcessId="') do (
    echo %%a > "%~dp0pharmaSmart.pid"
    goto :done
)

:done
if exist "%~dp0pharmaSmart.pid" (
    echo Application démarrée. PID enregistré dans pharmaSmart.pid
) else (
    echo Erreur: impossible de trouver le processus Java.
)
exit /b
