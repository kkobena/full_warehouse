import { Component, effect, output } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { CustomerDataTableComponent } from '../uninsured-customer-list/customer-data-table.component';
import { ICustomer } from '../../../shared/model/customer.model';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { TagModule } from 'primeng/tag';
import { PopoverModule } from 'primeng/popover';

@Component({
  selector: 'jhi-customer-overlay-panel',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    RippleModule,
    TooltipModule,
    FormsModule,
    PopoverModule,
    CustomerDataTableComponent,
    TagModule,
  ],
  providers: [],
  templateUrl: './customer-overlay-panel.component.html',
})
export class CustomerOverlayPanelComponent {
  readonly onCloseEvent = output<boolean>();
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

  protected onClose(op: any): void {
    //   this.customer = cust;
    this.onCloseEvent.emit(true);

    op.hide();
  }

  protected remove(): void {
    this.customer = null;
    this.selectedCustomerService.setCustomer(null);
  }
}
