import {inject} from '@angular/core';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {
  KeyboardShortcut
} from '../../../../entities/sales/selling-home/racourci/keyboard-shortcuts.service';
import {
  TauriKeyboardService
} from '../../../../entities/sales/selling-home/racourci/tauri-keyboard.service';
import {ShortcutsProvider} from '../../../../shared/shortcuts/shortcuts-provider.interface';
import {
  ShortcutsHelpDialogComponent
} from '../../../../shared/shortcuts/shortcuts-help-dialog.component';

// ============================================
// Types
// ============================================

export type SaleType = 'COMPTANT' | 'ASSURANCE' | 'CARNET';

export interface SaleShortcutCallbacks {
  // Navigation / Focus
  focusProductSearch: () => void;
  focusQuantity: () => void;
  focusCustomer: () => void;

  // Actions Produit
  addProduct: () => void;
  clearProduct: () => void;

  // Finalisation
  finalizeSale: () => void;
  putOnStandby: () => void;
  cancelSale: () => void;

  // Optionnels
  focusPayment?: () => void;
  printReceipt?: () => void;
  printInvoice?: () => void;
  applyDiscount?: () => void;
  removeDiscount?: () => void;
  saveAsPresale?: () => void;
  savePresale?: () => void;
}

export interface KeyboardShortcutsConfig {
  saleType: SaleType;
  isPresale?: () => boolean;
}

export interface KeyboardShortcutsMixin extends ShortcutsProvider {
  handleKeyboardEvent(event: KeyboardEvent): void;
}

// ============================================
// Input/Textarea filter
// ============================================

function shouldHandleEvent(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement;
  const isInInput =
    target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.getAttribute('role') === 'combobox';

  // F-keys : TOUJOURS actives (standard POS - le caissier tape F9 même dans le champ produit)
  if (/^F\d{1,2}$/.test(event.key)) {
    return true;
  }

  // Alt+key : TOUJOURS actifs (ne conflictent pas avec la saisie)
  if (event.altKey && !event.ctrlKey) {
    return true;
  }

  // Ctrl+key dans un input : laisser le navigateur gérer (copier/coller/couper)
  if (event.ctrlKey && isInInput) {
    return false;
  }

  // Delete, Backspace dans un input : laisser la saisie
  if ((event.key === 'Delete' || event.key === 'Backspace') && isInInput) {
    return false;
  }

  // Hors input : tout le reste est géré
  return !isInInput;
}

// ============================================
// Factory
// ============================================

/**
 * Crée un handler de raccourcis clavier pour un composant de vente.
 *
 * Retourne un objet qui implémente `ShortcutsProvider` (pour la modale d'aide)
 * et expose `handleKeyboardEvent()` (pour le host binding du composant).
 *
 * @example
 * ```typescript
 * private keyboardShortcuts = createKeyboardShortcuts(
 *   { saleType: 'COMPTANT', isPresale: () => this.isPresale() },
 *   {
 *     focusProductSearch: () => this.productHandling.focusProductSearch(),
 *     focusQuantity: () => this.quantityComponent()?.getFocus(),
 *     focusCustomer: () => this.openCustomerOverlay(),
 *     addProduct: () => this.productHandling.addCurrentProduct(),
 *     clearProduct: () => this.productHandling.resetProductSelection(),
 *     finalizeSale: () => this.onSave(),
 *     putOnStandby: () => this.onPutOnHold(),
 *     cancelSale: () => this.onCancel(),
 *   },
 * );
 *
 * handleKeyboardEvent(event: KeyboardEvent): void {
 *   this.keyboardShortcuts.handleKeyboardEvent(event);
 * }
 * ```
 */
export function createKeyboardShortcuts(
  config: KeyboardShortcutsConfig,
  callbacks: SaleShortcutCallbacks,
): KeyboardShortcutsMixin {
  const tauriService = inject(TauriKeyboardService);
  const modalService = inject(NgbModal);

  // Build the shortcut definitions
  const shortcuts = buildShortcuts(config, callbacks, tauriService, modalService);

  return {
    handleKeyboardEvent(event: KeyboardEvent): void {
      if (!shouldHandleEvent(event)) {
        return;
      }

      const eventKey = getEventKey(event);
      const shortcut = shortcuts.find(s => getShortcutKey(s) === eventKey);

      if (shortcut) {
        event.preventDefault();
        shortcut.action();
      }
    },

    getShortcutsByCategory(): Map<string, KeyboardShortcut[]> {
      const grouped = new Map<string, KeyboardShortcut[]>();
      const isTauri = tauriService.isRunningInTauri();

      for (const s of shortcuts) {
        // Filter by environment
        if (s.environmentRestriction === 'tauri' && !isTauri) {
          continue;
        }
        if (s.environmentRestriction === 'web' && isTauri) {
          continue;
        }

        const existing = grouped.get(s.category) || [];
        existing.push(s);
        grouped.set(s.category, existing);
      }

      return grouped;
    },

    isRunningInTauri(): boolean {
      return tauriService.isRunningInTauri();
    },
  };
}

// ============================================
// Build shortcut definitions
// ============================================

function buildShortcuts(
  config: KeyboardShortcutsConfig,
  cb: SaleShortcutCallbacks,
  tauriService: TauriKeyboardService,
  modalService: NgbModal,
): KeyboardShortcut[] {
  const shortcuts: KeyboardShortcut[] = [];
  const isPresale = config.isPresale ?? (() => false);

  // --- F-keys : Flux de vente (catégorie "Actions Vente") ---

  shortcuts.push({
    key: 'F1',
    description: 'Aide raccourcis',
    category: 'Aide',
    badge: 'Essentiel',
    action: () => {
      /* handled inline below */
    },
  });

  shortcuts.push({
    key: 'F2',
    description: 'Focus recherche produit',
    category: 'Actions Produit',
    badge: 'Essentiel',
    action: () => cb.focusProductSearch(),
  });

  shortcuts.push({
    key: 'F3',
    description: 'Focus quantité',
    category: 'Actions Produit',
    badge: 'Essentiel',
    action: () => cb.focusQuantity(),
  });

  shortcuts.push({
    key: 'F4',
    description: config.saleType === 'COMPTANT' ? 'Sélection client' : 'Focus recherche client',
    category: 'Navigation',
    badge: 'Essentiel',
    action: () => cb.focusCustomer(),
  });

  shortcuts.push({
    key: 'F5',
    description: 'Ajouter produit',
    category: 'Actions Produit',
    badge: 'Essentiel',
    action: () => cb.addProduct(),
  });

  shortcuts.push({
    key: 'F6',
    description: 'Effacer sélection produit',
    category: 'Actions Produit',
    action: () => cb.clearProduct(),
  });

  if (cb.focusPayment) {
    shortcuts.push({
      key: 'F7',
      description: 'Focus paiement',
      category: 'Finalisation',
      action: () => cb.focusPayment!(),
    });
  }

  if (cb.applyDiscount) {
    shortcuts.push({
      key: 'F8',
      description: 'Appliquer remise',
      category: 'Remises',
      action: () => cb.applyDiscount!(),
    });
  }

  shortcuts.push({
    key: 'F9',
    description: isPresale() ? 'Enregistrer prévente' : 'Finaliser (Payer)',
    category: 'Finalisation',
    badge: 'Important',
    action: () => {
      if (isPresale() && cb.saveAsPresale) {
        cb.saveAsPresale();
      } else {
        cb.finalizeSale();
      }
    },
  });

  if (!isPresale()) {
    shortcuts.push({
      key: 'F10',
      description: 'Mettre en attente',
      category: 'Finalisation',
      action: () => cb.putOnStandby(),
    });
  }

  // --- Alt+Lettre : Web-safe quick actions ---

  shortcuts.push({
    key: 'p',
    alt: true,
    description: 'Focus produit',
    category: 'Navigation Rapide',
    action: () => cb.focusProductSearch(),
  });

  shortcuts.push({
    key: 'q',
    alt: true,
    description: 'Focus quantité',
    category: 'Navigation Rapide',
    action: () => cb.focusQuantity(),
  });

  shortcuts.push({
    key: 'c',
    alt: true,
    description: 'Focus client',
    category: 'Navigation Rapide',
    action: () => cb.focusCustomer(),
  });

  shortcuts.push({
    key: 'f',
    alt: true,
    description: 'Finaliser',
    category: 'Navigation Rapide',
    action: () => {
      if (isPresale() && cb.saveAsPresale) {
        cb.saveAsPresale();
      } else {
        cb.finalizeSale();
      }
    },
  });

  shortcuts.push({
    key: 's',
    alt: true,
    description: 'Mettre en attente',
    category: 'Navigation Rapide',
    action: () => cb.putOnStandby(),
  });

  if (cb.printReceipt) {
    shortcuts.push({
      key: 't',
      alt: true,
      description: 'Imprimer ticket',
      category: 'Impression',
      action: () => cb.printReceipt!(),
    });
  }

  if (cb.printInvoice) {
    shortcuts.push({
      key: 'i',
      alt: true,
      description: 'Imprimer facture',
      category: 'Impression',
      action: () => cb.printInvoice!(),
    });
  }

  if (cb.applyDiscount) {
    shortcuts.push({
      key: 'r',
      alt: true,
      description: 'Appliquer remise',
      category: 'Remises',
      action: () => cb.applyDiscount!(),
    });
  }

  if (cb.removeDiscount) {
    shortcuts.push({
      key: 'r',
      alt: true,
      shift: true,
      description: 'Retirer remise',
      category: 'Remises',
      action: () => cb.removeDiscount!(),
    });
  }

  // --- Ctrl shortcuts : Desktop/Tauri only ---

  shortcuts.push({
    key: 's',
    ctrl: true,
    description: 'Mettre en attente',
    category: 'Desktop',
    environmentRestriction: 'tauri',
    badge: 'Desktop',
    action: () => cb.putOnStandby(),
  });

  shortcuts.push({
    key: 'Enter',
    ctrl: true,
    description: 'Finaliser rapidement',
    category: 'Desktop',
    environmentRestriction: 'tauri',
    badge: 'Desktop',
    action: () => cb.finalizeSale(),
  });

  if (cb.printReceipt) {
    shortcuts.push({
      key: 'p',
      ctrl: true,
      description: 'Imprimer ticket',
      category: 'Desktop',
      environmentRestriction: 'tauri',
      badge: 'Desktop',
      action: () => cb.printReceipt!(),
    });
  }

  // --- F1 special handling: open help modal ---
  // Override the F1 action to open the shortcuts help dialog
  const f1Shortcut = shortcuts.find(s => s.key === 'F1' && !s.alt && !s.ctrl);
  if (f1Shortcut) {
    const mixin = {
      getShortcutsByCategory: () => {
        const grouped = new Map<string, KeyboardShortcut[]>();
        const isTauri = tauriService.isRunningInTauri();
        for (const s of shortcuts) {
          if (s.environmentRestriction === 'tauri' && !isTauri) {
            continue;
          }
          if (s.environmentRestriction === 'web' && isTauri) {
            continue;
          }
          const existing = grouped.get(s.category) || [];
          existing.push(s);
          grouped.set(s.category, existing);
        }
        return grouped;
      },
      isRunningInTauri: () => tauriService.isRunningInTauri(),
    };

    f1Shortcut.action = () => {
      const modalRef = modalService.open(ShortcutsHelpDialogComponent, {
        size: 'xl',
        centered: true,
      });
      modalRef.componentInstance.shortcutsService = mixin;
    };
  }

  return shortcuts;
}

// ============================================
// Key matching helpers
// ============================================

function getShortcutKey(shortcut: KeyboardShortcut): string {
  const parts: string[] = [];
  if (shortcut.ctrl) {
    parts.push('ctrl');
  }
  if (shortcut.alt) {
    parts.push('alt');
  }
  if (shortcut.shift) {
    parts.push('shift');
  }
  if (shortcut.meta) {
    parts.push('meta');
  }
  parts.push(shortcut.key.toLowerCase());
  return parts.join('+');
}

function getEventKey(event: KeyboardEvent): string {
  const parts: string[] = [];
  if (event.ctrlKey) {
    parts.push('ctrl');
  }
  if (event.altKey) {
    parts.push('alt');
  }
  if (event.shiftKey) {
    parts.push('shift');
  }
  if (event.metaKey) {
    parts.push('meta');
  }
  parts.push(event.key.toLowerCase());
  return parts.join('+');
}
