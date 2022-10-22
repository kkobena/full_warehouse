import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';

@Component({
  selector: 'jhi-assured-customer-list',
  templateUrl: './assured-customer-list.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class AssuredCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  searchString?: string | null = '';

  constructor(public ref: DynamicDialogRef, public config: DynamicDialogConfig, protected customerService: CustomerService) {}

  ngOnInit(): void {
    this.customers = this.config.data.customers;
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
    this.customerService
      .queryAssuredCustomer({
        search: this.searchString,
      })
      .subscribe(res => (this.customers = res.body!));
  }
}
