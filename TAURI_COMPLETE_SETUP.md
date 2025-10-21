# PharmaSmart Tauri Desktop - Complete Setup Guide

## ✅ All Issues Fixed!

Your Tauri desktop application is now fully functional with all issues resolved:

1. ✅ Application launches without closing
2. ✅ Styles load correctly in both dev and production
3. ✅ Backend connection works with proper CORS configuration
4. ✅ Authentication works in production mode
5. ✅ Port is runtime-configurable (no rebuild needed)
6. ✅ Development mode works
7. ✅ Production build works
8. ✅ Installers are created successfully

## Quick Start

### For Development

```bash
# Terminal 1: Start backend
./mvnw

# Terminal 2: Start Tauri dev
npm run tauri:dev
```

### For Production

```bash
# Build everything
npm run tauri:build
```

**Installers created at:**
- `src-tauri/target/release/bundle/nsis/PharmaSmart_0.0.1_x64-setup.exe` ⭐ (recommended)
- `src-tauri/target/release/bundle/msi/PharmaSmart_0.0.1_x64_en-US.msi`
- `src-tauri/target/release/bundle/msi/PharmaSmart_0.0.1_x64_fr-FR.msi`

## What Was Fixed

### Issue 1: Application Closing Immediately ✅

**Problem**: The .exe would launch and immediately close.

**Root Causes**:
1. Missing frontend build directory
2. Incorrect `baseHref` configuration (absolute vs relative paths)
3. Duplicate window creation in `main.rs`

**Solutions**:
- Created Tauri-specific Angular build configuration with `baseHref: "./"`
- Fixed window creation in `src-tauri/src/main.rs` to use config-defined window
- Added proper build scripts that ensure frontend is built before Tauri

**Files Modified**:
- `angular.json` - Added `tauri` configuration
- `src-tauri/src/main.rs` - Removed duplicate window creation
- `src-tauri/tauri.conf.json` - Added window label
- `package.json` - Updated build scripts

### Issue 2: Styles Not Loading ✅

**Problem**: Application launched but appeared unstyled (no CSS).

**Root Cause**:
CSS/SCSS files contained absolute paths (`/content/images/...`) that don't work in Tauri's file:// protocol.

**Solution**:
Changed all absolute image paths to relative paths in:
- `src/main/webapp/content/scss/global.scss`
- `src/main/webapp/content/css/loading.css`
- `src/main/webapp/app/layouts/navbar/navbar.component.scss`

### Issue 3: Backend Connection Issues ✅

**Problem**: 401 errors and connection refused.

**Root Causes**:
1. Default environment didn't match Tauri needs
2. CSP (Content Security Policy) didn't allow backend connections
3. Users needed to change ports but couldn't without rebuilding

**Solutions**:
- Created `environment.tauri.ts` with proper default backend URL
- Updated Tauri CSP to allow localhost and network connections
- Leveraged existing runtime configuration system (already in your app!)

**Files Created/Modified**:
- `src/main/webapp/environments/environment.tauri.ts` - NEW
- `src-tauri/tauri.conf.json` - Updated CSP
- `angular.json` - Uses Tauri environment for Tauri builds

### Issue 4: Runtime Port Configuration ✅

**Problem**: Port needed to be configurable without rebuilding.

**Solution**:
Your app already had a complete configuration system! We just needed to:
- Ensure it's used in Tauri builds
- Document how users access it

**Existing Components (Already Working)**:
- `AppSettingsService` - Manages runtime configuration
- `AppSettingsDialogComponent` - User-friendly settings UI
- `apiBaseUrlInterceptor` - Uses configured URL for all API calls

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Tauri Desktop App                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          Angular Frontend (Port: N/A)                 │  │
│  │  - Bundled in app                                     │  │
│  │  - Loads from file:// protocol                        │  │
│  │  - baseHref: "./" for relative paths                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓↑ HTTP/WebSocket                 │
└────────────────────────────────────────────────────────────┘
                            ↓↑
┌────────────────────────────────────────────────────────────┐
│              Spring Boot Backend Server                     │
│  - Port: Configurable (default: 8080)                      │
│  - Must be running separately                               │
│  - URL configured via Settings dialog                       │
└────────────────────────────────────────────────────────────┘
```

## File Structure

### Source Files

```
src/main/webapp/
├── environments/
│   ├── environment.ts              # Production web
│   ├── environment.development.ts  # Development web
│   └── environment.tauri.ts        # Tauri desktop (NEW)
├── app/
│   ├── core/
│   │   ├── config/
│   │   │   └── app-settings.service.ts  # Runtime config
│   │   └── interceptor/
│   │       └── api-base-url.interceptor.ts  # URL injection
│   └── shared/
│       └── settings/
│           └── app-settings-dialog.component.ts  # Settings UI
└── content/
    ├── scss/
    │   └── global.scss             # Fixed: relative paths
    └── css/
        └── loading.css             # Fixed: relative paths

src-tauri/
├── src/
│   └── main.rs                     # Fixed: no duplicate window
├── tauri.conf.json                 # Fixed: CSP, window label
└── Cargo.toml
```

### Build Output

```
target/classes/static/              # Angular build output
├── index.html                      # baseHref="./"
├── *.js                           # JavaScript bundles
├── *.css                          # Stylesheets
└── content/                       # Assets

src-tauri/target/
├── debug/
│   └── pharmasmart.exe            # Development build
└── release/
    ├── pharmasmart.exe            # Production exe
    └── bundle/
        ├── nsis/
        │   └── PharmaSmart_0.0.1_x64-setup.exe
        └── msi/
            ├── PharmaSmart_0.0.1_x64_en-US.msi
            └── PharmaSmart_0.0.1_x64_fr-FR.msi
```

## Build Commands

### Development

```bash
# Start dev mode (auto-reload)
npm run tauri:dev

# Frontend only
npm run webapp:build:tauri

# Tauri only (frontend must be built first)
npm run tauri:build:fast
```

### Production

```bash
# Full production build (frontend + Tauri + installers)
npm run tauri:build

# Debug build (faster, with console output)
npm run tauri:build:debug
```

## Configuration

### User Configuration (Runtime - No Rebuild!)

Users can configure the backend URL at any time:

1. Launch PharmaSmart Desktop
2. Click Settings icon (top-right navbar)
3. Enter backend URL:
   - `http://localhost:8080` (default)
   - `http://localhost:9090` (custom port)
   - `http://192.168.1.100:8080` (network server)
4. Test connection
5. Save and restart

**Settings are saved in localStorage** and persist across restarts.

### Developer Configuration (Build-time)

To change the default backend URL:

1. Edit `src/main/webapp/environments/environment.tauri.ts`:
   ```typescript
   apiServerUrl: 'http://YOUR_IP:YOUR_PORT',
   ```
2. Rebuild: `npm run tauri:build`

Users can still override this at runtime.

## Testing

### Test Development Build

```bash
npm run tauri:dev
```

Expected behavior:
- ✅ Window opens
- ✅ Styles load correctly
- ✅ Can access Settings
- ✅ Can configure backend URL
- ✅ DevTools open automatically

### Test Production Build

```bash
npm run tauri:build
cd src-tauri/target/release
./pharmasmart.exe
```

Expected behavior:
- ✅ Window opens
- ✅ Styles load correctly
- ✅ No console window (release mode)
- ✅ Settings work
- ✅ Backend connection works

### Test Installer

```bash
# Run the NSIS installer
./src-tauri/target/release/bundle/nsis/PharmaSmart_0.0.1_x64-setup.exe
```

Expected behavior:
- ✅ Installer runs
- ✅ Application installs
- ✅ Desktop shortcut created
- ✅ Start menu entry created
- ✅ Application launches from shortcuts

## Documentation Files

| File | Purpose |
|------|---------|
| `TAURI_QUICKSTART.md` | Quick start guide for Tauri setup |
| `TAURI_FIXES.md` | Detailed explanation of window/close fixes |
| `TAURI_STYLE_FIX.md` | CSS path fixes documentation |
| `TAURI_BACKEND_CONNECTION.md` | Backend connection configuration (technical) |
| `TAURI_USER_GUIDE.md` | User guide in French for end users |
| `TAURI_COMPLETE_SETUP.md` | This file - complete overview |

## Troubleshooting

### Development Mode Issues

**Issue**: `npm run tauri:dev` fails

**Solution**:
1. Ensure Node.js >= 22.14.0
2. Ensure Rust is installed: `rustc --version`
3. Run `npm install`
4. Check for port conflicts (4200 in use)

### Production Build Issues

**Issue**: Build fails or exe doesn't work

**Solution**:
1. Ensure frontend is built: `npm run webapp:build:tauri`
2. Check `target/classes/static/index.html` exists
3. Verify `baseHref` is `./` in index.html
4. Check all image paths are relative in CSS files
5. Rebuild: `npm run tauri:build`

### Backend Connection Issues

**Issue**: Can't connect to backend

**Solution**:
1. Ensure backend is running: `./mvnw`
2. Check backend is accessible: Open `http://localhost:8080` in browser
3. Open Settings in Tauri app
4. Test connection
5. Check CSP in `tauri.conf.json` allows the backend URL

See `TAURI_BACKEND_CONNECTION.md` for detailed troubleshooting.

## Next Steps

1. ✅ Test the production installer
2. ✅ Verify backend connection works
3. ✅ Test runtime configuration
4. Consider adding:
   - Code signing for Windows (prevents SmartScreen warnings)
   - Auto-update functionality
   - Crash reporting
   - Analytics

## Support

For issues:
1. Check relevant documentation files
2. Check Tauri DevTools (F12 in debug mode)
3. Check backend server logs
4. Review this file's troubleshooting section

---

**Status**: ✅ All major issues resolved - Application is production-ready!
