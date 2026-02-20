import { ChangeDetectionStrategy, Component, computed, input, output, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductSearchComponent } from '../product-search/product-search.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { ProduitSearch } from '../../../../shared/model';

@Component({
  selector: 'app-product-search-section',
  templateUrl: './product-search-section.component.html',
  styleUrls: ['./product-search-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ProductSearchComponent, QuantiteProdutSaisieComponent],
})
export class ProductSearchSectionComponent {
  autofocus = input<boolean>(false);
  selectedProduct = input<ProduitSearch | null>(null);
  requiresCustomer = input<boolean>(false);
  hasCustomer = input<boolean>(true);
  showStock = input<boolean>(false);
  saleType = input<'COMPTANT' | 'CARNET' | 'ASSURANCE'>('COMPTANT');

  productSelected = output<ProduitSearch | null>();
  productScanned = output<ProduitSearch>();
  onKeyEnter = output<boolean>();
  addQuantite = output<number>();

  private produitboxRef = viewChild<ProductSearchComponent>('produitbox');
  private quantityBoxRef = viewChild<QuantiteProdutSaisieComponent>('quantityBox');

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

  focusProduitControl(): void {
    this.quantityBoxRef()?.focusProduitControl();
  }

  resetQuantity(qty: number): void {
    this.quantityBoxRef()?.reset(qty);
  }
}
