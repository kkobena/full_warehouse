import { ChangeDetectionStrategy, Component, computed, input, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductSearchComponent } from '../product-search/product-search.component';
import { ButtonComponent, InputNumberComponent } from '../../../../shared/ui';
import { ProduitSearch } from '../../../../shared/model';
import { DevisePipe } from 'app/shared/utils/devise';

@Component({
  selector: 'app-product-search-section',
  templateUrl: './product-search-section.component.html',
  styleUrls: ['./product-search-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, ProductSearchComponent, InputNumberComponent, ButtonComponent, DevisePipe],
})
export class ProductSearchSectionComponent {
  autofocus = input<boolean>(false);
  /** Colle la section à son parent par le haut (le mixin de style lui laisse 0,75 rem de marge). */
  sansMargeHaute = input<boolean>(false);
  selectedProduct = input<ProduitSearch | null>(null);
  requiresCustomer = input<boolean>(false);
  hasCustomer = input<boolean>(true);
  showStock = input<boolean>(false);
  saleType = input<'COMPTANT' | 'CARNET' | 'ASSURANCE'>('COMPTANT');
  /** Message affiché quand un client doit d'abord être choisi (ventes assurance et carnet). */
  customerRequiredMessage = input<string>("Sélectionnez un client avant d'ajouter un produit");

  productSelected = output<ProduitSearch | null>();
  productScanned = output<ProduitSearch>();
  onKeyEnter = output<boolean>();
  addQuantite = output<number>();

  private produitboxRef = viewChild<ProductSearchComponent>('produitbox');
  private quantityBoxRef = viewChild<InputNumberComponent>('quantityBox');

  protected readonly quantite = signal<number | null>(null);

  /** Anticipe la rupture : la quantité tapée dépasse le stock rayon, avant l'ajout et non dans le panier. */
  protected readonly depasseStock = computed(() => {
    const stock = this.selectedProduct()?.totalQuantity ?? null;
    return stock != null && Number(this.quantite() ?? 0) > stock;
  });

  /**
   * Un client est requis et manque : la recherche est grisée AVANT la frappe, avec la raison affichée,
   * plutôt qu'un toast quand le produit est déjà saisi.
   */
  protected rechercheBloquee = computed(() => this.requiresCustomer() && !this.hasCustomer());

  protected quantityIsValid = computed(() => {
    if (this.requiresCustomer()) {
      return !!this.selectedProduct() && this.hasCustomer();
    }
    return !!this.selectedProduct();
  });

  protected quantityIsDisabled = computed(() => {
    if (this.requiresCustomer()) {
      return !this.selectedProduct() || !this.hasCustomer();
    }
    return false;
  });

  getFocus(): void {
    this.produitboxRef()?.getFocus();
  }

  reset(): void {
    this.produitboxRef()?.reset();
  }

  afficherProduit(produit: ProduitSearch): void {
    this.produitboxRef()?.afficher(produit);
  }

  focusProduitControl(): void {
    setTimeout(() => {
      this.quantityBoxRef()?.focus();
      this.quantityBoxRef()?.select();
    }, 50);
  }

  resetQuantity(qty: number): void {
    this.quantite.set(qty || null);
  }

  protected ajouterQuantite(): void {
    const valeur = this.quantite();
    if (this.quantityIsValid() && valeur != null) {
      this.addQuantite.emit(valeur);
    }
  }
}
