import { Component, input, output, ChangeDetectionStrategy, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { Select } from 'primeng/select';
import { InputIcon } from 'primeng/inputicon';
import { IconField } from 'primeng/iconfield';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';

/**
 * Composant de présentation : Affichage liste des lignes de vente
 * 
 * Responsabilités :
 * - Afficher les lignes de vente dans un tableau
 * - Permettre édition quantité
 * - Permettre suppression d'une ligne
 * - Navigation clavier
 * 
 * Pas de logique métier - Composant pur (OnPush)
 */
@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  imports: [CommonModule, FormsModule, TranslateModule, TableModule, ButtonModule, InputTextModule, TooltipModule, Select, InputIcon, IconField, ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListComponent {
  // ViewChild
  private confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  
  // Inputs
  salesLines = input.required<ISalesLine[]>();
  isEditable = input(true);
  canEditPrice = input(false);
  selectedLineId = input<number | null>(null);
  saleId = input<number | undefined>(undefined);
  saleType = input<'COMPTANT' | 'ASSURANCE' | 'CARNET'>('COMPTANT');
  remises = input<IRemise[]>([]);
  currentRemise = input<IRemise | null>(null);
  showRemiseSection = input(true);

  // Outputs
  quantityChanged = output<{ line: ISalesLine; newQty: number }>();
  quantityRequestedChanged = output<{ line: ISalesLine; newQty: number }>();
  priceChanged = output<{ line: ISalesLine; newPrice: number }>();
  lineRemoved = output<ISalesLine>();
  lineSelected = output<ISalesLine>();
  discountChanged = output<{ line: ISalesLine; newDiscount: number }>();
  authorizationRequired = output<{ line: ISalesLine; action: 'delete' | 'discount' }>();
  remiseSelected = output<IRemise>();
  
  // Local state
  filterValue = signal('')
  selectedRemise = signal<IRemise | null>(null);

  // Méthodes pour les événements UI
  onQuantityRequestedChange(line: ISalesLine, newQty: string): void {
    const qty = Number(newQty);
    if (qty > 0) {
      this.quantityRequestedChanged.emit({ line, newQty: qty });
    }
  }

  onQuantitySoldChange(line: ISalesLine, newQty: string): void {
    const qty = Number(newQty);
    if (qty >= 0) {
      // Validation: quantitySold ne peut pas dépasser quantityRequested
      if (line.quantityRequested && qty > line.quantityRequested) {
        alert(`La quantité servie (${qty}) ne peut pas dépasser la quantité demandée (${line.quantityRequested})`);
        return;
      }
      this.quantityChanged.emit({ line, newQty: qty });
    }
  }

  onQuantityChange(line: ISalesLine, newQty: string): void {
    // Gardé pour compatibilité - redirige vers onQuantitySoldChange
    this.onQuantitySoldChange(line, newQty);
  }

  onPriceChange(line: ISalesLine, newPrice: string): void {
    const price = Number(newPrice);
    if (price > 0) {
      this.priceChanged.emit({ line, newPrice: price });
    }
  }

  onRemoveLine(line: ISalesLine): void {
    // Utiliser le modal de confirmation
    this.confirmDialog().onConfirm(
      () => this.authorizationRequired.emit({ line, action: 'delete' }),
      'Supprimer Produit',
      `Voulez-vous supprimer ${line.produitLibelle || 'ce produit'} ?`,
      undefined,
      () => {
        //TODO: Action on reject , le champ produitSearch reçoit le focus
      }
    );
  }

  onSelectLine(line: ISalesLine): void {
    this.lineSelected.emit(line);
  }

  onDiscountChange(line: ISalesLine, newDiscount: string): void {
    const discount = Number(newDiscount);
    if (discount >= 0) {
      this.discountChanged.emit({ line, newDiscount: discount });
    }
  }

  // Méthodes helper pour le template
  isLineSelected(line: ISalesLine): boolean {
    return this.selectedLineId() === line.id;
  }

  getLineTotal(line: ISalesLine): number {
    return (line.regularUnitPrice || 0) * (line.quantityRequested || 0) - (line.discountAmount || 0);
  }

  // Méthodes pour le footer
  getTotalQuantityRequested(): number {
    return this.salesLines().reduce((sum, line) => sum + (line.quantityRequested || 0), 0);
  }

  getTotalQuantitySold(): number {
    return this.salesLines().reduce((sum, line) => sum + (line.quantitySold || 0), 0);
  }

  getTotalAmount(): number {
    return this.salesLines().reduce((sum, line) => sum + this.getLineTotal(line), 0);
  }

  // Gestion remise
  onSelectRemise(): void {
    const remise = this.selectedRemise();
    if (remise) {
      this.remiseSelected.emit(remise);
    }
  }

  getRemiseTaux(): string {
    const remise = this.currentRemise();
    if (remise) {
      return remise.remiseValue + ' %';
    }
    return '';
  }

  // Filtrage
  onFilterChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterValue.set(value);
  }

  trackByLineId(_index: number, line: ISalesLine): number | undefined {
    return line.id;
  }
}
