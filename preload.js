const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer process
contextBridge.exposeInMainWorld('ipcRenderer', {
  send: (channel, data) => {
    // whitelist channels
    let validChannels = ['window-close', 'window-maximize', 'window-minimize'];
    if (validChannels.includes(channel)) {
      ipcRenderer.send(channel, data);
    }
  },
});

// Log that preload script loaded
console.log('Preload script loaded successfully');
