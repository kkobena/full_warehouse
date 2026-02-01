import { Injectable, inject } from '@angular/core';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { IUser } from '../../../../core/user/user.model';

/**
 * Service pour gérer l'afficheur client POS
 * 
 * Affiche en temps réel:
 * - Message de bienvenue
 * - Utilisateur connecté
 * - Produits ajoutés (nom, qté, prix)
 * - Total à payer
 * - Monnaie rendue
 */
@Injectable({
  providedIn: 'root',
})
export class CustomerDisplayService {
  private tauriPrinterService = inject(TauriPrinterService);

  /**
   * Initialiser l'afficheur avec message de bienvenue
   */
  initialize(storeName: string, user?: IUser): void {
    this.showWelcomeMessage(storeName);
    
    if (user) {
      this.updateDisplayForUser(user);
    }
  }

  /**
   * Afficher message de bienvenue
   */
  showWelcomeMessage(storeName: string): void {
    this.tauriPrinterService.showWelcomeMessage(storeName).catch(error => {
      console.error('Failed to show welcome message on customer display:', error);
    });
  }

  /**
   * Afficher utilisateur connecté
   */
  updateDisplayForUser(user: IUser): void {
    const userName = user.firstName || user.login || 'Caissier';
    this.tauriPrinterService.updateDisplayForUser(userName).catch(error => {
      console.error('Failed to update customer display for user:', error);
    });
  }

  /**
   * Afficher produit ajouté (nom, quantité, prix)
   */
  updateDisplayForProduct(productName: string, qty: number, price: number): void {
    this.tauriPrinterService.updateDisplayForProduct(productName, qty, price).catch(error => {
      console.error('Failed to update customer display for product:', error);
    });
  }

  /**
   * Afficher total à payer
   */
  updateDisplayForTotal(total: number): void {
    this.tauriPrinterService.updateDisplayForTotal(total).catch(error => {
      console.error('Failed to update customer display for total:', error);
    });
  }

  /**
   * Afficher monnaie rendue
   */
  updateDisplayForChange(change: number): void {
    this.tauriPrinterService.updateDisplayForChange(change).catch(error => {
      console.error('Failed to update customer display for change:', error);
    });
  }

  /**
   * Effacer l'afficheur
   */
  clear(): void {
    this.tauriPrinterService.clearCustomerDisplay().catch(error => {
      console.error('Failed to clear customer display:', error);
    });
  }
}
