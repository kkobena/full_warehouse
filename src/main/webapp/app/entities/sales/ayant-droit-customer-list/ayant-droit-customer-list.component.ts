import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

@Component({
    selector: 'jhi-ayant-droit-customer-list',
    templateUrl: './ayant-droit-customer-list.component.html',
    imports: [
        WarehouseCommonModule,
        FormsModule,
        TooltipModule,
        ButtonModule,
        InputTextModule,
        RippleModule,
        DynamicDialogModule,
        TableModule,
        ToolbarModule,
    ]
})
export class AyantDroitCustomerListComponent implements OnInit {
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected customerService = inject(CustomerService);

  customers: ICustomer[] = [];
  assure?: ICustomer | null;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

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
    this.customerService.queryAyantDroits(this.assure.id).subscribe(res => (this.customers = res.body!));
  }

  addAyantDroit(): void {
    this.ref.close(new Customer());
  }
}
