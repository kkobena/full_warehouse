import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, of } from 'rxjs';
import { AbilityService } from 'app/core/auth/ability.service';
import { ProduitSearch } from 'app/shared/model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ProduitsFavorisApiService } from '../../../products/data-access/services/produits-favoris-api.service';

/**
 * Favoris du comptoir, partagés par la grille, la recherche produit et le panier : épingler ou retirer depuis n'importe quel
 * endroit met la grille à jour sans rechargement de page. Les favoris sont communs à la pharmacie.
 */
@Injectable({ providedIn: 'root' })
export class FavorisComptoirService {
  private readonly api = inject(ProduitsFavorisApiService);
  private readonly notificationService = inject(NotificationService);

  /** Droit propre à la vente : le caissier n'a pas le catalogue, et peut quand même épingler. */
  readonly peutGerer = inject(AbilityService).canSignal('edit', 'ventes.favoris.gerer');

  readonly favoris = signal<ProduitSearch[]>([]);
  private readonly ids = computed(() => new Set(this.favoris().map(f => f.id)));

  /** Relit la grille : le stock de chaque tuile bouge à chaque vente. */
  charger(): void {
    this.api
      .lister()
      .pipe(catchError(() => of<ProduitSearch[]>([])))
      .subscribe(favoris => this.favoris.set(favoris));
  }

  estFavori(produitId: number | null | undefined): boolean {
    return produitId != null && this.ids().has(produitId);
  }

  /** Épingle le produit, ou le retire s'il l'est déjà. Un refus (grille pleine…) est dit à l'utilisateur, l'étoile ne bouge pas. */
  basculer(produitId: number, libelle: string): void {
    const retrait = this.estFavori(produitId);
    (retrait ? this.api.retirer(produitId) : this.api.ajouter(produitId)).subscribe({
      next: () => {
        this.charger();
        this.notificationService.success(
          retrait ? `« ${libelle} » est retiré des favoris.` : `« ${libelle} » est ajouté aux favoris du comptoir.`,
          'Favoris du comptoir',
        );
      },
      // Le message du serveur dit pourquoi (grille pleine, produit introuvable) ; ErrorService n'est pas injecté ici : il exige la traduction.
      error: err => this.notificationService.error(err?.error?.message || err?.error?.detail || 'Favori non modifié.', 'Favoris du comptoir'),
    });
  }
}
