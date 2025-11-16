@echo off
REM PharmaSmart Backend Launcher
REM This script launches the Spring Boot backend as a Tauri sidecar

REM Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"

REM Find the JAR file (assumes pattern: warehouse-*.jar)
for %%f in ("%SCRIPT_DIR%warehouse-*.jar") do set "JAR_FILE=%%f"

if not defined JAR_FILE (
    echo Error: Spring Boot JAR file not found in %SCRIPT_DIR%
    exit /b 1
)

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java Runtime Environment not found. Please install Java 17 or higher.
    exit /b 1
)

REM Set default port if not provided as argument
set "PORT=%~1"
if not defined PORT set "PORT=8080"

REM Launch Spring Boot with production profile
echo Starting PharmaSmart Backend on port %PORT%...
java -jar "%JAR_FILE%" --spring.profiles.active=prod --server.port=%PORT%
