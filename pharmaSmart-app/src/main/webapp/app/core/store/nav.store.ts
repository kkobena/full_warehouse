import { computed, inject, Injectable, signal } from '@angular/core';
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

  /**
   * L'arbre à plat, indexé par code.
   *
   * <p>Les écrans à sous-menu vertical cherchent un nœud précis — « comptabilite.balance » — pour
   * en tirer son libellé et son icône. Sans index, chacun parcourrait l'arbre à chaque rendu.
   * `computed` le reconstruit à chaque changement de `navTree`, c'est-à-dire une fois par session.
   */
  private readonly index = computed(() => {
    const map = new Map<string, INavNode>();
    const empiler = (noeuds: INavNode[]): void => {
      for (const noeud of noeuds) {
        map.set(noeud.code, noeud);
        if (noeud.children?.length) {
          empiler(noeud.children);
        }
      }
    };
    empiler(this.navTree());
    return map;
  });

  /** Le nœud portant ce code, ou `undefined` s'il est absent de l'arbre servi à cet utilisateur. */
  node(code: string): INavNode | undefined {
    return this.index().get(code);
  }

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

