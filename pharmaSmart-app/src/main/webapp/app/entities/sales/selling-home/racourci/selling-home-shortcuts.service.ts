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

  // Desktop-specific advanced features
  forceStock?: () => void;
  quickSearch?: () => void;
  toggleFullscreen?: () => void;
  quickCustomerAdd?: () => void;
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
   * Shortcuts available in both Desktop and Web
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
      badge: 'Essentiel',
      action: () => this.openShortcutsHelp(),
    });

    // ==========================================
    // NAVIGATION - Function Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'F2',
      category: 'Navigation',
      description: 'Focus recherche produit',
      badge: 'Essentiel',
      action: () => this.callbacks.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'F3',
      category: 'Navigation',
      description: 'Focus champ quantité',
      action: () => this.callbacks.focusQuantity(),
    });

    this.keyboardService.registerShortcut({
      key: 'F4',
      category: 'Navigation',
      description: 'Focus sélection client',
      action: () => this.callbacks.focusCustomer(),
    });

    // ==========================================
    // PRODUCT & SALE ACTIONS - Function Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'F5',
      category: 'Actions Produit',
      description: 'Ajouter le produit au panier',
      badge: 'Essentiel',
      action: () => this.callbacks.addProduct(),
    });

    this.keyboardService.registerShortcut({
      key: 'F6',
      category: 'Actions Produit',
      description: 'Effacer sélection produit',
      action: () => this.callbacks.clearProduct(),
    });

    this.keyboardService.registerShortcut({
      key: 'F7',
      category: 'Actions Produit',
      description: 'Voir détails stock produit',
      action: () => this.callbacks.viewProductStock(),
    });

    this.keyboardService.registerShortcut({
      key: 'F8',
      category: 'Remises',
      description: 'Appliquer une remise rapide',
      action: () => this.callbacks.applyDiscount(),
    });

    this.keyboardService.registerShortcut({
      key: 'F9',
      category: 'Finalisation',
      description: 'Finaliser la vente (Paiement)',
      badge: 'Important',
      action: () => this.callbacks.finalizeSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'F10',
      category: 'Finalisation',
      description: 'Mettre la vente en attente',
      action: () => this.callbacks.savePending(),
    });

    this.keyboardService.registerShortcut({
      key: 'F11',
      category: 'Finalisation',
      description: 'Voir les ventes en attente',
      action: () => this.callbacks.viewPendingSales(),
    });

    // ==========================================
    // PRODUCT ACTIONS
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'Delete',
      category: 'Actions Produit',
      description: 'Supprimer la ligne sélectionnée',
      action: () => this.callbacks.removeSelectedLine(),
    });

    this.keyboardService.registerShortcut({
      key: 'Escape',
      category: 'Navigation',
      description: 'Annuler / Quitter',
      action: () => this.callbacks.cancelSale(),
    });

    // ==========================================
    // SALE TYPES - Alt + Letter combinations
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'o',
      alt: true,
      category: 'Types de Vente',
      description: 'Basculer vers Vente Comptant',
      action: () => this.callbacks.switchToComptant(),
    });

    this.keyboardService.registerShortcut({
      key: 'a',
      alt: true,
      category: 'Types de Vente',
      description: 'Basculer vers Vente Assurance',
      action: () => this.callbacks.switchToAssurance(),
    });

    this.keyboardService.registerShortcut({
      key: 'k',
      alt: true,
      category: 'Types de Vente',
      description: 'Basculer vers Vente Carnet',
      action: () => this.callbacks.switchToCarnet(),
    });

    // ==========================================
    // QUANTITY - Alt + Arrow Keys
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'ArrowUp',
      alt: true,
      category: 'Gestion Quantité',
      description: 'Augmenter quantité (+1)',
      action: () => this.callbacks.incrementQuantity(1),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowDown',
      alt: true,
      category: 'Gestion Quantité',
      description: 'Diminuer quantité (-1)',
      action: () => this.callbacks.decrementQuantity(1),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowUp',
      alt: true,
      shift: true,
      category: 'Gestion Quantité',
      description: 'Augmenter quantité (+10)',
      action: () => this.callbacks.incrementQuantity(10),
    });

    this.keyboardService.registerShortcut({
      key: 'ArrowDown',
      alt: true,
      shift: true,
      category: 'Gestion Quantité',
      description: 'Diminuer quantité (-10)',
      action: () => this.callbacks.decrementQuantity(10),
    });

    // ==========================================
    // DISCOUNTS - Alt + R
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'r',
      alt: true,
      category: 'Remises',
      description: 'Appliquer une remise',
      action: () => this.callbacks.applyDiscount(),
    });

    this.keyboardService.registerShortcut({
      key: 'r',
      alt: true,
      shift: true,
      category: 'Remises',
      description: 'Retirer la remise',
      action: () => this.callbacks.removeDiscount(),
    });

    // ==========================================
    // QUICK NAVIGATION - Alt + Letter
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'p',
      alt: true,
      category: 'Navigation Rapide',
      description: 'Focus recherche produit',
      action: () => this.callbacks.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'q',
      alt: true,
      category: 'Navigation Rapide',
      description: 'Focus champ quantité',
      action: () => this.callbacks.focusQuantity(),
    });

    this.keyboardService.registerShortcut({
      key: 'c',
      alt: true,
      category: 'Navigation Rapide',
      description: 'Focus recherche client',
      action: () => this.callbacks.focusCustomer(),
    });

    this.keyboardService.registerShortcut({
      key: 'v',
      alt: true,
      category: 'Navigation Rapide',
      description: 'Focus sélection vendeur',
      action: () => this.callbacks.focusVendor(),
    });

    // ==========================================
    // PRINTING - Alt + I/T
    // ==========================================
    this.keyboardService.registerShortcut({
      key: 'i',
      alt: true,
      category: 'Impression',
      description: 'Imprimer la facture',
      action: () => this.callbacks.printInvoice(),
    });

    this.keyboardService.registerShortcut({
      key: 't',
      alt: true,
      category: 'Impression',
      description: 'Imprimer le ticket',
      action: () => this.callbacks.printReceipt(),
    });
  }

  /**
   * Desktop-specific shortcuts (can use Ctrl combinations safely)
   * These provide a more desktop-like, power-user experience
   */
  private registerTauriShortcuts(): void {
    if (!this.callbacks) return;

    const modifier = this.tauriService.getModifierKey();

    // ==========================================
    // TAURI POWER SHORTCUTS - Ctrl combinations
    // ==========================================

    // Save & Finalization
    this.keyboardService.registerShortcut({
      key: 's',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Mettre la vente en attente',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.savePending(),
    });

    this.keyboardService.registerShortcut({
      key: 'Enter',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Finaliser rapidement (paiement)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.finalizeSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'n',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Nouvelle vente (annuler vente actuelle)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.cancelSale(),
    });

    // Search & Navigation
    this.keyboardService.registerShortcut({
      key: 'f',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Recherche rapide produit (focus)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.focusProductSearch(),
    });

    this.keyboardService.registerShortcut({
      key: 'k',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Recherche omnidirectionnelle',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => {
        // Quick search across products/customers/pending sales
        if (this.callbacks.quickSearch) {
          this.callbacks.quickSearch();
        } else {
          this.callbacks.focusProductSearch();
        }
      },
    });

    this.keyboardService.registerShortcut({
      key: 'e',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Recherche client rapide',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.focusCustomer(),
    });

    // Printing
    this.keyboardService.registerShortcut({
      key: 'p',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Imprimer ticket (thermal)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.printReceipt(),
    });

    this.keyboardService.registerShortcut({
      key: 'p',
      ctrl: true,
      shift: true,
      category: '⚡ Desktop',
      description: 'Imprimer facture (A4)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.printInvoice(),
    });

    // Discounts & Special Actions
    this.keyboardService.registerShortcut({
      key: 'd',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Appliquer remise rapide',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.applyDiscount(),
    });

    this.keyboardService.registerShortcut({
      key: 'd',
      ctrl: true,
      shift: true,
      category: '⚡ Desktop',
      description: 'Retirer toutes les remises',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.removeDiscount(),
    });

    // Force stock (if available)
    if (this.callbacks.forceStock) {
      this.keyboardService.registerShortcut({
        key: 'f',
        ctrl: true,
        shift: true,
        category: '⚡ Desktop',
        description: 'Forcer le stock (outrepasser limite)',
        badge: 'Admin',
        environmentRestriction: 'tauri',
        action: () => this.callbacks.forceStock(),
      });
    }

    // Product management
    this.keyboardService.registerShortcut({
      key: 'Delete',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Supprimer ligne sélectionnée (confirmé)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.removeSelectedLine(),
    });

    this.keyboardService.registerShortcut({
      key: 'Backspace',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Effacer produit sélectionné',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.clearProduct(),
    });

    // Quantity shortcuts (faster for desktop)
    this.keyboardService.registerShortcut({
      key: '=',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Augmenter quantité (+1)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.incrementQuantity(1),
    });

    this.keyboardService.registerShortcut({
      key: '-',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Diminuer quantité (-1)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.decrementQuantity(1),
    });

    // View toggles
    this.keyboardService.registerShortcut({
      key: 'b',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Afficher ventes en attente (sidebar)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.viewPendingSales(),
    });

    this.keyboardService.registerShortcut({
      key: 'h',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Afficher historique stock',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.viewProductStock(),
    });

    // Sale type switching (with Ctrl for faster access)
    this.keyboardService.registerShortcut({
      key: '1',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Basculer vers Comptant (rapide)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.switchToComptant(),
    });

    this.keyboardService.registerShortcut({
      key: '2',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Basculer vers Assurance (rapide)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.switchToAssurance(),
    });

    this.keyboardService.registerShortcut({
      key: '3',
      ctrl: true,
      category: '⚡ Desktop',
      description: 'Basculer vers Carnet (rapide)',
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.callbacks.switchToCarnet(),
    });

    // Quick add customer (Desktop-specific UX enhancement)
    if (this.callbacks.quickCustomerAdd) {
      this.keyboardService.registerShortcut({
        key: 'a',
        ctrl: true,
        shift: true,
        category: '⚡ Desktop',
        description: 'Ajout client rapide',
        badge: 'Desktop',
        environmentRestriction: 'tauri',
        action: () => this.callbacks.quickCustomerAdd(),
      });
    }

    // Fullscreen toggle (Desktop-specific)
    if (this.callbacks.toggleFullscreen) {
      this.keyboardService.registerShortcut({
        key: 'F11',
        ctrl: true,
        category: '⚡ Desktop',
        description: 'Basculer plein écran',
        badge: 'Desktop',
        environmentRestriction: 'tauri',
        action: () => this.callbacks.toggleFullscreen(),
      });
    }

    // Help with Ctrl+/
    this.keyboardService.registerShortcut({
      key: '/',
      ctrl: true,
      category: '⚡ Desktop',
      description: "Aide raccourcis (alternative à F1)",
      badge: 'Desktop',
      environmentRestriction: 'tauri',
      action: () => this.openShortcutsHelp(),
    });
  }

  /**
   * Web-specific shortcuts (more conservative, avoid browser conflicts)
   * Focus on F-keys and Alt combinations that are safe in browsers
   */
  private registerWebShortcuts(): void {
    if (!this.callbacks) return;

    // ==========================================
    // WEB SAFE SHORTCUTS
    // ==========================================

    // Additional Alt shortcuts for common actions (safe in web)
    this.keyboardService.registerShortcut({
      key: 's',
      alt: true,
      category: '🌐 Web',
      description: 'Sauvegarder (mettre en attente)',
      badge: 'Web',
      environmentRestriction: 'web',
      action: () => this.callbacks.savePending(),
    });

    this.keyboardService.registerShortcut({
      key: 'f',
      alt: true,
      category: '🌐 Web',
      description: 'Finaliser la vente',
      badge: 'Web',
      environmentRestriction: 'web',
      action: () => this.callbacks.finalizeSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'n',
      alt: true,
      category: '🌐 Web',
      description: 'Nouvelle vente',
      badge: 'Web',
      environmentRestriction: 'web',
      action: () => this.callbacks.cancelSale(),
    });

    this.keyboardService.registerShortcut({
      key: 'b',
      alt: true,
      category: '🌐 Web',
      description: 'Ventes en attente',
      badge: 'Web',
      environmentRestriction: 'web',
      action: () => this.callbacks.viewPendingSales(),
    });

    this.keyboardService.registerShortcut({
      key: 'h',
      alt: true,
      category: '🌐 Web',
      description: 'Historique/Stock produit',
      badge: 'Web',
      environmentRestriction: 'web',
      action: () => this.callbacks.viewProductStock(),
    });

    // Note: F12 is reserved for DevTools, but F1-F11 are generally safe
    // We already use F1-F11 in common shortcuts
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
   * Get platform modifier key name for display
   */
  getModifierKeyName(): string {
    return this.tauriService.getModifierKey();
  }

  /**
   * Open shortcuts help modal
   */
  private openShortcutsHelp(): void {
    const modalRef = this.modalService.open(ShortcutsHelpDialogComponent, {
      size: 'xl',
      backdrop: 'static',
      scrollable: true,
      centered: true,
    });

    // Pass shortcuts service to modal so it can display dynamic content
    if (modalRef.componentInstance) {
      modalRef.componentInstance.shortcutsService = this;
    }
  }
}
