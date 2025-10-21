import { Component, inject, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { SharedModule } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';
import {
  UninsuredCustomerFormComponent
} from '../../customer/uninsured-customer-form/uninsured-customer-form.component';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from '../selling-home/sale-helper';

@Component({
  selector: 'jhi-customer-data-table',
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
    InputIcon
  ],
  templateUrl: './customer-data-table.component.html',
  styleUrls: ['./customer-data-table.scss']
})
export class CustomerDataTableComponent {
  customers: ICustomer[] = [];
  searchString?: string | null = '';
  readonly closeModalEvent = output<boolean>();
  protected customerService = inject(CustomerService);
  private selectedCustomerService = inject(SelectedCustomerService);
  private readonly modalService = inject(NgbModal);

  protected onSelect(customer: ICustomer): void {
    this.selectedCustomerService.setCustomer(customer);
    this.closeModalEvent.emit(true);
  }

  protected loadCustomers(): void {
    this.customerService
      .queryUninsuredCustomers({
        search: this.searchString
      })
      .subscribe(res => (this.customers = res.body!));
  }

  protected addUninsuredCustomer(): void {
    this.closeModalEvent.emit(true);
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      { header: 'FORMULAIRE D\'AJOUT DE NOUVEAU DE CLIENT ', entity: null },
      (resp: ICustomer) => {
        if (resp) {
          this.selectedCustomerService.setCustomer(resp);
        }
      }
    );
  }
}
