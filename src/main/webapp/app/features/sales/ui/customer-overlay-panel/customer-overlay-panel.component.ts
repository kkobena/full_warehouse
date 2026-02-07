import { Component, computed, inject, input, output, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { CustomerDataTableComponent } from '../../../../entities/sales/uninsured-customer-list/customer-data-table.component';
import { ICustomer } from '../../../../shared/model';
import { PopoverModule, Popover } from 'primeng/popover';
import { SalesFacade } from '../../data-access/facades/sales.facade';

/**
 * Customer Overlay Panel Component
 *
 * Composant de présentation pour afficher et sélectionner un client.
 * Utilise un popover pour afficher la liste des clients.
 *
 * Mode Store (par défaut): utilise SalesFacade pour la gestion du client
 * Mode Input/Output: utilise les inputs/outputs pour une utilisation autonome
 */
@Component({
  selector: 'jhi-customer-overlay-panel',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    FormsModule,
    PopoverModule,
    TooltipModule,
    CustomerDataTableComponent,
  ],
  templateUrl: './customer-overlay-panel.component.html',
  styleUrls: ['./customer-overlay-panel.component.scss'],
})
export class CustomerOverlayPanelComponent {
  private readonly facade = inject(SalesFacade);

  // Inputs
  /** Client passé en input (mode autonome) - prioritaire sur le store */
  readonly customerInput = input<ICustomer | null>(null, { alias: 'customer' });

  /** Utiliser le store (true) ou les inputs (false) */
  readonly useStore = input<boolean>(true);

  // Outputs
  readonly customerSelected = output<ICustomer>();
  readonly customerRemoved = output<void>();
  readonly closeEvent = output<boolean>({ alias: 'onCloseEvent' });

  // ViewChild
  readonly popover = viewChild<Popover>('op');

  // Computed - utilise soit l'input soit le store selon le mode
  protected readonly customer = computed(() => {
    if (!this.useStore()) {
      return this.customerInput();
    }
    return this.facade.selectedCustomer();
  });

  protected getLabel(): string {
    return 'Choisir client';
  }

  protected onClose(): void {
    const pop = this.popover();
    if (pop) {
      pop.hide();
    }
    this.closeEvent.emit(true);
  }

  protected onCustomerSelected(customer: ICustomer): void {
    if (this.useStore()) {
      this.facade.setCustomer(customer);
    }
    this.customerSelected.emit(customer);
    this.onClose();
  }

  protected remove(): void {
    if (this.useStore()) {
      this.facade.removeCustomer();
    }
    this.customerRemoved.emit();
  }

  protected togglePopover(event: Event): void {
    const pop = this.popover();
    if (pop) {
      pop.toggle(event);
    }
  }
}
