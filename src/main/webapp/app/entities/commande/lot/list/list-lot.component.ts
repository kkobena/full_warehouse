import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ILot } from '../../../../shared/model/lot.model';
import { IOrderLine } from '../../../../shared/model/order-line.model';
import { LotService } from '../lot.service';
import { FormLotComponent } from '../form-lot.component';

@Component({
  selector: 'jhi-form-lot',
  templateUrl: './list-lot.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class ListLotComponent implements OnInit {
  isSaving = false;
  lots: ILot[] = [];
  selectedEl!: ILot;
  orderLine?: IOrderLine;
  commandeId?: number;

  showUgAddNewBtn = true;

  constructor(
    protected entityService: LotService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private messageService: MessageService,
    private dialogService: DialogService,
    private modalService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.orderLine = this.config.data.orderLine;
    this.lots = this.orderLine.lots;
    this.commandeId = this.config.data.commandeId;
    this.showAddBtn();
  }

  showAddBtn(): void {
    const quantityReceived = this.orderLine.quantityReceived || this.orderLine.quantityRequested;
    this.showUgAddNewBtn = this.getLotQunatity() < quantityReceived;
  }

  edit(lot: ILot): void {
    this.ref = this.dialogService.open(FormLotComponent, {
      data: { entity: lot, orderLine: this.orderLine, commandeId: this.commandeId },
      width: '40%',
      header: 'Modification du lot ' + lot.numLot,
    });
    this.ref.onClose.subscribe((updateLot: ILot) => {
      if (updateLot) {
        this.removeLotFromLotsArray(lot);
        this.lots.push(updateLot);
        this.orderLine.lots = this.lots;
        this.showAddBtn();
      }
    });
  }

  delete(lot: ILot): void {
    this.confirmDialog(lot);
  }

  add(): void {
    this.ref = this.dialogService.open(FormLotComponent, {
      data: { entity: null, orderLine: this.orderLine, commandeId: this.commandeId },
      width: '40%',
      header: 'Ajout de lot',
    });
    this.ref.onClose.subscribe((entity: ILot) => {
      if (entity) {
        this.lots.push(entity);
        this.orderLine.lots = this.lots;
        this.showAddBtn();
      }
    });
  }

  protected onSaveError(err: any): void {
    this.isSaving = false;
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
      accept: () => {
        this.entityService.delete(lot).subscribe({
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
    this.orderLine.lots = newLots;
  }
}
