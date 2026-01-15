import { InjectionToken } from '@angular/core';

/**
 * Configuration pour les services de détection de scanner.
 * Ces valeurs peuvent être ajustées selon le type de scanner utilisé.
 */
export interface ScannerConfig {
  /** Délai maximum entre deux frappes pour être considéré comme un scan (ms) */
  scanDelayMs: number;

  /** Longueur minimum du code-barres pour être valide */
  scanMinLength: number;

  /** Durée maximum totale d'un scan (ms) */
  scanMaxTime: number;

  /** Délai avant reset si pas de nouvelle touche (ms) */
  resetDelay: number;

  /** Délai avant de terminer un scan après la dernière touche (ms) */
  endScanTimeout: number;
}

/**
 * Configuration par défaut pour les scanners de code-barres standard.
 * Compatible avec la plupart des scanners USB HID.
 */
export const DEFAULT_SCANNER_CONFIG: ScannerConfig = {
  scanDelayMs: 30,
  scanMinLength: 6,
  scanMaxTime: 500,
  resetDelay: 150,
  endScanTimeout: 100,
};

/**
 * Token d'injection pour la configuration du scanner.
 * Permet de personnaliser la configuration au niveau du module si nécessaire.
 *
 * @example
 * // Dans un module pour utiliser une config personnalisée :
 * providers: [
 *   { provide: SCANNER_CONFIG, useValue: { ...DEFAULT_SCANNER_CONFIG, scanDelayMs: 50 } }
 * ]
 */
export const SCANNER_CONFIG = new InjectionToken<ScannerConfig>('ScannerConfig', {
  providedIn: 'root',
  factory: () => DEFAULT_SCANNER_CONFIG,
});

/**
 * Liste des touches à ignorer lors de la détection de scan.
 * Ces touches ne sont pas des caractères imprimables ou sont des modificateurs.
 */
export const IGNORED_KEYS: readonly string[] = [
  'Shift',
  'Control',
  'Alt',
  'Meta',
  'CapsLock',
  'Tab',
  'Escape',
  'ArrowUp',
  'ArrowDown',
  'ArrowLeft',
  'ArrowRight',
  'Backspace',
  'Delete',
  'Insert',
  'Home',
  'End',
  'PageUp',
  'PageDown',
  'F1',
  'F2',
  'F3',
  'F4',
  'F5',
  'F6',
  'F7',
  'F8',
  'F9',
  'F10',
  'F11',
  'F12',
  'PrintScreen',
  'ScrollLock',
  'Pause',
  'ContextMenu',
  'NumLock',
  'Clear',
] as const;

/**
 * Vérifie si une touche doit être ignorée lors de la détection de scan.
 * @param key - La touche pressée (event.key)
 * @returns true si la touche doit être ignorée
 */
export function shouldIgnoreKey(key: string): boolean {
  return IGNORED_KEYS.includes(key) || (key.length > 1 && key !== 'Enter');
}
