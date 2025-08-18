import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './customer-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule]
})
export class CustomerDeleteDialogComponent {
  protected customerService = inject(CustomerService);
  activeModal = inject(NgbActiveModal);

  customer?: ICustomer;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
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
