# Backend Logs - Quick Reference

## 📍 Where Are My Logs?

### Windows
```
C:\Users\[YourUsername]\PharmaSmart\logs\pharmasmart.log
```
**Quick Access:** Press `Windows + R`, type `%USERPROFILE%\PharmaSmart\logs`, press Enter

### Linux
```
~/PharmaSmart/logs/pharmasmart.log
```

### macOS
```
~/PharmaSmart/logs/pharmasmart.log
```

---

## 📊 What's in the Logs Folder?

| File | Description |
|------|-------------|
| `pharmasmart.log` | **Current log** - Today's events |
| `pharmasmart.log.2025-01-16.0.gz` | **Yesterday's log** - Compressed archive |
| `pharmasmart.log.2025-01-15.0.gz` | **Older logs** - Previous days (up to 7 days) |

---

## ⚙️ Log Settings at a Glance

| Setting | Value |
|---------|-------|
| **Log Rotation** | Daily + when file reaches 10MB |
| **History** | 7 days (older logs deleted automatically) |
| **Max Total Size** | 100MB |
| **Compression** | Yes (gzip for archived logs) |
| **Format** | Timestamp, Level, Thread, Logger, Message |

---

## 🔍 How to Read Logs

### Log Entry Example
```
2025-01-16T10:30:45.123+00:00  INFO 12345 --- [main] c.k.warehouse.App : Application started
```

**What Each Part Means:**
- `2025-01-16T10:30:45.123` - When it happened
- `INFO` - Log level (DEBUG, INFO, WARN, ERROR)
- `12345` - Process ID
- `[main]` - Thread name
- `c.k.warehouse.App` - Which part of the code
- `Application started` - What happened

### Log Levels
- 🟢 **DEBUG** - Detailed information (application code)
- 🔵 **INFO** - Important events (startup, normal operations)
- 🟡 **WARN** - Warnings (potential issues)
- 🔴 **ERROR** - Errors (something went wrong)

---

## 🛠️ Common Problems & Solutions

### Problem: Backend won't start

**Step 1:** Open the log file
```
Windows: %USERPROFILE%\PharmaSmart\logs\pharmasmart.log
```

**Step 2:** Look for these messages

| Error Message | What It Means | Solution |
|---------------|---------------|----------|
| `Address already in use` | Port 8080 is occupied | Stop other apps using port 8080 |
| `Failed to configure a DataSource` | Can't connect to database | Check database is running |
| `OutOfMemoryError` | Not enough memory | Close other apps or increase Java memory |
| `ClassNotFoundException` | Missing files | Reinstall the application |
| `Permission denied` | No write access | Check folder permissions |

**Step 3:** Search for keywords
- Press `Ctrl + F` in Notepad
- Search for: `ERROR`, `Exception`, `Failed`, or `Unable to`

---

## 📋 Quick Commands

### View Current Log (Windows)
```batch
notepad %USERPROFILE%\PharmaSmart\logs\pharmasmart.log
```

### View Current Log (Linux/macOS)
```bash
cat ~/PharmaSmart/logs/pharmasmart.log
```

### Watch Log Live (Linux/macOS)
```bash
tail -f ~/PharmaSmart/logs/pharmasmart.log
```

### Open Log Directory (Windows)
```batch
explorer %USERPROFILE%\PharmaSmart\logs
```

### Delete Old Logs (Manual Cleanup)
**Windows:**
```batch
del /Q %USERPROFILE%\PharmaSmart\logs\*.gz
```

**Linux/macOS:**
```bash
rm ~/PharmaSmart/logs/*.gz
```

---

## 🎯 When to Check Logs

✅ **Check logs when:**
- Application won't start
- Backend shows errors
- App is slow or freezing
- After updates or configuration changes
- Before contacting support

❌ **No need to check if:**
- Everything works normally
- This is your first time running the app

---

## 📧 Sharing Logs with Support

If you need to send logs to support:

1. **Locate the log file:**
   - Windows: `%USERPROFILE%\PharmaSmart\logs\pharmasmart.log`

2. **Compress it (optional):**
   - Right-click → Send to → Compressed (zipped) folder

3. **Attach to email:**
   - Include today's log (`pharmasmart.log`)
   - If issue is recurring, include archived logs too

4. **What to include in email:**
   - When the problem started
   - What you were doing when it happened
   - Any error messages you saw
   - The log files

---

## 🔧 Advanced: Configure Logging

### Change Log Level (More/Less Detail)

**Location:** `src/main/resources/logback-spring.xml`

**Example:** To see more details, change from `WARN` to `DEBUG`
```xml
<logger level="DEBUG" name="com.kobe.warehouse"/>
```

### Change Log Location

**Location:** `src-tauri/src/backend_manager.rs` (line ~178)

**Default:**
```rust
let log_dir = home_dir.join("PharmaSmart").join("logs");
```

**Custom location:**
```rust
let log_dir = PathBuf::from("C:\\MyLogs\\PharmaSmart");
```

### Change Log Retention (Keep Logs Longer)

**Location:** `src/main/resources/logback-spring.xml` (line ~34)

**Default:** 7 days
```xml
<maxHistory>7</maxHistory>
```

**Change to 30 days:**
```xml
<maxHistory>30</maxHistory>
```

---

## ✅ Checklist: Is Logging Working?

- [ ] Application started successfully
- [ ] Log directory exists: `~/PharmaSmart/logs`
- [ ] Current log file exists: `pharmasmart.log`
- [ ] Log file has today's date/time entries
- [ ] Can open and read the log file
- [ ] Log file size is reasonable (< 10MB)
- [ ] Old logs are being archived (.gz files)

---

## 📚 Related Documentation

- **Full Setup Guide:** [TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md)
- **Backend Configuration:** [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md)
- **Technical Details:** `src/main/resources/logback-spring.xml`

---

## 💡 Tips

- Logs are **automatically managed** - no manual cleanup needed
- **Don't delete** the log directory while the app is running
- **Compress** old logs before sharing (saves bandwidth)
- **Check timestamps** to see when problems occurred
- **Search** for keywords instead of reading the entire log
- **Keep recent logs** if you need to report a recurring issue

---

**Need More Help?**

If logs don't help solve your problem:
1. Check [TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md) troubleshooting section
2. Search for the error message online
3. Contact support with your log files

---

*Last Updated: 2025-01-16*
