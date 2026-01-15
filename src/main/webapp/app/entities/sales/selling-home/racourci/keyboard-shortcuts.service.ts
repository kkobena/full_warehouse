import { computed, Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  alt?: boolean;
  shift?: boolean;
  meta?: boolean; // For macOS Cmd key
  description: string;
  category: string;
  action: () => void;
  badge?: string; // Optional badge like "Essentiel", "Tauri only", etc.
  environmentRestriction?: 'web' | 'tauri'; // Where this shortcut is available
}

@Injectable({
  providedIn: 'root',
})
export class KeyboardShortcutsService {
  private shortcuts = signal<Map<string, KeyboardShortcut>>(new Map());
  shortcutsList = computed(() => Array.from(this.shortcuts().values()));
  private shortcutTriggered$ = new Subject<{ shortcut: KeyboardShortcut; timestamp: number }>();

  // Browser shortcuts to avoid (especially important for Web environment)
  private readonly BROWSER_SHORTCUTS = [
    'ctrl+n', // New window
    'ctrl+t', // New tab
    'ctrl+w', // Close tab
    'ctrl+shift+t', // Reopen closed tab
    'ctrl+l', // Address bar
    'ctrl+k', // Search bar
    'ctrl+h', // History
    'ctrl+j', // Downloads
    'ctrl+u', // View source
    'ctrl++', // Zoom in
    'ctrl+-', // Zoom out
    'ctrl+0', // Reset zoom
    'ctrl+r', // Refresh
    'ctrl+shift+r', // Hard refresh
    'ctrl+shift+i', // DevTools
    'ctrl+shift+j', // Console
    'ctrl+shift+c', // Inspect element
    'f12', // DevTools
    'alt+left', // Back
    'alt+right', // Forward
    'ctrl+tab', // Next tab
    'ctrl+shift+tab', // Previous tab
    'ctrl+shift+n', // Incognito
    'ctrl+shift+delete', // Clear data
  ];

  registerShortcut(shortcut: KeyboardShortcut): boolean {
    const key = this.getShortcutKey(shortcut);

    // Check if this is a browser-reserved shortcut
    if (this.BROWSER_SHORTCUTS.includes(key)) {
      console.warn(`[Keyboard Shortcuts] Cannot register "${key}" - reserved by browser`);
      return false;
    }

    const currentShortcuts = this.shortcuts();

    // Check for conflicts
    if (currentShortcuts.has(key)) {
      console.warn(`[Keyboard Shortcuts] Overwriting existing shortcut: ${key}`);
    }

    currentShortcuts.set(key, shortcut);
    this.shortcuts.set(new Map(currentShortcuts));
    return true;
  }

  unregisterShortcut(shortcut: KeyboardShortcut): void {
    const key = this.getShortcutKey(shortcut);
    const currentShortcuts = this.shortcuts();
    currentShortcuts.delete(key);
    this.shortcuts.set(new Map(currentShortcuts));
  }

  handleKeyboardEvent(event: KeyboardEvent): boolean {
    const key = this.getEventKey(event);
    const shortcut = this.shortcuts().get(key);

    if (shortcut) {
      event.preventDefault();
      event.stopPropagation();
      shortcut.action();
      this.shortcutTriggered$.next({ shortcut, timestamp: Date.now() });
      return true;
    }
    return false;
  }

  getShortcutTriggered$() {
    return this.shortcutTriggered$.asObservable();
  }

  clearAll(): void {
    this.shortcuts.set(new Map());
  }

  getShortcutsByCategory(category: string): KeyboardShortcut[] {
    return this.shortcutsList().filter(s => s.category === category);
  }

  getAllCategories(): string[] {
    const categories = new Set<string>();
    this.shortcutsList().forEach(s => categories.add(s.category));
    return Array.from(categories);
  }

  private getShortcutKey(shortcut: KeyboardShortcut): string {
    const parts: string[] = [];
    if (shortcut.ctrl) parts.push('ctrl');
    if (shortcut.alt) parts.push('alt');
    if (shortcut.shift) parts.push('shift');
    if (shortcut.meta) parts.push('meta');
    parts.push(shortcut.key.toLowerCase());
    return parts.join('+');
  }

  private getEventKey(event: KeyboardEvent): string {
    const parts: string[] = [];
    if (event.ctrlKey) parts.push('ctrl');
    if (event.altKey) parts.push('alt');
    if (event.shiftKey) parts.push('shift');
    if (event.metaKey) parts.push('meta');
    parts.push(event.key.toLowerCase());
    return parts.join('+');
  }
}
