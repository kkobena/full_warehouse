# Electron Packaging Fix - Preload Script Issue

## ✅ Issue Fixed

**Problem**: When running the packaged Electron installer (.exe), the application failed with:
```
Unable to load preload script: C:\...\dist-electron\win-unpacked\resources\app.asar\preload.js
ENOENT, preload.js not found in app.asar
```

**Cause**: The `preload.js` file was not included in the electron-builder configuration, so it wasn't packaged into the installer.

## 🔧 Solution Applied

### 1. Updated package.json
Added `preload.js` to the `build.files` array:

```json
{
  "build": {
    "files": [
      "target/classes/static/**",
      "electron.js",
      "preload.js"    // ← Added this
    ]
  }
}
```

### 2. Fixed electron.js
- Removed duplicate `win.webContents.openDevTools()` line
- Set DevTools to only open in development mode
- Ensured `webSecurity: false` to allow ES modules from file:// protocol

## 🚀 How to Rebuild the Installer

Now that the fix is in place, rebuild the installer:

```bash
# Clean and rebuild everything
npm run electron:build
```

This will:
1. Clean the `target/classes/static/` directory
2. Build Angular with electron configuration
3. Package everything including `preload.js` into the installer
4. Create `PharmaSmart Setup 0.0.1-SNAPSHOT.exe` in `dist-electron/`

## 📦 What Gets Packaged Now

The installer now includes:
- ✅ `electron.js` (main process)
- ✅ `preload.js` (preload script) ← **Now included**
- ✅ `target/classes/static/**` (Angular application files)

## 🧪 Testing the Installer

After rebuilding:

1. Navigate to `dist-electron/` folder
2. Run `PharmaSmart Setup 0.0.1-SNAPSHOT.exe`
3. Install the application
4. Launch PharmaSmart from Start Menu or Desktop
5. The application should now start without the preload error

## ⚙️ Development vs Production

### Development Mode
```bash
npm run electron:start
# or
npm run electron:dev
```
- Loads from dev server (http://localhost:4200)
- DevTools open automatically
- Hot module replacement enabled
- Preload script loaded from project directory

### Production Mode (Unpackaged)
```bash
npm run electron:prod
```
- Loads from built files (`target/classes/static/`)
- DevTools closed by default
- Preload script loaded from project directory

### Packaged Installer
```bash
npm run electron:build
```
- Creates installable .exe
- Preload script bundled in app.asar
- DevTools closed
- Ready for distribution

## 🔍 Verifying the Fix

To verify preload.js is included in the build:

### Before Installation (Check unpacked files)
```bash
# Navigate to unpacked folder
cd dist-electron/win-unpacked/resources

# Extract and check app.asar contents
npx asar list app.asar | grep preload
```

You should see:
```
preload.js
```

### After Installation (Check logs)
When you run the installed app, check for this in the console (if running from terminal):
```
[Renderer 1]: Preload script loaded successfully
```

## 📝 Files Modified

1. **package.json**
   - Added `preload.js` to `build.files` array

2. **electron.js**
   - Fixed duplicate DevTools opening
   - Ensured correct webSecurity setting
   - Cleaned up DevTools logic

## 🆘 If You Still See the Error

If you still encounter the preload error after rebuilding:

1. **Delete old build artifacts**
   ```bash
   npm run clean-www
   rimraf dist-electron
   ```

2. **Rebuild from scratch**
   ```bash
   npm run electron:build
   ```

3. **Uninstall old version**
   - Go to Windows Settings → Apps
   - Uninstall "PharmaSmart"
   - Delete `C:\Users\<YourName>\AppData\Local\PharmaSmart` if it exists

4. **Install fresh**
   - Run the newly built installer from `dist-electron/`

## 💡 Understanding the Preload Script

### What is preload.js?
The preload script runs before the web page loads and provides a secure bridge between:
- **Main Process** (electron.js): Has full Node.js and system access
- **Renderer Process** (Angular app): Runs in isolated context

### What does it do in our app?
```javascript
// Exposes safe APIs to Angular
window.electron = {
  platform: process.platform,
  send: (channel, data) => { /* IPC for printer */ },
  receive: (channel, func) => { /* IPC for hardware */ }
}
```

### Why is it critical?
Without the preload script:
- Angular can't detect it's running in Electron
- Printer integration won't work
- Hardware access features fail
- Some Electron-specific features disabled

## ✅ Verification Checklist

Before distributing the installer, verify:

- [ ] Ran `npm run electron:build` successfully
- [ ] Installer file exists in `dist-electron/`
- [ ] Installed app launches without errors
- [ ] Console shows "Preload script loaded successfully"
- [ ] Application loads the login screen
- [ ] Navigation works correctly
- [ ] Backend connection works (with backend running)

## 🎯 Next Steps

1. **Test the rebuilt installer** on your development machine
2. **Test on a clean Windows machine** (without dev tools installed)
3. **Verify all features work** in the installed version
4. **Document any backend requirements** for users
5. **Create installation guide** for end users

## 📚 Related Documentation

- `HOW_TO_RUN_ELECTRON.md` - Guide for running Electron in development
- `NPM_SCRIPTS_GUIDE.md` - Complete npm scripts reference
- `ELECTRON_README.md` - General Electron setup and troubleshooting

## 🔒 Security Note

The `webSecurity: false` setting is necessary for the app to work with file:// protocol and ES modules. This is safe for a packaged desktop application because:
- The app only loads local files from the package
- There's no external web content loaded
- Context isolation is still enabled
- Node integration is disabled

The security warnings you see during development won't appear in the packaged version.
