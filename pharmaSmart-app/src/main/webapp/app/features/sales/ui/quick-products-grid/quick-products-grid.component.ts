import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { DevisePipe } from 'app/shared/utils/devise';
import { ProduitSearch } from '../../../../shared/model';
import { FavorisComptoirService } from '../../data-access/services/favoris-comptoir.service';

const CLE_OUVERT = 'pharmasmart_favoris_ouverts';

/** Rangée d'une tuile par produit favori : un clic ajoute le produit au panier, sans recherche ni quantité. */
@Component({
  selector: 'app-quick-products-grid',
  templateUrl: './quick-products-grid.component.html',
  styleUrl: './quick-products-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DevisePipe],
})
export class QuickProductsGridComponent implements OnInit {
  /** Vente en cours d'enregistrement : les tuiles ne répondent plus. */
  readonly disabled = input<boolean>(false);
  /** Le produit cliqué, au format de la recherche : l'écran de vente l'ajoute comme un produit scanné (quantité 1). */
  readonly productChosen = output<ProduitSearch>();

  private readonly service = inject(FavorisComptoirService);
  protected readonly favoris = this.service.favoris;
  protected readonly ouvert = signal<boolean>(lirePreference());

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.recharger();
  }

  /** Relit la grille : le stock de chaque tuile bouge à chaque vente. */
  recharger(): void {
    this.service.charger();
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
    timer(1500).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.recharger());
  }

  protected enRupture(produit: ProduitSearch): boolean {
    return (produit.totalQuantity ?? 0) <= 0;
  }

  protected libelleAccessible(produit: ProduitSearch): string {
    return `${produit.libelle}, ${produit.regularUnitPrice} francs${this.enRupture(produit) ? ', en rupture' : ''}`;
  }
}

function lirePreference(): boolean {
  try {
    return localStorage.getItem(CLE_OUVERT) !== '0';
  } catch {
    return true;
  }
}
