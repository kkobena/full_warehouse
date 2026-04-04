import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

/**
 * Types d'alertes stock — miroir de StockAlertType.java (backend).
 * Source unique : vue matérialisée mv_stock_alerts.
 */
export type StockAlertType = 'RUPTURE' | 'ALERTE' | 'PEREMPTION';

/** Map retournée par GET /api/stock/alerts/count */
export type StockAlertCountMap = Partial<Record<StockAlertType, number>>;

@Injectable({ providedIn: 'root' })
export class PeremptionAlertService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);

  // ── Signaux réactifs (lus par sidebar, navbar, dashboard) ──────────
  /** Péremptions (lots < 3 mois) */
  private readonly _peremptionCount = signal(0);
  readonly peremptionCount = this._peremptionCount.asReadonly();

  /** Ruptures de stock */
  private readonly _ruptureCount = signal(0);
  readonly ruptureCount = this._ruptureCount.asReadonly();

  /** Stock sous seuil minimum */
  private readonly _alerteCount = signal(0);
  readonly alerteCount = this._alerteCount.asReadonly();

  /**
   * Signal utilisé par la navigation (badge rouge "Gestion Péremptions").
   * = peremptionCount (alias pour compatibilité avec navigation.service.ts).
   */
  readonly urgentCount = this._peremptionCount.asReadonly();

  // ── Source unique : GET /api/stock/alerts/count ────────────────────
  /**
   * Récupère les compteurs depuis mv_stock_alerts via StockAlertReportService.
   * À appeler au démarrage + polling toutes les 15 min (main.component.ts).
   */
  fetchAlerts(): void {
    this.http
      .get<StockAlertCountMap>(this.config.getEndpointFor('api/stock/alerts/count'))
      .subscribe({
        next: data => {
          this._peremptionCount.set(data['PEREMPTION'] ?? 0);
          this._ruptureCount.set(data['RUPTURE'] ?? 0);
          this._alerteCount.set(data['ALERTE'] ?? 0);
        },
        error: () => {
          this._peremptionCount.set(0);
          this._ruptureCount.set(0);
          this._alerteCount.set(0);
        },
      });
  }
}
