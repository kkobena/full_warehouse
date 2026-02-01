import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { ButtonModule } from 'primeng/button';
import { IRemise } from '../../../../shared/model/remise.model';

export interface ThirdPartyAmount {
  id?: number;
  name: string;
  amount: number;
}

/**
 * Composant de présentation : Résumé des montants de la vente
 * 
 * Responsabilités :
 * - Afficher le total HT
 * - Afficher les remises
 * - Afficher la TVA
 * - Afficher le net à payer
 * - Afficher le montant donné et la monnaie
 * - Afficher la part assurance/tiers payants (ASSURANCE/CARNET)
 * - Afficher la dernière monnaie donnée
 * - Émettre événement pour ajouter/supprimer remise
 * 
 * Composant pur - Affichage uniquement (OnPush)
 */
@Component({
  selector: 'app-sale-summary',
  templateUrl: './sale-summary.component.html',
  imports: [CommonModule, CardModule, DividerModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaleSummaryComponent {
  // Inputs
  totalAmount = input(0);
  discountAmount = input(0);
  taxAmount = input(0);
  netAmount = input(0);
  amountGiven = input<number | null>(null);
  changeAmount = input<number | null>(null);
  itemCount = input(0);
  showPaymentDetails = input(false);
  currentRemise = input<IRemise | null>(null);
  
  // Assurance/Carnet
  saleType = input<string>('COMPTANT');
  thirdPartyTotal = input<number>(0);
  thirdPartyDetails = input<ThirdPartyAmount[]>([]);
  
  // Avoir (livraison partielle)
  isAvoir = input(false);
  
  // Dernière monnaie
  lastChangeGiven = input<number>(0);

  // Outputs
  addRemise = output<void>();
  removeRemise = output<void>();

  // Computed values
  getTotalHT(): number {
    return this.totalAmount() - this.taxAmount();
  }

  getNetToPay(): number {
    return this.totalAmount() - this.discountAmount();
  }

  hasDiscount(): boolean {
    return this.discountAmount() > 0;
  }

  hasTax(): boolean {
    return this.taxAmount() > 0;
  }

  showChange(): boolean {
    return this.showPaymentDetails() && this.changeAmount() !== null && this.changeAmount()! >= 0;
  }

  hasThirdParty(): boolean {
    return this.saleType() !== 'COMPTANT' && this.thirdPartyTotal() > 0;
  }

  hasMultipleThirdParties(): boolean {
    return this.thirdPartyDetails().length > 1;
  }

  showLastChange(): boolean {
    return this.lastChangeGiven() > 0;
  }

  onAddRemiseClick(): void {
    this.addRemise.emit();
  }

  onRemoveRemiseClick(): void {
    this.removeRemise.emit();
  }
}
