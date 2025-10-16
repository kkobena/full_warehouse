import { Component, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { ILot } from '../../../../shared/model/lot.model';
import { LotService } from '../lot.service';
import { FormLotComponent } from '../form-lot.component';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { IOrderLine } from '../../../../shared/model/order-line.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToastAlertComponent } from '../../../../shared/toast-alert/toast-alert.component';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../../shared/error.service';
import { HttpErrorResponse } from '@angular/common/http';
import { showCommonModal } from '../../../sales/selling-home/sale-helper';
import { Card } from 'primeng/card';
import { ConfirmationService } from 'primeng/api';
import { acceptButtonProps, rejectButtonProps } from '../../../../shared/util/modal-button-props';
import { ConfirmDialog } from 'primeng/confirmdialog';

@Component({
  selector: 'jhi-list-lot',
  templateUrl: './list-lot.component.html',
  styleUrls: ['../../../common-modal.component.scss', './list-lot.component.scss'],
  providers: [ConfirmationService],

  imports: [WarehouseCommonModule, ButtonModule, TooltipModule, TableModule, ToastAlertComponent, Card, ConfirmDialog],
})
export class ListLotComponent implements OnInit, OnDestroy {
  lots: ILot[] = [];
  header = 'Liste des lots';
  selectedEl!: ILot;
  deliveryItem?: IOrderLine;
  commandeId?: number;
  showUgAddNewBtn = true;
  private readonly entityService = inject(LotService);
  private destroy$ = new Subject<void>();
  private readonly modalService = inject(NgbModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly confirmationService = inject(ConfirmationService);
  ngOnInit(): void {
    this.lots = this.deliveryItem.lots;
    this.showAddBtn();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  showAddBtn(): void {
    this.showUgAddNewBtn = this.getLotQunatity() < (this.deliveryItem.quantityReceived || this.deliveryItem.quantityRequested);
  }

  edit(lot: ILot): void {
    this.openLotForm(lot, 'Modification du lot ' + lot.numLot);
  }

  delete(lot: ILot): void {
    this.confirmDialog(lot);
  }

  add(): void {
    this.openLotForm(null, 'Ajout de lot');
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected onSaveError(err: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(err));
  }

  private getLotQunatity(): number {
    return this.lots.reduce((first, second) => first + second.quantityReceived, 0);
  }

  private confirmDialog(lot: ILot): void {
    this.confirmationService.confirm({
      message: 'Voullez-vous supprimer ce lot  ?',
      header: 'Suppression',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.entityService
          .remove(lot.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
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

  private openLotForm(entity: ILot | null, header: string): void {
    showCommonModal(
      this.modalService,
      FormLotComponent,
      {
        entity,
        deliveryItem: this.deliveryItem,
        header,
        commandeId: this.commandeId,
      },
      updateLot => {
        if (updateLot) {
          if (entity) {
            this.removeLotFromLotsArray(entity);
          }
          this.lots.push(updateLot);
          this.deliveryItem.lots = this.lots;
          this.showAddBtn();
        }
      },
      'lg',
    );
  }
}
