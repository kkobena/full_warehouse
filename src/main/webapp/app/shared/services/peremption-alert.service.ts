import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface PeremptionAlerts {
  /** Lots déjà périmés non encore retirés du stock */
  expired: number;
  /** Lots expirant dans les 7 prochains jours */
  days7: number;
  /** Lots expirant dans les 30 prochains jours */
  days30: number;
}

@Injectable({ providedIn: 'root' })
export class PeremptionAlertService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);

  /** Signal réactif : somme lots périmés + expirant < 7j */
  private readonly _urgentCount = signal(0);
  readonly urgentCount = this._urgentCount.asReadonly();

  /**
   * Récupère les compteurs d'alertes depuis le backend.
   * À appeler au démarrage de l'app et toutes les 15 minutes.
   * Endpoint : GET /api/lot/alerts → { expired, days7, days30 }
   */
  fetchAlerts(): void {
    this.http.get<PeremptionAlerts>(this.config.getEndpointFor('api/lot/alerts')).subscribe({
      next: data => this._urgentCount.set((data.expired ?? 0) + (data.days7 ?? 0)),
      error: () => this._urgentCount.set(0),
    });
  }
}

