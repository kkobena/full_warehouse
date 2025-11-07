@echo off
chcp 65001 > nul
setlocal

echo Ce script va creer un nouvel utilisateur, une nouvelle base de donnees et un nouveau schema PostgreSQL.
echo Veuillez vous assurer que 'psql.exe' se trouve dans le PATH de votre systeme.
echo.

:: --- Valeurs par defaut ---
set "DEFAULT_PG_ADMIN_USER=postgres"
set "DEFAULT_DB_NAME=pharma_smart"
set "DEFAULT_USER=pharma_smart"

:: Demander les informations d'identification de l'administrateur
set /p "PG_ADMIN_USER=Entrez le nom d'utilisateur administrateur PostgreSQL [%DEFAULT_PG_ADMIN_USER%]: Valider pour la valeur par defaut. "
if not defined PG_ADMIN_USER set "PG_ADMIN_USER=%DEFAULT_PG_ADMIN_USER%"

:GetAdminPass
set /p "PG_ADMIN_PASS=Entrez le mot de passe administrateur PostgreSQL: "
if not defined PG_ADMIN_PASS (
    echo.
    echo Le mot de passe ne peut pas etre vide. Veuillez reessayer.
    echo.
    goto :GetAdminPass
)
echo.

:: Demander les details du nouvel utilisateur et de la base de donnees
set /p "NEW_DB_NAME=Entrez le nom de la nouvelle base de donnees [%DEFAULT_DB_NAME%]: Valider pour la valeur par defaut. "
if not defined NEW_DB_NAME set "NEW_DB_NAME=%DEFAULT_DB_NAME%"

set /p "NEW_USER=Entrez le nom d'utilisateur du nouvel utilisateur [%DEFAULT_USER%]: Valider pour la valeur par defaut. "
if not defined NEW_USER set "NEW_USER=%DEFAULT_USER%"

:GetNewPass
set /p "NEW_PASS=Entrez le mot de passe du nouvel utilisateur: "
if not defined NEW_PASS (
    echo.
    echo Le mot de passe ne peut pas etre vide. Veuillez reessayer.
    echo.
    goto :GetNewPass
)

:: Utiliser le nom de la base de donnees comme nom de schema
set "NEW_SCHEMA=%NEW_DB_NAME%"

echo.
echo --- Configuration ---
echo Utilisateur Admin: %PG_ADMIN_USER%
echo Nouvelle Base de Donnees: %NEW_DB_NAME%
echo Nouvel Utilisateur: %NEW_USER%
echo Nouveau Schema: %NEW_SCHEMA%
echo ---------------------
echo.

:: Definir le mot de passe pour l'outil de ligne de commande psql
set "PGPASSWORD=%PG_ADMIN_PASS%"

:: Creer l'utilisateur et la base de donnees
echo Creation de l'utilisateur '%NEW_USER%' et de la base de donnees '%NEW_DB_NAME%'...
psql -U "%PG_ADMIN_USER%" -d postgres -c "CREATE DATABASE %NEW_DB_NAME%;" -c "CREATE USER %NEW_USER% WITH PASSWORD '%NEW_PASS%';" -c "ALTER DATABASE %NEW_DB_NAME% OWNER TO %NEW_USER%;"

if %errorlevel% neq 0 (
    echo Une erreur est survenue lors de la creation de l'utilisateur ou de la base de donnees.
    goto :eof
)

echo Utilisateur et base de donnees crees avec succes.
echo.

:: Creer le schema et attribuer les privileges
echo Creation du schema '%NEW_SCHEMA%' et attribution des privileges...
psql -U "%PG_ADMIN_USER%" -d "%NEW_DB_NAME%" -c "CREATE SCHEMA %NEW_SCHEMA% AUTHORIZATION %NEW_USER%;" -c "GRANT ALL PRIVILEGES ON SCHEMA %NEW_SCHEMA% TO %NEW_USER%;" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA %NEW_SCHEMA% GRANT ALL ON TABLES TO %NEW_USER%;" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA %NEW_SCHEMA% GRANT ALL ON SEQUENCES TO %NEW_USER%;"

if %errorlevel% neq 0 (
    echo Une erreur est survenue lors de la creation du schema ou de l'attribution des privileges.
    goto :eof
)

echo Schema cree et privileges attribues avec succes.
echo.
echo --- Termine ! ---
echo.

:eof
endlocal
pause
