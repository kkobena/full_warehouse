import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ILot } from 'app/shared/model/lot.model';
import { LotService } from '../../../../../entities/commande/lot/lot.service';
import { FormLotComponent } from '../form/form-lot.component';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from 'app/shared/error.service';
import { HttpErrorResponse } from '@angular/common/http';
import { showCommonModal } from '../../../../../entities/sales/selling-home/sale-helper';
import { Card } from 'primeng/card';
import { NotificationService } from 'app/shared/services/notification.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';

@Component({
  selector: 'jhi-list-lot',
  templateUrl: './list-lot.component.html',
  styleUrls: ['../../../../../entities/common-modal.component.scss', './list-lot.component.scss'],
  imports: [CommonModule, ButtonModule, TooltipModule, TableModule, Card],
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
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

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
    this.openConfirmDialog(lot);
  }

  add(): void {
    this.openLotForm(null, 'Ajout de lot');
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected onSaveError(err: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
  }

  private getLotQunatity(): number {
    return this.lots.reduce((first, second) => first + second.quantityReceived, 0);
  }

  private openConfirmDialog(lot: ILot): void {
    this.confirmDialog.onConfirm(
      () => {
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
      'Suppression',
      'Voullez-vous supprimer ce lot  ?',
    );
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
