import { Component, OnInit, inject } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ILot } from '../../../../shared/model/lot.model';
import { LotService } from '../lot.service';
import { FormLotComponent } from '../form-lot.component';
import { IDeliveryItem } from '../../../../shared/model/delivery-item';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { acceptButtonProps, rejectButtonProps } from '../../../../shared/util/modal-button-props';
import { IOrderLine } from '../../../../shared/model/order-line.model';

@Component({
  selector: 'jhi-list-lot',
  templateUrl: './list-lot.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TooltipModule,
    ToastModule,
    TableModule,
    RippleModule,
    DynamicDialogModule,
    ConfirmDialogModule,
  ],
})
export class ListLotComponent implements OnInit {
  lots: ILot[] = [];
  selectedEl!: ILot;
  deliveryItem?: IOrderLine;
  commandeId?: number;
  showUgAddNewBtn = true;
  ref = inject(DynamicDialogRef);
  ref2 = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private readonly entityService = inject(LotService);
  private readonly messageService = inject(MessageService);
  private readonly dialogService = inject(DialogService);
  private readonly modalService = inject(ConfirmationService);

  ngOnInit(): void {
    this.deliveryItem = this.config.data.deliveryItem;
    this.lots = this.deliveryItem.lots;
    this.commandeId = this.config.data.commandeId;
    this.showAddBtn();
  }

  showAddBtn(): void {
    const quantityReceived = this.deliveryItem.quantityReceived || this.deliveryItem.quantityRequested;
    this.showUgAddNewBtn = this.getLotQunatity() < quantityReceived;
  }

  edit(lot: ILot): void {
    this.ref = this.dialogService.open(FormLotComponent, {
      data: { entity: lot, deliveryItem: this.deliveryItem, commandeId: this.commandeId },
      width: '40%',
      header: 'Modification du lot ' + lot.numLot,
    });
    this.ref.onClose.subscribe((updateLot: ILot) => {
      if (updateLot) {
        this.removeLotFromLotsArray(lot);
        this.lots.push(updateLot);
        this.deliveryItem.lots = this.lots;
        this.showAddBtn();
      }
    });
  }

  delete(lot: ILot): void {
    this.confirmDialog(lot);
  }

  add(): void {
    this.ref = this.dialogService.open(FormLotComponent, {
      data: { entity: null, deliveryItem: this.deliveryItem, commandeId: this.commandeId },
      width: '40%',
      header: 'Ajout de lot',
    });
    this.ref.onClose.subscribe((entity: ILot) => {
      if (entity) {
        this.lots.push(entity);
        this.deliveryItem.lots = this.lots;
        this.showAddBtn();
      }
    });
  }

  cancel(): void {
    this.ref2.destroy();
  }

  protected onSaveError(err: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err,
    });
  }

  private getLotQunatity(): number {
    return this.lots.reduce((first, second) => first + second.quantityReceived, 0);
  }

  private confirmDialog(lot: ILot): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer ce lot ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.entityService.remove(lot.id).subscribe({
          next: () => {
            this.removeLotFromLotsArray(lot);
          },
          error: err => this.onSaveError(err),
        });
      },
    });
  }

  private removeLotFromLotsArray(lot: ILot): void {
    const newLots = this.lots.filter(e => e.numLot !== lot.numLot);
    this.lots = newLots;
    this.deliveryItem.lots = newLots;
    this.showAddBtn();
  }
}
