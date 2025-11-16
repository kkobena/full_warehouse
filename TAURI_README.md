# Tauri Setup for PharmaSmart

This document explains how to use Tauri as an alternative to Electron for building desktop applications with PharmaSmart.

## What is Tauri?

Tauri is a modern framework for building smaller, faster, and more secure desktop applications using web technologies. Unlike Electron, Tauri uses the system's native webview (WebView2 on Windows, WebKit on macOS/Linux) instead of bundling Chromium, resulting in much smaller application sizes.

## Prerequisites

### 1. Rust Installation

Tauri requires Rust to be installed on your system.

**Windows:**

```bash
# Install Rust using rustup
# Download from: https://rustup.rs/
# Or use winget:
winget install Rustlang.Rustup
```

**Verify Rust installation:**

```bash
rustc --version
cargo --version
```

### 2. System Dependencies

**Windows:**

- Microsoft Visual C++ Build Tools
- WebView2 Runtime (usually pre-installed on Windows 10/11)

**macOS:**

```bash
xcode-select --install
```

**Linux (Debian/Ubuntu):**

```bash
sudo apt update
sudo apt install libwebkit2gtk-4.1-dev \
  build-essential \
  curl \
  wget \
  file \
  libssl-dev \
  libgtk-3-dev \
  libayatana-appindicator3-dev \
  librsvg2-dev
```

## Project Structure

```
full_warehouse/
├── src-tauri/                 # Tauri backend (Rust)
│   ├── src/
│   │   ├── main.rs           # Main Tauri application entry
│   │   └── lib.rs            # Library with commands
│   ├── icons/                # Application icons
│   ├── Cargo.toml            # Rust dependencies
│   ├── tauri.conf.json       # Tauri configuration
│   └── build.rs              # Build script
├── src/main/webapp/          # Angular frontend (unchanged)
└── package.json              # NPM scripts (updated with Tauri commands)
```

## Available NPM Scripts

### Development

```bash
# Start Tauri in development mode with hot-reload
npm run tauri:dev
```

This command will:

1. Start the Angular dev server on `http://localhost:4200`
2. Launch Tauri with the dev server URL
3. Enable Rust DevTools (in debug mode)
4. Watch for Rust code changes

### Building

```bash
# Build production-ready Tauri application
npm run tauri:build

# Build debug version (faster, larger)
npm run tauri:build:debug
```

Build outputs are located in `src-tauri/target/release/bundle/`

### Other Commands

```bash
# Run Tauri CLI directly
npm run tauri -- [command]

# Examples:
npm run tauri -- info          # Show system/dependency info
npm run tauri -- icon [path]   # Generate icons from image
```

## Configuration

### Tauri Configuration (`src-tauri/tauri.conf.json`)

Key configuration sections:

#### Build Settings

```json
{
  "build": {
    "beforeDevCommand": "npm run start",
    "devUrl": "http://localhost:4200",
    "beforeBuildCommand": "npm run webapp:build:prod",
    "frontendDist": "../target/classes/static"
  }
}
```

#### Window Settings

```json
{
  "app": {
    "windows": [
      {
        "title": "PharmaSmart",
        "width": 1280,
        "height": 800,
        "minWidth": 1024,
        "minHeight": 768
      }
    ]
  }
}
```

#### Bundle Settings

```json
{
  "bundle": {
    "targets": ["nsis", "msi"],
    "windows": {
      "nsis": {
        "languages": ["English", "French"]
      }
    }
  }
}
```

### Cargo Configuration (`src-tauri/Cargo.toml`)

Dependencies include:

- `tauri` - Core framework
- `tauri-plugin-shell` - Execute shell commands
- `tauri-plugin-http` - HTTP requests
- `tauri-plugin-dialog` - Native dialogs
- `tauri-plugin-fs` - File system access
- `serde` / `serde_json` - JSON serialization

## Icons

### Generating Icons

Tauri requires multiple icon sizes. Generate them from your existing favicon:

```bash
npm run tauri icon src/main/webapp/favicon.ico
```

This creates all required sizes:

- 32x32.png
- 128x128.png
- 128x128@2x.png
- icon.icns (macOS)
- icon.ico (Windows)

Icons are stored in `src-tauri/icons/`

## Security

Tauri has a strict security model by default:

### Content Security Policy (CSP)

Configured in `tauri.conf.json`:

```json
{
  "app": {
    "security": {
      "csp": "default-src 'self'; connect-src 'self' http://localhost:* ws://localhost:*"
    }
  }
}
```

### Allowlist

Only explicitly allowed APIs are available to the frontend. Currently enabled:

- Shell commands
- HTTP requests
- File system access
- Native dialogs

## Backend Integration

### Connecting to Spring Boot Backend

The Tauri app expects the Spring Boot backend to run on `http://localhost:8080`.

**Development workflow:**

```bash
# Terminal 1: Start Spring Boot backend
./mvnw

# Terminal 2: Start Tauri with Angular frontend
npm run tauri:dev
```

**Production workflow:**
You need to bundle the Spring Boot backend with the Tauri app or configure it to start the backend automatically.

### Example: Auto-start Backend (Advanced)

Modify `src-tauri/src/main.rs` to start the Spring Boot JAR on launch:

```rust
use tauri_plugin_shell::ShellExt;

fn main() {
    tauri::Builder::default()
        .setup(|app| {
            // Start Spring Boot backend
            let shell = app.shell();
            let _backend = shell.command("java")
                .args(["-jar", "warehouse.jar"])
                .spawn()
                .expect("Failed to start backend");

            Ok(())
        })
        // ... rest of setup
}
```

## Tauri vs Electron Comparison

| Feature              | Tauri                 | Electron                         |
| -------------------- | --------------------- | -------------------------------- |
| **Binary Size**      | ~3-5 MB               | ~100-150 MB                      |
| **Memory Usage**     | ~50-100 MB            | ~150-300 MB                      |
| **Runtime**          | Native WebView        | Bundled Chromium + Node.js       |
| **Startup Time**     | Faster                | Slower                           |
| **Security**         | Rust + strict CSP     | Node.js vulnerabilities possible |
| **Backend Language** | Rust                  | JavaScript/Node.js               |
| **Platform Support** | Windows, macOS, Linux | Windows, macOS, Linux            |

## Build Outputs

After running `npm run tauri:build`, you'll find installers in:

```
src-tauri/target/release/bundle/
├── nsis/
│   └── PharmaSmart_0.0.1_x64-setup.exe    # NSIS installer
└── msi/
    └── PharmaSmart_0.0.1_x64_en-US.msi    # MSI installer
```

## Troubleshooting

### "Rust not found"

```bash
# Ensure Rust is installed
rustup --version

# Update Rust
rustup update
```

### "WebView2 not found" (Windows)

Download and install WebView2 Runtime:
https://developer.microsoft.com/en-us/microsoft-edge/webview2/

### Build fails with OpenSSL errors

**Windows:**

```bash
# Install OpenSSL via vcpkg or use precompiled binaries
```

**Linux:**

```bash
sudo apt install libssl-dev
```

### Port 4200 already in use

```bash
# Kill the process using port 4200
# Windows:
netstat -ano | findstr :4200
taskkill /PID [PID] /F

# Linux/macOS:
lsof -ti:4200 | xargs kill -9
```

## Further Reading

- [Tauri Documentation](https://tauri.app/)
- [Tauri API Reference](https://tauri.app/reference/)
- [Tauri Plugins](https://tauri.app/plugin/)
- [Tauri Security](https://tauri.app/security/)

## Migration from Electron

Both Electron and Tauri can coexist in the project:

**Use Electron:**

```bash
npm run electron:dev
npm run electron:build
```

**Use Tauri:**

```bash
npm run tauri:dev
npm run tauri:build
```

Choose based on your requirements:

- **Tauri**: Smaller size, better performance, better security
- **Electron**: More mature ecosystem, easier Node.js integration

## Next Steps

1. **Install Rust** if not already installed
2. **Install dependencies**: `npm install`
3. **Generate icons**: `npm run tauri icon src/main/webapp/favicon.ico`
4. **Test in development**: `npm run tauri:dev`
5. **Build for production**: `npm run tauri:build`

## Support

For issues specific to:

- **Tauri**: https://github.com/tauri-apps/tauri/issues
- **PharmaSmart**: Contact your development team
  To install Rust on Windows:

  1. Download and run rustup-init.exe:

  - Visit: https://rustup.rs/
  - Or download directly: https://win.rustup.rs/x86_64
  - Run the installer and follow the prompts

  2. After installation, restart your terminal (MINGW64/Git Bash) to refresh the
     PATH
  3. Verify the installation:
     rustc --version
     cargo --version

  Additional requirements for Tauri on Windows:

  You may also need the following:

  - Microsoft Visual Studio C++ Build Tools (required for Windows builds)
    - Download: https://visualstudio.microsoft.com/visual-cpp-build-tools/
    - Install "Desktop development with C++" workload
  - WebView2 (usually pre-installed on Windows 10/11)
    - If needed: https://developer.microsoft.com/en-us/microsoft-edge/webview2/

  Quick installation steps:

  1. Open PowerShell as Administrator and run:

  # Install Rust

  Invoke-WebRequest -Uri https://win.rustup.rs/x86_64 -OutFile rustup-init.exe
  .\rustup-init.exe 2. Restart your MINGW64 terminal 3. Then try building again:
