import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {IPayment} from 'app/shared/model/payment.model';
import {PaymentService} from './payment.service';

@Component({
  templateUrl: './payment-delete-dialog.component.html',
})
export class PaymentDeleteDialogComponent {
  payment?: IPayment;

  constructor(protected paymentService: PaymentService, public activeModal: NgbActiveModal) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.paymentService.delete(id).subscribe(() => {

      this.activeModal.close();
    });
  }
}
