import { inject, Injectable, signal } from '@angular/core';
import { merge, Subject, take } from 'rxjs';
import { BaseScannerService, ScanProcessResult } from './scanner';
import { ScannerMode } from './scanner';
import { PrefixSuffixScannerService } from './scanner';
import { ConfigurationService } from './configuration.service';

const STORAGE_KEY = 'pharma_global_scanner_enabled';

/**
 * Service de scanner global qui capture les codes-barres au niveau de la page,
 * indépendamment du focus des champs de saisie.
 *
 * Supporte deux modes de détection :
 * - TIMING : détection par vitesse de frappe (par défaut)
 * - PREFIX_SUFFIX : détection par préfixe/suffixe (STX/ETX)
 *
 * Le mode est déterminé par le paramètre applicatif APP_SCANNER_MODE.
 */
@Injectable({ providedIn: 'root' })
export class GlobalScannerService extends BaseScannerService {
  private readonly _enabled = signal(false);
  readonly enabled = this._enabled.asReadonly();

  private readonly mode = signal<ScannerMode>('TIMING');
  private readonly prefixSuffixScanner = inject(PrefixSuffixScannerService);
  private readonly configService = inject(ConfigurationService);

  // Observable dédié pour les codes scannés via le mode TIMING
  private readonly scannedCode$ = new Subject<string>();

  // Observable unifié : merge du mode TIMING et du mode PREFIX_SUFFIX
  readonly onScan$ = merge(this.scannedCode$.asObservable(), this.prefixSuffixScanner.onScan$);

  constructor() {
    super();
    this.restoreState();
  }

  /**
   * Active le scanner global.
   * Charge le mode scanner depuis l'API et persiste l'état.
   */
  enable(): void {
    this._enabled.set(true);
    this.persistState(true);
    this.reset();
    this.loadScannerMode();
  }

  /**
   * Désactive le scanner global.
   */
  disable(): void {
    this._enabled.set(false);
    this.persistState(false);
    this.reset();
    this.prefixSuffixScanner.forceReset();
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
   * Vérifie si un scan est en cours (TIMING ou PREFIX_SUFFIX).
   */
  override isScanActive(): boolean {
    if (this.mode() === 'PREFIX_SUFFIX') {
      return this.prefixSuffixScanner.isScanActive();
    }
    return super.isScanActive();
  }

  /**
   * Traite un événement clavier complet.
   * Délègue au bon mode de détection (TIMING ou PREFIX_SUFFIX).
   */
  processKeyEvent(event: KeyboardEvent): { isScanInProgress: boolean } {
    if (!this._enabled()) {
      return { isScanInProgress: false };
    }

    if (this.mode() === 'PREFIX_SUFFIX') {
      const result = this.prefixSuffixScanner.processKeyEvent(event, this.config.prefixKey, this.config.suffixKey);
      if (result.handled) {
        return { isScanInProgress: result.isScanInProgress };
      }
      // Pas géré par prefix/suffix → ne rien faire (pas de double détection)
      return { isScanInProgress: false };
    }

    // Mode TIMING (existant)
    return super.processKey(event.key);
  }

  /**
   * Traite une touche pressée (mode TIMING uniquement, pour rétro-compatibilité).
   */
  override processKey(key: string): ScanProcessResult {
    if (!this._enabled()) {
      return { isScanInProgress: false, completedCode: null };
    }
    return super.processKey(key);
  }

  /**
   * Appelé quand un scan TIMING est complété avec succès.
   */
  protected override onScanCompleted(code: string): void {
    super.onScanCompleted(code);
    this.scannedCode$.next(code);
  }

  /**
   * Charge le mode scanner depuis l'API (APP_SCANNER_MODE).
   */
  private loadScannerMode(): void {
    this.configService
      .find('APP_SCANNER_MODE')
      .pipe(take(1))
      .subscribe({
        next: res => {
          const value = res.body?.value;
          this.mode.set(value === 'PREFIX_SUFFIX' ? 'PREFIX_SUFFIX' : 'TIMING');
        },
        error: () => {
          // Fallback sur TIMING en cas d'erreur
          this.mode.set('TIMING');
        },
      });
  }

  private restoreState(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'true') {
        this._enabled.set(true);
      }
    } catch {
      // localStorage non disponible
    }
  }

  private persistState(enabled: boolean): void {
    try {
      localStorage.setItem(STORAGE_KEY, String(enabled));
    } catch {
      // localStorage non disponible
    }
  }
}
