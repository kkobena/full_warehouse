# Sidecar Directory

This directory contains resources that will be bundled with the Tauri application.

---

## Contents

### For Bundled Backend Build

Place the Spring Boot JAR file here:
```
sidecar/
└── pharmaSmart-0.0.1-SNAPSHOT.jar
```

**How to prepare:**
```bash
# Run from project root
npm run tauri:prepare-sidecar
```

This script copies `target/pharmaSmart-*.jar` to this directory.

---

### For Complete Build (Bundled JRE)

Place both the JAR and JRE here:

```
sidecar/
├── pharmaSmart-0.0.1-SNAPSHOT.jar   ← Backend JAR
└── jre/                             ← Bundled JRE
    ├── bin/
    │   └── java.exe                 ← Java executable
    ├── lib/
    └── ...
```

**How to prepare:**

1. **Download JRE** from https://adoptium.net/temurin/releases/
   - Version: 21 (LTS) or 25
   - OS: Windows
   - Architecture: x64
   - Type: JRE (not JDK)
   - Format: ZIP Archive

2. **Extract the JRE**
   ```bash
   # Example: Extract OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip
   # This creates: jdk-21.0.5+11-jre/
   ```

3. **Copy to sidecar directory**
   ```bash
   # Windows CMD
   xcopy /E /I "path\to\jdk-21.0.5+11-jre" "src-tauri\sidecar\jre"

   # Git Bash / PowerShell
   cp -r path/to/jdk-21.0.5+11-jre src-tauri/sidecar/jre
   ```

4. **Verify structure**
   ```bash
   # Check that java.exe exists
   dir src-tauri\sidecar\jre\bin\java.exe
   ```

5. **Prepare backend JAR**
   ```bash
   npm run tauri:prepare-sidecar
   ```

---

## Build Modes

| Mode | JAR Required | JRE Required | Command |
|------|--------------|--------------|---------|
| **Standard** | ❌ | ❌ | `npm run tauri:build` |
| **Bundled Backend** | ✅ | ❌ | `npm run tauri:build:bundled` |
| **Complete (JRE)** | ✅ | ✅ | `npm run tauri:build:bundled-jre` |

---

## Directory Size

Be aware of the sizes:

- **JAR file**: ~100 MB
- **JRE**: ~200 MB
- **Total**: ~300 MB (when both are present)

This directory should be excluded from version control (`.gitignore`).

---

## .gitignore

The following should be ignored:

```gitignore
# JAR files
*.jar

# JRE directory
jre/
```

Only commit this README file, not the actual binaries.

---

## Troubleshooting

### "JAR file not found" error

**Solution:**
```bash
# Build the backend first
mvnw.cmd clean package -Pprod

# Then prepare sidecar
npm run tauri:prepare-sidecar
```

### "Bundled JRE not found" error

**Check:**
1. Does `src-tauri/sidecar/jre/` exist?
2. Does `src-tauri/sidecar/jre/bin/java.exe` exist?
3. Did you copy the entire JRE folder contents?

**Fix:**
```bash
# Verify java.exe exists
dir src-tauri\sidecar\jre\bin\java.exe

# If not found, re-extract and copy the JRE
```

---

## Related Documentation

- [TAURI-BUILD-MODES.md](../../TAURI-BUILD-MODES.md) - Build mode comparison
- [TAURI-BUNDLED-JRE-SETUP.md](../../TAURI-BUNDLED-JRE-SETUP.md) - Complete setup guide
- [TAURI_BACKEND_SETUP.md](../../TAURI_BACKEND_SETUP.md) - Technical documentation

---

*Last Updated: 2025-01-16*
