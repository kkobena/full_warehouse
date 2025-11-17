import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from 'environments/environment';

export interface AppSettings {
  apiServerUrl: string;
}

const DEFAULT_SETTINGS: AppSettings = {
  apiServerUrl: environment.apiServerUrl,
};

const SETTINGS_STORAGE_KEY = 'pharmasmart_app_settings';

@Injectable({
  providedIn: 'root',
})
export class AppSettingsService {
  public settings$: Observable<AppSettings>;
  private settingsSubject: BehaviorSubject<AppSettings>;
  private initialized = false;
  private initializationPromise: Promise<void> | null = null;

  constructor() {
    const storedSettings = this.loadSettings();
    this.settingsSubject = new BehaviorSubject<AppSettings>(storedSettings);
    this.settings$ = this.settingsSubject.asObservable();

    // Initialize Tauri backend URL if running in Tauri
    this.initializationPromise = this.initializeTauriBackendUrl();
  }

  /**
   * Wait for initialization to complete (useful for early API calls)
   */
  async waitForInitialization(): Promise<void> {
    if (this.initializationPromise) {
      await this.initializationPromise;
    }
  }

  /**
   * Get current settings
   */
  getSettings(): AppSettings {
    return this.settingsSubject.value;
  }

  /**
   * Get current API server URL
   */
  getApiServerUrl(): string {
    return this.settingsSubject.value.apiServerUrl;
  }

  /**
   * Update settings and persist to localStorage
   */
  updateSettings(settings: Partial<AppSettings>): void {
    const currentSettings = this.settingsSubject.value;
    const newSettings = { ...currentSettings, ...settings };

    this.settingsSubject.next(newSettings);
    this.saveSettings(newSettings);
  }

  /**
   * Update API server URL
   */
  updateApiServerUrl(url: string): void {
    // Remove trailing slash if present
    const cleanUrl = url.endsWith('/') ? url.slice(0, -1) : url;
    this.updateSettings({ apiServerUrl: cleanUrl });
  }

  /**
   * Reset settings to defaults
   */
  resetToDefaults(): void {
    this.settingsSubject.next(DEFAULT_SETTINGS);
    this.saveSettings(DEFAULT_SETTINGS);
  }

  /**
   * Test connection to API server
   */
  async testConnection(url?: string): Promise<boolean> {
    const testUrl = url || this.getApiServerUrl();
    try {
      const response = await fetch(`${testUrl}/management/health`, {
        method: 'GET',
        mode: 'cors',
      });
      return response.ok;
    } catch (error) {
      console.error('Connection test failed:', error);
      return false;
    }
  }

  /**
   * Initialize backend URL from Tauri if available
   */
  private async initializeTauriBackendUrl(): Promise<void> {
    if (this.initialized) {
      return;
    }

    try {
      const { invoke } = await import('@tauri-apps/api/core');

      // Get the configured backend URL from Tauri
      const tauriBackendUrl = await invoke<string>('get_backend_url_command');
      console.log('[AppSettingsService] Tauri backend URL:', tauriBackendUrl);

      // Only update if not already set by user
      const currentSettings = this.settingsSubject.value;
      const storedSettings = this.loadSettingsFromStorage();

      // If user hasn't manually set a different URL, use the Tauri backend URL
      if (!storedSettings || storedSettings.apiServerUrl === environment.apiServerUrl) {
        console.log('[AppSettingsService] Using Tauri backend URL:', tauriBackendUrl);
        this.settingsSubject.next({ apiServerUrl: tauriBackendUrl });
      } else {
        console.log('[AppSettingsService] Using user-configured URL:', currentSettings.apiServerUrl);
      }
    } catch (error) {
      console.warn('[AppSettingsService] Failed to get Tauri backend URL:', error);
    } finally {
      this.initialized = true;
    }
  }

  /**
   * Load settings from localStorage (returns null if not found)
   */
  private loadSettingsFromStorage(): AppSettings | null {
    try {
      const stored = localStorage.getItem(SETTINGS_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        // Validate that required properties exist
        if (parsed.apiServerUrl) {
          return parsed;
        }
      }
    } catch (error) {
      console.error('Failed to load settings from storage:', error);
    }
    return null;
  }

  /**
   * Load settings from localStorage with fallback to defaults
   */
  private loadSettings(): AppSettings {
    return this.loadSettingsFromStorage() || DEFAULT_SETTINGS;
  }

  /**
   * Save settings to localStorage
   */
  private saveSettings(settings: AppSettings): void {
    try {
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
    } catch (error) {
      console.error('Failed to save settings:', error);
    }
  }
}
