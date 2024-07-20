import { Component, effect, EventEmitter, Output } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { CustomerDataTableComponent } from '../uninsured-customer-list/customer-data-table.component';
import { ICustomer } from '../../../shared/model/customer.model';
import { SelectedCustomerService } from '../service/selected-customer.service';

@Component({
  selector: 'jhi-customer-overlay-panel',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    RippleModule,
    TooltipModule,
    FormsModule,
    OverlayPanelModule,
    CustomerDataTableComponent,
  ],
  providers: [],
  templateUrl: './customer-overlay-panel.component.html',
})
export class CustomerOverlayPanelComponent {
  @Output() onCloseEvent = new EventEmitter<boolean>();
  protected customer: ICustomer | null;

  constructor(private selectedCustomerService: SelectedCustomerService) {
    effect(() => {
      this.customer = this.selectedCustomerService.selectedCustomerSignal();
    });
  }

  protected getLabele(): string {
    //  if (this.customer) return 'Modifier client';
    return 'Choisir client';
  }

  protected onClose(op: OverlayPanel): void {
    //   this.customer = cust;
    this.onCloseEvent.emit(true);

    op.hide();
  }

  protected remove(): void {
    this.customer = null;
    this.selectedCustomerService.setCustomer(null);
  }
}
