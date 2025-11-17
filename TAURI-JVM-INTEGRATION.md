# Tauri JVM Integration - Automatic JVM Options

## ✅ Implementation Complete

The Tauri desktop application now **automatically applies optimized JVM options** when launching the bundled Spring Boot backend. No manual configuration required!

## 📍 Implementation Location

**File:** `src-tauri/src/backend_manager.rs`
**Lines:** 246-303
**Function:** `start_backend()`

## 🎯 JVM Options Applied Automatically

When the Tauri app starts the backend, it applies these JVM options:

```bash
# Memory Configuration (Conservative for Desktop)
-Xms512m                              # Initial heap: 512MB
-Xmx1g                                # Max heap: 1GB
-XX:MetaspaceSize=128m                # Initial metaspace
-XX:MaxMetaspaceSize=256m             # Max metaspace
-XX:MaxDirectMemorySize=256m          # Direct memory for NIO

# Garbage Collection
-XX:+UseG1GC                          # G1 Garbage Collector
-XX:MaxGCPauseMillis=200              # Target 200ms max pause

# Performance Optimizations
-XX:+UseStringDeduplication           # Reduce duplicate string memory
-XX:+UseCompressedOops                # Reduce memory footprint

# Monitoring & Diagnostics
-Xlog:gc*:file=logs/gc.log:time,level,tags
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=logs/heapdump.hprof

# System Configuration
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
-Djava.net.preferIPv4Stack=true
```

## 🔍 How It Works

### 1. Configuration Loading
```rust
let config = AppConfig::load(app);
let log_dir = config.get_log_dir();
let log_file = config.get_log_path();
```
- Loads `config.json` (port, logging paths)
- Creates log directory if it doesn't exist

### 2. JVM Options Preparation
```rust
// Prepare formatted arguments
let gc_log_arg = format!("-Xlog:gc*:file={}:time,level,tags", gc_log_path);
let heap_dump_arg = format!("-XX:HeapDumpPath={}", heap_dump_path);

// Build JVM options vector
let mut args: Vec<&str> = vec![
    "-Xms512m", "-Xmx1g",
    "-XX:+UseG1GC",
    // ... more options
];
```

### 3. Command Construction
```rust
// Final command structure:
// java [JVM_OPTIONS] -jar [JAR_PATH] [SPRING_OPTIONS]
args.push("-jar");
args.push(jar_path);
args.push("--spring.profiles.active=standalone,tauri,prod");
args.push(&format!("--server.port={}", port));
```

### 4. Process Launch
```rust
let (mut rx, child) = java_command
    .args(args)
    .spawn()?;
```

## 📊 Memory Footprint

With these settings, the Tauri desktop application uses:

| Component | Memory | Notes |
|-----------|--------|-------|
| JVM Heap | 512MB-1GB | For application data |
| Metaspace | 128-256MB | For class metadata |
| Direct Memory | 256MB | For NIO/database |
| **Total JVM** | **~900MB-1.5GB** | Conservative for desktop |
| Tauri Frontend | ~100-200MB | WebView renderer |
| **Total App** | **~1-1.7GB** | Complete application |

**Recommended System RAM:** 4GB minimum, 8GB recommended

## 🚀 Benefits

### 1. Automatic Optimization
- No manual JVM configuration needed
- Options tuned for desktop deployment
- Consistent across all installations

### 2. Smart Logging
- GC logs written to configured directory
- Heap dumps saved to logs folder
- All paths respect `config.json` settings

### 3. Memory Efficiency
- Conservative heap size (1GB max)
- Compressed object pointers enabled
- String deduplication reduces memory

### 4. Low Latency
- G1GC with 200ms target pause times
- Suitable for responsive desktop UI
- Minimal GC interruptions

### 5. Troubleshooting Ready
- Auto heap dump on OutOfMemoryError
- GC logging for performance analysis
- All logs in one directory

## 📝 Console Output

When the Tauri app starts, you'll see:

```
Starting PharmaSmart Standalone application...
Backend bundling is ENABLED
Using configuration: [path]/config.json
  - Port: 9080
  - Log Directory: [path]/logs
  - Log File: [path]/logs/pharmasmart.log

Starting Spring Boot backend on port 9080...
Using Java executable: java
Using JAR file: [path]/pharmaSmart-*.jar

JVM Options: ["-Xms512m", "-Xmx1g", "-XX:+UseG1GC", ...]

Backend process started with PID: 12345
Backend logs will be written to: [path]/logs/pharmasmart.log
Backend is ready and accepting connections on port 9080
```

## 🔧 Customization (Advanced)

If you need different JVM options for specific deployments, modify:

**File:** `src-tauri/src/backend_manager.rs`
**Section:** Lines 267-290

Example - Increase heap for high-volume pharmacy:
```rust
let mut args: Vec<&str> = vec![
    "-Xms1g",      // Changed from 512m
    "-Xmx2g",      // Changed from 1g
    // ... rest of options
];
```

Then rebuild: `npm run tauri:build:bundled-jre`

## 📁 Log Files Location

All backend logs are written to the configured log directory:

```
<installation_dir>/logs/
├── pharmasmart.log           # Application logs (Spring Boot)
├── gc.log                    # Garbage collection logs
└── heapdump.hprof           # Heap dump (only on OOM)
```

Location is determined by `config.json`:
```json
{
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  }
}
```

## 🎓 Understanding JVM Options

### Memory Options
- **-Xms**: Initial heap size (allocated at startup)
- **-Xmx**: Maximum heap size (JVM won't exceed this)
- **-XX:MetaspaceSize**: Space for class definitions
- **-XX:MaxDirectMemorySize**: Memory for NIO buffers

### Why 512MB-1GB for Desktop?
- Balances performance and memory usage
- Suitable for single-user desktop deployment
- Leaves RAM for OS and other apps
- Can grow to 1GB if needed

### Why G1GC?
- Low pause times (< 200ms)
- Good for interactive applications
- Automatic tuning
- Default in Java 11+

## 📈 Performance Monitoring

### View Real-time JVM Stats
```bash
# Find the Java process PID
jps -l | grep pharmaSmart

# Monitor memory and GC
jstat -gcutil <pid> 1000

# View all JVM flags
jcmd <pid> VM.flags
```

### Analyze GC Logs
Open `logs/gc.log` to see:
- GC pause times
- Memory usage patterns
- Collection frequencies
- Heap statistics

### Check Heap Dump (if OOM occurs)
```bash
# Use jhat (built-in)
jhat logs/heapdump.hprof

# Or use Eclipse Memory Analyzer (MAT)
# Import logs/heapdump.hprof
```

## ⚠️ Troubleshooting

### OutOfMemoryError: Java heap space

**Symptom:** Application crashes with OOM error

**Solution:**
1. Check `logs/heapdump.hprof` with MAT
2. Increase heap in `backend_manager.rs`:
   ```rust
   "-Xms1g", "-Xmx2g"  // Instead of 512m/1g
   ```
3. Rebuild Tauri app

### High Memory Usage

**Symptom:** App uses > 2GB RAM

**Check:**
1. View `logs/gc.log` for memory patterns
2. Monitor with `jstat -gc <pid> 1000`
3. Verify Hibernate cache settings
4. Check for memory leaks in heap dump

### Slow Startup

**Symptom:** Backend takes > 30s to start

**Solutions:**
1. Verify SSD (not HDD)
2. Exclude from antivirus scans
3. Increase code cache:
   ```rust
   "-XX:ReservedCodeCacheSize=256m"
   ```
4. Check CPU usage during startup

## 🔗 Related Files

- **JVM-CONFIGURATION.md** - Complete JVM tuning guide
- **JVM-OPTIONS-SUMMARY.md** - Quick reference
- **.jvmopts** - JVM options for server deployments
- **config.json** - Tauri backend configuration

## 🎉 Summary

The Tauri desktop application is now fully optimized with:
- ✅ Automatic JVM configuration
- ✅ Memory-efficient settings (512MB-1GB heap)
- ✅ Low-latency GC (G1GC, 200ms target)
- ✅ Production-ready monitoring (GC logs, heap dumps)
- ✅ Configurable via `config.json`

**No manual JVM configuration needed** - just build and distribute! 🚀
