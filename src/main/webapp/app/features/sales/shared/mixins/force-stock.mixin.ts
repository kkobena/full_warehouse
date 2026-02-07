import { Signal, WritableSignal, effect } from '@angular/core';
import { NgxSpinnerService } from 'ngx-spinner';
import { ISalesLine, ISales } from '../../../../shared/model';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { AuthorizationService } from '../../data-access/services/authorization.service';

/**
 * Détails d'erreur pour le forçage de stock
 */
export interface StockErrorDetails {
  errorKey: string | null;
  attemptedLine?: ISalesLine;
  isFromTableCellEdit?: boolean;
}

/**
 * Type pour le contexte du forçage de stock
 */
export type ForceStockContext = 'addProduct' | 'editCell' | null;

/**
 * Configuration pour le mixin de gestion du forçage de stock
 */
export interface ForceStockConfig {
  saleType: 'COMPTANT' | 'CARNET' | 'ASSURANCE';
}

/**
 * Interface pour le composant de dialogue de confirmation
 */
export interface ConfirmDialogHost {
  onConfirm(onConfirm: () => void, title: string, message: string, customButtons?: unknown, onCancel?: () => void): void;
}

/**
 * Fonctions de création de vente spécifiques au type de vente
 */
export interface ForceStockSaleOperations {
  createSale: (line: ISalesLine) => void;
  addProduct: (line: ISalesLine) => void;
}

/**
 * Contexte partagé pour les opérations de forçage de stock
 */
export interface ForceStockHandlingContext {
  facade: SalesFacade;
  authorizationService: AuthorizationService;
  spinner: NgxSpinnerService;
  config: ForceStockConfig;
  // Signals
  currentSale: Signal<ISales | null>;
  loading: Signal<boolean>;
  lastError: Signal<string | null>;
  // Writable signals for state management
  waitingForForceStockSuccess: WritableSignal<boolean>;
  previousLoadingState: WritableSignal<boolean>;
  forceStockContext: WritableSignal<ForceStockContext>;
  // Host functions
  getConfirmDialog: () => ConfirmDialogHost;
  resetProductSelection: () => void;
  // Operations spécifiques au type de vente
  operations: ForceStockSaleOperations;
}

/**
 * Mixin pour la gestion du forçage de stock dans les composants de vente
 *
 * Fournit les méthodes et effects communs pour :
 * - Détection des erreurs de stock
 * - Dialogue de confirmation de forçage
 * - Gestion du succès/échec après forçage
 * - Gestion du spinner de chargement
 *
 * @example
 * ```typescript
 * // Dans le composant
 * private forceStockHandling = createForceStockHandling({
 *   facade: this.facade,
 *   authorizationService: this.authorizationService,
 *   spinner: this.spinner,
 *   config: { saleType: 'COMPTANT' },
 *   currentSale: this.facade.currentSale,
 *   loading: this.facade.loading,
 *   lastError: this.facade.lastError,
 *   waitingForForceStockSuccess: this.waitingForForceStockSuccess,
 *   previousLoadingState: this.previousLoadingState,
 *   forceStockContext: this.forceStockContext,
 *   getConfirmDialog: () => this.confirmDialog(),
 *   resetProductSelection: () => this.resetProductSelection(),
 *   operations: {
 *     createSale: (line) => this.facade.createComptantSale(line),
 *     addProduct: (line) => this.facade.onAddProduit(line),
 *   },
 * });
 *
 * // Dans le constructor
 * constructor() {
 *   this.forceStockHandling.initializeEffects();
 * }
 * ```
 */
export function createForceStockHandling(context: ForceStockHandlingContext) {
  const {
    facade,
    authorizationService,
    spinner,
    currentSale,
    loading,
    lastError,
    waitingForForceStockSuccess,
    previousLoadingState,
    forceStockContext,
  } = context;

  /**
   * Gère l'erreur de stock insuffisant avec option de forçage
   */
  function handleStockError(errorDetails: StockErrorDetails): void {
    const isFromTableEdit = errorDetails.isFromTableCellEdit === true;
    const detectedContext: ForceStockContext = isFromTableEdit ? 'editCell' : 'addProduct';
    forceStockContext.set(detectedContext);

    context.getConfirmDialog().onConfirm(
      () => onForceStockConfirmed(errorDetails, detectedContext),
      'Forcer le stock',
      'La quantité saisie est supérieure à la quantité stock du produit. Voulez-vous continuer ?',
      undefined,
      () => onForceStockCancelled(),
    );
  }

  /**
   * Callback appelé quand l'utilisateur confirme le forçage de stock
   */
  function onForceStockConfirmed(errorDetails: StockErrorDetails, detectedContext: ForceStockContext): void {
    if (!errorDetails.attemptedLine) return;

    errorDetails.attemptedLine.forceStock = true;
    waitingForForceStockSuccess.set(true);

    if (detectedContext === 'editCell') {
      facade.updateItemQtyRequestedWithSet(errorDetails.attemptedLine);
    } else if (errorDetails.attemptedLine.id) {
      facade.updateItemQtyRequested(errorDetails.attemptedLine);
    } else {
      const sale = currentSale();
      if (!sale?.saleId) {
        context.operations.createSale(errorDetails.attemptedLine);
      } else {
        context.operations.addProduct(errorDetails.attemptedLine);
      }
    }
  }

  /**
   * Callback appelé quand l'utilisateur annule le forçage de stock
   * Note: Le reset du produit sélectionné est géré via souscription à saleReloadedSuccess$ dans le composant
   */
  function onForceStockCancelled(): void {
    facade.clearError();
    const ctx = forceStockContext();
    forceStockContext.set(null);

    if (ctx === 'editCell') {
      const sale = currentSale();
      if (sale?.saleId) {
        facade.loadSaleForEdit(sale.saleId);
      }
      // Reset géré via souscription à facade.saleReloadedSuccess$ dans le composant
    } else {
      context.resetProductSelection();
    }
  }

  /**
   * Crée l'effect pour observer les erreurs et gérer le forçage de stock
   * Note: Seules les erreurs de stock sont gérées ici. Les autres erreurs sont
   * gérées par le composant via son propre effet.
   */
  function setupErrorHandlingEffect(): void {
    effect(() => {
      const errorMsg = lastError();
      const errorDetails = facade.errorDetails();
      const waiting = waitingForForceStockSuccess();

      // Si on attend le résultat du forçage, ignorer pour éviter de montrer le dialog en double
      if (waiting) return;

      // Seules les erreurs de stock avec permission de forçage sont gérées ici
      if (errorMsg && errorDetails?.errorKey === 'stock' && authorizationService.canForceStock()) {
        handleStockError(errorDetails);
      }
      // Les autres erreurs sont gérées par le composant
    });
  }

  /**
   * Crée l'effect pour détecter le succès après forçage de stock
   * Note: Le reset du produit sélectionné est géré via souscription à productAddedSuccess$/lineUpdatedSuccess$ dans le composant
   */
  function setupForceStockSuccessEffect(): void {
    effect(() => {
      const isLoading = loading();
      const previousLoading = previousLoadingState();
      const waiting = waitingForForceStockSuccess();

      if (!waiting) {
        if (previousLoading !== isLoading) {
          previousLoadingState.set(isLoading);
        }
        return;
      }

      previousLoadingState.set(isLoading);

      // Succès: loading passe de true à false ET pas d'erreur
      if (previousLoading && !isLoading && !facade.errorDetails()) {
        waitingForForceStockSuccess.set(false);
        facade.clearError();
        forceStockContext.set(null);
        // Reset géré via souscription à facade.productAddedSuccess$ ou facade.lineUpdatedSuccess$ dans le composant
      }
    });
  }

  /**
   * Crée l'effect pour contrôler le spinner selon l'état loading
   */
  function setupSpinnerEffect(): void {
    effect(() => {
      loading() ? spinner.show() : spinner.hide();
    });
  }

  /**
   * Initialise tous les effects liés à la gestion du stock
   * À appeler dans le constructor du composant
   */
  function initializeEffects(): void {
    setupErrorHandlingEffect();
    setupForceStockSuccessEffect();
    setupSpinnerEffect();
  }

  return {
    handleStockError,
    onForceStockConfirmed,
    onForceStockCancelled,
    setupErrorHandlingEffect,
    setupForceStockSuccessEffect,
    setupSpinnerEffect,
    initializeEffects,
  };
}

/**
 * Type retourné par createForceStockHandling
 */
export type ForceStockHandling = ReturnType<typeof createForceStockHandling>;
