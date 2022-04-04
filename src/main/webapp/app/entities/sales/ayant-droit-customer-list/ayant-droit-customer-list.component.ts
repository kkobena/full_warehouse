import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';

@Component({
  selector: 'jhi-ayant-droit-customer-list',
  templateUrl: './ayant-droit-customer-list.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class AyantDroitCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  searchString?: string | null = '';
  assureId?: number | null;

  constructor(public ref: DynamicDialogRef, public config: DynamicDialogConfig, protected customerService: CustomerService) {}

  ngOnInit(): void {
    this.customers = this.config.data.customers;
    this.assureId = this.config.data.assureId;
  }

  onDbleClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  onSelect(customer: ICustomer): void {
    this.ref.close(customer);
  }

  cancel(): void {
    this.ref.close();
  }

  loadCustomers(): void {
    this.customerService.queryAyantDroits(this.assureId!).subscribe(res => (this.customers = res.body!));
  }
}
