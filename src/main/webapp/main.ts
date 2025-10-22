// Initialize Tauri APIs globally (runs in background, doesn't block bootstrap)
void import('./app/tauri-init').catch(() => {
  // Silently fail if tauri-init can't be loaded (expected in browser)
});

// Bootstrap Angular application
import('./bootstrap').catch((err: unknown) => console.error(err));
