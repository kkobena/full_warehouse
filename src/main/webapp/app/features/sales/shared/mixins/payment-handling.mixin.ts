import { Signal } from '@angular/core';
import { Observable } from 'rxjs';
import { IPayment } from '../../../../shared/model/payment.model';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { ISales } from '../../../../shared/model';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { NotificationService } from '../../../../shared/services/notification.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';

/**
 * Structure d'un paiement entrant depuis l'événement de paiement
 */
export interface PaymentEntry {
  mode: IPaymentMode;
  amount: number;
  amountEntered?: number;
}

/**
 * Interface pour le composant de mode de paiement
 */
export interface PaymentModeHost {
  selectedModes(): Array<{ mode: IPaymentMode; amount: number; amountEntered?: number }>;
  totalPaid(): number;
  changeAmount(): number;
  changeExact(): number;
  focusFirstMode(): void;
}

/**
 * Configuration pour le mixin de gestion des paiements
 */
export interface PaymentHandlingConfig {
  saleType: 'COMPTANT' | 'CARNET' | 'ASSURANCE';
  /** Seuil de tolérance pour considérer qu'il y a un reste à payer (ex: 5 FCFA) */
  toleranceThreshold?: number;
  /** Permet la vente différée */
  allowDiffere?: boolean;
}

/**
 * Contexte partagé pour les opérations de paiement
 */
export interface PaymentHandlingContext {
  facade: SalesFacade;
  notificationService: NotificationService;
  customerDisplay: CustomerDisplayService;
  config: PaymentHandlingConfig;
  // Signals
  currentSale: Signal<ISales | null>;
  salesLines: Signal<unknown[]>;
  canSave: Signal<boolean>;
  isCashRegisterOpen: Signal<boolean>;
  // Fonctions de l'hôte
  getPaymentModeComponent: () => PaymentModeHost | undefined;
  openCashRegister: () => void;
  resetForNewSale: () => void;
  // Callbacks optionnels
  onDiffereConfirmed?: () => void;
  onPaymentSuccess?: () => void;
  showConfirmDialog: (onConfirm: () => void, title: string, message: string, onCancel?: () => void) => void;
  /**
   * Fonction de sauvegarde personnalisée (optionnel)
   * Si fournie, sera utilisée à la place de facade.saveSale()
   * Utile pour ASSURANCE qui utilise facade.saveAssuranceSale(payments)
   */
  customSaveSale?: (payments: IPayment[]) => Observable<ISales | null>;
}

/**
 * Seuil de tolérance par défaut pour considérer qu'il y a un reste à payer
 * Si le reste est <= 5 FCFA, on considère que le paiement est complet
 */
const DEFAULT_TOLERANCE_THRESHOLD = 5;

/**
 * Mixin pour la gestion des paiements dans les composants de vente
 *
 * Fournit les méthodes communes pour :
 * - Conversion des paiements
 * - Validation des montants
 * - Gestion de la vente différée
 * - Finalisation sans paiement
 *
 * @example
 * ```typescript
 * // Dans le composant
 * private paymentHandling = createPaymentHandling({
 *   facade: this.facade,
 *   notificationService: this.notificationService,
 *   customerDisplay: this.customerDisplay,
 *   config: { saleType: 'COMPTANT', allowDiffere: true },
 *   currentSale: this.facade.currentSale,
 *   salesLines: this.facade.salesLines,
 *   canSave: this.canSave,
 *   isCashRegisterOpen: this.isCashRegisterOpen,
 *   getPaymentModeComponent: () => this.paymentMode(),
 *   openCashRegister: () => this.openCashRegister(),
 *   resetForNewSale: () => this.resetForNewSale(),
 *   showConfirmDialog: (onConfirm, title, message, onCancel) =>
 *     this.confirmDialog().onConfirm(onConfirm, title, message, undefined, onCancel),
 * });
 *
 * // Utilisation
 * onPaymentComplete(event: PaymentCompleteEvent): void {
 *   this.paymentHandling.processPayment(event);
 * }
 * ```
 */
export function createPaymentHandling(context: PaymentHandlingContext) {
  const { facade, notificationService, customerDisplay, config, currentSale, salesLines, canSave, isCashRegisterOpen } = context;
  const toleranceThreshold = config.toleranceThreshold ?? DEFAULT_TOLERANCE_THRESHOLD;

  /**
   * Convertit les paiements de PaymentCompleteEvent au format IPayment[]
   */
  function convertPayments(eventPayments: PaymentEntry[]): IPayment[] {
    return eventPayments.map(p => ({
      paymentMode: p.mode,
      paidAmount: p.amount,
      montantVerse: p.amountEntered || p.amount,
      netAmount: p.amount,
    }));
  }

  /**
   * Construit un événement de paiement depuis le composant payment-mode
   */
  function buildPaymentEvent(): PaymentCompleteEvent | null {
    const paymentModeComp = context.getPaymentModeComponent();
    if (!paymentModeComp) {
      notificationService.error('Erreur', 'Composant de paiement non disponible');
      return null;
    }

    const selectedModes = paymentModeComp.selectedModes();
    if (!selectedModes || selectedModes.length === 0) {
      return {
        payments: [],
        totalPaid: 0,
        change: 0,
        changeExact: 0,
        printReceipt: false,
        printInvoice: false,
      };
    }

    return {
      payments: selectedModes.map(entry => ({
        mode: entry.mode,
        amount: entry.amount || 0,
        amountEntered: entry.amountEntered,
      })),
      totalPaid: paymentModeComp.totalPaid(),
      change: paymentModeComp.changeAmount(),
      changeExact: paymentModeComp.changeExact(),
      printReceipt: false,
      printInvoice: false,
    };
  }

  /**
   * Vérifie si la vente peut être sauvegardée
   */
  function validateSaleForSave(): boolean {
    const sale = currentSale();
    if (!sale || !canSave()) {
      notificationService.warning('Vente invalide', "Veuillez ajouter au moins un produit avant d'enregistrer la vente");
      return false;
    }
    return true;
  }

  /**
   * Finalise la vente sans paiement (pour les ventes dont le montant à payer <= 0)
   * Utilisé principalement pour CARNET et ASSURANCE
   */
  function finalizeSaleWithoutPayment(): void {
    const sale = currentSale();
    if (!sale) return;

    // Montant entièrement couvert
    sale.montantVerse = 0;
    sale.payments = [];

    // Vérifier si la caisse est ouverte avant de finaliser
    if (!isCashRegisterOpen()) {
      context.openCashRegister();
    } else {
      completeSale();
    }
  }

  /**
   * Complète la vente après validation
   * Utilise customSaveSale si fourni, sinon facade.saveSale()
   */
  function completeSale(): void {
    const sale = currentSale();
    const payments = sale?.payments || [];

    // Utiliser la fonction de sauvegarde personnalisée si fournie
    const saveObservable = context.customSaveSale ? context.customSaveSale(payments) : facade.saveSale();

    saveObservable.subscribe({
      next: result => {
        if (result) {
          customerDisplay.clear();
          context.resetForNewSale();
          context.onPaymentSuccess?.();
        }
      },
      error: () => {
        // L'erreur est déjà gérée par le facade
      },
    });
  }

  /**
   * Calcule le reste à payer après les paiements
   */
  function calculateRestToPay(amountToBePaid: number, totalPaid: number): number {
    return amountToBePaid - totalPaid;
  }

  /**
   * Vérifie si le reste à payer dépasse le seuil de tolérance
   */
  function hasSignificantRestToPay(restToPay: number): boolean {
    return restToPay > toleranceThreshold;
  }

  /**
   * Formate un montant pour l'affichage
   */
  function formatAmount(amount: number): string {
    return amount.toLocaleString();
  }

  /**
   * Construit le message HTML pour la confirmation de vente différée
   */
  function buildDiffereConfirmMessage(amountToBePaid: number, montantVerse: number, restToPay: number): string {
    return `
      <div>Le montant versé est inférieur au montant dû.</div><br>
      <div class="text-end mb-2">
        Montant dû : <span class="fs-5 badge rounded-pill bg-danger-subtle text-danger-emphasis"><b>${formatAmount(amountToBePaid)} FCFA</b></span>
      </div>
      <div class="text-end mb-2">
        Montant versé : <span class="fs-5 badge rounded-pill bg-primary-subtle text-primary-emphasis"><b>${formatAmount(montantVerse)} FCFA</b></span>
      </div>
      <div class="text-end mb-2">
        Reste à payer : <span class="fs-5 badge rounded-pill bg-warning-subtle text-warning-emphasis"><b>${formatAmount(restToPay)} FCFA</b></span>
      </div><br>
      <div>Voulez-vous régler le reste en différé ?</div>
    `;
  }

  /**
   * Traite le paiement et gère la vente différée si nécessaire
   */
  function processPayment(event: PaymentCompleteEvent): void {
    const sale = currentSale();
    if (!sale) {
      notificationService.warning('Erreur', 'Aucune vente en cours');
      return;
    }

    // Appliquer les paiements
    sale.montantVerse = event.totalPaid || 0;
    sale.payments = convertPayments(event.payments);

    // Stocker les montants de monnaie
    sale.montantRendu = event.changeExact || 0;
    sale.montantRenduArrondi = event.change || 0;

    // Gérer vente différée si montant insuffisant
    const amountToBePaid = sale.amountToBePaid || 0;
    const restToPay = calculateRestToPay(amountToBePaid, sale.montantVerse);

    if (config.allowDiffere && hasSignificantRestToPay(restToPay) && !sale.differe) {
      // Proposer vente différée
      const message = buildDiffereConfirmMessage(amountToBePaid, sale.montantVerse, restToPay);

      context.showConfirmDialog(
        () => {
          sale.differe = true;
          context.onDiffereConfirmed?.();
        },
        'Vente différée',
        message,
        () => {
          // Annulé - remettre focus sur input règlement
          setTimeout(() => {
            context.getPaymentModeComponent()?.focusFirstMode();
          }, 100);
        },
      );
    } else {
      // Montant suffisant ou déjà différé → Finaliser
      if (!isCashRegisterOpen()) {
        context.openCashRegister();
      } else {
        completeSale();
      }
    }
  }

  /**
   * Gère la sauvegarde de la vente (appelée par le bouton Enregistrer)
   */
  function onSave(): void {
    if (!validateSaleForSave()) {
      return;
    }

    const sale = currentSale();
    if (!sale) return;

    const amountToBePaid = sale.amountToBePaid || 0;

    // Si montant à payer <= 0, finaliser sans paiement
    if (amountToBePaid <= 0) {
      finalizeSaleWithoutPayment();
      return;
    }

    // Construire l'événement de paiement depuis le composant
    const paymentEvent = buildPaymentEvent();
    if (paymentEvent) {
      processPayment(paymentEvent);
    }
  }

  return {
    convertPayments,
    buildPaymentEvent,
    validateSaleForSave,
    finalizeSaleWithoutPayment,
    completeSale,
    calculateRestToPay,
    hasSignificantRestToPay,
    formatAmount,
    buildDiffereConfirmMessage,
    processPayment,
    onSave,
  };
}

/**
 * Type retourné par createPaymentHandling
 */
export type PaymentHandling = ReturnType<typeof createPaymentHandling>;
