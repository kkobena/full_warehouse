import { computed, Injectable, signal } from '@angular/core';

export type LayoutMode = 'navbar' | 'sidebar';

const LAYOUT_STORAGE_KEY = 'pharmasmart_layout_mode';
const SIDEBAR_COLLAPSED_KEY = 'pharmasmart_sidebar_collapsed';

@Injectable({
  providedIn: 'root',
})
export class LayoutService {
  readonly layoutMode = signal<LayoutMode>(this.loadEffectiveLayoutMode());
  readonly sidebarCollapsed = signal<boolean>(this.loadSidebarCollapsed());

  readonly isSidebarMode = computed(() => this.layoutMode() === 'sidebar');
  readonly isNavbarMode = computed(() => this.layoutMode() === 'navbar');

  isSidebar(): boolean {
    return this.layoutMode() === 'sidebar';
  }

  setLayoutMode(mode: LayoutMode): void {
    this.layoutMode.set(mode);
    this.saveToStorage(LAYOUT_STORAGE_KEY, mode);
  }

  toggleLayout(): void {
    this.setLayoutMode(this.isSidebar() ? 'navbar' : 'sidebar');
  }

  isSidebarCollapsed(): boolean {
    return this.sidebarCollapsed();
  }

  setSidebarCollapsed(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
    this.saveToStorage(SIDEBAR_COLLAPSED_KEY, String(collapsed));
  }

  toggleSidebarCollapsed(): void {
    this.setSidebarCollapsed(!this.isSidebarCollapsed());
  }

  private loadEffectiveLayoutMode(): LayoutMode {
    const stored = this.loadFromStorage(LAYOUT_STORAGE_KEY, 'navbar');
    // Sur petit écran (< 768px), forcer navbar — la sidebar n'a pas de sens sur mobile
    if (stored === 'sidebar' && typeof window !== 'undefined' && window.innerWidth < 768) {
      return 'navbar';
    }
    return stored === 'navbar' || stored === 'sidebar' ? stored : 'navbar';
  }

  private loadSidebarCollapsed(): boolean {
    return this.loadFromStorage(SIDEBAR_COLLAPSED_KEY, 'false') === 'true';
  }

  private loadFromStorage(key: string, defaultValue: string): string {
    try {
      return localStorage.getItem(key) ?? defaultValue;
    } catch {
      return defaultValue;
    }
  }

  private saveToStorage(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch { /* silently ignore storage errors */ }
  }
}
