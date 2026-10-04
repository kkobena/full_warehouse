import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { FavorisComptoirService } from '../../data-access/services/favoris-comptoir.service';

/**
 * Étoile « favori » d'un produit : pleine s'il est épinglé à la grille du comptoir, vide sinon. Un clic épingle ou retire.
 * N'apparaît que pour qui a le droit de gérer les favoris : les autres voient la grille, pas l'étoile.
 */
@Component({
  selector: 'app-favori-etoile',
  imports: [NgbTooltip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (favoris.peutGerer()) {
      <button
        (click)="basculer($event)"
        [attr.aria-label]="(estFavori() ? 'Retirer ' : 'Épingler ') + libelle() + (estFavori() ? ' des favoris' : ' aux favoris')"
        [attr.aria-pressed]="estFavori()"
        [class.favori-actif]="estFavori()"
        [ngbTooltip]="estFavori() ? 'Retirer des favoris du comptoir' : 'Épingler aux favoris du comptoir'"
        class="favori-etoile"
        container="body"
        placement="top"
        type="button"
      >
        <i [class]="estFavori() ? 'pi pi-star-fill' : 'pi pi-star'" aria-hidden="true"></i>
      </button>
    }
  `,
  styles: `
    .favori-etoile {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 44px;
      min-height: 44px;
      padding: 0;
      border: 0;
      background: transparent;
      color: var(--pharma-text-muted, #5b6770);
      font-size: 1.1rem;
      border-radius: 50%;
    }

    .favori-etoile:hover,
    .favori-etoile:focus-visible {
      color: #b45309;
      background: #fef3c7;
    }

    .favori-actif {
      color: #b45309;
    }
  `,
})
export class FavoriEtoileComponent {
  readonly produitId = input.required<number>();
  readonly libelle = input<string>('');

  protected readonly favoris = inject(FavorisComptoirService);

  protected estFavori(): boolean {
    return this.favoris.estFavori(this.produitId());
  }

  protected basculer(event: Event): void {
    // Dans le panier, la ligne réagit à ses propres clics : l'étoile ne doit pas la sélectionner.
    event.stopPropagation();
    this.favoris.basculer(this.produitId(), this.libelle());
  }
}
