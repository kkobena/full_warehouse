import { Component, OnInit } from '@angular/core';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-uninsured-customer-list',
  templateUrl: './uninsured-customer-list.component.html',
  providers: [MessageService, DialogService, ConfirmationService, DynamicDialogRef, DynamicDialogConfig],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    TableModule,
  ],
})
export class UninsuredCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  searchString?: string | null = '';

  constructor(
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected customerService: CustomerService,
  ) {}

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
    console.log('sqdsqdqdqsd', this.ref);
    this.ref.destroy();
  }

  loadCustomers(): void {
    this.customerService
      .queryUninsuredCustomers({
        search: this.searchString,
      })
      .subscribe(res => (this.customers = res.body!));
  }
}
