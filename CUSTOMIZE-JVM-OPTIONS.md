# Customize JVM Options in Production

## Overview

PharmaSmart Tauri standalone application allows **full customization of JVM options** without recompiling. Simply edit the `config.json` file next to the application executable.

## 📁 Location

The configuration file is located at:
```
<Installation Directory>/config.json
```

Example locations:
- **Windows**: `C:\Program Files\PharmaSmart\config.json`
- **Linux**: `/opt/pharmasmart/config.json`
- **Portable**: `.\PharmaSmart\config.json`

## 📝 Configuration Format

### Complete Example

```json
{
  "server": {
    "port": 9080
  },
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  },
  "installation": {
    "directory": ""
  },
  "jvm": {
    "heap_min": "512m",
    "heap_max": "1g",
    "metaspace_size": "128m",
    "metaspace_max": "256m",
    "direct_memory_size": "256m",
    "max_gc_pause_millis": "200",
    "additional_options": []
  }
}
```

## ⚙️ JVM Configuration Options

### 1. Memory Settings

#### `heap_min` (Initial Heap Size)
- **Default**: `"512m"`
- **Description**: Starting heap memory allocated to JVM
- **Format**: Number followed by `m` (megabytes) or `g` (gigabytes)
- **Examples**:
  ```json
  "heap_min": "512m"   // 512 megabytes
  "heap_min": "1g"     // 1 gigabyte
  "heap_min": "2g"     // 2 gigabytes
  ```

#### `heap_max` (Maximum Heap Size)
- **Default**: `"1g"`
- **Description**: Maximum heap memory JVM can use
- **Format**: Number followed by `m` or `g`
- **Recommendation**: Set to same value as `heap_min` for stable performance
- **Examples**:
  ```json
  "heap_max": "1g"     // 1 gigabyte
  "heap_max": "2g"     // 2 gigabytes
  "heap_max": "4g"     // 4 gigabytes (high-volume)
  ```

#### `metaspace_size` (Initial Metaspace)
- **Default**: `"128m"`
- **Description**: Initial memory for class metadata
- **When to increase**: If you see "OutOfMemoryError: Metaspace" errors

#### `metaspace_max` (Maximum Metaspace)
- **Default**: `"256m"`
- **Description**: Maximum memory for class metadata
- **Recommendation**: Usually 256m-512m is sufficient

#### `direct_memory_size` (Direct Memory)
- **Default**: `"256m"`
- **Description**: Memory for NIO buffers (database connections, file I/O)
- **When to increase**: If you have many concurrent database connections

### 2. Garbage Collection

#### `max_gc_pause_millis` (Max GC Pause Time)
- **Default**: `"200"`
- **Description**: Target maximum pause time in milliseconds
- **Lower value**: More frequent, shorter GC pauses (better responsiveness)
- **Higher value**: Less frequent, longer pauses (better throughput)
- **Examples**:
  ```json
  "max_gc_pause_millis": "100"   // Ultra-responsive (more CPU)
  "max_gc_pause_millis": "200"   // Balanced (recommended)
  "max_gc_pause_millis": "500"   // Higher throughput
  ```

### 3. Additional Options

#### `additional_options` (Custom JVM Flags)
- **Default**: `[]`
- **Description**: Array of any additional JVM options
- **Use case**: Advanced tuning, debugging, monitoring
- **Examples**:
  ```json
  "additional_options": [
    "-XX:+PrintGCDetails",
    "-XX:+UseZGC",
    "-Dmy.custom.property=value"
  ]
  ```

## 💡 Common Scenarios

### Scenario 1: Small Desktop (4GB RAM)
**Use case:** Single user, small pharmacy

```json
"jvm": {
  "heap_min": "512m",
  "heap_max": "1g",
  "metaspace_size": "128m",
  "metaspace_max": "256m",
  "direct_memory_size": "256m",
  "max_gc_pause_millis": "200",
  "additional_options": []
}
```

**Total RAM used:** ~1-1.5GB

### Scenario 2: Medium Desktop (8GB RAM)
**Use case:** Single user, medium-volume pharmacy

```json
"jvm": {
  "heap_min": "1g",
  "heap_max": "2g",
  "metaspace_size": "256m",
  "metaspace_max": "384m",
  "direct_memory_size": "384m",
  "max_gc_pause_millis": "200",
  "additional_options": []
}
```

**Total RAM used:** ~2-2.7GB

### Scenario 3: High-Performance (16GB RAM)
**Use case:** High-volume pharmacy, multiple concurrent operations

```json
"jvm": {
  "heap_min": "2g",
  "heap_max": "4g",
  "metaspace_size": "384m",
  "metaspace_max": "512m",
  "direct_memory_size": "512m",
  "max_gc_pause_millis": "150",
  "additional_options": []
}
```

**Total RAM used:** ~4-5GB

### Scenario 4: Ultra-Low Latency
**Use case:** Real-time requirements, sub-millisecond pauses

```json
"jvm": {
  "heap_min": "2g",
  "heap_max": "4g",
  "metaspace_size": "384m",
  "metaspace_max": "512m",
  "direct_memory_size": "512m",
  "max_gc_pause_millis": "100",
  "additional_options": [
    "-XX:+UseZGC",
    "-XX:ZAllocationSpikeTolerance=2"
  ]
}
```

**Note:** ZGC replaces G1GC, provides sub-millisecond pauses

### Scenario 5: Debugging / Troubleshooting
**Use case:** Investigating memory issues or performance problems

```json
"jvm": {
  "heap_min": "1g",
  "heap_max": "2g",
  "metaspace_size": "256m",
  "metaspace_max": "384m",
  "direct_memory_size": "384m",
  "max_gc_pause_millis": "200",
  "additional_options": [
    "-XX:+PrintGCDetails",
    "-XX:+PrintGCDateStamps",
    "-XX:NativeMemoryTracking=summary",
    "-verbose:gc"
  ]
}
```

**Use JMC or VisualVM to analyze logs**

## 🔧 How to Apply Changes

### Step 1: Edit config.json
1. Open `config.json` with Notepad (Windows) or nano/vim (Linux)
2. Modify the `jvm` section
3. Save the file

### Step 2: Restart Application
- **Desktop App**: Close and restart PharmaSmart
- **Service** (Linux):
  ```bash
  sudo systemctl restart pharmasmart
  ```
- **Manual**:
  - Stop the current instance (Ctrl+C or Task Manager)
  - Start again

### Step 3: Verify Changes
Check the application console/logs for:
```
JVM Configuration:
  - Heap: 512m to 1g
  - Metaspace: 128m to 256m
  - Direct Memory: 256m
  - Max GC Pause: 200ms
Complete JVM Options: ["-Xms512m", "-Xmx1g", ...]
```

## ⚠️ Important Guidelines

### 1. Always Set heap_min = heap_max
**Why:** Prevents heap resizing overhead
```json
✅ Good:
"heap_min": "2g",
"heap_max": "2g"

❌ Bad:
"heap_min": "512m",
"heap_max": "4g"
```

### 2. Leave Room for OS
**Rule:** Use max 75% of total RAM for JVM
```
8GB RAM  → Max 6GB for JVM  → heap_max: "4g"
16GB RAM → Max 12GB for JVM → heap_max: "8g"
```

### 3. Watch for OutOfMemory Errors
If you see OOM errors:
1. Check `logs/heapdump.hprof` with Eclipse MAT
2. Increase `heap_max` if needed
3. Or fix memory leaks in application

### 4. Test Before Production
Always test configuration changes:
1. Apply changes in test environment
2. Monitor for 24-48 hours
3. Check `logs/gc.log` for issues
4. Then apply to production

### 5. Monitor Performance
```bash
# View real-time stats
jstat -gcutil <pid> 1000

# Check JVM flags
jcmd <pid> VM.flags

# Native memory tracking
jcmd <pid> VM.native_memory summary
```

## 🎯 Quick Reference Table

| Setting | Min | Default | Recommended Max | Notes |
|---------|-----|---------|-----------------|-------|
| heap_min/max | 256m | 512m/1g | 75% of RAM | Keep equal |
| metaspace_size | 64m | 128m | 512m | For class metadata |
| metaspace_max | 128m | 256m | 1g | Rarely needs > 512m |
| direct_memory_size | 128m | 256m | 2g | For NIO/DB |
| max_gc_pause_millis | 50 | 200 | 1000 | Lower = responsive |

## 🚨 Troubleshooting

### Config.json Not Found
**Error:** "config.json not found, using defaults"

**Solution:**
1. Check installation directory
2. Create `config.json` manually
3. Copy from `resources/config.default.json`

### Invalid JSON Format
**Error:** "Failed to parse config.json"

**Solution:**
1. Validate JSON: https://jsonlint.com/
2. Check for missing commas
3. Check for trailing commas (not allowed)
4. Ensure all strings use double quotes

### Changes Not Applied
**Problem:** Modified config.json but see old values

**Solution:**
1. Verify correct file location
2. Restart application completely
3. Check console output for loaded config
4. Verify JSON is valid

### OutOfMemoryError
**Error:** "java.lang.OutOfMemoryError: Java heap space"

**Solution:**
1. Increase `heap_max`: `"2g"` or `"4g"`
2. Analyze `logs/heapdump.hprof`
3. Check for memory leaks
4. Verify system has enough RAM

### Slow Performance
**Problem:** Application feels sluggish

**Solution:**
1. Check `logs/gc.log` for frequent GC
2. Increase `heap_max` if GC overhead > 10%
3. Lower `max_gc_pause_millis` for responsiveness
4. Monitor with `jstat -gcutil <pid> 1000`

## 📚 Additional Resources

- **JVM-CONFIGURATION.md** - Complete JVM tuning guide
- **JVM-OPTIONS-SUMMARY.md** - Quick reference
- **TAURI-JVM-INTEGRATION.md** - How it works internally
- **logs/gc.log** - GC activity logs
- **logs/heapdump.hprof** - Memory dump (on OOM)

## 🎉 Summary

You can now customize JVM options in production by:
1. Editing `config.json` next to the executable
2. Modifying the `jvm` section
3. Restarting the application

No recompilation needed! Changes take effect immediately on restart.
