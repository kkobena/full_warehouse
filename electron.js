const { app, BrowserWindow, protocol } = require('electron');
const path = require('path');
const url = require('url');
const fs = require('fs');

let win;

function createWindow() {
  win = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      nodeIntegration: false, // Security best practice
      contextIsolation: true, // Security best practice
      webSecurity: true,
      preload: path.join(__dirname, 'preload.js'), // Add preload script for secure IPC
    },
  });

  // Determine the start URL
  const startUrl = process.env.ELECTRON_START_URL || url.format({
    pathname: path.join(__dirname, 'target/classes/static/index.html'),
    protocol: 'file:',
    slashes: true,
  });

  console.log('Loading URL:', startUrl);

  // Check if file exists (for production)
  if (!process.env.ELECTRON_START_URL) {
    const indexPath = path.join(__dirname, 'target/classes/static/index.html');
    if (fs.existsSync(indexPath)) {
      console.log('index.html found at:', indexPath);
    } else {
      console.error('index.html NOT found at:', indexPath);
      console.error('Available files:', fs.readdirSync(path.join(__dirname, 'target/classes/static')).slice(0, 10));
    }
  }

  // Load the URL
  win.loadURL(startUrl);

  // Always open DevTools to debug white screen issues
 win.webContents.openDevTools();

  // Log console messages from renderer
  win.webContents.on('console-message', (event, level, message, line, sourceId) => {
    console.log('[Renderer]:', message);
  });

  // Log errors
  win.webContents.on('did-fail-load', (event, errorCode, errorDescription, validatedURL) => {
    console.error('Failed to load:', validatedURL);
    console.error('Error:', errorCode, errorDescription);
  });

  // Log when page finishes loading
  win.webContents.on('did-finish-load', () => {
    console.log('Page loaded successfully');
  });

  win.on('closed', () => {
    win = null;
  });
}

// Register file protocol for production build
app.whenReady().then(() => {
  // This allows loading local files with file:// protocol
  protocol.registerFileProtocol('file', (request, callback) => {
    const pathname = decodeURI(request.url.replace('file:///', ''));
    callback(pathname);
  });

  createWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (win === null) {
    createWindow();
  }
});

// Handle CSP errors in development
app.on('web-contents-created', (event, contents) => {
  contents.on('did-fail-load', (event, errorCode, errorDescription) => {
    console.error('Load failed:', errorCode, errorDescription);
  });
});
