import { Injectable, signal } from '@angular/core';

/**
 * Service to detect and handle Tauri-specific keyboard shortcuts
 * Tauri allows more aggressive shortcut usage since we control the whole app
 */
@Injectable({
  providedIn: 'root',
})
export class TauriKeyboardService {
  private isTauri = signal(false);

  constructor() {
    this.detectTauri();
  }

  /**
   * Detect if running in Tauri environment
   */
  private detectTauri(): void {
    // Check for Tauri-specific window properties
    if (typeof window !== 'undefined') {
      // @ts-ignore - Tauri adds __TAURI__ to window
      this.isTauri.set(!!window.__TAURI__);
    }
  }

  /**
   * Check if we're running in Tauri
   */
  isRunningInTauri(): boolean {
    return this.isTauri();
  }

  /**
   * Get platform-specific modifier key
   * Cmd on macOS, Ctrl on Windows/Linux
   */
  getModifierKey(): 'Cmd' | 'Ctrl' {
    return this.isMacOS() ? 'Cmd' : 'Ctrl';
  }

  /**
   * Check if event uses the platform-specific modifier
   */
  isPlatformModifier(event: KeyboardEvent): boolean {
    return this.isMacOS() ? event.metaKey : event.ctrlKey;
  }

  /**
   * Detect if running on macOS
   * Uses modern User Agent Client Hints API when available, falls back to userAgent
   */
  private isMacOS(): boolean {
    if (typeof window === 'undefined' || typeof navigator === 'undefined') {
      return false;
    }

    // Modern approach: Use User Agent Client Hints API if available
    // @ts-ignore - userAgentData may not be available in all browsers yet
    if (navigator.userAgentData) {
      // @ts-ignore
      return navigator.userAgentData.platform?.toUpperCase().indexOf('MAC') >= 0;
    }

    // Fallback: Check userAgent string (more reliable than deprecated platform)
    return /Mac|iPhone|iPad|iPod/.test(navigator.userAgent);
  }

  /**
   * Shortcuts that are safe to use in Tauri (won't conflict with app)
   * These would normally conflict in browsers
   */
  getTauriSafeShortcuts(): string[] {
    if (!this.isRunningInTauri()) {
      return [];
    }

    return [
      'ctrl+s', // Save (we can override)
      'ctrl+p', // Print (we control it)
      'ctrl+f', // Find (we can implement custom)
      'ctrl+n', // New sale
      'ctrl+w', // Close/Cancel
      'ctrl+shift+s', // Save as pending
      'ctrl+enter', // Quick finalize
      'ctrl+q', // Quick quantity
      'ctrl+d', // Discount
      'ctrl+shift+p', // Print invoice
      'ctrl+shift+f', // Force stock
    ];
  }

  /**
   * Check if a shortcut is available in current environment
   */
  isShortcutAvailable(shortcutKey: string): boolean {
    if (this.isRunningInTauri()) {
      // In Tauri, all shortcuts are available
      return true;
    }

    // In browser, check against safe shortcuts
    const browserSafeShortcuts = [
      'f1',
      'f2',
      'f3',
      'f4',
      'f5',
      'f6',
      'f7',
      'f8',
      'f9',
      'f10',
      'f11',
      'f12',
      'escape',
      'delete',
      'insert',
      'home',
      'end',
      'pageup',
      'pagedown',
    ];

    const isAltCombo = shortcutKey.includes('alt+');
    const isSafeKey = browserSafeShortcuts.some(safe => shortcutKey.includes(safe));

    return isAltCombo || isSafeKey;
  }
}
