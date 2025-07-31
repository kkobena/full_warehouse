import { Component, inject, OnInit } from '@angular/core';

import { DynamicDialogModule } from 'primeng/dynamicdialog';
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
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

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
  ],
})
export class AyantDroitCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  assure?: ICustomer | null;
  header: string;
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    this.loadCustomers();
  }

  onDbleClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  loadCustomers(): void {
    this.customerService.queryAyantDroits(this.assure.id).subscribe(res => (this.customers = res.body!));
  }

  addAyantDroit(): void {
    this.activeModal.close(new Customer());
  }
}
