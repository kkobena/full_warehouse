import { inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { NavApiService } from 'app/core/data-access/nav-api.service';
import { AbilityService } from 'app/core/auth/ability.service';
import { INavNode } from 'app/shared/model/nav-item.model';

/**
 * Store de navigation dynamique basé sur les Signals Angular.
 * Charge l'arbre de navigation depuis l'API et le met en cache.
 */
@Injectable({ providedIn: 'root' })
export class NavStore {
  private readonly api = inject(NavApiService);
  private readonly abilityService = inject(AbilityService);

  readonly navTree = signal<INavNode[]>([]);
  readonly loading = signal(false);
  readonly loaded = signal(false);

  /** Charge l'arbre de navigation si pas encore chargé. */
  load(): void {
    if (this.loaded()) return;
    this.loading.set(true);
    this.api
      .getMyNavItems()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: tree => {
          this.navTree.set(tree);
          this.loaded.set(true);
          // Initialise les abilities depuis l'arbre chargé
          this.abilityService.setFromNavTree(tree);
        },
        error: () => {
          // En cas d'erreur, on garde l'arbre vide
          this.loaded.set(false);
        },
      });
  }

  /** Invalide le cache — le prochain appel à load() rechargera depuis l'API. */
  invalidate(): void {
    this.loaded.set(false);
    this.navTree.set([]);
  }

  /** Mise à jour optimiste lors d'un drag & drop (avant la confirmation serveur). */
  applyLocalReorder(reordered: INavNode[]): void {
    this.navTree.set(reordered);
  }
}

