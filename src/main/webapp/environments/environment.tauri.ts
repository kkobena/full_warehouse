export const environment = {
  VERSION: __VERSION__,
  DEBUG_INFO_ENABLED: true,
  production: true,
  // For Tauri: Allow connection to backend on localhost or network IP
  // Users can change this to their backend server IP via Settings dialog
  apiServerUrl: 'http://localhost:8080',
  // Set to true when running as Tauri desktop app
  isTauri: true,
};
