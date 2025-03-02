import { Component, inject } from '@angular/core';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
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
  providers: [DialogService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    TableModule,
    CustomerDataTableComponent,
  ],
})
export class UninsuredCustomerListComponent {
  ref = inject(DynamicDialogRef);

  onSelectClose(event: any): void {
    this.ref.close(event);
  }

  cancel(): void {
    this.ref.destroy();
  }
}
