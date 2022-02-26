import { Component, OnInit } from '@angular/core';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';

@Component({
  selector: 'jhi-uninsured-customer-list',
  templateUrl: './uninsured-customer-list.component.html',
  styleUrls: ['./uninsured-customer-list.component.scss'],
  providers: [MessageService, DialogService, ConfirmationService],
})
export class UninsuredCustomerListComponent implements OnInit {
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
      .queryUninsuredCustomers({
        search: this.searchString,
      })
      .subscribe(res => (this.customers = res.body!));
  }
}
