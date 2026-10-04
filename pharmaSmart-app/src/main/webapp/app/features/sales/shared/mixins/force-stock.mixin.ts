import { NgZone, Signal, WritableSignal, effect, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ISalesLine, ISales, MotifForcageStock, ProduitSearch } from '../../../../shared/model';
import {
  ChoixStockInsuffisant,
  ForceStockChoiceModalComponent,
} from "../../ui";
import { SubstitutionComptoirService } from '../../data-access/services/substitution-comptoir.service';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { AuthorizationService } from '../../data-access/services/authorization.service';

/**
 * Détails d'erreur pour le forçage de stock
 */
export interface StockErrorDetails {
  errorKey: string | null;
  attemptedLine?: ISalesLine;
  isFromTableCellEdit?: boolean;
  /** Informations de stock réserve — présent uniquement quand errorKey === 'stock.reserve.available' */
  reserveInfo?: { rayonStock: number; reserveStock: number; totalAvailable: number };
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
  config: ForceStockConfig;
  // Signals
  currentSale: Signal<ISales | null>;
  loading: Signal<boolean>;
  lastError: Signal<string | null>;
  // Writable signals for state management
  waitingForForceStockSuccess: WritableSignal<boolean>;
  forceStockContext: WritableSignal<ForceStockContext>;
  // Host functions
  getConfirmDialog: () => ConfirmDialogHost;
  resetProductSelection: () => void;
  // Operations spécifiques au type de vente
  operations: ForceStockSaleOperations;
  /**
   * Ajoute un équivalent choisi à la place du produit en rupture, avec la quantité demandée. Fourni, il fait apparaître l'option
   * « Proposer un équivalent disponible » dans le modal de stock insuffisant ; absent, l'option n'est pas offerte.
   */
  ajouterSubstitut?: (produit: ProduitSearch, quantite: number) => void;
  substitution?: SubstitutionComptoirService;
  // Injectés par défaut ; fournis explicitement par les tests
  modalService?: NgbModal;
  zone?: NgZone;
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
    currentSale,
    loading,
    lastError,
    waitingForForceStockSuccess,
    forceStockContext,
  } = context;
  const modalService = context.modalService ?? inject(NgbModal);
  const zone = context.zone ?? inject(NgZone);
  const substitution = context.substitution ?? inject(SubstitutionComptoirService);

  /**
   * Stock insuffisant : le caissier choisit entre rupture (avoir) et écart d'inventaire,
   * chaque option n'étant offerte qu'avec son privilège.
   */
  function handleStockError(errorDetails: StockErrorDetails): void {
    const isFromTableEdit = errorDetails.isFromTableCellEdit === true;
    const detectedContext: ForceStockContext = isFromTableEdit ? 'editCell' : 'addProduct';
    forceStockContext.set(detectedContext);

    // NgZone.run : ouverte depuis un effect(), la modale doit déclencher la détection de changements
    zone.run(() => {
      const modalRef = modalService.open(ForceStockChoiceModalComponent, { backdrop: 'static', centered: true });
      const choix = modalRef.componentInstance as ForceStockChoiceModalComponent;
      choix.produitLibelle = errorDetails.attemptedLine?.produitLibelle;
      choix.quantiteDemandee = errorDetails.attemptedLine?.quantityRequested;
      choix.canRupture = authorizationService.canForceStock();
      choix.canEcart = authorizationService.canRegulariserStock();
      // Un équivalent remplace un produit qu'on s'apprête à ajouter ; modifier la quantité d'une ligne existante n'a pas de sens ici.
      const ligne = errorDetails.attemptedLine;
      choix.canSubstituer = !!context.ajouterSubstitut && detectedContext === 'addProduct' && !ligne?.id && ligne?.produitId != null;
      modalRef.result.then(
        (choisi: ChoixStockInsuffisant) =>
          choisi === 'SUBSTITUER' ? onSubstituer(errorDetails) : onForceStockConfirmed(errorDetails, detectedContext, choisi),
        () => onForceStockCancelled(),
      );
    });
  }

  /**
   * Le caissier renonce au produit en rupture : l'ajout n'a pas eu lieu, rien n'est à défaire. On liste les équivalents en stock ;
   * celui qu'il choisit est ajouté avec la quantité demandée, par le circuit ordinaire (donc avec ses contrôles : stock, allergie).
   * Sans choix, on revient à l'écran comme après un « Annuler ».
   */
  function onSubstituer(errorDetails: StockErrorDetails): void {
    const ligne = errorDetails.attemptedLine;
    facade.clearError();
    forceStockContext.set(null);
    if (!ligne?.produitId || !context.ajouterSubstitut) {
      context.resetProductSelection();
      return;
    }
    substitution
      .choisir({ id: ligne.produitId, libelle: ligne.produitLibelle ?? '', prixUnitaire: ligne.regularUnitPrice, quantite: ligne.quantityRequested })
      .subscribe(produit => {
        if (produit) {
          context.ajouterSubstitut?.(produit, ligne.quantityRequested ?? 1);
        } else {
          context.resetProductSelection();
        }
      });
  }

  /**
   * Callback appelé quand l'utilisateur confirme le forçage de stock.
   * Sans motif (transfert depuis la réserve), le serveur traite le reliquat en rupture.
   */
  function onForceStockConfirmed(errorDetails: StockErrorDetails, detectedContext: ForceStockContext, motif?: MotifForcageStock): void {
    if (!errorDetails.attemptedLine) return;

    errorDetails.attemptedLine.forceStock = true;
    if (motif) {
      errorDetails.attemptedLine.motifForcage = motif;
    }
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
   * Gère le cas où le stock rayon est insuffisant mais de la réserve est disponible.
   * Propose un transfert implicite réserve → rayon (forceStock=true côté backend).
   */
  function handleReserveStockError(errorDetails: StockErrorDetails): void {
    const isFromTableEdit = errorDetails.isFromTableCellEdit === true;
    const detectedContext: ForceStockContext = isFromTableEdit ? 'editCell' : 'addProduct';
    forceStockContext.set(detectedContext);

    const info = errorDetails.reserveInfo;
    const message = info
      ? `Stock rayon insuffisant (rayon\u00a0: ${info.rayonStock}, réserve\u00a0: ${info.reserveStock}, total\u00a0: ${info.totalAvailable}). Effectuer un transfert réserve → rayon et continuer\u00a0?`
      : 'Stock insuffisant en rayon. De la réserve est disponible. Effectuer un transfert réserve → rayon et continuer\u00a0?';

    context.getConfirmDialog().onConfirm(
      () => onForceStockConfirmed(errorDetails, detectedContext),
      'Stock en réserve',
      message,
      undefined,
      () => onForceStockCancelled(),
    );
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

      if (!errorMsg || !errorDetails) return;
      if (errorDetails.errorKey === 'stock' && (authorizationService.canForceStock() || authorizationService.canRegulariserStock())) {
        handleStockError(errorDetails);
      } else if (errorDetails.errorKey === 'stock.reserve.available' && authorizationService.canForceStock()) {
        handleReserveStockError(errorDetails);
      }
      // Les autres erreurs sont gérées par le composant
    });
  }

  /**
   * Crée l'effect pour détecter le succès après forçage de stock
   * Note: Le reset du produit sélectionné est géré via souscription à productAddedSuccess$/lineUpdatedSuccess$ dans le composant
   */
  function setupForceStockSuccessEffect(): void {
    let previousLoading = loading();

    effect(() => {
      const isLoading = loading();
      const waiting = waitingForForceStockSuccess();
      const prevLoading = previousLoading;
      previousLoading = isLoading;

      if (!waiting) return;

      // Succès: loading passe de true à false ET pas d'erreur
      if (prevLoading && !isLoading && !facade.errorDetails()) {
        waitingForForceStockSuccess.set(false);
        facade.clearError();
        forceStockContext.set(null);
        // Reset géré via souscription à facade.productAddedSuccess$ ou facade.lineUpdatedSuccess$ dans le composant
      }
    });
  }

  /**
   * Initialise tous les effects liés à la gestion du stock
   * À appeler dans le constructor du composant
   */
  function initializeEffects(): void {
    setupErrorHandlingEffect();
    setupForceStockSuccessEffect();
  }

  return {
    handleStockError,
    handleReserveStockError,
    onForceStockConfirmed,
    onForceStockCancelled,
    setupErrorHandlingEffect,
    setupForceStockSuccessEffect,
    initializeEffects,
  };
}

/**
 * Type retourné par createForceStockHandling
 */
export type ForceStockHandling = ReturnType<typeof createForceStockHandling>;
