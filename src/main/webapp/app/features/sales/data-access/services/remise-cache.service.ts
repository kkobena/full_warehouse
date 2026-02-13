import { computed, inject, Injectable, signal } from '@angular/core';
import { RemiseService } from '../../../../entities/remise/remise.service';
import { IRemise } from '../../../../shared/model';

/**
 * Service de cache pour les remises activées.
 *
 * Singleton (providedIn: 'root') : un seul appel HTTP quel que soit
 * le nombre de composants consommateurs (comptant, assurance, carnet).
 *
 * Usage : injecter puis lire `remises()` (signal readonly).
 * Appeler `refresh()` si les remises ont été modifiées côté admin.
 */
@Injectable({
  providedIn: 'root',
})
export class RemiseCacheService {
  private remiseService = inject(RemiseService);

  private readonly _remises = signal<IRemise[]>([]);
  private loaded = false;
  private loading = false;

  /** Remises activées (liste plate, prête pour p-select) */
  readonly remises = computed(() => this._remises());

  constructor() {
    this.load();
  }

  /** Force le rechargement depuis le serveur */
  refresh(): void {
    this.loaded = false;
    this.load();
  }

  private load(): void {
    if (this.loaded || this.loading) {
      return;
    }
    this.loading = true;
    this.remiseService.query().subscribe({
      next: res => {
        const all = res.body || [];
        this._remises.set(all.filter(r => r.enable));
        this.loaded = true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
