# JVM Configuration Guide for PharmaSmart

## Overview

This guide provides optimal JVM configurations for Java 25 with Spring Boot 4, tailored for different deployment scenarios.

## Quick Start

### Using .jvmopts File

```bash
# Windows
java @.jvmopts -jar pharmasmart.jar

# Linux/macOS
java @.jvmopts -jar pharmasmart.jar
```

### Direct Command Line

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -jar pharmasmart.jar
```

## Deployment Scenarios

### 1. Development/Testing (2GB RAM minimum)

```bash
-Xms512m
-Xmx1g
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-Xlog:gc*:file=logs/gc.log
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump.hprof
```

**Use case:** Local development, testing environments

### 2. Production - Small (4-8GB RAM)

```bash
-Xms2g
-Xmx4g
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-XX:MaxDirectMemorySize=512m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-Xlog:gc*:file=logs/gc-%t.log:time,level,tags:filecount=5,filesize=10M
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump-%t.hprof
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

**Use case:** Small pharmacy, single location, 10-50 concurrent users

### 3. Production - Medium (8-16GB RAM)

```bash
-Xms4g
-Xmx8g
-XX:MetaspaceSize=384m
-XX:MaxMetaspaceSize=768m
-XX:MaxDirectMemorySize=1g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=150
-XX:ConcGCThreads=4
-XX:ParallelGCThreads=8
-XX:InitiatingHeapOccupancyPercent=40
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-XX:ReservedCodeCacheSize=512m
-Xlog:gc*:file=logs/gc-%t.log:time,level,tags:filecount=10,filesize=20M
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump-%t.hprof
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

**Use case:** Medium pharmacy chain, 2-5 locations, 50-200 concurrent users

### 4. Production - Large (16GB+ RAM)

```bash
-Xms8g
-Xmx16g
-XX:MetaspaceSize=512m
-XX:MaxMetaspaceSize=1g
-XX:MaxDirectMemorySize=2g
-XX:+UseZGC
-XX:ZAllocationSpikeTolerance=2
-XX:ConcGCThreads=8
-XX:ParallelGCThreads=16
-XX:+UseStringDeduplication
-XX:ReservedCodeCacheSize=1g
-Xlog:gc*:file=logs/gc-%t.log:time,level,tags:filecount=10,filesize=50M
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump-%t.hprof
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

**Use case:** Large pharmacy chain, multiple locations, 200+ concurrent users

### 5. Tauri Desktop Application (Bundled Backend) ✅ AUTO-CONFIGURED

```bash
-Xms512m
-Xmx1g
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:MaxDirectMemorySize=256m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-Xlog:gc*:file=logs/gc.log:time,level,tags
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump.hprof
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
-Djava.net.preferIPv4Stack=true
```

**Use case:** Desktop standalone application

**Note:** These options are **automatically applied** by the Tauri backend manager (`src-tauri/src/backend_manager.rs:267-303`). The Rust code configures the JVM when launching the bundled Spring Boot backend. No manual configuration needed!

## Garbage Collector Selection

### G1GC (Recommended for most cases)

**Pros:**
- Balanced throughput and latency
- Excellent for 4GB+ heaps
- Predictable pause times
- Default in Java 25

**Configuration:**
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
```

### ZGC (For ultra-low latency)

**Pros:**
- Sub-millisecond pause times
- Scales to multi-TB heaps
- Ideal for real-time requirements

**Cons:**
- Higher CPU usage
- Requires more RAM overhead

**Configuration:**
```bash
-XX:+UseZGC
-XX:ZAllocationSpikeTolerance=2
```

**When to use:** High-traffic systems requiring consistent response times (< 10ms)

### Serial GC (For very small heaps)

**Configuration:**
```bash
-XX:+UseSerialGC
```

**When to use:** Containerized environments with < 512MB RAM

## Memory Sizing Guidelines

### Heap Size (-Xms / -Xmx)

**Rule of thumb:**
- Development: 512MB - 1GB
- Small production: 2GB - 4GB
- Medium production: 4GB - 8GB
- Large production: 8GB - 16GB+

**Formula:** Available RAM × 0.5 to 0.75 (leave room for OS and other processes)

**Example:** 16GB server → 8-12GB for JVM heap

### Metaspace

**Typical sizes:**
- Small apps (< 100 classes): 128MB - 256MB
- Medium apps (< 500 classes): 256MB - 512MB
- Large apps (> 500 classes): 512MB - 1GB

**PharmaSmart:** 168 entities + Spring classes → **256MB - 512MB**

### Direct Memory

Used for NIO buffers (database connections, file I/O):
- Small: 256MB
- Medium: 512MB - 1GB
- Large: 1GB - 2GB

## Performance Tuning

### Database Connection Pool

Adjust in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Adjust based on concurrent load
      minimum-idle: 10
      connection-timeout: 30000
```

**Formula:** `pool_size = (core_count × 2) + effective_spindle_count`
- 4-core CPU + 1 SSD = ~10 connections
- Scale up based on load testing

### Virtual Threads

Already enabled in `application-prod.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Benefits:**
- Thousands of concurrent requests with minimal overhead
- No need for large thread pools
- Perfect for I/O-bound operations (database, HTTP)

## Monitoring & Diagnostics

### Enable JMX (Production Monitoring)

```bash
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=true
-Dcom.sun.management.jmxremote.ssl=true
-Dcom.sun.management.jmxremote.password.file=/path/to/jmxremote.password
-Dcom.sun.management.jmxremote.access.file=/path/to/jmxremote.access
```

Connect with: `jconsole localhost:9010` or VisualVM

### GC Logging Analysis

View GC logs:
```bash
# Tail live GC log
tail -f logs/gc-*.log

# Analyze with GCViewer or similar tools
```

### Heap Dump Analysis

When OutOfMemoryError occurs, analyze heap dump:
```bash
# Using jhat (built-in)
jhat logs/heapdump-*.hprof

# Using Eclipse Memory Analyzer (MAT) - recommended
# Import .hprof file into Eclipse MAT
```

### Native Memory Tracking

Enable NMT:
```bash
-XX:NativeMemoryTracking=summary
```

Query memory usage:
```bash
jcmd <pid> VM.native_memory summary
```

### Flight Recorder (Low-overhead Profiling)

Start recording:
```bash
# At startup
-XX:StartFlightRecording=duration=60s,filename=logs/recording.jfr

# Or dynamically
jcmd <pid> JFR.start duration=60s filename=logs/recording.jfr
```

Analyze with Java Mission Control (JMC)

## Troubleshooting

### OutOfMemoryError: Java heap space

**Solutions:**
1. Increase `-Xmx` value
2. Check for memory leaks (analyze heap dump)
3. Enable GC logging to see allocation patterns
4. Review Hibernate caching configuration

### OutOfMemoryError: Metaspace

**Solutions:**
1. Increase `-XX:MaxMetaspaceSize`
2. Check for classloader leaks (analyze heap dump)
3. Review hot-reload/redeploy patterns in production

### High CPU Usage

**Diagnostic steps:**
1. Check GC overhead: `jstat -gcutil <pid> 1000`
2. Profile with Flight Recorder
3. Review application threads: `jstack <pid>`
4. Check for infinite loops or inefficient algorithms

### High Memory Usage

**Diagnostic steps:**
1. Take heap dump: `jcmd <pid> GC.heap_dump logs/heapdump.hprof`
2. Analyze with MAT or jhat
3. Look for large object retentions
4. Review caching strategies (Hibernate L2 cache, Caffeine)

### Slow Startup

**Solutions:**
1. Enable tiered compilation: `-XX:+TieredCompilation`
2. Increase code cache: `-XX:ReservedCodeCacheSize=512m`
3. Use class data sharing (CDS)
4. Profile with `-XX:+PrintCompilation`

## Best Practices

### 1. Always Set Min = Max Heap

```bash
-Xms4g -Xmx4g  # Good: prevents heap resizing
```

### 2. Monitor in Production

- Enable GC logging (< 1% overhead)
- Use JMX for real-time metrics
- Set up heap dump on OOM

### 3. Test Configuration

```bash
# Verify settings
java -XX:+PrintFlagsFinal -version | grep -i 'UseG1GC\|MaxHeapSize\|MetaspaceSize'

# Test with load
# Use JMeter, Gatling, or similar tools
```

### 4. Container Deployment

If deploying in Docker/Kubernetes:

```bash
# Let JVM auto-detect container limits
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
```

### 5. Backup Configuration

Always keep a copy of working JVM options:
```bash
# Save current options
jcmd <pid> VM.flags > logs/jvm-flags-backup.txt
```

## References

- [Java 25 Release Notes](https://openjdk.org/projects/jdk/25/)
- [Spring Boot 4 Documentation](https://spring.io/projects/spring-boot)
- [G1GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc)
- [Virtual Threads (Project Loom)](https://openjdk.org/jeps/444)

## Quick Reference Card

| Scenario | Xms/Xmx | Metaspace | GC | MaxGCPauseMillis |
|----------|---------|-----------|-----|------------------|
| Dev      | 512m/1g | 128m/256m | G1  | 200ms           |
| Small Prod | 2g/4g | 256m/512m | G1  | 200ms           |
| Medium Prod | 4g/8g | 384m/768m | G1  | 150ms           |
| Large Prod | 8g/16g | 512m/1g | ZGC | -               |
| Tauri Desktop | 512m/1g | 128m/256m | G1 | 200ms           |

---

**Note:** Always test JVM options in a staging environment before deploying to production. Monitor metrics and adjust based on actual application behavior.
