import { Component, effect, ElementRef, EventEmitter, inject, Output, viewChild } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { AlertInfoComponent } from '../../../../shared/alert/alert-info.component';
import { NgbAlertModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { CurrentSaleService } from '../../service/current-sale.service';
import { ISales } from '../../../../shared/model/sales.model';
import { HasAuthorityService } from '../../service/has-authority.service';
import { BaseSaleService } from '../../service/base-sale.service';
import { Authority } from '../../../../shared/constants/authority.constants';
import { SplitButtonModule } from 'primeng/splitbutton';

export type RemiseSignal = 'add' | 'remove' | 'update';

@Component({
  selector: 'jhi-product-table',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    ConfirmDialogModule,
    SplitButtonModule,
    NgbAlertModule,
  ],
  templateUrl: './product-table.component.html',
  styleUrls: ['./product-table.component.scss'],
})
export class ProductTableComponent {
  sale: ISales;
  @Output() itemQtySoldEvent = new EventEmitter<ISalesLine>();
  @Output() itemPriceEvent = new EventEmitter<ISalesLine>();
  @Output() deleteItemEvent = new EventEmitter<ISalesLine>();
  @Output() itemQtyRequestedEvent = new EventEmitter<ISalesLine>();
  @Output() addRemiseEvent = new EventEmitter<RemiseSignal>();
  forcerStockBtn = viewChild<ElementRef>('forcerStockBtn');
  hasAuthorityService = inject(HasAuthorityService);
  canModifiePrice: boolean;
  baseSaleService = inject(BaseSaleService);
  currentSaleService = inject(CurrentSaleService);
  confirmationService = inject(ConfirmationService);
  modalService = inject(NgbModal);
  protected typeAlert = 'success';
  protected actionsButtons: MenuItem[];
  private canRemoveItem: boolean;

  constructor() {
    this.canModifiePrice = this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFIER_PRIX);
    this.canRemoveItem = this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE);
    this.actionsButtons = [
      {
        label: 'Modifier',
        icon: 'pi pi-pencil',
        command: () => this.onRemiseChange(),
      },
      {
        label: 'Supprimer',
        icon: 'pi pi-trash',
        command: () => this.onRemoveRemise(),
      },
    ];
    effect(() => {
      this.sale = this.currentSaleService.currentSale();
    });
  }

  totalQtyProduit(): number {
    return this.currentSaleService.currentSale()?.salesLines.reduce((sum, current) => sum + current.quantityRequested, 0);
  }

  totalQtyServi(): number {
    return this.currentSaleService.currentSale()?.salesLines.reduce((sum, current) => sum + current.quantitySold, 0);
  }

  totalTtc(): number {
    return this.currentSaleService.currentSale()?.salesLines.reduce((sum, current) => sum + current.salesAmount, 0);
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
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  updateItemQtyRequested(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);

    if (newQty > 0) {
      if (newQty > this.baseSaleService.quantityMax()) {
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
    if (this.canRemoveItem) {
      this.confirmationService.confirm({
        message: ' Voullez-vous supprimer  ce produit ?',
        header: 'SUPPRESSION DE PRODUIT ',
        icon: 'pi pi-info-circle',
        accept: () => this.deleteItemEvent.emit(item),
        reject: () => {
          this.deleteItemEvent.emit(null);
        },
        key: 'deleteItem',
      });
    } else {
      this.deleteItemEvent.emit(item);
    }
  }

  onRemiseChange(): void {
    this.addRemiseEvent.emit('update');
  }

  onAddRemise(): void {
    this.addRemiseEvent.emit('add');
  }

  onRemoveRemise(): void {
    this.addRemiseEvent.emit('remove');
  }

  private onUpdateConfirmForceStock(salesLine: ISalesLine, message: string): void {
    this.confirmationService.confirm({
      message,
      header: 'FORCER LE STOCK ',
      icon: 'pi pi-info-circle',
      accept: () => this.itemQtyRequestedEvent.emit(salesLine),
      reject: () => this.itemQtyRequestedEvent.emit(null),
      key: 'forcerStock',
    });
    this.forcerStockBtn().nativeElement.focus();
  }
}
