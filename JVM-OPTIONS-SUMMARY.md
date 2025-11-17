# JVM Options Summary for PharmaSmart

## 📋 Files Created

1. **`.jvmopts`** - Complete JVM configuration file (recommended)
2. **`JVM-CONFIGURATION.md`** - Comprehensive guide and reference
3. **`start-prod.bat`** - Windows production startup script
4. **`start-prod.sh`** - Linux/macOS production startup script
5. **`pharmasmart.service`** - SystemD service configuration

## 🚀 Quick Start

### Option 1: Using Startup Scripts (Easiest)

**Windows:**
```cmd
start-prod.bat
```

**Linux/macOS:**
```bash
./start-prod.sh
```

### Option 2: Using .jvmopts File

```bash
java @.jvmopts -jar target/pharmaSmart-*.jar --spring.profiles.active=prod,standalone
```

### Option 3: Direct Command Line

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -jar target/pharmaSmart-*.jar
```

## 💡 Key Optimizations for Java 25 + Spring Boot 4

### 1. Virtual Threads ✅
Already enabled in `application-prod.yml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Benefits:**
- Handles thousands of concurrent requests efficiently
- Perfect for I/O-bound operations (database, HTTP)
- Minimal memory overhead

### 2. G1 Garbage Collector (Default)
```
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

**Benefits:**
- Low latency (target 200ms pause times)
- Excellent for 4GB+ heaps
- Automatic tuning

### 3. Memory Configuration
```
-Xms2g -Xmx4g                    # Heap: 2-4GB
-XX:MetaspaceSize=256m            # Class metadata: 256-512MB
-XX:MaxDirectMemorySize=512m      # NIO buffers: 512MB
```

**Adjust based on your system:**
- 8GB RAM → `-Xmx4g`
- 16GB RAM → `-Xmx8g`
- 32GB RAM → `-Xmx16g`

### 4. Performance Enhancements
```
-XX:+UseStringDeduplication       # Reduces memory for duplicate strings
-XX:+UseCompressedOops           # Reduces memory footprint
-XX:ReservedCodeCacheSize=256m   # More space for JIT-compiled code
```

### 5. Production Monitoring
```
-Xlog:gc*:file=logs/gc-%t.log    # GC logging
-XX:+HeapDumpOnOutOfMemoryError  # Auto heap dump on OOM
-XX:HeapDumpPath=logs/           # Heap dump location
```

## 📊 Configuration by Environment

### Development (Local Machine)
```bash
-Xms512m -Xmx1g -XX:+UseG1GC
```
**RAM Required:** 2GB minimum

### Small Production (4-8GB Server)
```bash
-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```
**Use Case:** Single pharmacy, 10-50 users

### Medium Production (8-16GB Server)
```bash
-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=150
```
**Use Case:** Pharmacy chain, 50-200 users

### Large Production (16GB+ Server)
```bash
-Xms8g -Xmx16g -XX:+UseZGC
```
**Use Case:** Multiple locations, 200+ users

### Tauri Desktop (Standalone App) ✅ AUTO-CONFIGURED
```bash
-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```
**Use Case:** Desktop application with bundled backend

**Note:** JVM options are **automatically applied** by the Tauri backend manager. No manual configuration needed!

## 🔧 Testing Your Configuration

### 1. Verify JVM Settings
```bash
java -XX:+PrintFlagsFinal -version | grep -E "UseG1GC|MaxHeapSize"
```

### 2. Check Running Application
```bash
# Get process ID
jps -l | grep pharmaSmart

# View actual JVM settings
jcmd <pid> VM.flags

# Monitor GC activity
jstat -gcutil <pid> 1000
```

### 3. Load Testing
Use Apache JMeter or similar tools to test under load:
- Monitor GC pause times
- Check memory usage
- Verify throughput

## 📈 Monitoring in Production

### Real-time Monitoring
```bash
# JVM stats
jstat -gcutil <pid> 1000

# Thread dump
jstack <pid> > thread-dump.txt

# Memory usage
jcmd <pid> VM.native_memory summary
```

### Log Analysis
```bash
# View GC logs
tail -f logs/gc-*.log

# Analyze with GCViewer (GUI tool)
# https://github.com/chewiebug/GCViewer
```

## 🐛 Troubleshooting

### OutOfMemoryError
1. Check heap dump: `logs/heapdump-*.hprof`
2. Analyze with Eclipse MAT or VisualVM
3. Increase `-Xmx` if needed

### High CPU
1. Take thread dump: `jstack <pid>`
2. Profile with Java Flight Recorder
3. Check GC overhead: `jstat -gcutil <pid>`

### Slow Startup
1. Enable compilation logs: `-XX:+PrintCompilation`
2. Increase code cache: `-XX:ReservedCodeCacheSize=512m`
3. Review Spring Boot startup time

## 📚 Additional Resources

- **JVM-CONFIGURATION.md** - Complete guide with examples
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **G1GC Tuning:** https://docs.oracle.com/en/java/javase/21/gctuning/
- **Virtual Threads:** https://openjdk.org/jeps/444

## ✅ Recommended Settings Summary

**For most production deployments:**

```bash
java \
  -Xms2g -Xmx4g \
  -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+UseCompressedOops \
  -Xlog:gc*:file=logs/gc.log \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC \
  -jar pharmaSmart.jar \
  --spring.profiles.active=prod,standalone \
  --server.port=9080
```

**Copy `.jvmopts` or use startup scripts for easier deployment!**

---

**Note:** Always test JVM configuration in a staging environment before production deployment. Monitor and adjust based on actual workload patterns.
