import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { ISalesLine } from '../../../../shared/model/sales-line.model';

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
  imports: [CommonModule, FormsModule, TranslateModule, TableModule, ButtonModule, InputTextModule, TooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListComponent {
  // Inputs
  salesLines = input.required<ISalesLine[]>();
  isEditable = input(true);
  selectedLineId = input<number | null>(null);
  saleId = input<number | undefined>(undefined);
  saleType = input<'COMPTANT' | 'ASSURANCE' | 'CARNET'>('COMPTANT');

  // Outputs
  quantityChanged = output<{ line: ISalesLine; newQty: number }>();
  quantityRequestedChanged = output<{ line: ISalesLine; newQty: number }>();
  lineRemoved = output<ISalesLine>();
  lineSelected = output<ISalesLine>();
  discountChanged = output<{ line: ISalesLine; newDiscount: number }>();
  authorizationRequired = output<{ line: ISalesLine; action: 'delete' | 'discount' }>();

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

  onRemoveLine(line: ISalesLine): void {
    // Demander confirmation simple d'abord
    const confirmed = confirm(`Supprimer ${line.produitLibelle || 'ce produit'} ?`);
    if (confirmed) {
      // Émettre l'événement qui déclenchera la demande d'autorisation dans le parent
      this.authorizationRequired.emit({ line, action: 'delete' });
    }
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
    return (line.regularUnitPrice || 0) * (line.quantitySold || 0) - (line.discountAmount || 0);
  }

  trackByLineId(_index: number, line: ISalesLine): number | undefined {
    return line.id;
  }
}
