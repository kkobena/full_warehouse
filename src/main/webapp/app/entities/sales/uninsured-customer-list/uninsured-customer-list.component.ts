import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { CustomerDataTableComponent } from './customer-data-table.component';

@Component({
  selector: 'jhi-uninsured-customer-list',
  templateUrl: './uninsured-customer-list.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    CustomerDataTableComponent,
  ],
})
export class UninsuredCustomerListComponent {
  header: string = null;
  private readonly activeModal = inject(NgbActiveModal);

  onSelectClose(event: any): void {
    this.activeModal.close(event);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
