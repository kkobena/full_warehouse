@echo off
REM ===================================================================
REM PharmaSmart Production Startup Script (Windows)
REM Java 25 + Spring Boot 4 - Optimized Configuration
REM ===================================================================

echo Starting PharmaSmart Production Server...
echo.

REM Set Java Home (adjust if needed)
REM set JAVA_HOME=C:\Program Files\Java\jdk-25
REM set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java version
java -version
echo.

REM Production JVM Options
set JVM_OPTS=-Xms2g -Xmx4g
set JVM_OPTS=%JVM_OPTS% -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
set JVM_OPTS=%JVM_OPTS% -XX:MaxDirectMemorySize=512m
set JVM_OPTS=%JVM_OPTS% -XX:+UseG1GC
set JVM_OPTS=%JVM_OPTS% -XX:MaxGCPauseMillis=200
set JVM_OPTS=%JVM_OPTS% -XX:+UseStringDeduplication
set JVM_OPTS=%JVM_OPTS% -XX:+UseCompressedOops
set JVM_OPTS=%JVM_OPTS% -Xlog:gc*:file=logs/gc-%%t.log:time,level,tags:filecount=5,filesize=10M
set JVM_OPTS=%JVM_OPTS% -XX:+HeapDumpOnOutOfMemoryError
set JVM_OPTS=%JVM_OPTS% -XX:HeapDumpPath=logs/heapdump-%%t.hprof
set JVM_OPTS=%JVM_OPTS% -XX:ErrorFile=logs/hs_err_pid%%p.log
set JVM_OPTS=%JVM_OPTS% -Dfile.encoding=UTF-8
set JVM_OPTS=%JVM_OPTS% -Duser.timezone=UTC
set JVM_OPTS=%JVM_OPTS% -Djava.net.preferIPv4Stack=true

REM Spring Boot Options
set SPRING_OPTS=--spring.profiles.active=prod,standalone
set SPRING_OPTS=%SPRING_OPTS% --server.port=9080

REM Create logs directory if it doesn't exist
if not exist logs mkdir logs

REM Find the JAR file
for %%f in (target\pharmaSmart-*.jar) do set JAR_FILE=%%f

if "%JAR_FILE%"=="" (
    echo ERROR: JAR file not found in target directory
    echo Please build the project first: mvnw clean package -Pprod
    pause
    exit /b 1
)

echo Using JAR: %JAR_FILE%
echo JVM Options: %JVM_OPTS%
echo Spring Options: %SPRING_OPTS%
echo.
echo Press Ctrl+C to stop the server
echo ===================================================================
echo.

REM Start the application
java %JVM_OPTS% -jar %JAR_FILE% %SPRING_OPTS%
