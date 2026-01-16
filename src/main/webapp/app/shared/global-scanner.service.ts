import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { BaseScannerService, ScanProcessResult } from './scanner';

const STORAGE_KEY = 'pharma_global_scanner_enabled';

/**
 * Service de scanner global qui capture les codes-barres au niveau de la page,
 * indépendamment du focus des champs de saisie.
 *
 * Ce service étend BaseScannerService et ajoute :
 * - Activation/désactivation du scanner global
 * - Persistance de l'état dans localStorage
 * - Observable dédié pour les scans complétés
 *
 * @example
 * // Dans un composant :
 * globalScannerService.enable();
 * globalScannerService.onScan$.subscribe(code => console.log('Scanned:', code));
 *
 * // Dans un @HostListener :
 * const result = globalScannerService.processKey(event.key);
 * if (result.isScanInProgress) event.preventDefault();
 */
@Injectable({ providedIn: 'root' })
export class GlobalScannerService extends BaseScannerService {
  // État activé/désactivé (signal pour réactivité)
  private readonly _enabled = signal(false);
  readonly enabled = this._enabled.asReadonly();

  // Observable dédié pour les codes scannés (en plus de onScanEvent$)
  private readonly scannedCode$ = new Subject<string>();
  readonly onScan$ = this.scannedCode$.asObservable();

  constructor() {
    super();
    this.restoreState();
  }

  /**
   * Active le scanner global.
   * L'état est persisté dans localStorage.
   */
  enable(): void {
    this._enabled.set(true);
    this.persistState(true);
    this.reset();
  }

  /**
   * Désactive le scanner global.
   * L'état est persisté dans localStorage.
   */
  disable(): void {
    this._enabled.set(false);
    this.persistState(false);
    this.reset();
  }

  /**
   * Bascule l'état du scanner global.
   */
  toggle(): void {
    if (this._enabled()) {
      this.disable();
    } else {
      this.enable();
    }
  }

  /**
   * Traite une touche pressée.
   * Ne fait rien si le scanner est désactivé.
   *
   * @param key - La touche pressée (event.key)
   * @returns Résultat indiquant l'état du scan
   */
  override processKey(key: string): ScanProcessResult {
    // Si désactivé, ne rien faire
    if (!this._enabled()) {
      return { isScanInProgress: false, completedCode: null };
    }

    return super.processKey(key);
  }

  /**
   * Appelé quand un scan est complété avec succès.
   * Émet sur les deux observables (onScanEvent$ et onScan$).
   */
  protected override onScanCompleted(code: string): void {
    super.onScanCompleted(code);
    this.scannedCode$.next(code);
  }

  /**
   * Restaure l'état depuis localStorage.
   */
  private restoreState(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'true') {
        this._enabled.set(true);
      }
    } catch {
      // localStorage non disponible (SSR, etc.)
    }
  }

  /**
   * Persiste l'état dans localStorage.
   */
  private persistState(enabled: boolean): void {
    try {
      localStorage.setItem(STORAGE_KEY, String(enabled));
    } catch {
      // localStorage non disponible (SSR, etc.)
    }
  }
}
