import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IPaymentMode } from 'app/shared/model/payment-mode.model';
import { PaymentModeService } from './payment-mode.service';

@Component({
  templateUrl: './payment-mode-delete-dialog.component.html',
})
export class PaymentModeDeleteDialogComponent {
  paymentMode?: IPaymentMode;

  constructor(
    protected paymentModeService: PaymentModeService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.paymentModeService.delete(id).subscribe(() => {
      this.eventManager.broadcast('paymentModeListModification');
      this.activeModal.close();
    });
  }
}
