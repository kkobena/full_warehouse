import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { Popover } from 'primeng/popover';
import { InputIcon } from 'primeng/inputicon';
import { IconField } from 'primeng/iconfield';
import { IRemise, ISalesLine } from '../../../../shared/model';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { MessageService } from 'primeng/api';

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
  imports: [
    CommonModule,
    FormsModule,
    TranslateModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    TooltipModule,
    Popover,
    InputIcon,
    IconField,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListComponent {

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private remisePopover = viewChild<Popover>('remisePopover');

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
  removeRemise = output<void>();
  remiseActionCancelled = output<void>();
  private readonly messageService = inject(MessageService);

  // Local state
  filterValue = signal('');
  selectedRemise = signal<IRemise | null>(null);
  isEditMode = signal(false);

  /** Remises disponibles pour le popover (exclut la remise courante en mode édition) */
  availableRemises = computed(() => {
    const all = this.remises();
    if (this.isEditMode()) {
      const currentId = this.currentRemise()?.id;
      return all.filter(r => r.id !== currentId);
    }
    return all;
  });

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
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `La quantité servie (${qty}) ne peut pas dépasser la quantité demandée (${line.quantityRequested})`,
          life: 5000,
        });
        return;
      }
      this.quantityChanged.emit({ line, newQty: qty });
    }
  }

  onPriceChange(line: ISalesLine, newPrice: string): void {
    const price = Number(newPrice);
    if (price > 0) {
      this.priceChanged.emit({ line, newPrice: price });
    }
  }

  onRemoveLine(line: ISalesLine): void {
    // Utiliser le modal de confirmation
    this.confirmDialog.onConfirm(
      () => this.authorizationRequired.emit({ line, action: 'delete' }),
      'Supprimer Produit',
      `Voulez-vous supprimer ${line.produitLibelle || 'ce produit'} ?`,
      undefined,
      () => {
        //TODO: Action on reject , le champ produitSearch reçoit le focus
      },
    );
  }

  onSelectLine(line: ISalesLine): void {
    this.lineSelected.emit(line);
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

  onRemoveRemise(): void {
    this.selectedRemise.set(null);
    this.removeRemise.emit();
  }

  /** Ouvre le popover en mode ajout */
  openAddRemisePopover(event: Event): void {
    this.isEditMode.set(false);
    this.remisePopover()?.toggle(event);
  }

  /** Ouvre le popover en mode édition (exclut la remise courante) */
  openEditRemisePopover(event: Event): void {
    this.isEditMode.set(true);
    this.remisePopover()?.toggle(event);
  }

  /** Sélection d'une remise depuis le popover */
  onPopoverRemiseSelect(remise: IRemise): void {
    this.remisePopover()?.hide();
    if (this.isEditMode()) {
      this.confirmDialog.onConfirm(
        () => this.remiseSelected.emit(remise),
        'Modifier la remise',
        `Voulez-vous remplacer la remise actuelle par "${remise.valeur}" ?`,
        undefined,
        () => this.remiseActionCancelled.emit(),
      );
    } else {
      this.remiseSelected.emit(remise);
    }
  }

  /** Taux affiché selon le type de vente */
  getRemiseRate(remise: IRemise): string {
    const rate = this.saleType() === 'COMPTANT' ? remise.vnoDiscountRate : remise.voDiscountRate;
    return rate != null ? rate + ' %' : '';
  }

  getRemiseTaux(): string {
    const remise = this.currentRemise();
    if (remise) {
      return this.getRemiseRate(remise);
    }
    return '';
  }

  // Filtrage
  onFilterChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterValue.set(value);
  }
}
