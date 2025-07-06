@echo off
REM --- Variables de connexion DB ---
REM SET DB_USER=warehouse
REM SET DB_PASSWORD=warehouse2802
SET DB_HOST=localhost
SET DB_PORT=3306
SET DB_NAME=warehouse
SET MAGASIN_EMAIL=badoukobena@gmail.com

REM --- Chemin vers JRE local ---
set "JAVA_HOME=%~dp0jre"

REM --- Lancement de l'application Spring Boot en arrière-plan (détaché du terminal) ---
start "PharmaSmart" /min "%JAVA_HOME%\bin\java.exe" ^
 -jar "%~dp0pharmaSmart.jar" ^
 --server.port=8080 ^
 --spring.profiles.active=prod ^
 --spring.datasource.url="jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useLegacyDatetimeCode=false&serverTimezone=UTC&characterEncoding=UTF-8" ^
REM --spring.datasource.username=%DB_USER% ^
REM --spring.datasource.password=%DB_PASSWORD% ^
--mail.email=
 >> "%~dp0pharma_smart_start_up.log" 2>&1

REM --- Attendre 10 secondes pour laisser le temps au processus de démarrer ---
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


REM Récupérer le PID du processus Java (le plus récent)
REM Clique droit sur start-app.bat → Créer un raccourci
REM Clic droit sur le raccourci → Propriétés
REM Dans Exécuter, choisis Minimisé
REM Double-clique sur le raccourci
REM Planificateur de tâches (plus de contrôle)
REM Ouvrir le planificateur de tâches (taskschd.msc)
REM Nom : pharmaSmart Launcher
REM Exécuter avec les autorisations maximales
REM Onglet Déclencheurs :
REM -Nouveau → Au démarrage de session
REM Onglet Actions :
REM Nouveau : Programme : cmd.exe
REM Arguments : /c "chemin\vers\start_up.bat"
REM Onglet Conditions : Décoche "Démarrer uniquement si l’ordinateur est en courant secteur" si nécessaire

