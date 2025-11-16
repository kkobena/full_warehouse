import { Injectable, inject } from '@angular/core';
import { KeyboardShortcut, KeyboardShortcutsService } from './keyboard-shortcuts.service';
import { TauriKeyboardService } from './tauri-keyboard.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ShortcutsHelpDialogComponent } from '../../../../shared/shortcuts/shortcuts-help-dialog.component';

export interface SalesShortcutCallbacks {
  // Navigation
  focusProductSearch: () => void;
  focusQuantity: () => void;
  focusCustomer: () => void;
  focusVendor: () => void;

  // Product actions
  addProduct: () => void;
  removeSelectedLine: () => void;
  clearProduct: () => void;
  viewProductStock: () => void;

  // Sale types
  switchToComptant: () => void;
  switchToAssurance: () => void;
  switchToCarnet: () => void;
  switchToDepotAgree: () => void;

  // Payment & Finalization
  finalizeSale: () => void;
  savePending: () => void;
  viewPendingSales: () => void;
  cancelSale: () => void;

  // Quantity
  incrementQuantity: (amount: number) => void;
  decrementQuantity: (amount: number) => void;

  // Discounts
  applyDiscount: () => void;
  removeDiscount: () => void;

  // Printing
  printInvoice: () => void;
  printReceipt: () => void;

  // Tauri-specific
  forceStock?: () => void;
  quickSearch?: () => void;
}

@Injectable({
  providedIn: 'root',
})
export class SellingHomeShortcutsService {
  private keyboardService = inject(KeyboardShortcutsService);
  private tauriService = inject(TauriKeyboardService);
  private modalService = inject(NgbModal);
  private callbacks?: SalesShortcutCallbacks;

  /**
   * Register all shortcuts for the sales interface
   */
  registerAll(callbacks: SalesShortcutCallbacks): void {
    this.callbacks = callbacks;

    // Clear existing shortcuts
    this.keyboardService.clearAll();

    // Register shortcuts based on environment
    if (this.tauriService.isRunningInTauri()) {
      this.registerTauriShortcuts();
    } else {
      this.registerWebShortcuts();
    }

    // Register common shortcuts (work everywhere)
    this.registerCommonShortcuts();
  }

  /**
   * Unregister all shortcuts
   */
  unregisterAll(): void {
    this.keyboardService.clearAll();
    this.callbacks = undefined;
  }

  /**
   * Shortcuts available in both Tauri and Web
   */
  private registerCommonShortcuts(): void {
    if (!this.callbacks) return;

    // ==========================================
    // HELP - F1
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'F1',
      category: 'Aide',
      description: "Afficher l'aide des raccourcis clavier",
      action: () => this.openShortcutsHelp(),
    });

    // ==========================================
    // NAVIGATION - Function Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'F2',
      category: 'Navigation',
      description: 'Rechercher un produit',
      action: () => this.callbacks!.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'F3',
      category: 'Navigation',
      description: 'Modifier la quantité',
      action: () => this.callbacks!.focusQuantity(),
    });

    this.keyboardService.registerShortcut({
      key: 'F4',
      category: 'Navigation',
      description: 'Sélectionner un client',
      action: () => this.callbacks!.focusCustomer(),
    });

    // ==========================================
    // SALE ACTIONS - Function Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'F5',
      category: 'Vente',
      description: 'Ajouter le produit au panier',
      action: () => this.callbacks!.addProduct(),
    });

    this.keyboardService.registerShortcut({
      key: 'F9',
      category: 'Vente',
      description: 'Finaliser la vente (Paiement)',
      action: () => this.callbacks!.finalizeSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'F10',
      category: 'Vente',
      description: 'Mettre la vente en attente',
      action: () => this.callbacks!.savePending(),
    });

    this.keyboardService.registerShortcut({
      key: 'F11',
      category: 'Vente',
      description: 'Voir les ventes en attente',
      action: () => this.callbacks!.viewPendingSales(),
    });

    // ==========================================
    // PRODUCT ACTIONS
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'Delete',
      category: 'Produit',
      description: 'Supprimer la ligne sélectionnée',
      action: () => this.callbacks!.removeSelectedLine(),
    });

    this.keyboardService.registerShortcut({
      key: 'Escape',
      category: 'Navigation',
      description: 'Annuler / Quitter',
      action: () => this.callbacks!.cancelSale(),
    });

    // ==========================================
    // SALE TYPES - Alt combinations
    // ==========================================
    this.keyboardService.registerShortcut({
      key: '1',
      alt: true,
      category: 'Type de vente',
      description: 'Vente Comptant',
      action: () => this.callbacks!.switchToComptant(),
    });

    this.keyboardService.registerShortcut({
      key: '2',
      alt: true,
      category: 'Type de vente',
      description: 'Vente Assurance',
      action: () => this.callbacks!.switchToAssurance(),
    });

    this.keyboardService.registerShortcut({
      key: '3',
      alt: true,
      category: 'Type de vente',
      description: 'Vente Carnet',
      action: () => this.callbacks!.switchToCarnet(),
    });

    this.keyboardService.registerShortcut({
      key: '4',
      alt: true,
      category: 'Type de vente',
      description: 'Vente Dépôt Agréé',
      action: () => this.callbacks!.switchToDepotAgree(),
    });

    // ==========================================
    // QUANTITY - Alt + Arrow Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'ArrowUp',
      alt: true,
      category: 'Quantité',
      description: 'Augmenter quantité (+1)',
      action: () => this.callbacks!.incrementQuantity(1),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowDown',
      alt: true,
      category: 'Quantité',
      description: 'Diminuer quantité (-1)',
      action: () => this.callbacks!.decrementQuantity(1),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowUp',
      alt: true,
      shift: true,
      category: 'Quantité',
      description: 'Augmenter quantité (+10)',
      action: () => this.callbacks!.incrementQuantity(10),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowDown',
      alt: true,
      shift: true,
      category: 'Quantité',
      description: 'Diminuer quantité (-10)',
      action: () => this.callbacks!.decrementQuantity(10),
    });

    // ==========================================
    // DISCOUNTS - Alt + R
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'r',
      alt: true,
      category: 'Remise',
      description: 'Appliquer une remise',
      action: () => this.callbacks!.applyDiscount(),
    });

    // ==========================================
    // QUICK NAVIGATION - Alt + Letter
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'p',
      alt: true,
      category: 'Navigation',
      description: 'Focus recherche produit',
      action: () => this.callbacks!.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'q',
      alt: true,
      category: 'Navigation',
      description: 'Focus quantité',
      action: () => this.callbacks!.focusQuantity(),
    });

    this.keyboardService.registerShortcut({
      key: 'c',
      alt: true,
      category: 'Navigation',
      description: 'Focus client',
      action: () => this.callbacks!.focusCustomer(),
    });

    this.keyboardService.registerShortcut({
      key: 'v',
      alt: true,
      category: 'Navigation',
      description: 'Focus vendeur',
      action: () => this.callbacks!.focusVendor(),
    });
  }

  /**
   * Tauri-specific shortcuts (can use Ctrl combinations safely)
   */
  private registerTauriShortcuts(): void {
    if (!this.callbacks) return;

    const modifier = this.tauriService.getModifierKey();

    // ==========================================
    // TAURI POWER SHORTCUTS - Ctrl combinations
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 's',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+S: Mettre en attente (Tauri)`,
      action: () => this.callbacks!.savePending(),
    });

    this.keyboardService.registerShortcut({
      key: 'p',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+P: Imprimer ticket (Tauri)`,
      action: () => this.callbacks!.printReceipt(),
    });

    this.keyboardService.registerShortcut({
      key: 'p',
      ctrl: true,
      shift: true,
      category: '⚡ Tauri',
      description: `${modifier}+Shift+P: Imprimer facture (Tauri)`,
      action: () => this.callbacks!.printInvoice(),
    });

    this.keyboardService.registerShortcut({
      key: 'n',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+N: Nouvelle vente (Tauri)`,
      action: () => this.callbacks!.cancelSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'f',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+F: Recherche rapide produit (Tauri)`,
      action: () => this.callbacks!.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'd',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+D: Remise rapide (Tauri)`,
      action: () => this.callbacks!.applyDiscount(),
    });

    this.keyboardService.registerShortcut({
      key: 'Enter',
      ctrl: true,
      category: '⚡ Tauri',
      description: `${modifier}+Enter: Finaliser rapidement (Tauri)`,
      action: () => this.callbacks!.finalizeSale(),
    });

    // Force stock if available
    if (this.callbacks.forceStock) {
      this.keyboardService.registerShortcut({
        key: 'f',
        ctrl: true,
        shift: true,
        category: '⚡ Tauri',
        description: `${modifier}+Shift+F: Forcer le stock (Tauri)`,
        action: () => this.callbacks!.forceStock!(),
      });
    }
  }

  /**
   * Web-specific shortcuts (more conservative)
   */
  private registerWebShortcuts(): void {
    // In web, we stick to safe shortcuts only
    // Most are already registered in registerCommonShortcuts
    // Add any web-specific shortcuts here if needed
  }

  /**
   * Get all registered shortcuts grouped by category
   */
  getShortcutsByCategory(): Map<string, KeyboardShortcut[]> {
    const shortcuts = this.keyboardService.shortcutsList();
    const grouped = new Map<string, KeyboardShortcut[]>();

    shortcuts.forEach(shortcut => {
      const existing = grouped.get(shortcut.category) || [];
      existing.push(shortcut);
      grouped.set(shortcut.category, existing);
    });

    return grouped;
  }

  /**
   * Check if running in Tauri
   */
  isRunningInTauri(): boolean {
    return this.tauriService.isRunningInTauri();
  }

  /**
   * Open shortcuts help modal
   */
  private openShortcutsHelp(): void {
    this.modalService.open(ShortcutsHelpDialogComponent, {
      size: 'xl',
      backdrop: 'static',
      scrollable: true,
    });
  }
}
