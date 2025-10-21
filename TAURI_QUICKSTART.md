# Tauri Quick Start Guide

Quick reference for getting started with Tauri in PharmaSmart.

## Prerequisites Checklist

- [ ] Rust installed (`rustc --version`)
- [ ] Cargo installed (`cargo --version`)
- [ ] Node.js >= 22.14.0 (`node --version`)
- [ ] NPM dependencies installed (`npm install`)

## Installation Steps

### 1. Install Rust (if not installed)

**Windows:**
```bash
# Download and run rustup-init.exe from https://rustup.rs/
# Or use winget:
winget install Rustlang.Rustup
```

**macOS/Linux:**
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

### 2. Verify Installation
```bash
rustc --version
cargo --version
```

### 3. Install NPM Dependencies
```bash
npm install
```

This will install `@tauri-apps/cli` as a dev dependency.

### 4. Generate Icons (Important!)
```bash
npm run tauri icon src/main/webapp/favicon.ico
```

This creates all required icon sizes in `src-tauri/icons/`.

## Development Workflow

### Start Development Server

**Option 1: Tauri only (recommended)**
```bash
npm run tauri:dev
```

This will:
1. Start Angular dev server on port 4200
2. Launch Tauri window automatically
3. Enable hot-reload for both frontend and Rust changes

**Option 2: Backend + Tauri**
```bash
# Terminal 1: Start Spring Boot backend
./mvnw

# Terminal 2: Start Tauri
npm run tauri:dev
```

## Building for Production

### Build Application
```bash
npm run tauri:build
```

This creates installers in `src-tauri/target/release/bundle/`:
- **NSIS**: `nsis/PharmaSmart_0.0.1_x64-setup.exe` (recommended)
- **MSI**: `msi/PharmaSmart_0.0.1_x64_en-US.msi`

### Debug Build (faster, for testing)
```bash
npm run tauri:build:debug
```

## Common Commands

```bash
# Check Tauri environment
npm run tauri -- info

# Generate icons from image
npm run tauri -- icon <path-to-image>

# Run specific Tauri command
npm run tauri -- <command>
```

## Troubleshooting

### "Rust not found"
```bash
# Restart terminal/shell after Rust installation
# Or source the environment:
source $HOME/.cargo/env  # Linux/macOS
```

### "WebView2 not installed" (Windows)
Download: https://developer.microsoft.com/microsoft-edge/webview2/

### Build errors with OpenSSL
**Windows:** Install Visual C++ Build Tools
**Linux:** `sudo apt install libssl-dev build-essential`

### Port 4200 in use
```bash
# Windows
netstat -ano | findstr :4200
taskkill /PID <PID> /F

# Linux/macOS
lsof -ti:4200 | xargs kill -9
```

## File Structure

```
src-tauri/
├── src/
│   ├── main.rs          # Entry point
│   └── lib.rs           # Commands/APIs
├── icons/               # App icons (generated)
├── Cargo.toml           # Rust dependencies
├── tauri.conf.json      # Tauri config
└── build.rs             # Build script
```

## Configuration

### Change Window Size
Edit `src-tauri/tauri.conf.json`:
```json
{
  "app": {
    "windows": [{
      "width": 1280,
      "height": 800
    }]
  }
}
```

### Change App Name/Version
Edit `src-tauri/Cargo.toml`:
```toml
[package]
name = "pharmasmart"
version = "0.0.1"
```

### Enable Developer Tools
DevTools open automatically in dev mode. To disable:
Comment out in `src-tauri/src/main.rs`:
```rust
// #[cfg(debug_assertions)]
// {
//     window.open_devtools();
// }
```

## Next Steps

1. ✅ Install Rust
2. ✅ Run `npm install`
3. ✅ Generate icons: `npm run tauri icon src/main/webapp/favicon.ico`
4. ✅ Test: `npm run tauri:dev`
5. ✅ Build: `npm run tauri:build`

## Documentation

- Full Guide: [TAURI_README.md](TAURI_README.md)
- Tauri Docs: https://tauri.app/
- Rust Book: https://doc.rust-lang.org/book/

## Getting Help

- Tauri Discord: https://discord.gg/tauri
- GitHub Issues: https://github.com/tauri-apps/tauri/issues
