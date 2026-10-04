import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';
import { applyChrome, CHROME_STORAGE_KEY, ChromeName, readStoredChrome, toChrome } from './chrome-theme';

/**
 * Couleur de l'application (barres, en-têtes) du poste : posée en `data-chrome` sur `<html>`, mémorisée
 * dans le navigateur. Second axe, indépendant du fond de page (`ThemeService`). Voir
 * docs/PLAN-THEMES-APPLICATION.md.
 */
@Injectable({
  providedIn: 'root',
})
export class ChromeThemeService {
  private readonly document = inject(DOCUMENT);

  readonly chrome = signal<ChromeName>(readStoredChrome());

  /** Pose le choix du poste et suit les changements faits dans une autre fenêtre (même `localStorage`). */
  loadCurrentChrome(): void {
    applyChrome(this.chrome(), this.document.documentElement);
    this.document.defaultView?.addEventListener('storage', event => {
      if (event.key === CHROME_STORAGE_KEY) {
        const name = toChrome(event.newValue);
        this.chrome.set(name);
        applyChrome(name, this.document.documentElement);
      }
    });
  }

  setChrome(name: ChromeName): void {
    this.chrome.set(name);
    applyChrome(name, this.document.documentElement);
    try {
      localStorage.setItem(CHROME_STORAGE_KEY, name);
    } catch { /* silently ignore storage errors */ }
  }
}
