import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { DevisePipe } from 'app/shared/utils/devise';
import { formatNumber } from 'app/shared/utils/format-utils';
import { ProduitSearch } from '../../../../shared/model';
import { FavorisComptoirService } from '../../data-access/services/favoris-comptoir.service';
import { BadgePromoComponent } from '../../../promotion/ui/badge-promo/badge-promo.component';
import { IPromotionArticle } from '../../../promotion/models/promotion.model';
import { PromotionsEnVigueurService } from '../../../promotion/data-access/services/promotions-en-vigueur.service';

const CLE_OUVERT = 'pharmasmart_favoris_ouverts';

/** Rangée d'une tuile par produit favori : un clic ajoute le produit au panier, sans recherche ni quantité. */
@Component({
  selector: 'app-quick-products-grid',
  templateUrl: './quick-products-grid.component.html',
  styleUrl: './quick-products-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DevisePipe, BadgePromoComponent],
})
export class QuickProductsGridComponent implements OnInit {
  /** Vente en cours d'enregistrement : les tuiles ne répondent plus. */
  readonly disabled = input<boolean>(false);
  /** Le produit cliqué, au format de la recherche : l'écran de vente l'ajoute comme un produit scanné (quantité 1). */
  readonly productChosen = output<ProduitSearch>();

  private readonly service = inject(FavorisComptoirService);
  protected readonly favoris = this.service.favoris;
  private readonly promotions = inject(PromotionsEnVigueurService);
  protected readonly ouvert = signal<boolean>(lirePreference());

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.recharger();
  }

  /** Relit la grille : le stock de chaque tuile bouge à chaque vente. */
  recharger(): void {
    this.service.charger();
    this.promotions.charger();
  }

  protected basculer(): void {
    const ouvert = !this.ouvert();
    this.ouvert.set(ouvert);
    try {
      localStorage.setItem(CLE_OUVERT, ouvert ? '1' : '0');
    } catch {
      /* préférence d'affichage : sans stockage, elle ne survit pas au rechargement */
    }
  }

  protected choisir(produit: ProduitSearch): void {
    if (this.disabled()) {
      return;
    }
    this.productChosen.emit(produit);
    // Le clic change le stock : on relit la grille une fois l'ajout passé.
    timer(1500)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recharger());
  }

  protected promotion(produit: ProduitSearch): IPromotionArticle | undefined {
    return this.promotions.pour(produit.id);
  }

  /**
   * Le prix barré ne s'affiche que si un clic le fait payer : la tuile ajoute une unité, et une
   * promotion « dès 3 » ne remise pas une unité seule.
   */
  protected prixRemise(produit: ProduitSearch): number | null {
    const promo = this.promotion(produit);
    return promo && promo.quantiteMin <= 1 ? promo.prixApresPromo : null;
  }

  /** Le prix barré sans devise : celle du prix remisé suffit, et la tuile garde son pied sur une ligne. */
  protected prixSansDevise(prix: number | null | undefined): string {
    return formatNumber(prix);
  }

  protected enRupture(produit: ProduitSearch): boolean {
    return (produit.totalQuantity ?? 0) <= 0;
  }

  protected libelleAccessible(produit: ProduitSearch): string {
    const remise = this.prixRemise(produit);
    const prix =
      remise == null ? `${produit.regularUnitPrice} francs` : `en promotion à ${remise} francs au lieu de ${produit.regularUnitPrice}`;
    return `${produit.libelle}, ${prix}${this.enRupture(produit) ? ', en rupture' : ''}`;
  }
}

function lirePreference(): boolean {
  try {
    return localStorage.getItem(CLE_OUVERT) !== '0';
  } catch {
    return true;
  }
}
