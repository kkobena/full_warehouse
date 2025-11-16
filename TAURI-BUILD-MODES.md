# Tauri Build Modes - Quick Reference

PharmaSmart offers three different Tauri build configurations to suit different deployment scenarios.

---

## Quick Comparison

| Feature                | Standard             | Bundled Backend           | Complete (with JRE)           |
| ---------------------- | -------------------- | ------------------------- | ----------------------------- |
| **Configuration File** | `tauri.conf.json`    | `tauri.bundled.conf.json` | `tauri.bundled-jre.conf.json` |
| **Frontend**           | ✅ Included          | ✅ Included               | ✅ Included                   |
| **Backend JAR**        | ❌ External          | ✅ Bundled                | ✅ Bundled                    |
| **JRE**                | ❌ External          | ❌ External               | ✅ Bundled                    |
| **Installer Size**     | ~50 MB               | ~150 MB                   | ~200 MB                       |
| **Requires Java**      | ✅ Yes (for backend) | ✅ Yes                    | ❌ No                         |
| **Build Time**         | Fast (~2 min)        | Medium (~5 min)           | Slow (~8 min)                 |
| **Portability**        | Low                  | Medium                    | **High**                      |
| **Best For**           | Development          | Systems with Java         | **Maximum portability**       |

---

## Mode 1: Standard Build

### What's Included

- ✅ Tauri frontend application
- ❌ Backend must run separately
- ❌ Java must be installed separately

### Build Command

```bash
npm run tauri:build
```

### When to Use

- **Development**: Fast iteration, backend changes frequently
- **Client-Server**: Multiple clients connect to one backend server
- **Network deployment**: Backend on dedicated server, clients on workstations

### Pros

- ✅ Smallest installer size
- ✅ Fastest build time
- ✅ Easy to update backend independently
- ✅ Multiple clients can share one backend

### Cons

- ❌ Requires separate backend server
- ❌ User must configure backend URL
- ❌ More complex deployment

### User Experience

1. Install PharmaSmart frontend
2. Start backend separately (or connect to remote backend)
3. Configure backend URL in app settings
4. Application connects to backend

---

## Mode 2: Bundled Backend Build

### What's Included

- ✅ Tauri frontend application
- ✅ Spring Boot backend JAR
- ❌ Java must be installed separately

### Build Commands

```bash
# Production build
npm run tauri:build:bundled

# Debug build (faster)
npm run tauri:build:bundled:debug
```

### When to Use

- **Standalone desktop**: App runs entirely on local machine
- **Systems with Java**: Target machines already have Java installed
- **Offline use**: No network/server required

### Pros

- ✅ Self-contained application
- ✅ No backend configuration needed
- ✅ Works offline
- ✅ Smaller than Complete mode

### Cons

- ❌ Still requires Java installation
- ❌ Larger installer than Standard mode
- ❌ User must install Java first

### User Experience

1. Install Java/JRE on target machine
2. Install PharmaSmart
3. Launch app → backend starts automatically
4. Application works immediately

### Setup Required

**Before building:** Copy backend JAR to `src-tauri/sidecar/`

```bash
npm run tauri:prepare-sidecar
```

---

## Mode 3: Complete Build (Bundled JRE) 🎉

### What's Included

- ✅ Tauri frontend application
- ✅ Spring Boot backend JAR
- ✅ Java Runtime Environment (JRE)

### Build Commands

```bash
# Production build
npm run tauri:build:bundled-jre

# Debug build (faster)
npm run tauri:build:bundled-jre:debug
```

### When to Use

- **Maximum portability**: Target machines may not have Java
- **Pharmacy/retail environments**: Clean systems without developer tools
- **Easy deployment**: Just install and run
- **Controlled environment**: Bundle specific JRE version

### Pros

- ✅ **No Java installation required**
- ✅ Works on any Windows machine
- ✅ Completely standalone
- ✅ Controlled JRE version (no compatibility issues)
- ✅ Easiest for end users

### Cons

- ❌ Largest installer size (~200 MB)
- ❌ Longest build time
- ❌ Must rebuild to update JRE

### User Experience

1. Install PharmaSmart (single installer)
2. Launch app → everything works immediately
3. **No additional software needed**

### Setup Required

**Before building:**

1. **Download JRE** (e.g., Eclipse Temurin 21):

   - Visit: https://adoptium.net/temurin/releases/
   - Download: JRE 21 (Windows x64, ZIP archive)

2. **Extract JRE to sidecar directory**:

   ```bash
   # Extract downloaded ZIP
   # Copy to project:
   xcopy /E /I "jdk-21.0.5+11-jre" "src-tauri\sidecar\jre"
   ```

3. **Verify directory structure**:

   ```
   src-tauri/sidecar/
   ├── jre/
   │   ├── bin/
   │   │   └── java.exe    ← Must exist!
   │   └── lib/
   └── pharmaSmart-*.jar
   ```

4. **Prepare backend JAR**:
   ```bash
   npm run tauri:prepare-sidecar
   ```

**Full guide:** See [TAURI-BUNDLED-JRE-SETUP.md](TAURI-BUNDLED-JRE-SETUP.md)

---

## Build Scripts Summary

### Standard Mode

```bash
npm run tauri:build              # Production
npm run tauri:build:debug        # Debug (faster)
npm run tauri:build:fast         # Skip frontend rebuild
```

### Bundled Backend Mode

```bash
npm run tauri:build:bundled             # Production
npm run tauri:build:bundled:debug       # Debug
npm run tauri:build:bundled:fast        # Fast build
```

### Complete Mode (Bundled JRE)

```bash
npm run tauri:build:bundled-jre         # Production
npm run tauri:build:bundled-jre:debug   # Debug
npm run tauri:build:bundled-jre:fast    # Fast build
```

---

## Recommended Use Cases

### For Development Teams

→ **Standard Build**

- Fastest iteration
- Backend on dev server
- Multiple developers share backend

### For Enterprise Deployment (with IT support)

→ **Bundled Backend Build**

- Java managed by IT department
- Standalone app on each workstation
- Offline capable

### For End Users / Pharmacy Retail

→ **Complete Build (Bundled JRE)** ⭐ RECOMMENDED

- No technical knowledge required
- Install and run immediately
- No dependencies to manage
- Maximum reliability

---

## Decision Tree

```
Do users have Java installed?
├─ Yes → Bundled Backend Build
└─ No → Complete Build (Bundled JRE) ⭐

Is backend shared across multiple clients?
├─ Yes → Standard Build
└─ No → Bundled Backend or Complete Build

Is this for development?
├─ Yes → Standard Build
└─ No → Complete Build (Bundled JRE) ⭐

Maximum portability needed?
└─ Yes → Complete Build (Bundled JRE) ⭐
```

---

## Installation Output

### Standard Build

```
src-tauri/target/release/bundle/
└── nsis/PharmaSmart_0.0.1_x64-setup.exe (50 MB)
```

### Bundled Backend Build

```
src-tauri/target/release/bundle/
└── nsis/PharmaSmart-Standalone_0.0.1_x64-setup.exe (150 MB)
```

### Complete Build (Bundled JRE)

```
src-tauri/target/release/bundle/
└── nsis/PharmaSmart-Complete_0.0.1_x64-setup.exe (200 MB)
```

---

## Related Documentation

- [TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md) - Detailed technical documentation
- [TAURI-BUNDLED-JRE-SETUP.md](TAURI-BUNDLED-JRE-SETUP.md) - Complete build setup guide
- [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md) - Backend URL configuration
- [CUSTOM-TITLEBAR.md](CUSTOM-TITLEBAR.md) - Custom titlebar documentation

---

_Last Updated: 2025-01-16_
