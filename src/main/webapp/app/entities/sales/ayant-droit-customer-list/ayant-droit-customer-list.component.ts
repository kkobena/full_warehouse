import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';

@Component({
  selector: 'jhi-ayant-droit-customer-list',
  templateUrl: './ayant-droit-customer-list.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class AyantDroitCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  searchString?: string | null = '';
  assure?: ICustomer | null;

  constructor(
    public ref: DynamicDialogRef,
    public ref2: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private dialogService: DialogService,
    protected customerService: CustomerService
  ) {}

  ngOnInit(): void {
    this.assure = this.config.data.assure;
    this.loadCustomers();
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
    this.customerService.queryAyantDroits(this.assure?.id!).subscribe(res => (this.customers = res.body!));
  }

  addAyantDroit(): void {
    this.ref.close(new Customer());
  }
}
