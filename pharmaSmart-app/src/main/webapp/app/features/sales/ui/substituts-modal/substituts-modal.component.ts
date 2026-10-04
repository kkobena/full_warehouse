import { ModalDeplacableDirective } from 'app/shared/utils/modal-deplacable.directive';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DevisePipe } from 'app/shared/utils/devise';
import { BadgeComponent, ButtonComponent } from '../../../../shared/ui';
import { ProduitSearch } from '../../../../shared/model';
import { SubstitutPropose, SubstitutsApiService } from '../../data-access/services/substituts-api.service';
import { ProduitBadgesComponent } from '../produit-badges/produit-badges.component';

/** Le produit dont on cherche un équivalent : ce qu'il faut pour comparer (prix, quantité demandée). */
export interface OrigineSubstitution {
  id: number;
  libelle: string;
  /** Prix unitaire de l'original, pour afficher l'écart ; absent, aucun écart n'est affiché. */
  prixUnitaire?: number | null;
  /** Quantité demandée : un substitut dont le stock ne la couvre pas est signalé. */
  quantite?: number | null;
}

type Etat = 'chargement' | 'pret' | 'erreur';

/**
 * Équivalents disponibles d'un produit, avec le prix comparé et le stock : le préparateur décide devant le client.
 * Ferme avec le produit choisi (au format de la recherche, donc ajoutable tel quel), ou rejette sur « Fermer ».
 */
@Component({
  selector: 'app-substituts-modal',
  templateUrl: './substituts-modal.component.html',
  styleUrl: './substituts-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-comptoir-mode]': 'mode',
    '[attr.data-comptoir-doc]': "doc ? 'true' : null",
  },
  imports: [ModalDeplacableDirective, ButtonComponent, BadgeComponent, DevisePipe, ProduitBadgesComponent],
})
export class SubstitutsModalComponent implements OnInit {
  /** Accent du type de vente en cours, lu sur la page de vente : la fenêtre, rendue hors d'elle, ne peut pas l'hériter. */
  protected readonly mode = document.querySelector('app-sales-home')?.getAttribute('data-comptoir-mode') ?? 'comptant';
  protected readonly doc = document.querySelector('app-sales-home')?.getAttribute('data-comptoir-doc') === 'true';

  // Renseignée via componentInstance avant le premier rendu.
  origine!: OrigineSubstitution;

  protected readonly etat = signal<Etat>('chargement');
  protected readonly propositions = signal<SubstitutPropose[]>([]);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(SubstitutsApiService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.api
      .lireDisponibles(this.origine.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: propositions => {
          this.propositions.set(propositions);
          this.etat.set('pret');
        },
        error: () => this.etat.set('erreur'),
      });
  }

  /** Prix du substitut moins celui de l'original : négatif, le client y gagne. Nul si l'original n'a pas de prix connu. */
  protected ecart(proposition: SubstitutPropose): number | null {
    const prixOrigine = this.origine.prixUnitaire;
    const prix = proposition.produit.regularUnitPrice;
    return prixOrigine == null || prix == null ? null : prix - prixOrigine;
  }

  protected stockInsuffisant(proposition: SubstitutPropose): boolean {
    const demandee = this.origine.quantite;
    return demandee != null && (proposition.produit.totalQuantity ?? 0) < demandee;
  }

  protected choisir(produit: ProduitSearch): void {
    this.activeModal.close(produit);
  }

  protected fermer(): void {
    this.activeModal.dismiss('fermer');
  }
}
