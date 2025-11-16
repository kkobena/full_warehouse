/**
 * Tauri Initialization Script
 *
 * This script initializes Tauri APIs and exposes them globally for debugging
 * and backward compatibility with code expecting window.__TAURI__
 */

// Dynamically import and expose Tauri APIs
export async function initializeTauri(): Promise<void> {
  if (typeof window === 'undefined') {
    return;
  }

  // Check if we're actually running in Tauri runtime (not just browser with package installed)
  // @ts-ignore
  if (!window.__TAURI_INTERNALS__) {
    console.log('ℹ️ Tauri not available (running in browser)');
    return;
  }

  try {
    // Import core Tauri APIs
    const { invoke } = await import('@tauri-apps/api/core');

    // Create global Tauri object for backward compatibility
    // @ts-ignore
    window.__TAURI__ = {
      core: {
        invoke,
      },
    };

    console.log('✅ Tauri initialized successfully');
    console.log('Available: window.__TAURI__.core.invoke');

    return;
  } catch (error) {
    console.error('❌ Failed to initialize Tauri:', error);
  }
}

// Auto-initialize on import
if (typeof window !== 'undefined') {
  // Use void to explicitly ignore the promise (this runs async in background)
  void initializeTauri();
}
