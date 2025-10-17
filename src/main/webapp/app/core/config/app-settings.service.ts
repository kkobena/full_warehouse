import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from 'environments/environment';

export interface AppSettings {
  apiServerUrl: string;
}

const DEFAULT_SETTINGS: AppSettings = {
  apiServerUrl: environment.apiServerUrl
};

const SETTINGS_STORAGE_KEY = 'pharmasmart_app_settings';


@Injectable({
  providedIn: 'root'
})
export class AppSettingsService {
  private settingsSubject: BehaviorSubject<AppSettings>;
  public settings$: Observable<AppSettings>;

  constructor() {
    const storedSettings = this.loadSettings();
    this.settingsSubject = new BehaviorSubject<AppSettings>(storedSettings);
    this.settings$ = this.settingsSubject.asObservable();
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
        mode: 'cors'
      });
      return response.ok;
    } catch (error) {
      console.error('Connection test failed:', error);
      return false;
    }
  }

  /**
   * Load settings from localStorage
   */
  private loadSettings(): AppSettings {
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
      console.error('Failed to load settings:', error);
    }
    return DEFAULT_SETTINGS;
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
