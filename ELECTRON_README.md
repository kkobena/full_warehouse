# Electron Desktop Application Guide

## Running the Electron Application

### Development Mode (Recommended for Development)

**Option 1: Using the combined script**
```bash
npm run electron:start
```
This command will:
- Start the Angular dev server on port 4200
- Wait for it to be ready
- Launch Electron and load from http://localhost:4200
- Enable hot module replacement (HMR)

**Option 2: Manual approach**
```bash
# Terminal 1 - Start Angular dev server
npm start

# Terminal 2 - Once dev server is ready, start Electron
npm run electron:dev
```

### Production Mode

**Step 1: Build the Angular application for Electron**
```bash
npm run electron:prod
```

This command will:
1. Build Angular with the `electron` configuration (outputs to `target/classes/static/`)
2. Launch Electron loading from the built files

**Step 2: Build a distributable executable (optional)**
```bash
npm run electron:build
```

This creates an installable Windows executable in the `dist-electron` folder.

## Troubleshooting

### White Screen / Angular Not Loading

**Symptoms**: Electron opens but shows a blank white screen

**Solutions**:

1. **For Development Mode**:
   - Ensure the Angular dev server is running first (`npm start`)
   - Wait for "Compiled successfully" message before starting Electron
   - Check that http://localhost:4200 works in a browser first

2. **For Production Mode**:
   - Make sure you ran the build command first: `ng build --configuration=electron`
   - Check that files exist in `target/classes/static/` directory
   - Look for error messages in the terminal where you started Electron

### Window Opens in Fullscreen

**Fixed**: The window configuration has been updated to prevent fullscreen on startup. The window now opens at 1400x900 pixels.

### No Close Button / Titlebar Not Showing

**Symptoms**: The custom titlebar with close button doesn't appear

**Causes & Solutions**:

1. **Angular Not Loaded**: If Angular doesn't load, the titlebar component won't render
   - Follow the "White Screen" troubleshooting steps above

2. **Check Console**: Open DevTools (automatically opens in dev mode) and check for errors

3. **Verify Component**: Ensure `TitlebarComponent` is imported in `app.component.ts`

### DevTools

- **Development mode**: DevTools open automatically
- **Production mode**: Press `Ctrl+Shift+I` (Windows/Linux) or `Cmd+Option+I` (Mac)

### Checking Logs

When you run Electron, the terminal will show detailed logs:
- ⏳ Page started loading...
- ✅ DOM is ready
- ✅ Page loaded successfully
- [Renderer]: Console messages from Angular

If you see errors, they will be marked with ❌ and include details.

## Environment Detection

The application automatically detects whether it's running in:
- **Browser**: Standard web environment
- **Electron**: Desktop application environment

Components like the titlebar and layout padding adjust automatically based on the environment.

## Key Features

### Custom Titlebar
- Only shown when running in Electron
- Draggable area for moving the window
- Minimize, Maximize/Restore, and Close buttons
- Styled to match the application theme

### Frameless Window
- Modern look without OS titlebar
- 32px custom titlebar at the top
- Main content automatically adjusts padding

### IPC Communication
- Secure communication between main and renderer processes
- Window control functions (minimize, maximize, close)
- Can be extended for printer access, file operations, etc.

## File Structure

```
├── electron.js              # Main Electron process
├── preload.js              # Secure bridge between main and renderer
├── package.json            # Electron scripts and build config
└── src/main/webapp/app/
    └── layouts/
        └── titlebar/       # Custom titlebar component
```

## Build Configuration

The `electron` configuration in `angular.json`:
- Sets `baseHref` to `./` for relative paths (required for file:// protocol)
- Disables service worker (not needed in Electron)
- Outputs to `target/classes/static/`
- Optimizes for production

## Security

- **Context Isolation**: Enabled (prevents renderer from accessing Node.js directly)
- **Node Integration**: Disabled (security best practice)
- **Preload Script**: Used for secure IPC exposure
- **CSP**: Content Security Policy configured for production builds

## Common Commands Reference

| Command | Description |
|---------|-------------|
| `npm run electron:start` | Start dev server + Electron together |
| `npm run electron:dev` | Start Electron in dev mode (requires dev server) |
| `npm run electron:prod` | Build and run in production mode |
| `npm run electron:build` | Create distributable executable |
| `npm run electron` | Run Electron (production mode, requires built files) |

## Tips

1. **Always use development mode** during development for faster iteration
2. **Test production builds** before creating distributables
3. **Check terminal output** for helpful debugging information
4. **Clear cache** if you experience issues: Delete `target/classes/static/` and rebuild
