import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type LayoutMode = 'navbar' | 'sidebar';

const LAYOUT_STORAGE_KEY = 'pharmasmart_layout_mode';
const SIDEBAR_COLLAPSED_KEY = 'pharmasmart_sidebar_collapsed';

/**
 * Service for managing application layout mode (navbar vs sidebar).
 * Layout preference is persisted in localStorage.
 */
@Injectable({
  providedIn: 'root',
})
export class LayoutService {
  private layoutModeSubject: BehaviorSubject<LayoutMode>;
  public layoutMode$: Observable<LayoutMode>;

  private sidebarCollapsedSubject: BehaviorSubject<boolean>;
  public sidebarCollapsed$: Observable<boolean>;

  constructor() {
    const storedMode = this.loadLayoutMode();
    this.layoutModeSubject = new BehaviorSubject<LayoutMode>(storedMode);
    this.layoutMode$ = this.layoutModeSubject.asObservable();

    const storedCollapsed = this.loadSidebarCollapsed();
    this.sidebarCollapsedSubject = new BehaviorSubject<boolean>(storedCollapsed);
    this.sidebarCollapsed$ = this.sidebarCollapsedSubject.asObservable();
  }

  /**
   * Get current layout mode
   */
  getLayoutMode(): LayoutMode {
    return this.layoutModeSubject.value;
  }

  /**
   * Check if current layout is sidebar
   */
  isSidebar(): boolean {
    return this.layoutModeSubject.value === 'sidebar';
  }

  /**
   * Check if current layout is navbar
   */
  isNavbar(): boolean {
    return this.layoutModeSubject.value === 'navbar';
  }

  /**
   * Set layout mode
   */
  setLayoutMode(mode: LayoutMode): void {
    this.layoutModeSubject.next(mode);
    this.saveLayoutMode(mode);
  }

  /**
   * Toggle between navbar and sidebar
   */
  toggleLayout(): void {
    const newMode: LayoutMode = this.isSidebar() ? 'navbar' : 'sidebar';
    this.setLayoutMode(newMode);
  }

  /**
   * Load layout mode from localStorage
   */
  private loadLayoutMode(): LayoutMode {
    try {
      const stored = localStorage.getItem(LAYOUT_STORAGE_KEY);
      if (stored === 'navbar' || stored === 'sidebar') {
        return stored;
      }
    } catch (error) {
      console.error('Failed to load layout mode:', error);
    }
    return 'navbar'; // Default to navbar
  }

  /**
   * Save layout mode to localStorage
   */
  private saveLayoutMode(mode: LayoutMode): void {
    try {
      localStorage.setItem(LAYOUT_STORAGE_KEY, mode);
    } catch (error) {
      console.error('Failed to save layout mode:', error);
    }
  }

  /**
   * Get current sidebar collapsed state
   */
  isSidebarCollapsed(): boolean {
    return this.sidebarCollapsedSubject.value;
  }

  /**
   * Set sidebar collapsed state
   */
  setSidebarCollapsed(collapsed: boolean): void {
    this.sidebarCollapsedSubject.next(collapsed);
    this.saveSidebarCollapsed(collapsed);
  }

  /**
   * Toggle sidebar collapsed state
   */
  toggleSidebarCollapsed(): void {
    this.setSidebarCollapsed(!this.isSidebarCollapsed());
  }

  /**
   * Load sidebar collapsed state from localStorage
   */
  private loadSidebarCollapsed(): boolean {
    try {
      const stored = localStorage.getItem(SIDEBAR_COLLAPSED_KEY);
      return stored === 'true';
    } catch (error) {
      console.error('Failed to load sidebar collapsed state:', error);
    }
    return false; // Default to expanded
  }

  /**
   * Save sidebar collapsed state to localStorage
   */
  private saveSidebarCollapsed(collapsed: boolean): void {
    try {
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, collapsed.toString());
    } catch (error) {
      console.error('Failed to save sidebar collapsed state:', error);
    }
  }
}
