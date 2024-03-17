import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { AlertInfoComponent } from '../../../../shared/alert/alert-info.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';

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
    AlertInfoComponent,
    ConfirmDialogModule,
  ],
  templateUrl: './product-table.component.html',
  styleUrls: ['./product-table.component.scss'],
})
export class ProductTableComponent {
  @Input('salesLines') salesLines: ISalesLine[] = [];
  @Output() itemQtySoldEvent = new EventEmitter<ISalesLine>();
  @Output() itemPriceEvent = new EventEmitter<ISalesLine>();
  @Output() deleteItemEvent = new EventEmitter<ISalesLine>();
  @Output() itemQtyRequestedEvent = new EventEmitter<ISalesLine>();
  @Input('canUpdatePu') canUpdatePu: boolean = false;
  @Input('qtyMaxToSel') qtyMaxToSel: number;
  @ViewChild('forcerStockBtn')
  forcerStockBtn?: ElementRef;

  constructor(
    private modalService: NgbModal,
    private confirmationService: ConfirmationService,
  ) {}

  totalQtyProduit(): number {
    return this.salesLines.reduce((sum, current) => sum + current.quantityRequested, 0);
  }

  totalQtyServi(): number {
    return this.salesLines.reduce((sum, current) => sum + current.quantitySold, 0);
  }

  totalTtc(): number {
    return this.salesLines.reduce((sum, current) => sum + current.salesAmount, 0);
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
      if (newQty > this.qtyMaxToSel) {
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
    this.confirmationService.confirm({
      message: ' Voullez-vous detacher  ce produit ?',
      header: 'RUPPRESSION DE PRODUIT ',
      icon: 'pi pi-info-circle',
      accept: () => this.deleteItemEvent.emit(item),
      reject: () => {
        this.deleteItemEvent.emit(null);
      },
      key: 'deleteItem',
    });
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
    this.forcerStockBtn.nativeElement.focus();
  }
}
