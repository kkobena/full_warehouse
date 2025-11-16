# Tauri Bundled JRE Setup Guide

This guide explains how to build a completely standalone PharmaSmart desktop application that includes both the Spring Boot backend and a Java Runtime Environment (JRE), requiring **no external Java installation** on the target system.

---

## Overview

PharmaSmart supports three Tauri build modes:

| Mode                | Configuration File            | What's Bundled           | Requires Java?  |
| ------------------- | ----------------------------- | ------------------------ | --------------- |
| **Standard**        | `tauri.conf.json`             | Frontend only            | ✅ Yes (system) |
| **Bundled Backend** | `tauri.bundled.conf.json`     | Frontend + Backend JAR   | ✅ Yes (system) |
| **Complete (JRE)**  | `tauri.bundled-jre.conf.json` | Frontend + Backend + JRE | ❌ No           |

This guide focuses on the **Complete (JRE)** mode.

---

## Prerequisites

### Build Machine Requirements

- **Node.js** >= 22.14.0
- **Rust** toolchain (for Tauri)
- **Java JDK** >= 21 (for building the Spring Boot JAR)
- **Maven** wrapper (`mvnw.cmd` or `./mvnw`)

### Download JRE

You need to download a portable JRE to bundle with your application.

**Recommended: Eclipse Temurin JRE**

1. Visit: https://adoptium.net/temurin/releases/
2. Select:
   - **Version**: 21 (LTS) or 25
   - **Operating System**: Windows
   - **Architecture**: x64
   - **Package Type**: JRE
   - **Image Type**: ZIP Archive
3. Download the ZIP file (e.g., `OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip`)

**Alternative: Oracle JRE**

- Visit: https://www.oracle.com/java/technologies/downloads/
- Download the JRE (not JDK) for Windows x64

---

## Setup Steps

### Step 1: Prepare the JRE

1. **Extract the downloaded JRE ZIP file**

   Example:

   ```
   OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip
   └── jdk-21.0.5+11-jre/
       ├── bin/
       │   ├── java.exe
       │   └── ...
       ├── lib/
       └── ...
   ```

2. **Create the sidecar directory** (if it doesn't exist):

   ```bash
   mkdir src-tauri\sidecar
   ```

3. **Copy the extracted JRE to the sidecar directory**:

   ```bash
   # Copy the entire JRE folder
   xcopy /E /I "path\to\jdk-21.0.5+11-jre" "src-tauri\sidecar\jre"
   ```

   Your directory structure should look like this:

   ```
   src-tauri/
   ├── sidecar/
   │   ├── jre/
   │   │   ├── bin/
   │   │   │   ├── java.exe      ← Important!
   │   │   │   └── ...
   │   │   ├── lib/
   │   │   └── ...
   │   └── pharmaSmart-*.jar     ← Will be created by prepare-sidecar script
   ├── tauri.bundled-jre.conf.json
   └── ...
   ```

**IMPORTANT**: The JRE must be at `src-tauri/sidecar/jre/` with `bin/java.exe` inside it.

### Step 2: Build the Spring Boot Backend

Build the backend JAR file:

```bash
# Windows CMD
mvnw.cmd clean package -Pprod

# Git Bash / Linux / macOS
./mvnw clean package -Pprod
```

This creates: `target/pharmaSmart-{version}.jar`

### Step 3: Prepare the Sidecar

Copy the JAR file to the sidecar directory:

```bash
npm run tauri:prepare-sidecar
```

This script copies `target/pharmaSmart-*.jar` to `src-tauri/sidecar/`.

### Step 4: Build the Tauri App with Bundled JRE

**Production Build:**

```bash
npm run tauri:build:bundled-jre
```

**Debug Build (faster compilation):**

```bash
npm run tauri:build:bundled-jre:debug
```

**Fast Build (skip frontend rebuild):**

```bash
npm run tauri:build:bundled-jre:fast
```

### Step 5: Locate the Installer

The installer will be created at:

```
src-tauri/target/release/bundle/
├── nsis/
│   └── PharmaSmart-Complete_0.0.1_x64-setup.exe   ← NSIS installer
└── msi/
    └── PharmaSmart-Complete_0.0.1_x64_en-US.msi   ← MSI installer
```

---

## How It Works

### JRE Detection Flow

The Rust backend manager (`src-tauri/src/backend_manager.rs`) uses this logic:

1. **Check for bundled JRE** at `resources/sidecar/jre/bin/java.exe`
2. If found → Use bundled JRE ✅
3. If not found → Check for system Java
4. If system Java found → Use system Java ✅
5. If neither found → Show error ❌

### Runtime Behavior

When the app starts:

```
[Tauri] Checking for Java...
[Tauri] Found bundled JRE at: C:\Program Files\PharmaSmart-Complete\resources\sidecar\jre\bin\java.exe
[Tauri] Using bundled JRE...
[Tauri] Locating backend JAR file...
[Tauri] Found JAR file: C:\Program Files\PharmaSmart-Complete\resources\sidecar\pharmaSmart-0.0.1-SNAPSHOT.jar
[Tauri] Starting Spring Boot backend...
[Tauri] Backend process started with PID: 12345
[Tauri] Backend is ready and accepting connections on port 8080
```

---

## File Sizes

Be aware of the increased file size:

| Component           | Approximate Size |
| ------------------- | ---------------- |
| Tauri Frontend      | ~50 MB           |
| Spring Boot JAR     | ~100 MB          |
| JRE (bundled)       | ~200 MB          |
| **Total Installer** | **~350 MB**      |

The NSIS installer compresses this to approximately **~150-200 MB**.

---

## Testing

### Test the Bundled App Locally

After building, test the installer:

1. Install the app on a test machine **WITHOUT Java**
2. Run the installed application
3. Verify the backend starts successfully
4. Check the splash screen shows "Using bundled JRE..."

### Test on Clean System

**Recommended**: Test on a clean Windows VM or machine without Java installed to ensure the bundled JRE works correctly.

---

## Troubleshooting

### Issue: "Bundled JRE not found"

**Symptom**: App shows "Checking system Java..." instead of "Using bundled JRE..."

**Solution**:

1. Verify JRE is at `src-tauri/sidecar/jre/`
2. Check `java.exe` exists at `src-tauri/sidecar/jre/bin/java.exe`
3. Rebuild the app

### Issue: "Java not found" error on startup

**Cause**: JRE wasn't bundled correctly

**Solution**:

1. Check `src-tauri/tauri.bundled-jre.conf.json` includes:
   ```json
   "resources": [
     "sidecar/pharmaSmart-*.jar",
     "sidecar/jre/**/*"
   ]
   ```
2. Verify the JRE folder structure
3. Rebuild with `npm run tauri:build:bundled-jre`

### Issue: Build is very slow

**Cause**: Bundling 200MB+ of JRE files takes time

**Solutions**:

- Use `npm run tauri:build:bundled-jre:debug` for faster debug builds
- Use `npm run tauri:build:bundled-jre:fast` if frontend hasn't changed
- Build on a machine with SSD (not HDD)

### Issue: Installer is too large

**Cause**: JRE adds ~200MB to the bundle

**Solutions**:

- Use NSIS installer (better compression than MSI)
- Consider using the **Bundled Backend** mode instead (requires Java on target system)
- Use a minimal JRE distribution (see below)

---

## Advanced: Using Minimal JRE

To reduce bundle size, create a minimal JRE with only required modules using `jlink`:

### Step 1: Determine Required Modules

Run the JAR and analyze dependencies:

```bash
java -jar target/pharmaSmart-*.jar --dry-run
jdeps --print-module-deps target/pharmaSmart-*.jar
```

### Step 2: Create Minimal JRE

```bash
# Example: Create minimal JRE with common modules
jlink ^
  --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.xml,java.desktop ^
  --output src-tauri/sidecar/jre-minimal ^
  --compress=2 ^
  --no-header-files ^
  --no-man-pages
```

### Step 3: Update Configuration

Update `src-tauri/tauri.bundled-jre.conf.json`:

```json
"resources": [
  "sidecar/pharmaSmart-*.jar",
  "sidecar/jre-minimal/**/*"
]
```

Update `src-tauri/src/backend_manager.rs`:

```rust
let jre_dir = resource_dir.join("sidecar").join("jre-minimal");
```

This can reduce JRE size from ~200MB to ~50-80MB.

---

## Comparison of Build Modes

### Standard Mode (`tauri.conf.json`)

**Pros:**

- Smallest installer size (~50MB)
- Fastest build time
- Easy to update backend separately

**Cons:**

- Requires Java installation on target system
- User must configure backend URL

**Use Case:** Development, tech-savvy users with Java installed

---

### Bundled Backend Mode (`tauri.bundled.conf.json`)

**Pros:**

- No backend configuration needed
- Backend always matches frontend version
- Medium installer size (~150MB)

**Cons:**

- Still requires Java installation
- Larger than standard mode

**Use Case:** Deployment to systems with Java already installed

---

### Complete Mode (`tauri.bundled-jre.conf.json`)

**Pros:**

- **No Java installation required** ✅
- Completely standalone application
- Works on any Windows system

**Cons:**

- Largest installer size (~200MB)
- Longest build time
- JRE updates require rebuilding entire app

**Use Case:** Deployment to systems without Java, maximum portability

---

## Build Scripts Reference

```bash
# Complete Mode (with bundled JRE)
npm run tauri:build:bundled-jre         # Production build
npm run tauri:build:bundled-jre:debug   # Debug build (faster)
npm run tauri:build:bundled-jre:fast    # Skip frontend rebuild

# Bundled Backend Mode (requires system Java)
npm run tauri:build:bundled             # Production build
npm run tauri:build:bundled:debug       # Debug build
npm run tauri:build:bundled:fast        # Fast build

# Standard Mode (frontend only)
npm run tauri:build                     # Production build
npm run tauri:build:debug               # Debug build
npm run tauri:build:fast                # Fast build
```

---

## Configuration Files

### `src-tauri/tauri.bundled-jre.conf.json`

Key settings:

```json
{
  "productName": "PharmaSmart-Complete",
  "identifier": "com.kobe.warehouse.complete",
  "bundle": {
    "resources": [
      "sidecar/pharmaSmart-*.jar", // Spring Boot JAR
      "sidecar/jre/**/*" // Bundled JRE
    ]
  }
}
```

---

## Updating the Bundled JRE

To update to a newer JRE version:

1. Download the new JRE ZIP
2. Delete `src-tauri/sidecar/jre/`
3. Extract new JRE to `src-tauri/sidecar/jre/`
4. Rebuild: `npm run tauri:build:bundled-jre`

---

## Security Considerations

- **JRE Security**: Keep the bundled JRE updated with security patches
- **Bundle Verification**: Ensure JRE comes from a trusted source (Adoptium, Oracle)
- **Code Signing**: Consider signing the installer for Windows SmartScreen

---

## Related Documentation

- [TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md) - Bundled backend setup
- [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md) - Backend URL configuration
- [LOGS-QUICK-REFERENCE.md](LOGS-QUICK-REFERENCE.md) - Logging guide
- [CUSTOM-TITLEBAR.md](CUSTOM-TITLEBAR.md) - Custom titlebar documentation

---

_Last Updated: 2025-01-16_
