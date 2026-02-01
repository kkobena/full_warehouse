import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ICustomer } from '../../../../shared/model/customer.model';

/**
 * Composant de présentation : Sélection/affichage client
 * 
 * Responsabilités :
 * - Afficher le client sélectionné
 * - Permettre recherche client
 * - Permettre ajout nouveau client
 * - Permettre suppression client (vente comptant)
 * 
 * Composant pur - Pas de logique métier (OnPush)
 */
@Component({
  selector: 'app-customer-selector',
  templateUrl: './customer-selector.component.html',
  imports: [CommonModule, FormsModule, AutoCompleteModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerSelectorComponent {
  // Inputs
  selectedCustomer = input<ICustomer | null>(null);
  customers = input<ICustomer[]>([]);
  required = input(false);
  canRemove = input(true);
  placeholder = input('Rechercher un client...');
  minSearchLength = input(3);

  // Outputs
  customerSelected = output<ICustomer>();
  customerRemoved = output<void>();
  customerAdd = output<void>();
  searchChanged = output<string>();

  // Méthodes pour les événements UI
  onCustomerSelect(customer: ICustomer): void {
    this.customerSelected.emit(customer);
  }

  onRemoveCustomer(): void {
    this.customerRemoved.emit();
  }

  onAddCustomer(): void {
    this.customerAdd.emit();
  }

  onSearchChange(searchTerm: string): void {
    if (searchTerm.length >= this.minSearchLength()) {
      this.searchChanged.emit(searchTerm);
    }
  }

  // Helper methods
  hasCustomer(): boolean {
    return this.selectedCustomer() !== null;
  }

  getCustomerDisplay(customer: ICustomer | null): string {
    if (!customer) return '';
    const parts = [];
    if (customer.firstName) parts.push(customer.firstName);
    if (customer.lastName) parts.push(customer.lastName);
    return parts.join(' ') || customer.code || '';
  }

  getCustomerInfo(customer: ICustomer | null): string {
    if (!customer) return '';
    const parts = [];
    if (customer.code) parts.push(`Code: ${customer.code}`);
    if (customer.phone) parts.push(`Tél: ${customer.phone}`);
    return parts.join(' | ');
  }

  trackByCustomerId(_index: number, customer: ICustomer): number | undefined {
    return customer.id;
  }
}
