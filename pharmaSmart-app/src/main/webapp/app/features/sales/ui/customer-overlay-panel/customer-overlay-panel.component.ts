import { Component, computed, inject, input, output, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { CustomerSearchTableComponent } from '../customer-search-table/customer-search-table.component';
import { ICustomer } from '../../../../shared/model';
import { PopoverModule, Popover } from 'primeng/popover';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { CommonModule } from "@angular/common";

/**
 * Customer Overlay Panel Component
 *
 * Composant de présentation pour afficher et sélectionner un client.
 * Utilise un popover pour afficher la liste des clients.
 *
 * - Sans client: bouton "+" ouvre le popover pour sélectionner
 * - Avec client: affiche les infos, bouton "edit" ouvre le popover pour remplacer,
 *   bouton "x" supprime le client
 */
@Component({
  selector: 'app-customer-overlay-panel',
  imports: [CommonModule, ButtonModule, FormsModule, PopoverModule, TooltipModule, CustomerSearchTableComponent],
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
    return 'Choisir un client';
  }

  protected onClose(): void {
    const pop = this.popover();
    if (pop) {
      pop.hide();
    }
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
