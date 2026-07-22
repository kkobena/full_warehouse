import { Component, computed, inject, input, output, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbPopover, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../../../../shared/ui';
import { CustomerSearchTableComponent } from '../customer-search-table/customer-search-table.component';
import { ICustomer } from '../../../../shared/model';
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
  imports: [CommonModule, ButtonComponent, FormsModule, NgbPopover, NgbTooltip, CustomerSearchTableComponent],
  templateUrl: './customer-overlay-panel.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  readonly editPopover = viewChild<NgbPopover>('editPopover');
  readonly addPopover = viewChild<NgbPopover>('addPopover');

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
    this.editPopover()?.close();
    this.addPopover()?.close();
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
}
