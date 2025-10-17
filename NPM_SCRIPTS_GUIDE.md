# NPM Scripts Guide - PharmaSmart

## 🚀 Electron Desktop Application Scripts

### Development Mode (Hot Reload)

```bash
# Recommended: Start dev server and Electron together
npm run electron:start
```
- Starts Angular dev server on http://localhost:4200
- Waits for dev server to be ready
- Launches Electron in development mode
- Changes reflect immediately (Hot Module Replacement)

```bash
# Alternative: Manual control
# Terminal 1
npm start

# Terminal 2 (after "Compiled successfully" appears)
npm run electron:dev
```
- More control over starting/stopping each process
- Useful for debugging

### Production Mode (Built Files)

```bash
# Build and run Electron (Recommended)
npm run electron:prod
```
- Cleans previous build
- Builds Angular with electron configuration
- Launches Electron with built files

```bash
# Just run Electron (requires previous build)
npm run electron
```
- Only launches Electron
- Assumes files are already built in `target/classes/static/`

### Create Distributable Installer

```bash
# Create Windows installer (.exe)
npm run electron:build
```
- Cleans and builds Angular
- Creates installable package in `dist-electron/` folder
- Output: `PharmaSmart Setup 0.0.1-SNAPSHOT.exe`

```bash
# Build without packaging (faster for testing)
npm run electron:build-dir
```
- Creates unpacked application files
- Useful for testing before creating full installer

```bash
# Full package (same as electron:build)
npm run electron:package
```
- Alternative name for electron:build

---

## 🌐 Web Application Scripts

### Development Server

```bash
# Start dev server with HMR
npm start
# or
npm run serve
```
- Runs on http://localhost:4200
- Proxies API requests to backend on port 9080
- Hot Module Replacement enabled

```bash
# Start with SSL/TLS
npm run start-tls
```
- HTTPS development server

### Production Build

```bash
# Full production build (web app)
npm run webapp:prod
```
- Cleans output directory
- Builds for production
- Output: `target/classes/static/`

```bash
# Development build
npm run webapp:build
```
- Cleans and builds in development mode
- Faster than production build

---

## 🧪 Testing Scripts

```bash
# Run all tests with coverage
npm test
```
- Runs Jest tests
- Generates coverage report
- Runs linter first

```bash
# Watch mode for testing
npm run test:watch
```
- Tests re-run on file changes

---

## 🧹 Cleaning Scripts

```bash
# Clean web output directory
npm run clean-www
```
- Removes `target/classes/static/`

```bash
# Clean everything
npm run cleanup
```
- Removes entire `target/` directory

---

## 📝 Code Quality Scripts

```bash
# Run linter
npm run lint
```
- Checks code style with ESLint

```bash
# Fix linting issues automatically
npm run lint:fix
```
- Auto-fixes lint errors where possible

```bash
# Check code formatting
npm run prettier:check
```
- Verifies Prettier formatting

```bash
# Format code
npm run prettier:format
```
- Formats all code files with Prettier

---

## 📋 Quick Reference Table

| Task | Command | Notes |
|------|---------|-------|
| **Develop Electron with hot reload** | `npm run electron:start` | ✅ Recommended for development |
| **Develop Electron manually** | `npm start` + `npm run electron:dev` | More control |
| **Test Electron production build** | `npm run electron:prod` | Builds and runs |
| **Run Electron (already built)** | `npm run electron` | Assumes build exists |
| **Create Windows installer** | `npm run electron:build` | Creates .exe installer |
| **Start web dev server** | `npm start` | For browser development |
| **Build web for production** | `npm run webapp:prod` | For web deployment |
| **Run tests** | `npm test` | Unit tests with coverage |
| **Fix code style** | `npm run lint:fix` | Auto-fix linting |
| **Format code** | `npm run prettier:format` | Auto-format code |
| **Clean build output** | `npm run clean-www` | Remove old builds |

---

## 🔄 Typical Workflows

### Working on Features (Electron)
```bash
# Terminal 1: Start backend
./mvnw

# Terminal 2: Start Electron with hot reload
npm run electron:start

# Make changes to code - they reflect immediately
```

### Working on Features (Web Browser)
```bash
# Terminal 1: Start backend
./mvnw

# Terminal 2: Start Angular dev server
npm start

# Open http://localhost:4200 in browser
```

### Testing Production Build (Electron)
```bash
# Build and run
npm run electron:prod

# If it works, create installer
npm run electron:build
```

### Preparing for Release
```bash
# 1. Clean everything
npm run cleanup

# 2. Run tests
npm test

# 3. Fix any issues
npm run lint:fix
npm run prettier:format

# 4. Create production build
npm run electron:build

# 5. Test the installer in dist-electron/
```

---

## ⚠️ Important Notes

### Backend Required
The application needs the Spring Boot backend running:
```bash
./mvnw
```

### Port Configuration
- **Dev server**: http://localhost:4200
- **Backend (dev)**: http://localhost:9080
- **Backend (prod)**: http://localhost:8080

### Memory Issues
If builds crash with "Fatal error":
1. Close other applications
2. Try building without running Electron:
   ```bash
   ng build --configuration=electron
   ```
3. Then run Electron separately:
   ```bash
   npm run electron
   ```

### Clean Builds
If you see stale data or weird errors:
```bash
npm run clean-www
npm run electron:prod
```

### Node/NPM Requirements
- Node.js >= 22.14.0
- npm >= 11.0.0

Check versions:
```bash
node --version
npm --version
```

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 4200 already in use | Kill process or use different port |
| "index.html NOT found" | Run `ng build --configuration=electron` first |
| Electron shows white screen | Check backend is running, check DevTools (Ctrl+Shift+I) |
| Build crashes with Fatal error | Close apps to free memory, build and run separately |
| "ng serve exited with code..." | Use `npm run electron:prod` instead of `electron:start` |
| Changes not reflecting | For hot reload use `electron:start`, not `electron:prod` |

---

## 💡 Pro Tips

1. **Use `electron:start` for development** - Fastest feedback loop
2. **Test with `electron:prod` before creating installer** - Catches build issues early
3. **Keep backend running** - App won't work without it
4. **Check DevTools** - Press Ctrl+Shift+I in Electron window for debugging
5. **Watch terminal output** - Electron logs helpful info about loading status
6. **Clean builds regularly** - Use `npm run clean-www` if things seem off

---

## 📦 Output Locations

- **Web builds**: `target/classes/static/`
- **Electron installers**: `dist-electron/`
- **Test coverage**: `target/test-results/`
- **Bundle analysis**: `target/stats.html`
