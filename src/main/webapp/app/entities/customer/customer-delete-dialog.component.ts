import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {ICustomer} from 'app/shared/model/customer.model';
import {CustomerService} from './customer.service';

@Component({
  templateUrl: './customer-delete-dialog.component.html',
})
export class CustomerDeleteDialogComponent {
  customer?: ICustomer;

  constructor(protected customerService: CustomerService, public activeModal: NgbActiveModal) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.customerService.delete(id).subscribe(() => {

      this.activeModal.close();
    });
  }
}
