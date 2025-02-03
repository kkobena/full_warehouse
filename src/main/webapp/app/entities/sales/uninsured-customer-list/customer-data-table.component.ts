import { Component, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { ConfirmationService, SharedModule } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';
import { UninsuredCustomerFormComponent } from '../../customer/uninsured-customer-form/uninsured-customer-form.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-customer-data-table',
  providers: [ConfirmationService, DialogService],
  imports: [
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    RippleModule,
    SharedModule,
    TableModule,
    TooltipModule,
    WarehouseCommonModule,
    FormsModule,
    IconField,
    InputIcon,
  ],
  templateUrl: './customer-data-table.component.html',
})
export class CustomerDataTableComponent {
  customers: ICustomer[] = [];
  searchString?: string | null = '';
  readonly closeModalEvent = output<boolean>();
  ref!: DynamicDialogRef;

  constructor(
    protected customerService: CustomerService,
    private dialogService: DialogService,
    private selectedCustomerService: SelectedCustomerService,
  ) {}

  onSelect(customer: ICustomer): void {
    this.selectedCustomerService.setCustomer(customer);
    this.closeModalEvent.emit(true);
  }

  loadCustomers(): void {
    this.customerService
      .queryUninsuredCustomers({
        search: this.searchString,
      })
      .subscribe(res => (this.customers = res.body!));
  }

  addUninsuredCustomer(): void {
    this.closeModalEvent.emit(true);
    this.ref = this.dialogService.open(UninsuredCustomerFormComponent, {
      data: { entity: null },
      header: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT ",
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      this.selectedCustomerService.setCustomer(resp);
    });
  }
}
