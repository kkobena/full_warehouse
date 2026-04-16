import { computed, inject, Injectable, signal } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { DashboardService } from 'app/home/dashboard.service';

/**
 * Service singleton pour les compteurs d'alertes de la pharmacie.
 * Expose des signaux réactifs consommables par la navbar, les dashboards,
 * et tout composant souhaitant afficher des badges d'alerte.
 *
 * Polling automatique toutes les 5 minutes.
 */
@Injectable({ providedIn: 'root' })
export class AlertBadgeService {
  // ─── Signaux exposés ─────────────────────────────────────────────────────

  /** Lots périmés ou expirant prochainement */
  readonly peremptionCount = signal(0);

  /** Produits en rupture de stock (StockAlertType.RUPTURE) */
  readonly ruptureCount = signal(0);

  /** Produits urgents à réapprovisionner — couverture de stock insuffisante (rupture + sous seuil selon VMM) */
  readonly urgentCount = signal(0);

  /** Ajustements de stock récents (24h) */
  readonly ajustementCount = signal(0);

  /** Entrées en stock récentes (24h) */
  readonly entreeCount = signal(0);

  /** Modifications de prix récentes (24h) */
  readonly prixModifCount = signal(0);

  /** Factures tiers-payant dont l'échéance de règlement est dépassée */
  readonly facturationOverdueCount = signal(0);


  // ─── Privé ───────────────────────────────────────────────────────────────

  private readonly dashboardService = inject(DashboardService);
  private pollingSubscription: Subscription | null = null;
  private initialized = false;

  // ─── API publique ─────────────────────────────────────────────────────────

  /**
   * Démarre le polling et charge les compteurs immédiatement.
   * Idempotent — peut être appelé plusieurs fois sans effet de bord.
   */
  init(): void {
    if (this.initialized) return;
    this.initialized = true;
    this.refresh();
    // Rafraîchissement toutes les 5 minutes
    this.pollingSubscription = interval(5 * 60 * 1000).subscribe(() => this.refresh());
  }

  /** Force un rechargement immédiat des compteurs. */
  refresh(): void {
    this.dashboardService.getAlertCounts().subscribe({
      next: res => {
        const d = res.body;
        if (!d) return;
        this.peremptionCount.set(d.peremptionCount ?? 0);
        this.ruptureCount.set(d.ruptureCount ?? 0);
        this.urgentCount.set(d.urgentCount ?? 0);
        this.ajustementCount.set(d.ajustementCount ?? 0);
        this.entreeCount.set(d.entreeCount ?? 0);
        this.prixModifCount.set(d.prixModifCount ?? 0);
        this.facturationOverdueCount.set(d.facturationOverdueCount ?? 0);
      },
      error: () => {
        // Silencieux — les compteurs restent à leur dernière valeur connue
      },
    });
  }

  /** Arrête le polling (utile lors des tests). */
  destroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
    this.initialized = false;
  }
}

