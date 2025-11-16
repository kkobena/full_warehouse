import { Component, computed, inject, input, output, Signal, viewChild, ViewEncapsulation } from '@angular/core';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { IRemise, Remise } from '../../../shared/model/remise.model';
import { RemiseCacheService } from '../../sales/service/remise-cache.service';
import { ISales } from '../../../shared/model/sales.model';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonError } from '../../sales/selling-home/sale-helper';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-vente-depot-table',
  imports: [
    Button,
    WarehouseCommonModule,
    ConfirmDialogComponent,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    Tooltip,
    FormsModule,
  ],
  templateUrl: './vente-depot-table.component.html',
  styleUrl: './vente-depot-table.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class VenteDepotTableComponent {
  readonly itemQtySoldEvent = output<ISalesLine>();
  readonly itemPriceEvent = output<ISalesLine>();
  readonly deleteItemEvent = output<ISalesLine>();
  readonly itemQtyRequestedEvent = output<ISalesLine>();
  readonly addRemiseEvent = output<Remise>();
  readonly onSave = output<boolean>();
  readonly quantityMax = input<number>(1000);
  readonly canApplyDiscount = input<boolean>();
  readonly canRemoveItem = input<boolean>();
  readonly canModifyPrice = input<boolean>();
  remiseCacheService = inject(RemiseCacheService);
  sale = input<ISales>();
  totalQtyProduit: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.quantityRequested, 0));
  totalQtyServi: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.quantitySold, 0));
  totalTtc: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.salesAmount, 0));
  remiseTaux: Signal<string> = computed(() => {
    const currentSale = this.sale();
    if (currentSale.remise) {
      const remise = currentSale.remise;
      if (remise.type === 'remiseProduit') {
        return this.getTaux(remise, currentSale.type);
      }
      return remise.remiseValue + ' %';
    }
    return '';
  });
  protected selectedRemise: any;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly modalService = inject(NgbModal);

  constructor() {}

  onSelectRemise(): void {
    this.addRemiseEvent.emit(this.selectedRemise);
  }

  updateItemQtySold(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);
    if (newQty >= 0) {
      if (newQty > salesLine.quantityRequested) {
        this.openInfoDialog(
          `La quantité saisie  ${newQty}  ne doit pas être supérieure à la quantité demandée ${salesLine.quantityRequested}`,
          'alert alert-danger',
        );
      } else {
        salesLine.quantitySold = newQty;
        this.itemQtySoldEvent.emit(salesLine);
      }
    }
  }

  openInfoDialog(message: string, infoClass: string): void {
    showCommonError(this.modalService, message, infoClass);
  }

  updateItemQtyRequested(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);

    if (newQty > 0) {
      if (newQty > this.quantityMax()) {
        this.onUpdateConfirmForceStock(salesLine, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
      } else {
        salesLine.quantityRequested = newQty;
        this.itemQtyRequestedEvent.emit(salesLine);
      }
    }
  }

  updateItemPrice(salesLine: ISalesLine, event: any): void {
    const newPrice = Number(event.target.value);
    if (newPrice > 0) {
      salesLine.regularUnitPrice = newPrice;
      this.itemPriceEvent.emit(salesLine);
    }
  }

  onDeleteItem(item: ISalesLine): void {
    if (this.canRemoveItem()) {
      this.confimDialog().onConfirm(
        () => this.deleteItemEvent.emit(item),
        'Supprimer Produit',
        ' Voullez-vous supprimer  ce produit ?',
        null,
        () => this.deleteItemEvent.emit(null),
      );
    } else {
      this.deleteItemEvent.emit(item);
    }
  }

  protected getTaux(entity: IRemise, type: string): string {
    const taut = entity.grilles.filter(grille => grille.grilleType === type)[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }

  protected save(): void {
    this.onSave.emit(true);
  }

  private onUpdateConfirmForceStock(salesLine: ISalesLine, message: string): void {
    this.confimDialog().onConfirm(
      () => this.itemQtyRequestedEvent.emit(salesLine),
      'Forcer le stock',
      message,
      null,
      () => this.itemQtyRequestedEvent.emit(null),
    );
  }
}
