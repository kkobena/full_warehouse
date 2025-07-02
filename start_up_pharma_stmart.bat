@echo off
REM --- Variables de connexion DB ---
SET DB_USER=warehouse
SET DB_PASSWORD=warehouse2802
SET DB_HOST=localhost
SET DB_PORT=3306
SET DB_NAME=warehouse

REM --- Chemin vers JRE local ---
set "JAVA_HOME=%~dp0jre"

REM --- Optionnel : timezone utilisateur Windows (à définir ou laisser vide) ---
REM set "USER_TIMEZONE=UTC"

REM --- Commande de lancement en arrière-plan ---
start "" /b cmd /c ^
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0pharmaSmart.jar" ^
    --server.port=8080 ^
    --spring.profiles.active=prod ^
    --spring.datasource.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useLegacyDatetimeCode=false&serverTimezone=UTC&characterEncoding=UTF-8 ^
    --spring.datasource.username=%DB_USER% ^
    --spring.datasource.password=%DB_PASSWORD% ^
    > "%~dp0pharma_smart_start_up.log" 2>&1

REM --- Attendre 60 secondes que le processus démarre ---
timeout /t 60 >nul

REM --- Récupérer le PID du processus Java correspondant au jar ---
for /f "skip=1 tokens=2 delims=," %%a in ('
    wmic process where "CommandLine like '%%pharmaSmart.jar%%'" get ProcessId /format:csv
') do (
    echo %%a > "%~dp0pharmaSmart.pid"
    goto :pid_found
)

:pid_found
echo Application démarrée. PID enregistré dans pharmaSmart.pid

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

