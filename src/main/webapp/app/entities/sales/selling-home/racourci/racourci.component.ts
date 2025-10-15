
import { Component, OnInit, OnDestroy, HostListener, ViewChild, ElementRef, inject, signal, effect } from '@angular/core';
import { KeyboardShortcut, KeyboardShortcutsService } from './keyboard-shortcuts.service';

@Component({
  selector: 'jhi-racourci',
  imports: [],
  templateUrl: './racourci.component.html',
  styleUrl: './racourci.component.scss'
})

export class RacourciComponent implements OnInit, OnDestroy {
  @ViewChild('productSearchInput') productSearchInput!: ElementRef;
  @ViewChild('quantityInput') quantityInput!: ElementRef;
  @ViewChild('referenceInput') referenceInput?: ElementRef;
  @ViewChild('commentaireInput') commentaireInput?: ElementRef;

  showShortcutsModal = signal(false);
  shortcutNotification = signal('');

  private keyboardShortcutsService = inject(KeyboardShortcutsService);
  private notificationTimeout?: number;

  constructor() {
    // Effet pour auto-cacher la notification
    effect(() => {
      if (this.shortcutNotification()) {
        if (this.notificationTimeout) {
          clearTimeout(this.notificationTimeout);
        }
        this.notificationTimeout = window.setTimeout(() => {
          this.shortcutNotification.set('');
        }, 2000);
      }
    });
  }

  ngOnInit(): void {
    this.registerShortcuts();

    // Afficher notification des raccourcis
    this.keyboardShortcutsService.getShortcutTriggered$().subscribe(description => {
      this.shortcutNotification.set(description);
    });
  }

  ngOnDestroy(): void {
    this.keyboardShortcutsService.clearAll();
    if (this.notificationTimeout) {
      clearTimeout(this.notificationTimeout);
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    const isInputField = target.tagName === 'INPUT' ||
                        target.tagName === 'TEXTAREA' ||
                        target.isContentEditable;

    // Touches fonctionnelles qui marchent partout
    const globalKeys = ['f1', 'f2', 'f3', 'f4', 'f9', 'f10', 'f11', 'escape'];

    // Combinaisons Alt qui marchent partout (n'interfèrent généralement pas avec le navigateur)
    const isAltCombo = event.altKey && !event.ctrlKey && !event.shiftKey;

    const shouldHandle = !isInputField || globalKeys.includes(event.key.toLowerCase()) || isAltCombo;

    if (shouldHandle) {
      this.keyboardShortcutsService.handleKeyboardEvent(event);
    }
  }

  private registerShortcuts(): void {
    // ==========================================
    // NAVIGATION & FOCUS - Touches fonctionnelles
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F1',
      category: 'Navigation',
      description: 'Afficher les raccourcis clavier',
      action: () => this.toggleShortcutsModal()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F2',
      category: 'Navigation',
      description: 'Rechercher un produit',
      action: () => this.focusProductSearch()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F3',
      category: 'Navigation',
      description: 'Modifier la quantité',
      action: () => this.focusQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F4',
      category: 'Navigation',
      description: 'Sélectionner un client',
      action: () => this.openCustomerModal()
    });

    // ==========================================
    // TYPES DE VENTE - Alt + Chiffre (safe)
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: '&', // Alt + 1 sur clavier français
      alt: true,
      category: 'Types de vente',
      description: 'Vente Comptant',
      action: () => this.selectSaleType('COMPTANT')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'é', // Alt + 2 sur clavier français
      alt: true,
      category: 'Types de vente',
      description: 'Vente Assurance',
      action: () => this.selectSaleType('ASSURANCE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '"', // Alt + 3 sur clavier français
      alt: true,
      category: 'Types de vente',
      description: 'Vente Carnet',
      action: () => this.selectSaleType('CARNET')
    });

    // ==========================================
    // REMISES - Alt + R (évite Ctrl+R du navigateur)
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'r',
      alt: true,
      category: 'Remises',
      description: 'Appliquer une remise',
      action: () => this.openDiscountModal()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'r',
      alt: true,
      shift: true,
      category: 'Remises',
      description: 'Supprimer la remise',
      action: () => this.removeDiscount()
    });

    // ==========================================
    // MODES DE PAIEMENT - F6-F8 (F5 réservé au refresh)
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F6',
      category: 'Paiement',
      description: 'Paiement Espèces',
      action: () => this.focusPaymentMode('ESPECE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F7',
      category: 'Paiement',
      description: 'Paiement Carte Bancaire',
      action: () => this.focusPaymentMode('CB')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F8',
      category: 'Paiement',
      description: 'Paiement Chèque',
      action: () => this.focusPaymentMode('CHEQUE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F11', // F11 en plein écran, mais on peut l'utiliser si besoin
      category: 'Paiement',
      description: 'Paiement Virement',
      action: () => this.focusPaymentMode('VIREMENT')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'p',
      alt: true,
      category: 'Paiement',
      description: 'Ajouter un mode de paiement',
      action: () => this.addPaymentMode()
    });

    // ==========================================
    // ACTIONS PRINCIPALES
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F9',
      category: 'Actions',
      description: 'Terminer la vente (Enter)',
      action: () => this.save()
    });

    // Alternative avec Enter sur le montant payé
    this.keyboardShortcutsService.registerShortcut({
      key: 'Enter',
      category: 'Actions',
      description: 'Valider la saisie',
      action: () => {
        // Logique contextuelle selon le focus
        const activeElement = document.activeElement as HTMLElement;
        if (activeElement.classList.contains('payment-mode-input')) {
          this.save();
        }
      }
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F10',
      category: 'Actions',
      description: 'Mettre en attente',
      action: () => this.putOnHold()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'n',
      alt: true,
      category: 'Actions',
      description: 'Nouvelle vente',
      action: () => this.newSale()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'Escape',
      category: 'Actions',
      description: 'Annuler / Fermer',
      action: () => this.cancel()
    });

    // ==========================================
    // GESTION DES PRODUITS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'Delete',
      alt: true,
      category: 'Produits',
      description: 'Supprimer le dernier produit',
      action: () => this.removeLastProduct()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '+',
      alt: true,
      category: 'Produits',
      description: 'Augmenter la quantité',
      action: () => this.incrementLastProductQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '-',
      alt: true,
      category: 'Produits',
      description: 'Diminuer la quantité',
      action: () => this.decrementLastProductQuantity()
    });

    // ==========================================
    // IMPRESSION - Alt au lieu de Ctrl
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'i',
      alt: true,
      category: 'Impression',
      description: 'Imprimer le ticket',
      action: () => this.printReceipt()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'i',
      alt: true,
      shift: true,
      category: 'Impression',
      description: 'Imprimer la facture',
      action: () => this.printInvoice()
    });

    // ==========================================
    // NAVIGATION RAPIDE DANS LE TABLEAU
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'ArrowUp',
      alt: true,
      category: 'Navigation',
      description: 'Sélectionner produit précédent',
      action: () => this.selectPreviousProduct()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'ArrowDown',
      alt: true,
      category: 'Navigation',
      description: 'Sélectionner produit suivant',
      action: () => this.selectNextProduct()
    });
  }

  // ==========================================
  // MÉTHODES D'ACTION
  // ==========================================

  private focusProductSearch(): void {
    setTimeout(() => {
      this.productSearchInput?.nativeElement?.focus();
    }, 0);
  }

  private focusQuantity(): void {
    setTimeout(() => {
      this.quantityInput?.nativeElement?.focus();
      this.quantityInput?.nativeElement?.select();
    }, 0);
  }

  private openCustomerModal(): void {
    // Votre logique
    console.log('Ouvrir modal client');
  }

  private selectSaleType(type: string): void {
    // Votre logique
    console.log('Type de vente:', type);
  }

  private openDiscountModal(): void {
    console.log('Ouvrir modal remise');
  }

  private removeDiscount(): void {
    console.log('Supprimer remise');
  }

  private focusPaymentMode(mode: string): void {
    setTimeout(() => {
      const input = document.getElementById(mode) as HTMLInputElement;
      if (input) {
        input.focus();
        input.select();
      }
    }, 0);
  }

  private addPaymentMode(): void {
    console.log('Ajouter mode de paiement');
  }

  private save(): void {
    if (this.canSave()) {
      console.log('Terminer la vente');
    } else {
      this.shortcutNotification.set('⚠️ Montant à payer non soldé');
    }
  }

  private putOnHold(): void {
    console.log('Mettre en attente');
  }

  private newSale(): void {
    console.log('Nouvelle vente');
  }

  private cancel(): void {
    if (this.showShortcutsModal()) {
      this.showShortcutsModal.set(false);
    } else {
      console.log('Annuler');
    }
  }

  private removeLastProduct(): void {
    console.log('Supprimer dernier produit');
  }

  private incrementLastProductQuantity(): void {
    console.log('Augmenter quantité');
  }

  private decrementLastProductQuantity(): void {
    console.log('Diminuer quantité');
  }

  private selectPreviousProduct(): void {
    console.log('Produit précédent');
  }

  private selectNextProduct(): void {
    console.log('Produit suivant');
  }

  private printReceipt(): void {
    console.log('Imprimer ticket');
  }

  private printInvoice(): void {
    console.log('Imprimer facture');
  }

  private toggleShortcutsModal(): void {
    this.showShortcutsModal.update(v => !v);
  }

  private canSave(): boolean {
    return true;
 //   return this.currentSaleService.currentSale()?.amountToBePaid === 0;
  }

  // Helper pour le template
  formatShortcutKey(shortcut: KeyboardShortcut): string {
    let key = '';
    if (shortcut.ctrl) key += 'Ctrl + ';
    if (shortcut.alt) key += 'Alt + ';
    if (shortcut.shift) key += 'Shift + ';

    // Conversion des caractères spéciaux français
    const keyMap: Record<string, string> = {
      '&': '1',
      'é': '2',
      '"': '3',
      "'": '4',
      '(': '5',
      '-': '6',
      'è': '7',
      '_': '8',
      'ç': '9',
      'à': '0'
    };

    const displayKey = keyMap[shortcut.key] || shortcut.key;
    key += displayKey.toUpperCase();
    return key;
  }

  getShortcutsByCategory(category: string) {
    return this.keyboardShortcutsService.shortcutsList()
      .filter(s => s.category === category);
  }
}
/*
import { isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  effect,
  inject,
  OnInit,
  PLATFORM_ID,
  viewChild,
  signal,
  HostListener
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
// ... autres imports existants
import { KeyboardShortcutsService } from './keyboard-shortcuts.service';
import { DialogModule } from 'primeng/dialog';

@Component({
  selector: 'jhi-selling-home',
  imports: [
    // ... vos imports existants
    DialogModule
  ],
  templateUrl: './selling-home.component.html',
  styleUrl: './selling-home.component.scss'
})
export class SellingHomeComponent implements OnInit, AfterViewInit {
  // ... propriétés existantes

  // Nouveaux signaux pour les raccourcis
  showShortcutsModal = signal(false);
  shortcutNotification = signal('');
  private inputBuffer = signal('');
  private bufferTimeout?: number;

  private readonly keyboardShortcutsService = inject(KeyboardShortcutsService);

  constructor() {
    // ... votre constructeur existant

    // Effect pour les notifications
    effect(() => {
      if (this.shortcutNotification()) {
        if (this.bufferTimeout) {
          clearTimeout(this.bufferTimeout);
        }
        this.bufferTimeout = window.setTimeout(() => {
          this.shortcutNotification.set('');
        }, 2000);
      }
    });

    this.canForceStock = this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);
    this.initCustomerEffect();
    this.quantityMessage = this.translateLabel('stockInsuffisant');

    handleSaleEvents(this.saleEventManager, ['saveResponse', 'completeSale', 'responseEvent', 'inputBoxFocus'], event => {
      switch (event.name) {
        case 'saveResponse':
          this.handleSaveResponse(event);
          break;
        case 'completeSale':
          this.handleCompleteSale(event);
          break;
        case 'responseEvent':
          this.handleResponseEvent(event);
          break;
        case 'inputBoxFocus':
          this.handleInputBoxFocus(event);
          break;
      }
    });
  }

  ngOnInit(): void {
    // ... votre code existant

    // Enregistrer les raccourcis
    this.registerKeyboardShortcuts();

    // S'abonner aux notifications
    this.keyboardShortcutsService.getShortcutTriggered$()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(description => {
        this.shortcutNotification.set(description);
      });

    // ... reste du code existant
  }

  ngOnDestroy(): void {
    this.keyboardShortcutsService.clearAll();
    if (this.bufferTimeout) {
      clearTimeout(this.bufferTimeout);
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    const isInputField = target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.isContentEditable;

    // Touches fonctionnelles qui marchent partout
    const functionalKeys = ['F1', 'F2', 'F3', 'F4', 'F5', 'F6', 'F7', 'F8', 'F9', 'F10', 'F11', 'F12'];
    const isFunctionalKey = functionalKeys.includes(event.key);

    // Combinaisons Alt
    const isAltCombo = event.altKey && !event.ctrlKey && !event.shiftKey;

    // Saisie rapide au pavé numérique quand pas dans un input
    if (!isInputField) {
      if (this.isNumericKey(event)) {
        this.handleQuickEntry(event);
        return;
      }

      if (event.key === '*' || event.key === '%' || event.key === '-') {
        this.handleOperatorKey(event);
        return;
      }
    }

    const shouldHandle = !isInputField || isFunctionalKey || isAltCombo || event.key === 'Escape';

    if (shouldHandle) {
      this.keyboardShortcutsService.handleKeyboardEvent(event);
    }
  }

  private registerKeyboardShortcuts(): void {
    // ==========================================
    // RECHERCHE & PRODUITS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F2',
      category: 'Produits',
      description: '🔍 Rechercher produit',
      action: () => this.focusProductSearch()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F3',
      category: 'Produits',
      description: '📦 Modifier quantité',
      action: () => this.focusQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F4',
      category: 'Produits',
      description: '➕ Ajouter produit',
      action: () => this.addProductFromKeyboard()
    });

    // ==========================================
    // PAIEMENTS (Standard POS)
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F6',
      category: 'Paiement',
      description: '💵 Espèces',
      action: () => this.selectPaymentMode('ESPECE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F7',
      category: 'Paiement',
      description: '💳 Carte Bancaire',
      action: () => this.selectPaymentMode('CB')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F8',
      category: 'Paiement',
      description: '📝 Chèque',
      action: () => this.selectPaymentMode('CHEQUE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F11',
      category: 'Paiement',
      description: '🏦 Virement',
      action: () => this.selectPaymentMode('VIREMENT')
    });

    // ==========================================
    // FINALISATION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F9',
      category: 'Transaction',
      description: '✓ Finaliser la vente',
      action: () => this.completeSaleFromKeyboard()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F10',
      category: 'Transaction',
      description: '⏸️ Mettre en attente',
      action: () => this.putSaleOnHold()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'Escape',
      category: 'Transaction',
      description: '❌ Annuler / Retour',
      action: () => this.handleEscape()
    });

    // ==========================================
    // CLIENT
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F12',
      category: 'Client',
      description: '👤 Rechercher client',
      action: () => this.focusCustomerSearch()
    });

    // ==========================================
    // TYPES DE VENTE - Alt + Chiffres
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: '1',
      alt: true,
      category: 'Types vente',
      description: 'Vente Comptant',
      action: () => this.switchToComptant()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '2',
      alt: true,
      category: 'Types vente',
      description: 'Vente Assurance',
      action: () => this.switchToAssurance()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '3',
      alt: true,
      category: 'Types vente',
      description: 'Vente Carnet',
      action: () => this.switchToCarnet()
    });

    // ==========================================
    // REMISES
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'r',
      alt: true,
      category: 'Remises',
      description: '💸 Appliquer remise',
      action: () => this.openDiscountModal()
    });

    // ==========================================
    // NAVIGATION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F1',
      category: 'Aide',
      description: '❓ Aide - Raccourcis clavier',
      action: () => this.toggleShortcutsModal()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'n',
      alt: true,
      category: 'Navigation',
      description: '🆕 Nouvelle vente',
      action: () => this.resetAll()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'v',
      alt: true,
      category: 'Navigation',
      description: '📋 Ventes en attente',
      action: () => this.openPindingSide()
    });

    // ==========================================
    // IMPRESSION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'i',
      alt: true,
      category: 'Impression',
      description: '🖨️ Imprimer ticket',
      action: () => this.togglePrintTicket()
    });

    // ==========================================
    // GESTION PRODUITS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'Delete',
      alt: true,
      category: 'Produits',
      description: '🗑️ Supprimer dernier produit',
      action: () => this.removeLastProduct()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '+',
      alt: true,
      category: 'Produits',
      description: '➕ Augmenter quantité',
      action: () => this.incrementQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '-',
      alt: true,
      category: 'Produits',
      description: '➖ Diminuer quantité',
      action: () => this.decrementQuantity()
    });
  }

  // ==========================================
  // MÉTHODES D'ACTION POUR LES RACCOURCIS
  // ==========================================

  private focusProductSearch(): void {
    setTimeout(() => {
      this.produitbox()?.getFocus();
    }, 0);
  }

  private focusQuantity(): void {
    setTimeout(() => {
      this.quantyBox()?.focusProduitControl();
    }, 0);
  }

  private addProductFromKeyboard(): void {
    if (this.produitSelected && this.quantyBox()?.value > 0) {
      this.addQuantity(this.quantyBox().value);
    }
  }

  private selectPaymentMode(mode: string): void {
    if (!this.currentSaleService.currentSale()) {
      this.showError(this.translateLabel('noSaleInProgress'));
      return;
    }

    if (this.isComptant()) {
      setTimeout(() => {
        const input = document.getElementById(mode) as HTMLInputElement;
        if (input) {
          input.focus();
          input.select();
        }
      }, 0);
    }
  }

  private completeSaleFromKeyboard(): void {
    if (!this.currentSaleService.currentSale()) {
      this.showError(this.translateLabel('noSaleInProgress'));
      return;
    }

    if (this.currentSaleService.currentSale().salesLines.length === 0) {
      this.showError(this.translateLabel('noProductsInSale'));
      return;
    }

    this.save();
  }

  private putSaleOnHold(): void {
    if (!this.currentSaleService.currentSale()) {
      this.showError(this.translateLabel('noSaleInProgress'));
      return;
    }

    if (this.isAssurance() || this.isCartnet()) {
      this.setEnAttenteAssurance();
    } else {
      this.comptantComponent()?.finalyseSale(true);
    }
  }

  private handleEscape(): void {
    if (this.showShortcutsModal()) {
      this.showShortcutsModal.set(false);
    } else if (this.pendingSalesSidebar) {
      this.pendingSalesSidebar = false;
    } else if (this.currentSaleService.currentSale()) {
      this.confimDialog().onConfirm(
        () => this.previousState(),
        'Annuler la vente',
        'Voulez-vous vraiment annuler cette vente ?'
      );
    }
  }

  private focusCustomerSearch(): void {
    if (this.isVoSale()) {
      setTimeout(() => {
        this.assuranceDataComponent()?.searchInput()?.nativeElement?.focus();
      }, 0);
    }
  }

  private switchToComptant(): void {
    if (this.active !== this.COMPTANT) {
      const event: any = { nextId: this.COMPTANT, preventDefault: () => {} };
      this.onNavChange(event);
    }
  }

  private switchToAssurance(): void {
    if (this.active !== this.ASSURANCE) {
      const event: any = { nextId: this.ASSURANCE, preventDefault: () => {} };
      this.onNavChange(event);
    }
  }

  private switchToCarnet(): void {
    if (this.active !== this.CARNET) {
      const event: any = { nextId: this.CARNET, preventDefault: () => {} };
      this.onNavChange(event);
    }
  }

  private openDiscountModal(): void {
    // Votre logique d'ouverture du modal remise
    this.showNotification('Ouverture modal remise');
  }

  private toggleShortcutsModal(): void {
    this.showShortcutsModal.update(v => !v);
  }

  private togglePrintTicket(): void {
    this.printTicket = !this.printTicket;
    this.showNotification(this.printTicket ? '✓ Impression ticket activée' : '✗ Impression ticket désactivée');
  }

  private removeLastProduct(): void {
    const sale = this.currentSaleService.currentSale();
    if (sale && sale.salesLines.length > 0) {
      const lastLine = sale.salesLines[sale.salesLines.length - 1];
      // Appeler votre méthode de suppression
      this.showNotification('Dernier produit supprimé');
    }
  }

  private incrementQuantity(): void {
    const currentQty = this.quantyBox()?.value || 1;
    this.quantyBox()?.reset(currentQty + 1);
  }

  private decrementQuantity(): void {
    const currentQty = this.quantyBox()?.value || 1;
    if (currentQty > 1) {
      this.quantyBox()?.reset(currentQty - 1);
    }
  }

  private showNotification(message: string): void {
    this.shortcutNotification.set(message);
  }

  // ==========================================
  // SAISIE RAPIDE (POS Standard)
  // ==========================================

  private isNumericKey(event: KeyboardEvent): boolean {
    return /^[0-9]$/.test(event.key) || event.key === '.' || event.key === ',';
  }

  private handleQuickEntry(event: KeyboardEvent): void {
    event.preventDefault();

    const currentBuffer = this.inputBuffer();
    this.inputBuffer.set(currentBuffer + event.key);

    if (this.bufferTimeout) {
      clearTimeout(this.bufferTimeout);
    }

    this.bufferTimeout = window.setTimeout(() => {
      this.inputBuffer.set('');
    }, 3000);
  }

  private handleOperatorKey(event: KeyboardEvent): void {
    event.preventDefault();
    const buffer = this.inputBuffer();

    switch(event.key) {
      case '*':
        // Multiplicateur: 3* = quantité 3
        if (buffer) {
          this.quantyBox()?.reset(parseInt(buffer));
          this.inputBuffer.set('');
          this.focusProductSearch();
        }
        break;

      case '%':
        // Remise en %: 10% = remise de 10%
        if (buffer) {
          this.applyDiscountPercent(parseFloat(buffer));
          this.inputBuffer.set('');
        }
        break;

      case '-':
        // Supprimer dernier article ou remise en montant
        if (buffer) {
          this.applyDiscountAmount(parseFloat(buffer));
          this.inputBuffer.set('');
        } else {
          this.removeLastProduct();
        }
        break;
    }
  }

  private applyDiscountPercent(percent: number): void {
    this.showNotification(`Remise ${percent}% appliquée`);
    // Votre logique de remise
  }

  private applyDiscountAmount(amount: number): void {
    this.showNotification(`Remise ${amount} appliquée`);
    // Votre logique de remise
  }

  // Helper pour formatter les raccourcis
  formatShortcutKey(shortcut: KeyboardShortcut): string {
    let key = '';
    if (shortcut.ctrl) key += 'Ctrl + ';
    if (shortcut.alt) key += 'Alt + ';
    if (shortcut.shift) key += 'Shift + ';
    key += shortcut.key.toUpperCase();
    return key;
  }

  getShortcutsByCategory(category: string) {
    return this.keyboardShortcutsService.getShortcutsByCategory(category);
  }

  // ... reste de votre code existant
}

*/
/*
// shortcuts-help-modal.component.ts
import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { KeyboardShortcut, KeyboardShortcutsService } from '../keyboard-shortcuts.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-shortcuts-help-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shortcuts-help-modal.component.html',
  styleUrls: ['./shortcuts-help-modal.component.scss']
})
export class ShortcutsHelpModalComponent {
  activeModal = inject(NgbActiveModal);
  private keyboardShortcutsService = inject(KeyboardShortcutsService);

  getShortcutsByCategory(category: string): KeyboardShortcut[] {
    return this.keyboardShortcutsService.getShortcutsByCategory(category);
  }

  formatShortcutKey(shortcut: KeyboardShortcut): string {
    let key = '';
    if (shortcut.ctrl) key += 'Ctrl + ';
    if (shortcut.alt) key += 'Alt + ';
    if (shortcut.shift) key += 'Shift + ';

    // Conversion des caractères spéciaux français
    const keyMap: Record<string, string> = {
      '&': '1', 'é': '2', '"': '3', "'": '4',
      '(': '5', '-': '6', 'è': '7', '_': '8',
      'ç': '9', 'à': '0'
    };

    const displayKey = keyMap[shortcut.key] || shortcut.key;
    key += displayKey.toUpperCase();
    return key;
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }
}



<!-- shortcuts-help-modal.component.html -->
<div class="modal-header shortcuts-modal-header">
  <h4 class="modal-title">
    <i class="pi pi-keyboard"></i>
    Guide des Raccourcis Clavier
  </h4>
  <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()"></button>
</div>

<div class="modal-body shortcuts-modal-body">
  <div class="shortcuts-help">
    <div class="shortcuts-intro">
      <p>
        <i class="pi pi-info-circle"></i>
        Raccourcis optimisés pour une utilisation rapide au comptoir
      </p>
    </div>

    <div class="shortcuts-grid">
      <!-- Produits & Recherche -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-search"></i> Produits & Recherche</h4>
        @for (shortcut of getShortcutsByCategory('Produits'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Paiement -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-money-bill"></i> Paiement</h4>
        @for (shortcut of getShortcutsByCategory('Paiement'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Transaction -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-check-circle"></i> Transaction</h4>
        @for (shortcut of getShortcutsByCategory('Transaction'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Types de Vente -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-shopping-cart"></i> Types de Vente</h4>
        @for (shortcut of getShortcutsByCategory('Types vente'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Client -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-user"></i> Client</h4>
        @for (shortcut of getShortcutsByCategory('Client'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Remises -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-percentage"></i> Remises</h4>
        @for (shortcut of getShortcutsByCategory('Remises'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Navigation -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-compass"></i> Navigation</h4>
        @for (shortcut of getShortcutsByCategory('Navigation'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>

      <!-- Impression -->
      <div class="shortcuts-section">
        <h4><i class="pi pi-print"></i> Impression</h4>
        @for (shortcut of getShortcutsByCategory('Impression'); track shortcut.key) {
          <div class="shortcut-item">
            <kbd class="shortcut-key">{{ formatShortcutKey(shortcut) }}</kbd>
            <span class="shortcut-desc">{{ shortcut.description }}</span>
          </div>
        }
      </div>
    </div>

    <!-- Guide de saisie rapide -->
    <div class="shortcuts-quick-guide">
      <h4>⚡ Saisie Rapide</h4>
      <ul>
        <li><kbd>3</kbd> puis <kbd>*</kbd> = Définir quantité à 3</li>
        <li><kbd>10</kbd> puis <kbd>%</kbd> = Remise 10%</li>
        <li><kbd>-</kbd> = Supprimer dernier article</li>
        <li>Saisie directe au pavé numérique pour la recherche rapide</li>
      </ul>
    </div>
  </div>
</div>

<div class="modal-footer shortcuts-modal-footer">
  <div class="shortcuts-tip">
    <i class="pi pi-lightbulb"></i>
    Appuyez sur <kbd>F1</kbd> à tout moment pour afficher cette aide
  </div>
  <button type="button" class="btn btn-secondary" (click)="dismiss()">
    <i class="pi pi-times"></i>
    Fermer
  </button>
</div>


// shortcuts-help-modal.component.scss

.shortcuts-modal-header {
  background: linear-gradient(to bottom, #6b9ab8, #5b89a6);
  color: white;
  border-bottom: none;

  .modal-title {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    font-weight: 600;
    font-size: 1.25rem;

    i {
      font-size: 1.5rem;
    }
  }

  .btn-close {
    filter: brightness(0) invert(1);
    opacity: 0.8;

    &:hover {
      opacity: 1;
    }
  }
}

.shortcuts-modal-body {
  padding: 1.5rem;
  background: #f8f9fa;
  max-height: 70vh;
  overflow-y: auto;
}

.shortcuts-help {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
}

.shortcuts-intro {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #5b89a6;
  margin-bottom: 1.5rem;

  p {
    margin: 0;
    color: #1565c0;
    font-size: 0.95rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;

    i {
      font-size: 1.1rem;
    }
  }
}

.shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-bottom: 1.5rem;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.shortcuts-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid #e9ecef;

  h4 {
    color: #5b89a6;
    font-size: 0.95rem;
    font-weight: 600;
    margin: 0 0 1rem 0;
    padding-bottom: 0.5rem;
    border-bottom: 2px solid #e5e7eb;
    display: flex;
    align-items: center;
    gap: 0.5rem;

    i {
      font-size: 1rem;
    }
  }
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.shortcut-key {
  background: linear-gradient(to bottom, #ffffff, #f5f7fa);
  border: 1px solid #dfe3e8;
  border-radius: 4px;
  padding: 4px 10px;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  color: #333333;
  box-shadow: 0 2px 0 #cbd5e0,
              0 1px 2px rgba(0, 0, 0, 0.1);
  text-transform: uppercase;
  white-space: nowrap;
  min-width: 60px;
  text-align: center;
}

.shortcut-desc {
  color: #6c757d;
  font-size: 0.9rem;
  text-align: right;
  flex: 1;
  margin-left: 1rem;
}

.shortcuts-quick-guide {
  background: linear-gradient(135deg, #fff3cd 0%, #ffe69c 100%);
  border-left: 4px solid #ffc107;
  border-radius: 8px;
  padding: 1rem 1.5rem;

  h4 {
    color: #856404;
    font-size: 1rem;
    font-weight: 600;
    margin: 0 0 1rem 0;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      padding: 0.5rem 0;
      color: #856404;
      display: flex;
      align-items: center;
      gap: 0.5rem;

      kbd {
        font-size: 0.85rem;
        padding: 4px 8px;
        background: #fff;
        border: 1px solid #ffc107;
        border-radius: 4px;
        color: #856404;
        font-weight: 600;
      }
    }
  }
}

.shortcuts-modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-top: 1px solid #dee2e6;
  padding: 1rem 1.5rem;

  @media (max-width: 576px) {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}

.shortcuts-tip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #6c757d;
  font-size: 0.9rem;

  i {
    color: #f59e0b;
    font-size: 1.1rem;
  }

  kbd {
    font-size: 0.8rem;
    padding: 2px 6px;
    background: #e9ecef;
    border: 1px solid #dee2e6;
    border-radius: 3px;
    color: #495057;
  }
}

// Animation de scroll
.shortcuts-modal-body {
  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #5b89a6;
    border-radius: 4px;

    &:hover {
      background: #4a7890;
    }
  }
}



// selling-home.component.ts
import { Component, OnInit, AfterViewInit, HostListener, signal, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ShortcutsHelpModalComponent } from './shortcuts-help-modal/shortcuts-help-modal.component';
import { KeyboardShortcutsService } from './keyboard-shortcuts.service';

@Component({
  selector: 'jhi-selling-home',
  // ... vos imports existants
  templateUrl: './selling-home.component.html',
  styleUrl: './selling-home.component.scss'
})
export class SellingHomeComponent implements OnInit, AfterViewInit {
  // ... vos propriétés existantes

  shortcutNotification = signal('');
  private inputBuffer = signal('');
  private bufferTimeout?: number;

  private readonly keyboardShortcutsService = inject(KeyboardShortcutsService);
  private readonly modalService = inject(NgbModal);

  constructor() {
    // ... votre constructeur existant

    // Effect pour les notifications
    effect(() => {
      if (this.shortcutNotification()) {
        if (this.bufferTimeout) {
          clearTimeout(this.bufferTimeout);
        }
        this.bufferTimeout = window.setTimeout(() => {
          this.shortcutNotification.set('');
        }, 2000);
      }
    });
  }

  ngOnInit(): void {
    // ... votre code existant

    // Enregistrer les raccourcis
    this.registerKeyboardShortcuts();

    // S'abonner aux notifications
    this.keyboardShortcutsService.getShortcutTriggered$()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(description => {
        this.shortcutNotification.set(description);
      });
  }

  ngOnDestroy(): void {
    this.keyboardShortcutsService.clearAll();
    if (this.bufferTimeout) {
      clearTimeout(this.bufferTimeout);
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    const isInputField = target.tagName === 'INPUT' ||
                        target.tagName === 'TEXTAREA' ||
                        target.isContentEditable;

    // Touches fonctionnelles
    const functionalKeys = ['F1', 'F2', 'F3', 'F4', 'F6', 'F7', 'F8', 'F9', 'F10', 'F11', 'F12'];
    const isFunctionalKey = functionalKeys.includes(event.key);

    // Combinaisons Alt
    const isAltCombo = event.altKey && !event.ctrlKey && !event.shiftKey;

    // Saisie rapide
    if (!isInputField) {
      if (this.isNumericKey(event)) {
        this.handleQuickEntry(event);
        return;
      }

      if (event.key === '*' || event.key === '%' || event.key === '-') {
        this.handleOperatorKey(event);
        return;
      }
    }

    const shouldHandle = !isInputField || isFunctionalKey || isAltCombo || event.key === 'Escape';

    if (shouldHandle) {
      this.keyboardShortcutsService.handleKeyboardEvent(event);
    }
  }

  private registerKeyboardShortcuts(): void {
    // ==========================================
    // AIDE
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F1',
      category: 'Aide',
      description: '❓ Aide - Raccourcis clavier',
      action: () => this.openShortcutsModal()
    });

    // ==========================================
    // RECHERCHE & PRODUITS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F2',
      category: 'Produits',
      description: '🔍 Rechercher produit',
      action: () => this.focusProductSearch()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F3',
      category: 'Produits',
      description: '📦 Modifier quantité',
      action: () => this.focusQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F4',
      category: 'Produits',
      description: '➕ Ajouter produit',
      action: () => this.addProductFromKeyboard()
    });

    // ==========================================
    // PAIEMENTS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F6',
      category: 'Paiement',
      description: '💵 Espèces',
      action: () => this.selectPaymentMode('ESPECE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F7',
      category: 'Paiement',
      description: '💳 Carte Bancaire',
      action: () => this.selectPaymentMode('CB')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F8',
      category: 'Paiement',
      description: '📝 Chèque',
      action: () => this.selectPaymentMode('CHEQUE')
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F11',
      category: 'Paiement',
      description: '🏦 Virement',
      action: () => this.selectPaymentMode('VIREMENT')
    });

    // ==========================================
    // FINALISATION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F9',
      category: 'Transaction',
      description: '✓ Finaliser la vente',
      action: () => this.completeSaleFromKeyboard()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'F10',
      category: 'Transaction',
      description: '⏸️ Mettre en attente',
      action: () => this.putSaleOnHold()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'Escape',
      category: 'Transaction',
      description: '❌ Annuler / Retour',
      action: () => this.handleEscape()
    });

    // ==========================================
    // CLIENT
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'F12',
      category: 'Client',
      description: '👤 Rechercher client',
      action: () => this.focusCustomerSearch()
    });

    // ==========================================
    // TYPES DE VENTE
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: '1',
      alt: true,
      category: 'Types vente',
      description: 'Vente Comptant',
      action: () => this.switchToComptant()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '2',
      alt: true,
      category: 'Types vente',
      description: 'Vente Assurance',
      action: () => this.switchToAssurance()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '3',
      alt: true,
      category: 'Types vente',
      description: 'Vente Carnet',
      action: () => this.switchToCarnet()
    });

    // ==========================================
    // REMISES
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'r',
      alt: true,
      category: 'Remises',
      description: '💸 Appliquer remise',
      action: () => this.openDiscountModal()
    });

    // ==========================================
    // NAVIGATION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'n',
      alt: true,
      category: 'Navigation',
      description: '🆕 Nouvelle vente',
      action: () => this.resetAll()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: 'v',
      alt: true,
      category: 'Navigation',
      description: '📋 Ventes en attente',
      action: () => this.openPindingSide()
    });

    // ==========================================
    // IMPRESSION
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'i',
      alt: true,
      category: 'Impression',
      description: '🖨️ Toggle impression ticket',
      action: () => this.togglePrintTicket()
    });

    // ==========================================
    // GESTION PRODUITS
    // ==========================================

    this.keyboardShortcutsService.registerShortcut({
      key: 'Delete',
      alt: true,
      category: 'Produits',
      description: '🗑️ Supprimer dernier produit',
      action: () => this.removeLastProduct()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '+',
      alt: true,
      category: 'Produits',
      description: '➕ Augmenter quantité',
      action: () => this.incrementQuantity()
    });

    this.keyboardShortcutsService.registerShortcut({
      key: '-',
      alt: true,
      category: 'Produits',
      description: '➖ Diminuer quantité',
      action: () => this.decrementQuantity()
    });
  }

  // ==========================================
  // MÉTHODES POUR LES RACCOURCIS
  // ==========================================

  private openShortcutsModal(): void {
    this.modalService.open(ShortcutsHelpModalComponent, {
      size: 'xl',
      centered: true,
      scrollable: true,
      backdrop: 'static'
    });
  }

  // ... le reste de vos méthodes d'action
  // (focusProductSearch, focusQuantity, addProductFromKeyboard, etc.)
  // Comme dans la version précédente

  // ... votre code existant
}

<!-- À la fin de votre template -->

<!-- Notification des raccourcis -->
@if (shortcutNotification()) {
  <div class="shortcut-notification">
    <i class="pi pi-check-circle"></i>
    {{ shortcutNotification() }}
  </div>
}

<!-- Bouton d'aide flottant -->
<button
  class="help-button pharma-btn"
  (click)="openShortcutsModal()"
  title="Raccourcis clavier (F1)"
  type="button">
  <i class="pi pi-question"></i>
</button>


// Notification
.shortcut-notification {
  position: fixed;
  top: 80px;
  right: 20px;
  background: linear-gradient(135deg, #5cb85c 0%, #4cae4c 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
  animation: slideInRight 0.3s ease-out;

  i {
    font-size: 1.2rem;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

// Bouton d'aide flottant
.help-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5b89a6, #4a7890);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  transition: all 0.3s ease;
  z-index: 1000;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  }

  i {
    font-size: 1.3rem;
  }
}

@media (max-width: 768px) {
  .help-button {
    bottom: 10px;
    right: 10px;
    width: 45px;
    height: 45px;
  }
}
 */
