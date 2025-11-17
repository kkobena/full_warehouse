# JVM Configuration Quick Start Guide

## 🚀 5-Minute Setup

This guide shows you how to quickly configure JVM settings for your PharmaSmart installation.

## 📁 Step 1: Open config.json

**Location:** Next to `PharmaSmart.exe`

**Windows:**
```
Right-click config.json → Open with → Notepad
```

**Linux/macOS:**
```bash
nano config.json
# or
vim config.json
```

## 📋 Step 2: Choose Your Scenario

The `config.json` file includes ready-to-use examples in the `_jvm_examples` section. Simply **copy the configuration that matches your needs** to the `jvm` section.

### Available Configurations:

| Scenario | RAM | Users | Description |
|----------|-----|-------|-------------|
| **development** | 2GB | Dev only | Development/Testing |
| **small_desktop** | 4GB | 1 user | Default - Single user |
| **medium_desktop** | 8GB | 1 user | Single user, high volume |
| **high_performance** | 16GB | 1 user | Maximum performance |
| **ultra_low_latency** | 16GB+ | 1 user | Real-time, sub-ms pauses |
| **small_production_server** | 4-8GB | 10-50 | Small pharmacy |
| **medium_production_server** | 8-16GB | 50-200 | Medium pharmacy chain |
| **large_production_server** | 16GB+ | 200+ | Large pharmacy chain |
| **debugging** | 8GB | Any | Troubleshooting issues |

## ✏️ Step 3: Copy Configuration

### Example: Upgrade to Medium Desktop

**Before:**
```json
{
  "server": { "port": 9080 },
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  },
  "jvm": {
    "heap_min": "512m",
    "heap_max": "1g",
    "metaspace_size": "128m",
    "metaspace_max": "256m",
    "direct_memory_size": "256m",
    "max_gc_pause_millis": "200",
    "additional_options": []
  },
  "_jvm_examples": {
    "medium_desktop": {
      "description": "Medium Desktop (8GB RAM)",
      "heap_min": "1g",
      "heap_max": "2g",
      "metaspace_size": "256m",
      "metaspace_max": "384m",
      "direct_memory_size": "384m",
      "max_gc_pause_millis": "200",
      "additional_options": []
    }
  }
}
```

**After:** Copy values from `medium_desktop` to `jvm`:
```json
{
  "server": { "port": 9080 },
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  },
  "jvm": {
    "heap_min": "1g",          ← Changed
    "heap_max": "2g",          ← Changed
    "metaspace_size": "256m",  ← Changed
    "metaspace_max": "384m",   ← Changed
    "direct_memory_size": "384m", ← Changed
    "max_gc_pause_millis": "200",
    "additional_options": []
  }
}
```

**Note:** Keep the `_jvm_examples` section - it's ignored by the application and serves as reference.

## 🔄 Step 4: Save & Restart

1. **Save** the file (Ctrl+S in Notepad)
2. **Close** PharmaSmart completely
3. **Restart** PharmaSmart
4. **Verify** in console/logs:
   ```
   JVM Configuration:
     - Heap: 1g to 2g
     - Metaspace: 256m to 384m
     - Direct Memory: 384m
     - Max GC Pause: 200ms
   ```

## 📊 Configuration Reference Table

### Desktop Configurations

#### Small Desktop (Default)
```json
"heap_min": "512m",
"heap_max": "1g",
"metaspace_size": "128m",
"metaspace_max": "256m",
"direct_memory_size": "256m",
"max_gc_pause_millis": "200"
```
**RAM:** 4GB | **Use:** Single user, low-medium volume

#### Medium Desktop
```json
"heap_min": "1g",
"heap_max": "2g",
"metaspace_size": "256m",
"metaspace_max": "384m",
"direct_memory_size": "384m",
"max_gc_pause_millis": "200"
```
**RAM:** 8GB | **Use:** Single user, medium-high volume

#### High Performance
```json
"heap_min": "2g",
"heap_max": "4g",
"metaspace_size": "384m",
"metaspace_max": "512m",
"direct_memory_size": "512m",
"max_gc_pause_millis": "150"
```
**RAM:** 16GB | **Use:** Single user, high volume, fast operations

### Server Configurations

#### Small Production Server
```json
"heap_min": "2g",
"heap_max": "2g",
"metaspace_size": "256m",
"metaspace_max": "384m",
"direct_memory_size": "384m",
"max_gc_pause_millis": "200"
```
**RAM:** 4-8GB | **Users:** 10-50 | **Use:** Single location

#### Medium Production Server
```json
"heap_min": "4g",
"heap_max": "4g",
"metaspace_size": "384m",
"metaspace_max": "512m",
"direct_memory_size": "512m",
"max_gc_pause_millis": "150"
```
**RAM:** 8-16GB | **Users:** 50-200 | **Use:** 2-5 locations

#### Large Production Server
```json
"heap_min": "8g",
"heap_max": "8g",
"metaspace_size": "512m",
"metaspace_max": "1g",
"direct_memory_size": "1g",
"max_gc_pause_millis": "100"
```
**RAM:** 16GB+ | **Users:** 200+ | **Use:** Multiple locations

### Special Configurations

#### Ultra-Low Latency (ZGC)
```json
"heap_min": "4g",
"heap_max": "4g",
"metaspace_size": "512m",
"metaspace_max": "768m",
"direct_memory_size": "1g",
"max_gc_pause_millis": "50",
"additional_options": [
  "-XX:+UseZGC",
  "-XX:ZAllocationSpikeTolerance=2"
]
```
**RAM:** 16GB+ | **Use:** Real-time requirements, sub-millisecond pauses

#### Debugging
```json
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
```
**Use:** Investigating performance issues, memory leaks

## 💡 Pro Tips

### Tip 1: Always Match heap_min and heap_max
```json
✅ Good:
"heap_min": "2g",
"heap_max": "2g"

❌ Bad:
"heap_min": "512m",
"heap_max": "4g"
```
**Why:** Prevents performance overhead from heap resizing

### Tip 2: Leave 25% RAM for System
```
8GB RAM  → Use max 6GB  → heap_max: "4g"
16GB RAM → Use max 12GB → heap_max: "8g"
32GB RAM → Use max 24GB → heap_max: "16g"
```

### Tip 3: Start Conservative, Scale Up
1. Start with **small_desktop** (1GB heap)
2. Monitor for 1-2 days
3. If memory usage consistently > 80%, upgrade to **medium_desktop**
4. Repeat until optimal

### Tip 4: Monitor GC Logs
```bash
# Check GC activity
tail -f logs/gc.log

# Look for:
# - Pause times > max_gc_pause_millis
# - Frequent full GCs
# - High memory usage
```

## ⚠️ Common Mistakes

### Mistake 1: Setting heap too high
```json
❌ System has 8GB RAM, setting heap_max: "8g"
✅ System has 8GB RAM, setting heap_max: "4g" or "6g"
```
**Why:** OS and other apps need RAM too

### Mistake 2: Forgetting to save
- Edit config.json
- **MUST SAVE** (Ctrl+S)
- Then restart

### Mistake 3: Invalid JSON
```json
❌ "heap_max": "2g",  ← Trailing comma before closing }
}

✅ "heap_max": "2g"   ← No comma
}
```
**Validate:** Use https://jsonlint.com/

## 🔍 Verification

After restart, check if settings applied:

### Method 1: Console Output
```
JVM Configuration:
  - Heap: 2g to 2g
  - Metaspace: 256m to 384m
  - Direct Memory: 384m
  - Max GC Pause: 200ms
Complete JVM Options: ["-Xms2g", "-Xmx2g", ...]
```

### Method 2: JVM Process
```bash
# Find process
jps -l | grep pharmaSmart

# Check flags
jcmd <pid> VM.flags | grep -E "Heap|Metaspace"
```

## 📚 Need More Help?

- **Full Guide:** [CUSTOMIZE-JVM-OPTIONS.md](CUSTOMIZE-JVM-OPTIONS.md)
- **Technical Details:** [JVM-CONFIGURATION.md](JVM-CONFIGURATION.md)
- **Integration Info:** [TAURI-JVM-INTEGRATION.md](TAURI-JVM-INTEGRATION.md)

## 🎯 Quick Decision Tree

```
Do you have >= 16GB RAM?
  ├─ YES → Use "high_performance" or "ultra_low_latency"
  └─ NO
      │
      Do you have >= 8GB RAM?
        ├─ YES → Use "medium_desktop" or "medium_production_server"
        └─ NO
            │
            Do you have >= 4GB RAM?
              ├─ YES → Use "small_desktop" (default)
              └─ NO → Use "development"
```

## ✅ Summary

1. **Open** config.json next to PharmaSmart.exe
2. **Copy** values from `_jvm_examples` section
3. **Paste** into `jvm` section
4. **Save** file
5. **Restart** application
6. **Verify** in console output

**That's it!** No compilation, no complex setup - just edit and restart! 🎉
