import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { GlobalScannerService } from '../../../../shared/global-scanner.service';
import { ScanOrchestratorService } from '../../../../shared/scanner';

/**
 * Service de scan du module vente — wrapper autour de {@link ScanOrchestratorService}.
 *
 * Responsabilité : abstraction matérielle uniquement (HID / USB CDC).
 * Aucune logique métier, aucun appel HTTP.
 *
 * Doit être déclaré dans `providers` du composant hébergeur (avec `ScanOrchestratorService`).
 */
@Injectable()
export class SalesScannerService {
  private readonly orchestrator = inject(ScanOrchestratorService);
  private readonly globalScanner = inject(GlobalScannerService);

  /** 'SERIAL' | 'HID' | null — utilisé par le badge UI. */
  readonly scannerMode = this.orchestrator.scannerMode;
  /** Bouton reconnexion CDC visible si HID forcé dans Tauri avec un poste configuré. */
  readonly canReconnect = this.orchestrator.canReconnect;
  /** Observable de codes-barres bruts unifié (HID + SERIAL). */
  readonly onScan$: Observable<string> = this.orchestrator.onScan$;

  constructor() {
    this.orchestrator.configure({
      eventName: 'scan-vente',
      hidSource$: this.globalScanner.onScan$,
      hidEnable: () => this.globalScanner.enable(),
      hidDisable: () => this.globalScanner.disable(),
    });
  }

  setup(posteId?: number): Promise<void> {
    return this.orchestrator.setup(posteId);
  }

  teardown(): Promise<void> {
    return this.orchestrator.teardown();
  }

  reconnect(): Promise<void> {
    return this.orchestrator.reconnect();
  }

  /**
   * Délègue le traitement clavier à GlobalScannerService (mode HID).
   * No-op si SERIAL est actif (pas de frappe clavier à traiter).
   */
  processKeyEvent(event: KeyboardEvent): { isScanInProgress: boolean } {
    if (this.scannerMode() === 'SERIAL') {
      return { isScanInProgress: false };
    }
    return this.globalScanner.processKeyEvent(event);
  }
}
