import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SaleType = 'COMPTANT' | 'ASSURANCE' | 'CARNET';

export interface SaleTypeOption {
  type: SaleType;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-sale-type-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sale-type-selector.component.html',
  styleUrl: './sale-type-selector.component.scss',
})
export class SaleTypeSelectorComponent {
  // Inputs
  selectedType = input<SaleType>('COMPTANT');

  // Outputs
  typeSelected = output<SaleType>();

  // Options
  readonly saleTypes: SaleTypeOption[] = [
    { type: 'COMPTANT', label: 'Comptant', icon: 'pi-wallet' },
    { type: 'ASSURANCE', label: 'Assurance', icon: 'pi-shield' },
    { type: 'CARNET', label: 'Carnet', icon: 'pi-book' },
  ];

  onTypeSelect(type: SaleType): void {
    if (type !== this.selectedType()) {
      this.typeSelected.emit(type);
    }
  }

  isSelected(type: SaleType): boolean {
    return this.selectedType() === type;
  }
}
