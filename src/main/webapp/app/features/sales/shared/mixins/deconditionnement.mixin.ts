import { effect, inject, Signal } from '@angular/core';
import { ISalesLine } from '../../../../shared/model';
import { IProduit } from '../../../../shared/model';
import { IDecondition } from '../../../../shared/model/decondition.model';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { DeconditionService } from '../../../../entities/decondition/decondition.service';
import { ProduitService } from '../../../../entities/produit/produit.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ConfirmDialogHost } from './force-stock.mixin';

/**
 * Opérations spécifiques au type de vente pour le déconditionnement
 */
export interface DeconditionnementSaleOperations {
  createSale: (line: ISalesLine) => void;
  addProduct: (line: ISalesLine) => void;
}

/**
 * Contexte partagé pour les opérations de déconditionnement
 */
export interface DeconditionnementHandlingContext {
  facade: SalesFacade;
  /** Signal indiquant qu'un forçage de stock est en attente - évite les conflits entre les deux effets */
  waitingForForceStockSuccess: Signal<boolean>;
  /** Fonction retournant le composant de dialogue de confirmation actif */
  getConfirmDialog: () => ConfirmDialogHost;
  /** Remet le champ de recherche produit dans l'état initial */
  resetProductSelection: () => void;
  /** Opérations d'ajout spécifiques au type de vente (COMPTANT / ASSURANCE / CARNET) */
  operations: DeconditionnementSaleOperations;
}

/**
 * Mixin pour la gestion du déconditionnement dans les composants de vente.
 *
 * Fournit les méthodes et effects pour :
 * - Détection des erreurs `stockChInsufisant`
 * - Vérification du stock CH via `attemptedLine.produit.produitId` (= `ProduitSearch.parentId`)
 * - Dialogue de confirmation de déconditionnement
 * - Création d'un enregistrement `Decondition` via l'API
 * - Relance de l'opération initiale (ajout produit / mise à jour quantité) après succès
 * - Annulation propre (effacement erreur + reset sélection produit)
 *
 * Prérequis :
 *  - `createSalesLineFromProduct` doit renseigner `produit.produitId` avec `ProduitSearch.parentId`
 *    (fait dans `sales-line.utils.ts`)
 *  - `createForceStockHandling` doit être initialisé avant ce mixin
 *
 * @example
 * ```typescript
 * private deconditionnementHandling = createDeconditionnementHandling({
 *   facade: this.facade,
 *   waitingForForceStockSuccess: this.waitingForForceStockSuccess,
 *   getConfirmDialog: () => this.confirmDialog(),
 *   resetProductSelection: () => this.productHandling.resetProductSelection(),
 *   operations: {
 *     createSale: (line) => this.facade.createComptantSale(line),
 *     addProduct: (line) => this.facade.onAddProduit(line),
 *   },
 * });
 *
 * constructor() {
 *   this.forceStockHandling.initializeEffects();
 *   this.deconditionnementHandling.initializeEffects();
 * }
 * ```
 */
export function createDeconditionnementHandling(context: DeconditionnementHandlingContext) {
  const deconditionService = inject(DeconditionService);
  const produitService = inject(ProduitService);
  const notificationService = inject(NotificationService);

  const { facade, waitingForForceStockSuccess, getConfirmDialog, resetProductSelection, operations } = context;

  /**
   * Verrou de réentrance : empêche l'effet de se redéclencher pendant qu'un flux
   * de déconditionnement est déjà en cours (requête HTTP, dialogue ouvert, etc.)
   * Utilisation d'un boolean simple (pas un signal) pour ne pas provoquer de re-run de l'effet.
   */
  let isHandlingDeconditionnement = false;

  /** Libère le verrou. Appelée dans tous les chemins de sortie du flux. */
  function releaseHandling(): void {
    isHandlingDeconditionnement = false;
  }

  /**
   * Point d'entrée principal.
   * Lit `attemptedLine.produit.produitId` (= parentId du ProduitSearch, renseigné dans
   * createSalesLineFromProduct) pour obtenir l'ID CH, puis récupère le produit CH
   * afin de vérifier son stock.
   */
  function handleDeconditionnement(errorDetails: { errorKey: string | null; originalError: any; attemptedLine?: ISalesLine }): void {
    if (isHandlingDeconditionnement) return;
    isHandlingDeconditionnement = true;

    const attemptedLine = errorDetails.attemptedLine;
    const chParentId = attemptedLine?.produit?.produitId;

    if (!chParentId) {
      releaseHandling();
      facade.clearError();
      notificationService.error('Stock insuffisant - ce produit ne peut pas être déconditionné');
      resetProductSelection();
      return;
    }

    // Récupérer le produit CH (conditionnement parent) pour vérifier son stock
    produitService.find(chParentId).subscribe(res => {
      const chProduit = res.body;
      if (chProduit && (chProduit.totalQuantity ?? 0) > 0) {
        // Le dialogue prend le relais ; le verrou est libéré dans confirm/cancel
        confirmDeconditionnement(attemptedLine, chProduit);
      } else {
        releaseHandling();
        facade.clearError();
        notificationService.error('Stock insuffisant - le conditionnement ne contient plus de stock');
        resetProductSelection();
      }
    });
  }

  /**
   * Affiche le dialogue de confirmation et orchestre la création du Decondition
   */
  function confirmDeconditionnement(line: ISalesLine, chProduit: IProduit): void {
    getConfirmDialog().onConfirm(
      () => onConfirmDeconditionnement(line, chProduit),
      'Déconditionnement nécessaire',
      'Le stock du conditionnement est insuffisant. Voulez-vous déconditionner ?',
      undefined,
      () => onCancelDeconditionnement(),
    );
  }

  /**
   * Callback de confirmation : crée le Decondition puis relance l'opération initiale
   */
  function onConfirmDeconditionnement(line: ISalesLine, chProduit: IProduit): void {
    const qtyDetail = chProduit.itemQty;
    if (!qtyDetail) {
      releaseHandling();
      facade.clearError();
      notificationService.error('Données de conditionnement incorrectes (itemQty manquant)');
      resetProductSelection();
      return;
    }

    const qtyMvt = line.quantityRequested ?? 1;
    const qtyDeconditionner = Math.round(qtyMvt / qtyDetail);

    const decondition: IDecondition = {
      qtyMvt: qtyDeconditionner,
      produitId: chProduit.id,
    };

    deconditionService.create(decondition).subscribe({
      next: () => {
        releaseHandling();
        facade.clearError();
        retryOperation(line);
      },
      error: () => {
        releaseHandling();
        facade.clearError();
        notificationService.error('Erreur lors du déconditionnement');
        resetProductSelection();
      },
    });
  }

  /**
   * Relance l'opération initiale (create / add / update) après déconditionnement réussi
   */
  function retryOperation(line: ISalesLine): void {
    if (line.id) {
      // Ligne existante : mettre à jour la quantité demandée
      facade.updateItemQtyRequested(line);
    } else {
      const currentSale = facade.currentSale();
      if (!currentSale?.saleId) {
        operations.createSale(line);
      } else {
        operations.addProduct(line);
      }
    }
  }

  /**
   * Callback d'annulation : nettoie l'état d'erreur et remet le focus sur le champ produit
   */
  function onCancelDeconditionnement(): void {
    releaseHandling();
    facade.clearError();
    resetProductSelection();
  }

  /**
   * Crée l'effect qui observe les errorDetails pour `stockChInsufisant`.
   *
   * - Retourne immédiatement si errorDetails est null/mauvaise clé ou force-stock en attente.
   * - Le verrou `isHandlingDeconditionnement` empêche une double exécution pendant un flux async.
   */
  function setupDeconditionnementEffect(): void {
    effect(() => {
      const errorDetails = facade.errorDetails();
      const waiting = waitingForForceStockSuccess();

      // Sortie rapide : pas d'erreur pertinente ou force-stock prioritaire
      if (!errorDetails || errorDetails.errorKey !== 'stockChInsufisant' || waiting) return;

      handleDeconditionnement(errorDetails);
    });
  }

  /**
   * Initialise les effects de déconditionnement.
   * À appeler dans le constructor du composant, APRÈS forceStockHandling.initializeEffects()
   */
  function initializeEffects(): void {
    setupDeconditionnementEffect();
  }

  return {
    handleDeconditionnement,
    onConfirmDeconditionnement,
    onCancelDeconditionnement,
    setupDeconditionnementEffect,
    initializeEffects,
  };
}

/**
 * Type retourné par createDeconditionnementHandling
 */
export type DeconditionnementHandling = ReturnType<typeof createDeconditionnementHandling>;
