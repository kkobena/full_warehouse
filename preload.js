const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer process
contextBridge.exposeInMainWorld('electron', {
  // Platform info
  platform: process.platform,

  // IPC communication for printer and hardware access
  send: (channel, data) => {
    const validChannels = ['print-receipt', 'open-cash-drawer'];
    if (validChannels.includes(channel)) {
      ipcRenderer.send(channel, data);
    }
  },

  receive: (channel, func) => {
    const validChannels = ['print-complete', 'drawer-opened'];
    if (validChannels.includes(channel)) {
      ipcRenderer.on(channel, (event, ...args) => func(...args));
    }
  }
});

// Log that preload script loaded
console.log('Preload script loaded successfully');
