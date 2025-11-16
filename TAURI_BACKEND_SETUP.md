# Tauri Backend Integration

This document explains the three build options for the PharmaSmart Tauri desktop application.

## Build Options

PharmaSmart supports **three build configurations**:

### 1. **Standard Build** (Default)

- ✅ Frontend only
- ✅ Connects to external Spring Boot backend (must be started separately)
- ✅ Monitors backend availability with splash screen
- ✅ Shows waiting screen until backend is ready
- ✅ Smallest bundle size (~50MB)
- ✅ Fastest build time
- ⚠️ **Requires:** Backend server running separately
- **Use case:** Development, server-based deployment, client-server architecture

### 2. **Bundled Backend Build**

- ✅ Frontend + Spring Boot backend bundled together
- ✅ Automatically launches backend when app starts
- ✅ Monitors backend health and readiness
- ✅ Backend persists after app closes (allows multiple instances)
- ✅ Medium bundle size (~150MB)
- ⚠️ **Requires:** Java/JRE installed on user's machine
- **Use case:** Standalone desktop deployment where Java is pre-installed

### 3. **Complete Build (Bundled JRE)** - NEW! 🎉

- ✅ Frontend + Backend + JRE bundled together
- ✅ **NO Java installation required** on target system
- ✅ Completely standalone and portable
- ✅ Works on any Windows machine
- ⚠️ Largest bundle size (~200MB)
- ⚠️ Longest build time
- **Use case:** Maximum portability, deployment to systems without Java
- **Documentation:** See [TAURI-BUNDLED-JRE-SETUP.md](TAURI-BUNDLED-JRE-SETUP.md)

## Architecture

### Components

1. **Backend Manager** (`src-tauri/src/backend_manager.rs`)

   - Finds the bundled JAR file in resources directory
   - Spawns Java process directly with Spring Boot arguments
   - Configures log file location in user's home directory
   - Monitors backend logs (stdout/stderr)
   - Performs health checks on startup
   - Handles process cleanup on shutdown
   - Sets Spring profiles: `tauri` and `prod`

2. **Main Application** (`src-tauri/src/main.rs`)

   - **Bundled mode**: Initializes backend state, starts backend, manages lifecycle
   - **Standard mode**: Monitors external backend health, emits health status events
   - Exposes `check_backend_health` Tauri command (both modes)
   - Exposes `get_backend_status` Tauri command (bundled mode only)
   - Emits `backend-status` events (bundled mode)
   - Emits `backend-health-status` events (standard mode)
   - Shows error dialog if backend fails to start (bundled mode)

3. **Build Script** (`scripts/prepare-sidecar.js`)

   - Builds Spring Boot JAR if needed
   - Copies JAR to `src-tauri/sidecar/` directory
   - Cleans up old JAR files

4. **Splash Screen UI** (`src/main/webapp/app/shared/backend-splash/`)

   - Angular component displaying backend startup/availability progress
   - Real-time progress bar (0-100%) with status messages
   - **Bundled mode**: Shows "PharmaSmart Standalone" title, listens to `backend-status` events
   - **Standard mode**: Shows "PharmaSmart Client" title, polls backend health
   - Auto-hides when backend reaches "ready" state
   - Shows error state with helpful messages and "Close" button if startup/connection fails
   - Uses PrimeNG progress bar with smooth animations

5. **Backend Status Service** (`src/main/webapp/app/core/tauri/backend-status.service.ts`)

   - Detects Tauri mode (bundled, standard, or web)
   - **Bundled mode**: Subscribes to `backend-status` events from Rust
   - **Standard mode**: Polls backend using `check_backend_health` command + listens to `backend-health-status` events
   - **Web mode**: Immediately marks as ready (no Tauri)
   - Provides Observable stream of backend status for components
   - Fetches backend URL from Tauri using `get_backend_url_command`

6. **App Settings Service** (`src/main/webapp/app/core/config/app-settings.service.ts`)

   - Manages API server URL configuration
   - **Tauri mode**: Automatically fetches backend URL from `get_backend_url_command` on startup
   - **Web mode**: Uses environment configuration
   - Stores user-configured URLs in localStorage
   - Provides priority: User settings > Tauri backend URL > Environment config
   - Exposes `waitForInitialization()` for components that need to wait for backend URL

7. **API Base URL Interceptor** (`src/main/webapp/app/core/interceptor/api-base-url.interceptor.ts`)
   - HTTP interceptor that prepends backend URL to all API requests
   - Uses `AppSettingsService` to get current backend URL
   - Automatically converts relative URLs (`/api/...`) to absolute URLs
   - Essential for Tauri apps where frontend runs on `tauri://localhost`

## Configuration

### Backend URL (Standard Mode)

**New in v2:** For standard builds, you can configure the backend URL to connect to a backend on a different machine or port.

**Default:** `http://localhost:8080`

---

#### 🎯 Simple Method (Recommended for Non-Technical Users)

**See:** [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md) for step-by-step instructions with pictures.

**Quick Summary:**

1. Create a file named `backend-url.txt` next to `PharmaSmart.exe`
2. Open it with Notepad
3. Type the backend address (example: `http://192.168.1.100:8080`)
4. Save and close
5. Launch PharmaSmart

**Priority:** Config file > Environment variable > Default

---

#### 🔧 Advanced Method (Environment Variable)

For system administrators and technical users:

**Usage Examples:**

```bash
# Windows - Connect to backend on local network
set BACKEND_URL=http://192.168.1.100:8080
PharmaSmart.exe

# Windows PowerShell
$env:BACKEND_URL="http://192.168.1.100:8080"
.\PharmaSmart.exe

# Linux/macOS
BACKEND_URL=http://192.168.1.100:8080 ./pharmasmart

# Connect to backend on custom port
set BACKEND_URL=http://localhost:9090
PharmaSmart.exe
```

**Features:**

- ✅ Configurable backend URL via `BACKEND_URL` environment variable
- ✅ Falls back to `http://localhost:8080` if not set
- ✅ Frontend automatically detects backend URL from Tauri
- ✅ Health monitoring uses configured URL
- ✅ Error messages show the actual URL being used
- ✅ Useful for client-server deployments across network

**Technical Details:**

- Configuration priority: 1) `backend-url.txt` file, 2) `BACKEND_URL` environment variable, 3) Default `http://localhost:8080`
- Rust command: `get_backend_url_command()` returns configured URL
- `AppSettingsService` automatically fetches URL from Tauri on startup
- `apiBaseUrlInterceptor` prepends the backend URL to all API requests
- `BackendStatusService` monitors backend health using the configured URL
- Health checks use full URL instead of just port number
- Configuration is read once at application startup
- User can still override URL manually via settings UI (stored in localStorage)

### Backend Port (Bundled Mode)

For bundled builds, the backend port is hardcoded in `src-tauri/src/main.rs`:

```rust
const BACKEND_PORT: u16 = 8080;
```

You can also read this from `package.json` config:

```json
"config": {
  "backend_port": "8080"
}
```

### Backend Log Files (Bundled Mode)

**New in v2:** When running in bundled mode, the Spring Boot backend automatically writes logs to files for troubleshooting.

#### 📋 Quick Summary

| Item                       | Details                                              |
| -------------------------- | ---------------------------------------------------- |
| **Log Location (Windows)** | `C:\Users\[You]\PharmaSmart\logs\pharmasmart.log`    |
| **Quick Access (Windows)** | Press `Win+R`, type `%USERPROFILE%\PharmaSmart\logs` |
| **Log Rotation**           | Daily + when reaching 10MB                           |
| **Retention**              | 7 days                                               |
| **Max Size**               | 100MB total                                          |
| **Compression**            | Yes (.gz for old logs)                               |
| **Auto-Cleanup**           | Yes (deletes logs older than 7 days)                 |

**📚 See:** [LOGS-QUICK-REFERENCE.md](LOGS-QUICK-REFERENCE.md) for detailed logging guide.

#### 📁 Log File Location:

- **Windows**: `C:\Users\[YourUsername]\PharmaSmart\logs\pharmasmart.log`
- **Linux**: `~/.local/share/PharmaSmart/logs/pharmasmart.log` or `~/PharmaSmart/logs/pharmasmart.log`
- **macOS**: `~/Library/Application Support/PharmaSmart/logs/pharmasmart.log` or `~/PharmaSmart/logs/pharmasmart.log`

**How to Find Logs:**

**Windows:**

1. Press `Windows + R`
2. Type: `%USERPROFILE%\PharmaSmart\logs`
3. Press Enter

**Linux/macOS:**

```bash
# Open log directory
cd ~/PharmaSmart/logs

# View current log
cat pharmasmart.log

# View log with live updates
tail -f pharmasmart.log
```

**Log Features:**

- ✅ **Rolling files**: Logs rotate daily and when they reach 10MB
- ✅ **Retention**: Keeps 7 days of history (older logs are automatically deleted)
- ✅ **Size limit**: Maximum 100MB total log size
- ✅ **Compression**: Old logs are compressed (.gz) to save space
- ✅ **Timestamped**: Each log line includes timestamp, level, and thread info

**Log Files:**

- `pharmasmart.log` - Current log file
- `pharmasmart.log.2025-01-16.0.gz` - Archived logs from previous days
- `pharmasmart.log.2025-01-15.0.gz` - Older archived logs

**Configuration:**

- Configured in `src/main/resources/logback-spring.xml`
- Uses Spring profiles: `tauri` and `prod`
- Log level: `DEBUG` for application code, `WARN` for third-party libraries

### Health Check Endpoint

The backend manager checks the following endpoint for readiness:

```
http://localhost:{PORT}/management/health
```

This endpoint is configured in `SecurityConfiguration.java` to be publicly accessible (no authentication required).

### Startup Timeout

Default timeout for backend startup is **60 seconds**. This is configured in `backend_manager.rs`:

```rust
wait_for_backend_ready(port, 60).await
```

### Progress Bar Status Codes

The splash screen displays different status codes during startup:

- `initializing` (0%): Initial state
- `checking_java` (10%): Validating Java version
- `finding_jar` (20%): Locating Spring Boot JAR file
- `starting` (30%): Preparing to launch backend
- `launched` (40%): Backend process spawned
- `waiting` (50-95%): Health check polling
- `ready` (100%): Backend is ready, splash screen hides
- `error`: Startup failed, shows error message

## Build Process

### Standard Build (Frontend Only)

```bash
# Production build
npm run tauri:build

# Debug build (faster, with debug symbols)
npm run tauri:build:debug

# Fast build (assumes frontend already built)
npm run tauri:build:fast
```

**What happens:**

1. Build Angular frontend (`webapp:build:tauri`)
2. Build Tauri desktop application
3. Create NSIS/MSI installers in `src-tauri/target/release/bundle/`
4. Output: **PharmaSmart.exe** (~10MB)

### Bundled Build (Frontend + Backend)

```bash
# Production build
npm run tauri:build:bundled

# Debug build (faster, with debug symbols)
npm run tauri:build:bundled:debug

# Fast build (assumes frontend and JAR already built)
npm run tauri:build:bundled:fast
```

**What happens:**

1. Build Angular frontend (`webapp:build:tauri`)
2. Build Spring Boot JAR (`mvnw.cmd clean package -Pprod`)
3. Copy JAR to `src-tauri/sidecar/`
4. Build Tauri desktop application with `bundled-backend` feature
5. Create NSIS/MSI installers in `src-tauri/target/release/bundle/`
6. Output: **PharmaSmart-Standalone.exe** (~60MB including JAR)

## Development Mode

For development with hot-reload:

```bash
# Terminal 1: Start Spring Boot backend manually
./mvnw.cmd

# Terminal 2: Start Tauri in dev mode
npm run tauri:dev
```

**Note:** In dev mode, both build configurations use the same behavior - the frontend connects to the manually-started backend on port 8080. The bundled backend feature only activates in production builds.

## Choosing the Right Build

| Feature                | Standard Build                   | Bundled Build                 |
| ---------------------- | -------------------------------- | ----------------------------- |
| **Size**               | ~10MB                            | ~60MB                         |
| **Backend Included**   | ❌ No                            | ✅ Yes                        |
| **Requires Java**      | ❌ No                            | ✅ Yes (JRE)                  |
| **Auto-start Backend** | ❌ No                            | ✅ Yes                        |
| **Use Case**           | Client app connecting to server  | Standalone desktop app        |
| **Deployment**         | Requires separate backend server | Self-contained                |
| **Configuration**      | `tauri.conf.json`                | `tauri.bundled.conf.json`     |
| **Cargo Feature**      | (none)                           | `bundled-backend`             |
| **Build Command**      | `npm run tauri:build`            | `npm run tauri:build:bundled` |

## How It Works

### Standard Build Startup

1. **Tauri app starts** → `main.rs` is executed
2. **Backend URL loaded** → Reads `BACKEND_URL` environment variable (defaults to `http://localhost:8080`)
3. **Backend monitoring starts** → Polls `/management/health` endpoint at configured URL
4. **Splash screen displays** → Shows "Waiting for backend server at [URL]..."
5. **Backend health check** → Every 500ms, checks if backend is available
6. **Backend ready** → Splash screen hides, frontend loads
7. **Timeout (30s)** → If backend not found, shows error with "Close" button and suggests setting `BACKEND_URL`

**Note:** User must start Spring Boot backend separately before or while the app is waiting, or set the `BACKEND_URL` environment variable to point to a running backend server on another machine.

### Bundled Build Startup

1. **Tauri app starts** → `main.rs` is executed with `bundled-backend` feature
2. **Backend state initialized** → `BackendState` created with port 8080
3. **Splash screen displayed** → Angular shows progress bar overlay
4. **Java availability check** → Verifies Java/JRE is installed (10% progress)
5. **Find JAR file** → Backend manager locates `pharmaSmart-*.jar` in resources (20% progress)
6. **Spawn Java process** → Executes `java -jar pharmaSmart-*.jar --server.port=8080` (30% progress)
7. **Process launched** → Backend process started with PID (40% progress)
8. **Health monitoring** → Backend manager polls `/api/management/health` (50-95% progress)
9. **Ready signal** → Once health check passes (within 60s), backend is marked ready (100% progress)
10. **Splash screen hides** → Angular removes the progress overlay
11. **Frontend loads** → Angular app connects to `http://localhost:8080`

**Note:** If startup fails, the splash screen displays an error message with a "Close" button to dismiss it.

### Bundled Build Shutdown

1. **User closes Tauri app** → App window closes
2. **Backend continues running** → Java process remains active in background
3. **Backend persists** → Available for future app launches or other instances

**Note:** The backend process is NOT automatically stopped when Tauri closes. This allows:

- Multiple app instances to share the same backend
- Faster restart times (backend already running)
- Development workflow improvements

To manually stop the backend, see "Backend Process Not Stopping" section below.

## Troubleshooting

### Cannot Connect to Backend (Standard Mode)

**Splash Screen:** "Backend not available at http://localhost:8080. Please start the backend server or configure backend URL."

**Possible Causes:**

- Backend server is not running
- Backend is running on a different machine or port
- Firewall blocking connection
- Backend URL not configured correctly

**Solutions:**

#### ✅ Simple Solution (Recommended)

1. **Create a config file** named `backend-url.txt` next to `PharmaSmart.exe`
2. **Open it with Notepad** and type the backend address:
   ```
   http://192.168.1.100:8080
   ```
3. **Save** and restart PharmaSmart

**See:** [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md) for detailed step-by-step instructions.

#### 🔧 Advanced Solutions

1. **Start the backend server** if it's supposed to be local:

   ```bash
   ./mvnw.cmd  # Windows
   ./mvnw      # Linux/macOS
   ```

2. **Set the BACKEND_URL environment variable** to point to remote backend:

   ```bash
   # Windows CMD
   set BACKEND_URL=http://192.168.1.100:8080
   PharmaSmart.exe

   # Windows PowerShell
   $env:BACKEND_URL="http://192.168.1.100:8080"
   .\PharmaSmart.exe

   # Linux/macOS
   BACKEND_URL=http://192.168.1.100:8080 ./pharmasmart
   ```

3. **Verify backend is accessible** from the client machine:

   ```bash
   curl http://192.168.1.100:8080/management/health
   ```

   Or open in web browser: `http://192.168.1.100:8080/management/health`

4. **Check firewall settings** on both machines
5. **Verify network connectivity** between client and server

### Backend Fails to Start (Bundled Mode)

**Error Dialog:** "Failed to start backend server: Timeout waiting for backend on port 8080"

**Possible Causes:**

- Java Runtime not installed or not in PATH
- Port 8080 already in use

**First Step: Check the Log Files**

Before troubleshooting, check the backend log files for detailed error messages:

**Windows:**

1. Press `Windows + R`
2. Type: `%USERPROFILE%\PharmaSmart\logs`
3. Press Enter
4. Open `pharmasmart.log` with Notepad

**What to Look For:**

- `ERROR` or `WARN` messages (usually in red or yellow)
- Port binding errors: "Address already in use"
- Database connection errors
- Missing dependencies or configuration errors
- JAR file corrupted or missing
- Backend takes longer than 60 seconds to start

**Solutions:**

1. Verify Java is installed: `java -version`
2. Check port availability: `netstat -ano | findstr :8080`
3. Increase timeout in `backend_manager.rs`
4. Check logs in Tauri console (dev mode with DevTools)
5. Click "Close" button on splash screen to dismiss the error and access the app

### Manually Stopping the Backend

The backend process continues running after closing the Tauri app. To stop it manually:

**Windows (stop all Java processes):**

```bash
taskkill /IM java.exe /F
```

**Windows (stop specific PharmaSmart backend):**

```bash
# Find the process
netstat -ano | findstr :8080
# Then kill by PID
taskkill /PID <PID> /F
```

**Linux/macOS:**

```bash
pkill -f warehouse
```

**Alternative:** The backend will automatically reuse the same port on next launch. If port 8080 is already in use by the existing backend, the new instance will fail to start and use the existing one instead.

### JAR Not Found in Bundle

Ensure the build script ran successfully:

```bash
npm run tauri:prepare-sidecar
```

Check that `src-tauri/sidecar/` contains the JAR file.

## File Structure

```
src-tauri/
├── sidecar/
│   ├── pharmaSmart-*.jar          # Spring Boot JAR (copied during build)
│   └── .gitignore                 # Ignore JAR files in git
├── src/
│   ├── main.rs                    # Tauri entry point + conditional backend lifecycle + get_backend_status command
│   ├── backend_manager.rs         # Backend process management (bundled-backend feature)
│   └── printer/                   # Printer integration modules
├── Cargo.toml                     # Rust dependencies + bundled-backend feature
├── tauri.conf.json                # Standard build configuration
└── tauri.bundled.conf.json        # Bundled build configuration

src/main/webapp/app/
├── core/
│   ├── tauri/
│   │   └── backend-status.service.ts      # Tauri backend status monitoring
│   ├── config/
│   │   └── app-settings.service.ts        # API URL configuration (Tauri-aware)
│   └── interceptor/
│       └── api-base-url.interceptor.ts    # HTTP interceptor for API URL
└── shared/backend-splash/
    ├── backend-splash.component.ts        # Splash screen component
    ├── backend-splash.component.html      # Splash screen template
    └── backend-splash.component.scss      # Splash screen styles

scripts/
└── prepare-sidecar.js             # Build script to prepare JAR for bundling
```

## Advanced Configuration

### Custom Spring Profiles

Edit `backend_manager.rs` to change the Spring profile:

```rust
.args([
    "-jar",
    jar_path.to_str().ok_or("Invalid JAR path")?,
    "--spring.profiles.active=YOUR_PROFILE",  // Change this
    &format!("--server.port={}", port),
])
```

### JVM Memory Options

Add memory settings in `backend_manager.rs`:

```rust
.args([
    "-Xms512m",
    "-Xmx2048m",
    "-jar",
    jar_path.to_str().ok_or("Invalid JAR path")?,
    "--spring.profiles.active=prod",
    &format!("--server.port={}", port),
])
```

### Multiple Backend Instances

To support multiple instances on different ports:

1. Modify `BACKEND_PORT` in `main.rs`
2. Read port from config file or environment
3. Ensure Angular environment points to the correct port

## Security Considerations

- ✅ Backend runs locally (localhost only)
- ✅ No external network exposure by default
- ✅ Process isolation (separate Java process)
- ⚠️ Backend persists after app exit (manual stop required if needed)

## Requirements

- **Java Runtime**: JRE (any version compatible with Spring Boot, must be in PATH)
- **Rust**: For building Tauri (installed via rustup)
- **Node.js**: 22.14.0+ with npm 11.0.0+
- **Maven**: Included via Maven Wrapper (mvnw)

## License

UNLICENSED - Proprietary software
