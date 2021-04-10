import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { PaymentFournisseurService } from './payment-fournisseur.service';

@Component({
  templateUrl: './payment-fournisseur-delete-dialog.component.html',
})
export class PaymentFournisseurDeleteDialogComponent {
  paymentFournisseur?: IPaymentFournisseur;

  constructor(
    protected paymentFournisseurService: PaymentFournisseurService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.paymentFournisseurService.delete(id).subscribe(() => {
      this.eventManager.broadcast('paymentFournisseurListModification');
      this.activeModal.close();
    });
  }
}
