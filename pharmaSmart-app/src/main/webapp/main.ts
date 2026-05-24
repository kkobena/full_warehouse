// Initialize Tauri APIs globally (runs in background, doesn't block bootstrap)
void import('./app/tauri-init').catch(() => {});

// Bootstrap Angular application
import('./bootstrap').catch((err: unknown) => console.error(err));
