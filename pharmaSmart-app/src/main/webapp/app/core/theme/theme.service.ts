import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';

export type ThemeName = 'ardoise' | 'menthe' | 'clair';

export interface ThemeOption {
  name: ThemeName;
  label: string;
}

/** Mêmes noms que `$pharma-themes` dans `_pharma-themes.scss` ; le défaut en tête. */
export const THEMES: readonly ThemeOption[] = [
  { name: 'menthe', label: 'Menthe' },
  { name: 'ardoise', label: 'Ardoise' },
  { name: 'clair', label: 'Clair' },
];

const DEFAULT_THEME: ThemeName = 'menthe';
const THEME_STORAGE_KEY = 'pharmasmart_theme';

/** Thème du poste : posé en `data-theme` sur `<html>`, mémorisé dans le navigateur. */
@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly theme = signal<ThemeName>(this.loadFromStorage());

  loadCurrentTheme(): void {
    this.apply(this.theme());
  }

  setTheme(name: ThemeName): void {
    this.theme.set(name);
    this.apply(name);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, name);
    } catch { /* silently ignore storage errors */ }
  }

  private apply(name: ThemeName): void {
    this.document.documentElement.setAttribute('data-theme', name);
  }

  private loadFromStorage(): ThemeName {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY);
      return THEMES.some(t => t.name === stored) ? (stored as ThemeName) : DEFAULT_THEME;
    } catch {
      return DEFAULT_THEME;
    }
  }
}
