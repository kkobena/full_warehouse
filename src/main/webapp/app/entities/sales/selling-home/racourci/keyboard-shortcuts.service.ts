import { computed, Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  alt?: boolean;
  shift?: boolean;
  description: string;
  category: string;
  action: () => void;
}

@Injectable({
  providedIn: 'root',
})
export class KeyboardShortcutsService {
  private shortcuts = signal<Map<string, KeyboardShortcut>>(new Map());
  shortcutsList = computed(() => Array.from(this.shortcuts().values()));
  private shortcutTriggered$ = new Subject<string>();
  // Raccourcis navigateur à éviter
  private readonly BROWSER_SHORTCUTS = [
    'ctrl+n',
    'ctrl+t',
    'ctrl+w',
    'ctrl+shift+t',
    'ctrl+l',
    'ctrl+k',
    'ctrl+d',
    'ctrl+shift+d',
    'ctrl+h',
    'ctrl+j',
    'ctrl+p',
    'ctrl+s',
    'ctrl+f',
    'ctrl+g',
    'ctrl+shift+i',
    'f12',
    'ctrl+u',
    'ctrl++',
    'ctrl+-',
    'ctrl+0',
    'f5',
    'ctrl+r',
    'ctrl+shift+r',
    'alt+left',
    'alt+right',
    'ctrl+tab',
    'ctrl+shift+tab',
  ];

  registerShortcut(shortcut: KeyboardShortcut): boolean {
    const key = this.getShortcutKey(shortcut);

    if (this.BROWSER_SHORTCUTS.includes(key)) {
      return false;
    }

    const currentShortcuts = this.shortcuts();
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
      this.shortcutTriggered$.next(shortcut.description);
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

  private getShortcutKey(shortcut: KeyboardShortcut): string {
    return `${shortcut.ctrl ? 'ctrl+' : ''}${shortcut.alt ? 'alt+' : ''}${shortcut.shift ? 'shift+' : ''}${shortcut.key.toLowerCase()}`;
  }

  private getEventKey(event: KeyboardEvent): string {
    return `${event.ctrlKey ? 'ctrl+' : ''}${event.altKey ? 'alt+' : ''}${event.shiftKey ? 'shift+' : ''}${event.key.toLowerCase()}`;
  }
}
