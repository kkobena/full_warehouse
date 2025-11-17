#!/bin/bash
# ===================================================================
# PharmaSmart Production Startup Script (Linux/macOS)
# Java 25 + Spring Boot 4 - Optimized Configuration
# ===================================================================

echo "Starting PharmaSmart Production Server..."
echo ""

# Set Java Home (adjust if needed)
# export JAVA_HOME=/usr/lib/jvm/jdk-25
# export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
java -version
echo ""

# Production JVM Options
JVM_OPTS="-Xms2g -Xmx4g"
JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=512m"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"
JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=logs/gc-%t.log:time,level,tags:filecount=5,filesize=10M"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=logs/heapdump-%t.hprof"
JVM_OPTS="$JVM_OPTS -XX:ErrorFile=logs/hs_err_pid%p.log"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
JVM_OPTS="$JVM_OPTS -Duser.timezone=UTC"
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true"

# Spring Boot Options
SPRING_OPTS="--spring.profiles.active=prod,standalone"
SPRING_OPTS="$SPRING_OPTS --server.port=9080"

# Create logs directory if it doesn't exist
mkdir -p logs

# Find the JAR file
JAR_FILE=$(ls target/pharmaSmart-*.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found in target directory"
    echo "Please build the project first: ./mvnw clean package -Pprod"
    exit 1
fi

echo "Using JAR: $JAR_FILE"
echo "JVM Options: $JVM_OPTS"
echo "Spring Options: $SPRING_OPTS"
echo ""
echo "Press Ctrl+C to stop the server"
echo "==================================================================="
echo ""

# Start the application
exec java $JVM_OPTS -jar "$JAR_FILE" $SPRING_OPTS
