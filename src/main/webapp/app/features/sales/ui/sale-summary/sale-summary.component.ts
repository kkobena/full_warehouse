import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ThirdPartyAmount {
  id?: number;
  name: string;
  amount: number;
}

/**
 * Composant de présentation : Résumé des montants de la vente
 * 
 * Responsabilités :
 * - Afficher le total
 * - Afficher les remises
 * - Afficher la TVA
 * - Afficher le net à payer
 * - Afficher la monnaie
 * - Afficher la part assurance/tiers payants (ASSURANCE/CARNET)
 * - Afficher la dernière monnaie donnée
 * 
 * Composant pur - Affichage uniquement (OnPush)
 */
@Component({
  selector: 'app-sale-summary',
  templateUrl: './sale-summary.component.html',
  styleUrls: ['./sale-summary.component.scss'],
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaleSummaryComponent {
  // Inputs
  totalAmount = input(0);
  discountAmount = input(0);
  taxAmount = input(0);
  netAmount = input(0);
  changeAmount = input<number | null>(null);
  
  // Assurance/Carnet
  saleType = input<string>('COMPTANT');
  thirdPartyTotal = input<number>(0);
  thirdPartyDetails = input<ThirdPartyAmount[]>([]);
  
  // Dernière monnaie
  lastChangeGiven = input<number>(0);

  // Computed values
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
    return this.changeAmount() !== null && this.changeAmount()! > 0;
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
}