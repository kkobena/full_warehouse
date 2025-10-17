# How to Run the Electron Desktop Application

## ✅ Correct Commands

### For Development (with Hot Reload)

**Option 1: Automatic (Recommended)**
```bash
npm run electron:start
```
This automatically:
1. Starts Angular dev server on http://localhost:4200
2. Waits for it to be ready
3. Launches Electron pointing to the dev server
4. Enables hot module replacement (changes reflect immediately)

**Option 2: Manual**
```bash
# Terminal 1 - Start Angular dev server
npm start

# Terminal 2 - Wait for "Compiled successfully", then start Electron
npm run electron:dev
```

### For Production (Built Files)

**Important**: Production mode requires building first!

```bash
# Step 1: Build Angular for Electron
ng build --configuration=electron

# Step 2: Run Electron (loads from built files)
npm run electron
```

**Or use the combined command:**
```bash
npm run electron:prod
```
This runs both steps automatically.

## ❌ Common Mistakes

### Error: "ng serve exited with code 2147483651"

**Cause**: You ran `npm run electron:start` but the Angular dev server crashed.

**Solutions**:
1. Make sure port 4200 is not already in use
2. Check if you have enough memory (close other applications)
3. Try running just `npm start` first to see if the dev server works
4. If dev server has issues, use production mode instead: `npm run electron:prod`

### Error: "index.html NOT found"

**Cause**: Trying to run Electron in production mode without building first.

**Solution**:
```bash
# Build Angular first
ng build --configuration=electron

# Then run Electron
npm run electron
```

### Error: White screen in Electron

**Causes & Solutions**:

1. **Not waiting for dev server**:
   - Use `npm run electron:start` (it waits automatically)
   - Or wait for "Compiled successfully" before running `npm run electron:dev`

2. **Production files outdated**:
   - Rebuild: `ng build --configuration=electron`
   - Then run: `npm run electron`

3. **Backend not running**:
   - Development mode expects backend on http://localhost:9080
   - Start backend: `./mvnw` (in separate terminal)

## 📋 Quick Reference

| What You Want | Command to Use |
|---------------|----------------|
| Develop with hot reload | `npm run electron:start` |
| Develop manually | `npm start` + `npm run electron:dev` |
| Test production build | `ng build --configuration=electron` + `npm run electron` |
| Build + run production | `npm run electron:prod` |
| Create installer | `npm run electron:build` |

## 🔍 Checking if Electron is Working

When Electron starts successfully, you should see in the terminal:

```
Production mode - Loading from: file:///C:/Users/.../index.html
[Renderer 1]: Preload script loaded successfully
✅ DOM is ready
✅ Page loaded successfully
[Renderer 1]: Application started
```

If you see these messages, Electron is working! The window should show your application.

## 🐛 Debugging

### Check Electron Console Output
The terminal where you ran Electron shows detailed logs:
- Page loading status
- Errors from Angular
- Network requests

### Open DevTools in Electron
Press `Ctrl+Shift+I` (Windows/Linux) or `Cmd+Option+I` (Mac) to open developer tools inside the Electron window.

### Verify Build Files Exist
```bash
# Check if index.html was built
ls target/classes/static/index.html

# See all built files
ls target/classes/static/
```

## ⚙️ Current Configuration

- **Dev Mode URL**: http://localhost:4200 (proxied to backend at :9080)
- **Prod Build Output**: `target/classes/static/`
- **Base Href for Electron**: `./` (relative paths for file:// protocol)
- **Window Size**: 1400 x 900 pixels
- **Frame Style**: Standard OS titlebar (minimize, maximize, close buttons)

## 🔒 Security Warnings

You may see security warnings in the console when running in development mode:
```
Electron Security Warning (Disabled webSecurity)
```

**This is normal and expected**. These warnings:
- Only appear in development/debug mode
- Allow Angular to work with file:// protocol
- Won't appear in packaged production builds
- Are necessary for ES modules to load from local files

## 📦 Creating a Distributable

To create an installable .exe file:

```bash
# Build and package
npm run electron:build
```

This creates an installer in the `dist-electron/` folder.

## 💡 Tips

1. **Development Mode is Faster**: Use `npm run electron:start` during development for instant updates
2. **Test Production Builds**: Before creating an installer, test with `npm run electron:prod`
3. **Clear Cache**: If you see stale data, delete `target/classes/static/` and rebuild
4. **Memory Issues**: If build crashes, close other applications to free up memory
5. **Backend Required**: The app needs the Spring Boot backend running to function

## 🆘 Still Having Issues?

1. Check that Node.js version is >= 22.14.0: `node --version`
2. Check that npm version is >= 11.0.0: `npm --version`
3. Try cleaning and rebuilding:
   ```bash
   npm run clean-www
   ng build --configuration=electron
   npm run electron
   ```
4. Check the Electron terminal output for specific error messages
5. Open DevTools in Electron (Ctrl+Shift+I) and check the Console tab
