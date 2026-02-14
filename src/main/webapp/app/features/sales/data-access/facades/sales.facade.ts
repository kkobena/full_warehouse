import { computed, inject, Injectable } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, finalize, map, Observable, of, pipe, Subject, switchMap, tap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISales, SaleId, UpdateSaleInfo } from '../../../../shared/model/sales.model';
import { createSalesLineFromProduct } from '../utils/sales-line.utils';
import { IClientTiersPayant, ICustomer, IRemise, ISalesLine, ProduitSearch, SalesStatut } from '../../../../shared/model';
import { PrintService } from '../services/print.service';

/**
 * Configuration pour la création d'une vente
 */
interface CreateSaleConfig {
  saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET';
  buildSale: (initialLine: ISalesLine) => ISales;
  apiCall: (sale: ISales) => Observable<ISales>;
  defaultErrorMessage: string;
}

/**
 * Options pour executeAndReloadSale
 */
interface ExecuteAndReloadOptions {
  errorMessage: string;
  successSubject?: Subject<void>;
  clearErrorOnSuccess?: boolean;
}

/**
 * Sales Facade
 * High-level API for sales operations
 * Encapsulates store and API service interactions
 * Provides simple, business-focused methods for components
 *
 * @example
 * // In component:
 * facade = inject(SalesFacade);
 *
 * // Read state (montants calculés côté backend)
 * currentSale = this.facade.currentSale;
 * totalAmount = this.facade.currentSale()?.salesAmount;
 * netAmount = this.facade.currentSale()?.netAmount;
 *
 * // Execute actions
 * this.facade.createComptantSale();
 * this.facade.addProductToSale(product, quantity);
 * this.facade.saveSale();
 */
@Injectable({ providedIn: 'root' })
export class SalesFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly printService = inject(PrintService);

  // Events - Success subjects for component subscriptions
  private readonly standbySuccessSubject = new Subject<void>();
  readonly standbySuccess$ = this.standbySuccessSubject.asObservable();

  private readonly productAddedSuccessSubject = new Subject<void>();
  readonly productAddedSuccess$ = this.productAddedSuccessSubject.asObservable();

  private readonly lineUpdatedSuccessSubject = new Subject<void>();
  readonly lineUpdatedSuccess$ = this.lineUpdatedSuccessSubject.asObservable();

  private readonly lineRemovedSuccessSubject = new Subject<void>();
  readonly lineRemovedSuccess$ = this.lineRemovedSuccessSubject.asObservable();

  private readonly saleReloadedSuccessSubject = new Subject<void>();
  readonly saleReloadedSuccess$ = this.saleReloadedSuccessSubject.asObservable();

  private readonly customerRemovedSuccessSubject = new Subject<void>();
  readonly customerRemovedSuccess$ = this.customerRemovedSuccessSubject.asObservable();

  private readonly cancelSaleSuccessSubject = new Subject<void>();
  readonly cancelSaleSuccess$ = this.cancelSaleSuccessSubject.asObservable();

  private readonly customerSetSuccessSubject = new Subject<void>();
  readonly customerSetSuccess$ = this.customerSetSuccessSubject.asObservable();

  private readonly remiseUpdatedSuccessSubject = new Subject<void>();
  readonly remiseUpdatedSuccess$ = this.remiseUpdatedSuccessSubject.asObservable();

  private readonly tiersPayantAddedSuccessSubject = new Subject<IClientTiersPayant>();
  readonly tiersPayantAddedSuccess$ = this.tiersPayantAddedSuccessSubject.asObservable();

  // ============================================
  // EXPOSE STORE STATE (Read-only)
  // ============================================

  /** Current sale */
  readonly currentSale = this.store.currentSale;

  /** Selected customer */
  readonly selectedCustomer = this.store.selectedCustomer;

  /** Cashier user */
  readonly cashier = this.store.cashier;

  /** Seller user */
  readonly seller = this.store.seller;

  /** Sale type */
  readonly saleType = this.store.saleType;

  /** Pending tiers payants (avant création vente backend) */
  readonly pendingTiersPayants = this.store.pendingTiersPayants;

  /** Is edit mode */
  readonly isEdit = this.store.isEdit;

  /** Is saving */
  readonly isSaving = this.store.isSaving;

  /** Loading state */
  readonly loading = this.store.loading;

  /** Error message */
  readonly error = this.store.error;

  /** Error details for advanced handling */
  readonly errorDetails = this.store.errorDetails;

  /** Last error for compatibility */
  readonly lastError = this.store.error;

  /** Pending sales */
  readonly pendingSales = this.store.pendingSales;

  /** Selected product for search */
  readonly selectedProduct = this.store.selectedProduct;

  // ============================================
  // EXPOSE COMPUTED SELECTORS
  // ============================================

  /** Sales lines of current sale */
  readonly salesLines = this.store.salesLines;

  /** Total items count */
  readonly totalItems = this.store.totalItems;

  /** Can save current sale */
  readonly canSave = this.store.canSave;

  /** Is VO sale (insurance/carnet) */
  readonly isVOSale = this.store.isVOSale;

  /** Has customer */
  readonly hasCustomer = this.store.hasCustomer;

  /** Sales lines count */
  readonly salesLinesCount = this.store.salesLinesCount;

  /** Is sale empty */
  readonly isEmpty = this.store.isEmpty;

  /** Remaining amount to pay */
  readonly remainingAmount = this.store.remainingAmount;

  /** Total quantity requested (quantité demandée) */
  readonly totalQuantityRequested = this.store.totalQuantityRequested;

  /** Total quantity sold (quantité servie) */
  readonly totalQuantitySold = this.store.totalQuantitySold;

  /** Is avoir - true if quantityRequested !== quantitySold */
  readonly isAvoir = this.store.isAvoir;

  /** Type de prescription */
  readonly typePrescription = computed(() => this.store.typePrescription());

  // Computed amounts - expose from store
  readonly totalAmount = computed(() => this.currentSale()?.salesAmount || 0);
  readonly discountAmount = computed(() => this.currentSale()?.discountAmount || 0);
  readonly taxAmount = computed(() => this.currentSale()?.taxAmount || 0);
  readonly netAmount = computed(() => this.currentSale()?.netAmount || 0);
  readonly amountToBePaid = computed(() => this.currentSale()?.amountToBePaid || 0);
  readonly canSaveSale = this.canSave;

  // ============================================
  // PRIVATE HELPERS (Shared logic)
  // ============================================

  /**
   * Extrait errorKey et errorMessage depuis une erreur HTTP API
   */
  private extractApiError(error: any, defaultMessage: string): { errorMessage: string; errorKey: string | null } {
    let errorMessage = defaultMessage;
    let errorKey: string | null = null;

    if (error?.error) {
      errorKey = error.error.errorKey ?? null;

      if (errorKey === 'stock') {
        errorMessage = error.error.message || error.error.detail || 'Stock insuffisant';
      } else if (errorKey === 'stockChInsufisant') {
        errorMessage = 'Stock insuffisant - Déconditionnement nécessaire';
      } else if (error.error.message) {
        errorMessage = error.error.message;
      } else if (error.error.detail) {
        errorMessage = error.error.detail;
      }
    }

    return { errorMessage, errorKey };
  }

  /**
   * Gère l'erreur de stock lors de la création d'une vente (pas de reload nécessaire)
   */
  private handleCreateSaleError(error: any, initialLine: ISalesLine, defaultMessage: string): Observable<null> {
    console.error('Error creating sale:', error);
    const { errorMessage, errorKey } = this.extractApiError(error, defaultMessage);

    if (errorKey === 'stock') {
      this.store.setError(errorMessage);
      this.store.setLastErrorDetails({
        errorKey,
        originalError: error,
        attemptedLine: initialLine,
        isFromTableCellEdit: false,
      });
      // NE PAS afficher le toast - le dialog sera affiché par l'effect
    } else {
      this.store.setError(errorMessage);
      this.notificationService.error(errorMessage);
    }

    this.store.setLoading(false);
    return of(null);
  }

  /**
   * Pipeline commun pour la création de vente (COMPTANT, ASSURANCE, CARNET)
   */
  private createSalePipeline(config: CreateSaleConfig) {
    return pipe(
      tap(() => {
        this.store.setSaleType(config.saleType);
        this.store.setError(null);
      }),
      switchMap((initialLine: ISalesLine) => {
        if (!initialLine) {
          throw new Error('Impossible de créer une vente sans produit');
        }

        const sale = config.buildSale(initialLine);
        this.store.setLoading(true);

        return config.apiCall(sale).pipe(
          map(createdSale => ({ createdSale, initialLine })),
          catchError(error => this.handleCreateSaleError(error, initialLine, config.defaultErrorMessage)),
        );
      }),
      tap({
        next: (result: { createdSale: ISales; initialLine: ISalesLine } | null) => {
          if (result?.createdSale) {
            this.store.setCurrentSale(result.createdSale);
            this.store.setIsEdit(true);
            this.store.clearError();
            this.productAddedSuccessSubject.next();
          }
          this.store.setLoading(false);
        },
      }),
    );
  }

  /**
   * Logique commune pour ajouter un produit à une vente existante (COMPTANT ou ASSURANCE/CARNET)
   * Gère le cas d'erreur de stock avec reload de la vente et reconstruction de la ligne
   */
  private addProductWithStockHandling(salesLine: ISalesLine, addApiCall$: Observable<any>): void {
    const currentSale = this.store.currentSale();
    if (!currentSale || !currentSale.saleId) {
      console.error('No current sale to add product to');
      return;
    }

    this.store.setLoading(true);

    addApiCall$
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error adding product:', error);
          const { errorMessage, errorKey } = this.extractApiError(error, "Erreur lors de l'ajout du produit");

          // Si erreur de stock, recharger la vente pour récupérer l'état actuel
          // La ligne peut déjà exister si le produit a été ajouté précédemment
          if (errorKey === 'stock') {
            return this.apiService.findSale(currentSale.saleId!).pipe(
              tap(reloadedSale => {
                if (reloadedSale) {
                  this.store.setCurrentSale(reloadedSale);

                  // Trouver la ligne qui correspond au produit ajouté
                  const existingLine = reloadedSale.salesLines?.find(line => line.produitId === salesLine.produitId);

                  const lineToAttempt: ISalesLine = existingLine
                    ? {
                        ...existingLine,
                        quantityRequested: salesLine.quantityRequested || 1,
                        saleCompositeId: existingLine.saleCompositeId || {
                          id: currentSale.saleId!.id,
                          saleDate: currentSale.saleId!.saleDate,
                        },
                      }
                    : salesLine;

                  this.store.setError(errorMessage);
                  this.store.setLastErrorDetails({
                    errorKey,
                    originalError: error,
                    attemptedLine: lineToAttempt,
                    isFromTableCellEdit: false,
                  });
                }
              }),
              tap(() => this.store.setLoading(false)),
              map((): null => null),
            );
          }

          // Pour les autres erreurs, traitement normal
          this.store.setError(errorMessage);
          this.store.setLastErrorDetails({
            errorKey,
            originalError: error,
            attemptedLine: salesLine,
            isFromTableCellEdit: false,
          });
          this.notificationService.error(errorMessage);
          this.store.setLoading(false);
          return of(null);
        }),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          this.store.clearError();
          this.productAddedSuccessSubject.next();
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Logique commune pour updateItemQtyRequested et updateItemQtyRequestedWithSet
   * Exécute un appel API de mise à jour de quantité, recharge la vente, gère les erreurs
   */
  private executeQtyUpdate(isAjoutProduit: boolean, apiCall$: Observable<any>): void {
    const currentSale = this.store.currentSale();
    if (!currentSale || !currentSale.saleId) {
      console.error('No current sale to update product');
      return;
    }

    this.store.setLoading(true);

    apiCall$
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error updating product quantity:', error);

          let errorMessage = 'Erreur lors de la mise à jour du produit';
          if (error?.error) {
            if (error.error.message) {
              errorMessage = error.error.message;
            } else if (error.error.detail) {
              errorMessage = error.error.detail;
            }
          }

          this.store.setError(errorMessage);
          this.notificationService.error(errorMessage);
          this.store.setLoading(false);
          return of(null);
        }),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          this.store.clearError();
          isAjoutProduit ? this.productAddedSuccessSubject.next() : this.lineUpdatedSuccessSubject.next();
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Pattern commun : exécuter un appel API, recharger la vente, mettre à jour le store
   * Utilisé par removeLine, updateLineQuantitySold, updateLinePrice, applyLineDiscount, updateRemise
   */
  private executeAndReloadSale(apiCall$: Observable<any>, saleId: SaleId, options: ExecuteAndReloadOptions): void {
    this.store.setLoading(true);

    apiCall$
      .pipe(
        switchMap(() => this.apiService.findSale(saleId)),
        catchError(error => {
          console.error('Error:', error);
          this.notificationService.error(options.errorMessage);
          this.store.setError(options.errorMessage);
          this.store.setLoading(false);
          return of(null);
        }),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          if (options.clearErrorOnSuccess) {
            this.store.clearError();
          }
          options.successSubject?.next();
        }
        this.store.setLoading(false);
      });
  }

  // ============================================
  // BUSINESS ACTIONS (High-level)
  // ============================================

  /**
   * Create a new comptant sale
   * @param initialLine - REQUIRED first product to add to the sale (cannot create empty sale)
   */
  createComptantSale = rxMethod<ISalesLine>(
    this.createSalePipeline({
      saleType: 'COMPTANT',
      defaultErrorMessage: 'Erreur lors de la création de la vente',
      apiCall: sale => this.apiService.createComptantSale(sale),
      buildSale: initialLine => ({
        statut: SalesStatut.ACTIVE,
        salesLines: [initialLine],
        customerId: this.store.selectedCustomer()?.id,
        natureVente: 'COMPTANT',
        typePrescription: 'PRESCRIPTION',
        cassierId: this.store.cashier()?.id,
        sellerId: this.store.seller()?.id,
        type: 'VNO',
        categorie: 'VNO',
        differe: false,
        sansBon: false,
        avoir: false,
      }),
    }),
  );

  /**
   * Create a new insurance sale
   * @param initialLine - REQUIRED first product to add to the sale (cannot create empty sale)
   */
  createAssuranceSale = rxMethod<ISalesLine>(
    this.createSalePipeline({
      saleType: 'ASSURANCE',
      defaultErrorMessage: 'Erreur lors de la création de la vente assurance',
      apiCall: sale => this.apiService.createAssuranceSale(sale),
      buildSale: initialLine => ({
        salesLines: [initialLine],
        customerId: this.store.selectedCustomer()?.id,
        natureVente: 'ASSURANCE',
        typePrescription: this.store.typePrescription() || 'PRESCRIPTION',
        cassierId: this.store.cashier()?.id,
        sellerId: this.store.seller()?.id,
        type: 'VO',
        categorie: 'VO',
        tiersPayants: this.store.pendingTiersPayants(),
      }),
    }),
  );

  /**
   * Create a new carnet sale
   * @param initialLine - REQUIRED first product to add to the sale (cannot create empty sale)
   */
  createCarnetSale = rxMethod<ISalesLine>(
    this.createSalePipeline({
      saleType: 'CARNET',
      defaultErrorMessage: 'Erreur lors de la création de la vente carnet',
      // Note: Utilise /assurance endpoint (partagé CARNET/ASSURANCE), différencié par sale.natureVente
      apiCall: sale => this.apiService.createAssuranceSale(sale),
      buildSale: initialLine => ({
        salesLines: [initialLine],
        customerId: this.store.selectedCustomer()?.id,
        natureVente: 'CARNET',
        typePrescription: this.store.typePrescription() || 'PRESCRIPTION',
        cassierId: this.store.cashier()?.id,
        sellerId: this.store.seller()?.id,
        type: 'VO',
        categorie: 'VO',
        tiersPayants: this.store.pendingTiersPayants(),
      }),
    }),
  );

  /**
   * Save current sale (finalize)
   * Returns an Observable that emits the saved sale on success, null on error
   */
  saveSale(): Observable<ISales | null> {
    this.store.setIsSaving(true);
    this.store.setError(null);

    const currentSale = this.store.currentSale();
    if (!currentSale) {
      const error = 'Aucune vente en cours';
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    // Calculer les montants avant sauvegarde
    this.calculateSaleAmounts(currentSale);

    // Appliquer le flag avoir automatiquement
    currentSale.avoir = this.store.isAvoir();

    // Vérifier si client obligatoire pour avoir
    if (currentSale.avoir && !currentSale.customerId) {
      const error = 'Un client est obligatoire pour une vente avec avoir (livraison partielle)';
      this.notificationService.error(error);
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    const saleType = this.store.saleType();
    let saveObservable: Observable<ISales>;

    if (saleType === 'COMPTANT') {
      saveObservable = this.apiService.saveCashSale(currentSale);
    } else if (saleType === 'ASSURANCE' || saleType === 'CARNET') {
      // CARNET utilise les mêmes endpoints qu'ASSURANCE
      saveObservable = this.apiService.saveAssuranceSale(currentSale);
    } else {
      const error = 'Type de vente non supporté';
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    return saveObservable.pipe(
      tap(result => {
        // Print if needed
        const printInvoice = this.store.printInvoice();
        const printReceipt = this.store.printReceipt();

        if (printInvoice && result.saleId) {
          this.printService.printInvoice(result.saleId);
        }

        if (printReceipt && result.saleId) {
          this.printService.printReceipt(result.saleId).subscribe();
        }

        // Reset after successful save
        this.store.resetCurrentSale();
        this.store.setIsSaving(false);
      }),
      catchError(error => {
        console.error('Error saving sale:', error);
        const errorMessage = error?.error?.message || error.message || "Erreur lors de l'enregistrement de la vente";
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        return of(null);
      }),
    );
  }

  /**
   * Calculate sale amounts before saving
   * NOTE: montantRendu et montantRenduArrondi sont déjà calculés par le frontend
   * avec les règles métier (seuil de 5 FCFA, arrondi au multiple de 5)
   */
  private calculateSaleAmounts(sale: ISales): void {
    const montantVerse = Number(sale.montantVerse) || 0;
    const amountToBePaid = Number(sale.amountToBePaid) || 0;
    const restToPay = amountToBePaid - montantVerse;

    // Montant payé = minimum entre montant versé et montant à payer
    sale.payrollAmount = restToPay <= 0 ? amountToBePaid : montantVerse;

    // Reste à payer = maximum entre 0 et le reste
    sale.restToPay = Math.max(restToPay, 0);

    // Monnaie: ne recalculer que si pas déjà définie par le frontend
    // Le frontend gère les règles métier (seuil 5 FCFA, arrondi)
    if (sale.montantRendu === undefined || sale.montantRendu === null) {
      sale.montantRendu = restToPay < 0 ? Math.abs(restToPay) : 0;
    }
  }

  /**
   * Load sale for editing
   * Uniquement pour modification  d'une vente cloturée
   * Ne pas utiliser pour recharger la vente courante en édition
   */
  loadSale = rxMethod<SaleId>(
    pipe(
      tap(() => {
        this.store.setLoading(true);
        this.store.setError(null);
      }),
      switchMap(saleId => this.apiService.findSale(saleId)),
      tap({
        next: sale => {
          this.store.setCurrentSale(sale);

          // Mettre à jour le saleType dans le store selon la natureVente
          if (sale.natureVente === 'ASSURANCE') {
            this.store.setSaleType('ASSURANCE');
          } else if (sale.natureVente === 'CARNET') {
            this.store.setSaleType('CARNET');
          } else {
            this.store.setSaleType('COMPTANT');
          }

          if (sale.customer) {
            this.store.setSelectedCustomer(sale.customer);
          }

          if (sale.cassier) {
            this.store.setCashier(sale.cassier);
          }

          if (sale.seller) {
            this.store.setSeller(sale.seller);
          }

          this.store.setLoading(false);
          this.saleReloadedSuccessSubject.next();
        },
        error: error => {
          this.store.setError(error.message || 'Erreur lors du chargement de la vente');
          this.store.setLoading(false);
        },
      }),
      catchError(error => {
        console.error('Error loading sale:', error);
        return of(null);
      }),
    ),
  );

  /**
   * Load sale for editing
   * Uniquement pour modification  d'une vente cloturée
   * Ne pas utiliser pour recharger la vente courante en édition
   */
  loadSaleForEdit = rxMethod<SaleId>(
    pipe(
      tap(() => {
        this.store.setLoading(true);
        this.store.setError(null);
      }),
      switchMap(saleId => this.apiService.findSaleForEdit(saleId)), //Ne pas utilise findSale ici
      tap({
        next: sale => {
          this.store.setCurrentSale(sale);
          this.store.setIsEdit(true);

          // Mettre à jour le saleType dans le store selon la natureVente
          if (sale.natureVente === 'ASSURANCE') {
            this.store.setSaleType('ASSURANCE');
          } else if (sale.natureVente === 'CARNET') {
            this.store.setSaleType('CARNET');
          } else {
            this.store.setSaleType('COMPTANT');
          }

          if (sale.customer) {
            this.store.setSelectedCustomer(sale.customer);
          }

          if (sale.cassier) {
            this.store.setCashier(sale.cassier);
          }

          if (sale.seller) {
            this.store.setSeller(sale.seller);
          }

          this.store.setLoading(false);
          this.saleReloadedSuccessSubject.next();
        },
        error: error => {
          this.store.setError(error.message || 'Erreur lors du chargement de la vente');
          this.store.setLoading(false);
        },
      }),
      catchError(error => {
        console.error('Error loading sale:', error);
        return of(null);
      }),
    ),
  );

  /**
   * Cancel current sale
   */
  cancelSale = rxMethod<void>(
    pipe(
      tap(() => {
        this.store.setLoading(true);
        this.store.setError(null);
      }),
      switchMap(() => {
        const currentSale = this.store.currentSale();
        if (!currentSale || !currentSale.id || !currentSale.saleId) {
          throw new Error('Aucune vente en cours');
        }
        const saleId: SaleId = currentSale.saleId;
        const isVno = this.store.saleType() === 'COMPTANT';
        if (isVno) {
          return this.apiService.deletePreventeComptant(saleId); // Ne pas utiliser cancelComptant(saleId):pour les ventes cloturées, le endpoint de suppression est différent (deletePreventeComptant) pour respecter les règles métier de suppression des ventes comptant
        } else {
          return this.apiService.deletePreventeAssurance(saleId); // Ne pas utiliser cancelAssurance(saleId):pour les ventes cloturées, le endpoint de suppression est différent (deletePreventeAssurance) pour respecter les règles métier de suppression des ventes assurance/carnet
        }
      }),
      tap({
        next: () => {
          this.store.resetCurrentSale();
          this.store.setLoading(false);
          this.cancelSaleSuccessSubject.next();
        },
        error: error => {
          this.store.setError(error.message || "Erreur lors de l'annulation de la vente");
          this.store.setLoading(false);
        },
      }),
      catchError(error => {
        console.error('Error canceling sale:', error);
        return of(null);
      }),
    ),
  );

  // ============================================
  // PRODUCT MANAGEMENT
  // ============================================

  /**
   * Initialize a new comptant sale
   * @param initialLine - REQUIRED first product to add (matches original behavior - no empty sales)
   */
  initializeComptantSale(initialLine: ISalesLine): void {
    if (!initialLine) {
      throw new Error('Impossible de créer une vente sans produit');
    }
    this.store.setSaleType('COMPTANT');
    // this.store.resetCurrentSale();
    this.createComptantSale(initialLine);
  }

  /**
   * Initialize a new assurance sale (without initial product - customer required first)
   */
  initializeAssuranceSale(): void {
    this.store.setSaleType('ASSURANCE');
    //  this.store.resetCurrentSale();
    this.store.setPendingTiersPayants([]);
  }

  /**
   * Initialize a new carnet sale (without initial product - customer required first)
   */
  initializeCarnetSale(): void {
    this.store.setSaleType('CARNET');
    // this.store.resetCurrentSale();
    this.store.setPendingTiersPayants([]);
  }

  /**
   * Update tiers payants - stocke dans pendingTiersPayants si pas de vente, sinon dans currentSale
   * Called when customer is selected or when tiers payants are modified in UI
   */
  updateSaleTiersPayants(tiersPayants: IClientTiersPayant[]): void {
    const currentSale = this.store.currentSale();
    if (currentSale?.saleId) {
      // Vente existe sur le backend, mettre à jour currentSale
      this.store.setCurrentSale({
        ...currentSale,
        tiersPayants: tiersPayants,
      });
    } else {
      // Pas encore de vente backend, stocker dans pendingTiersPayants
      this.store.setPendingTiersPayants(tiersPayants);
    }
  }

  /**
   * Remove a tiers payant from the current sale
   * If sale exists (has saleId), makes API call to remove and recalculate amounts
   * If sale doesn't exist, just removes locally
   * @param tiersPayant - The tiers payant to remove
   * @param onSuccess - Optional callback called after successful removal
   */
  removeTiersPayantFromSale(tiersPayant: IClientTiersPayant, onSuccess?: () => void): void {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      return;
    }

    // Calculer la nouvelle liste sans le tiers payant
    const updatedTiersPayants = (currentSale.tiersPayants || []).filter(tp => tp.id !== tiersPayant.id);

    // Si la vente existe sur le backend, faire un appel API
    if (currentSale.saleId && tiersPayant.id) {
      this.store.setLoading(true);

      this.apiService
        .removeThirdPartyFromSales(tiersPayant.id, currentSale.saleId)
        .pipe(
          switchMap(() => this.apiService.findSale(currentSale.saleId!)),
          catchError(error => {
            console.error('Error removing tiers payant:', error);
            this.notificationService.error('Erreur lors de la suppression du tiers payant');
            this.store.setLoading(false);
            return of(null);
          }),
          finalize(() => this.store.setLoading(false)),
        )
        .subscribe(saleFromBackend => {
          if (saleFromBackend) {
            this.store.setCurrentSale(saleFromBackend);
            if (onSuccess) {
              onSuccess();
            }
          }
        });
    } else {
      // Si pas de vente backend, juste mettre à jour localement
      this.store.setCurrentSale({
        ...currentSale,
        tiersPayants: updatedTiersPayants,
      });
      if (onSuccess) {
        onSuccess();
      }
    }
  }

  /**
   * Add a tiers payant complémentaire to the current sale
   * If sale exists (has saleId), makes API call to add and recalculate amounts
   * In all cases, emits success event for UI update
   * @param tiersPayant - The tiers payant to add
   */
  addTiersPayantToSale(tiersPayant: IClientTiersPayant): void {
    const currentSale = this.store.currentSale();

    // Si la vente existe sur le backend (a un saleId), faire un appel API
    if (currentSale?.saleId) {
      this.store.setLoading(true);

      this.apiService
        .addTiersPayantComplementaire(currentSale.saleId, tiersPayant)
        .pipe(
          switchMap(() => this.apiService.findSale(currentSale.saleId!)),
          catchError(error => {
            console.error('Error adding tiers payant:', error);
            this.notificationService.error("Erreur lors de l'ajout du tiers payant complémentaire");
            this.store.setLoading(false);
            return of(null);
          }),
          finalize(() => this.store.setLoading(false)),
        )
        .subscribe(saleFromBackend => {
          if (saleFromBackend) {
            this.store.setCurrentSale(saleFromBackend);
            // Émettre l'événement de succès avec le nouveau tiers payant
            this.tiersPayantAddedSuccessSubject.next(tiersPayant);
          }
        });
    } else {
      // Pas de vente backend: mise à jour locale du store si currentSale existe
      if (currentSale) {
        const existingTiersPayants = currentSale.tiersPayants || [];
        const updatedTiersPayants = [...existingTiersPayants, tiersPayant];
        this.store.setCurrentSale({
          ...currentSale,
          tiersPayants: updatedTiersPayants,
        });
      }
      // Toujours émettre l'événement pour que l'UI se mette à jour
      this.tiersPayantAddedSuccessSubject.next(tiersPayant);
    }
  }

  /**
   * Save assurance sale with payment modes and tiers payants
   * @param paymentModes - Payment modes used
   * @returns Observable that emits the saved sale
   */
  saveAssuranceSale(paymentModes: any[]): Observable<ISales | null> {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      this.notificationService.error('Aucune vente en cours');
      return of(null);
    }

    if (!currentSale.customerId) {
      this.notificationService.error('Client obligatoire pour vente ASSURANCE');
      return of(null);
    }

    // TODO: Ajouter les payment modes et tiers payants à la vente
    currentSale.payments = paymentModes;

    return this.saveSale();
  }

  /**
   * Create comptant sale with first product (matches original createComptant)
   * @param salesLine - First product to add to the sale
   */
  createComptant(salesLine: ISalesLine): void {
    this.initializeComptantSale(salesLine);
  }

  /**
   * Add product to existing sale (matches original onAddProduit)
   * @param salesLine - Product line to add
   */
  onAddProduit(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemComptant(salesLine));
  }

  /**
   * Add product to existing CARNET sale
   * Uses /add-item/assurance endpoint (shared by ASSURANCE and CARNET)
   * @param salesLine - Product line to add
   */
  onAddProduitCarnet(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemAssurance(salesLine));
  }

  /**
   * Update item quantity with force stock (matches original processQtyRequested)
   * Used when forcing stock after confirmation dialog
   * Uses INCREMENT endpoint to add to existing quantity
   * @param salesLine - Product line to update with forceStock = true
   */
  updateItemQtyRequested(salesLine: ISalesLine): void {
    const saleType = this.store.saleType();
    const apiCall =
      saleType === 'ASSURANCE' || saleType === 'CARNET'
        ? this.apiService.incrementItemQtyRequestedAssurance(salesLine)
        : this.apiService.incrementItemQtyRequested(salesLine);

    this.executeQtyUpdate(true, apiCall);
  }

  /**
   * Update item quantity requested with SET (for force stock from table cell edit)
   * Uses SET endpoint to REPLACE quantity (not increment)
   * @param salesLine - Product line to update with forceStock = true
   */
  updateItemQtyRequestedWithSet(salesLine: ISalesLine): void {
    const saleType = this.store.saleType();
    const apiCall =
      saleType === 'ASSURANCE' || saleType === 'CARNET'
        ? this.apiService.setItemQtyRequestedAssurance(salesLine)
        : this.apiService.setItemQtyRequested(salesLine);

    this.executeQtyUpdate(false, apiCall);
  }

  /**
   * Remove line by saleLineId (matches original removeLine)
   * @param saleLineId - Composite ID of the line to remove
   */
  removeLine(saleLineId: any): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) return;
    const saleType = this.store.saleType();
    this.executeAndReloadSale(
      saleType === 'COMPTANT' ? this.apiService.deleteItem(saleLineId) : this.apiService.deleteItemFromAssurance(saleLineId),
      currentSale.saleId,
      {
        errorMessage: 'Erreur lors de la suppression de la ligne',
        successSubject: this.lineRemovedSuccessSubject,
      },
    );
  }

  /**
   * Set selected product in search
   */
  setSelectedProduct(product: any | null): void {
    this.store.setSelectedProductData(product);
  }

  /**
   * Add product to current sale
   * Reproduit la logique de createSalesLine() de l'original selling-home.component.ts
   * Envoie au backend qui calcule tous les montants, puis recharge la vente
   */
  addProductToSale(product: ProduitSearch, quantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale || !currentSale.saleId) {
      console.error('No current sale to add product to');
      return;
    }

    // Construction ISalesLine avec l'utilitaire
    const newLine: ISalesLine = createSalesLineFromProduct(product, quantity, currentSale);

    this.store.setLoading(true);

    // Envoyer au backend pour calcul des montants
    this.apiService
      .addItemComptant(newLine)
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error adding product:', error);
          const { errorMessage, errorKey } = this.extractApiError(error, "Erreur lors de l'ajout du produit");

          // Stocker l'erreur avec les détails pour traitement par le composant
          this.store.setError(errorMessage);
          this.store.setLastErrorDetails({ errorKey, originalError: error, attemptedLine: newLine });
          this.notificationService.error(errorMessage);
          this.store.setLoading(false);
          return of(null);
        }),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          this.productAddedSuccessSubject.next();
        }
        this.store.setLoading(false);
        this.setSelectedProduct(null); // Clear selection
      });
  }

  /**
   * Update line quantity
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  updateLineQuantitySold(lineId: number, newQuantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) return;

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) return;

    const updatedLine: ISalesLine = {
      ...line,
      quantitySold: newQuantity,
      saleCompositeId: currentSale.saleId,
    };

    this.executeAndReloadSale(this.apiService.updateItemQtySold(updatedLine), currentSale.saleId, {
      errorMessage: 'Erreur lors de la mise à jour de la quantité',
      successSubject: this.lineUpdatedSuccessSubject,
    });
  }

  /**
   * Update line quantity requested
   * Used when editing quantity from table cell
   * Uses SET endpoint to REPLACE quantity (not increment)
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  updateLineQuantityRequested(lineId: number, newQuantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) return;

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) return;

    const updatedLine: ISalesLine = {
      ...line,
      quantityRequested: newQuantity,
      saleCompositeId: currentSale.saleId,
    };

    this.store.setLoading(true);

    // Route vers le bon endpoint selon le type de vente
    const saleType = this.store.saleType();
    const apiCall =
      saleType === 'ASSURANCE' || saleType === 'CARNET'
        ? this.apiService.setItemQtyRequestedAssurance(updatedLine)
        : this.apiService.setItemQtyRequested(updatedLine);

    apiCall
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error updating quantity requested:', error);

          // Extraire le message d'erreur et errorKey
          let errorMessage = 'Erreur lors de la mise à jour de la quantité demandée';
          let errorKey: string | undefined;

          if (error?.error) {
            if (error.error.message) {
              errorMessage = error.error.message;
            } else if (error.error.detail) {
              errorMessage = error.error.detail;
            }
            errorKey = error.error.errorKey;
          }

          // Si erreur de stock pour modification cellule:
          // NE PAS recharger la vente car cela écraserait la valeur saisie par l'utilisateur
          // On a déjà toutes les infos nécessaires dans updatedLine
          if (errorKey === 'stock') {
            // S'assurer que saleCompositeId est correctement formé
            if (!updatedLine.saleCompositeId && currentSale.saleId) {
              updatedLine.saleCompositeId = {
                id: currentSale.saleId.id,
                saleDate: currentSale.saleId.saleDate,
              };
            }

            // Stocker l'erreur avec la ligne qui contient la NOUVELLE quantité voulue
            this.store.setError(errorMessage);
            this.store.setLastErrorDetails({
              errorKey,
              originalError: error,
              attemptedLine: updatedLine, // Contient newQuantity
              isFromTableCellEdit: true,
            });
            // NE PAS afficher le toast - le dialog sera affiché par l'effect
            this.store.setLoading(false);
            return of(null);
          } else {
            // Autres erreurs : toast simple
            this.notificationService.error(errorMessage);
            this.store.setError(errorMessage);
            this.store.setLastErrorDetails({
              errorKey: errorKey || null,
              originalError: error,
              attemptedLine: updatedLine,
              isFromTableCellEdit: true,
            });
            this.store.setLoading(false);
            return of(null);
          }
        }),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          // Clear l'erreur en cas de succès (important pour le force stock)
          this.store.setError(null);
          this.store.setLastErrorDetails(null);
          this.lineUpdatedSuccessSubject.next();
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Update line unit price
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  updateLinePrice(lineId: number, newPrice: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) return;

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) return;

    const updatedLine: ISalesLine = {
      ...line,
      regularUnitPrice: newPrice,
      saleCompositeId: currentSale.saleId,
    };

    this.executeAndReloadSale(this.apiService.updateItemPrice(updatedLine), currentSale.saleId, {
      errorMessage: 'Erreur lors de la mise à jour du prix',
    });
  }

  /**
   * Apply discount to line
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  applyLineDiscount(lineId: number, discountAmount: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) return;

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) return;

    const updatedLine: ISalesLine = {
      ...line,
      discountAmount,
      saleCompositeId: currentSale.saleId,
    };

    this.executeAndReloadSale(this.apiService.updateItemPrice(updatedLine), currentSale.saleId, {
      errorMessage: "Erreur lors de l'application de la remise",
    });
  }

  /**
   * Update global remise (discount) on current sale
   * @param remise - Remise to apply, or undefined to remove remise
   */
  updateRemise(remise?: IRemise): void {
    const currentSale = this.store.currentSale();
    const saleType = this.store.saleType();
    if (!currentSale?.saleId) {
      this.notificationService.error('Aucune vente en cours');
      return;
    }

    const isComptant = saleType === 'COMPTANT';
    const key = { id: currentSale.saleId, value: remise?.id! };

    let action$;
    if (remise) {
      action$ = isComptant ? this.apiService.addRemise(key) : this.apiService.addAssuranceRemise(key);
    } else {
      action$ = isComptant
        ? this.apiService.removeRemiseFromCashSale(currentSale.saleId)
        : this.apiService.removeRemiseFromAssuranceSale(currentSale.saleId);
    }

    this.executeAndReloadSale(action$, currentSale.saleId, {
      errorMessage: 'Erreur lors de la mise à jour de la remise',
      successSubject: this.remiseUpdatedSuccessSubject,
    });
  }

  // ============================================
  // CUSTOMER MANAGEMENT
  // ============================================

  /**
   * Set customer for current sale
   * Backend recalculates amounts and reloads full sale
   */
  setCustomer(customer: ICustomer): void {
    const currentSale = this.store.currentSale();

    // Si pas encore de vente créée (pas de saleId), on stocke juste le client
    // La vente sera créée avec ce client lors de l'ajout du premier produit
    if (!currentSale?.saleId) {
      this.store.setSelectedCustomer(customer);
      return;
    }

    this.store.setLoading(true);
    this.store.setSelectedCustomer(customer);

    // Determine which endpoint to use based on sale type
    const updateSaleInfo: UpdateSaleInfo = { id: currentSale.saleId, value: customer.id! };
    const updateObservable$ =
      currentSale.type === 'VNO'
        ? this.apiService.addCustommerToCashSale(updateSaleInfo)
        : this.apiService.changeAssuranceCustomer(updateSaleInfo);
    //TODO: synchroniser tiersPayants dans la store avec le nouveau client pour les vente ASSURANCE/CARNET au retour de l'appel API
    updateObservable$
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error setting customer:', error);
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale({ ...sale, differe: currentSale.differe, avoir: currentSale.avoir }); // Conserver les flags differe et avoir qui ne sont pas gérés par le backend
          this.customerSetSuccessSubject.next();
        }
      });
  }

  /**
   * Remove customer from current sale
   * Backend recalculates amounts and reloads full sale
   */
  removeCustomer(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      return;
    }

    this.store.setLoading(true);
    this.store.setSelectedCustomer(null);

    // Update sale without customer
    const updatedSale: ISales = {
      ...currentSale,
      customerId: undefined,
    };

    // Determine which endpoint to use based on sale type
    const updateObservable$ =
      currentSale.natureVente === 'VO' ? this.apiService.updateComptantSale(updatedSale) : this.apiService.updateAssuranceSale(updatedSale);

    updateObservable$
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error removing customer:', error);
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          this.customerRemovedSuccessSubject.next();
        }
      });
  }

  // ============================================
  // PENDING SALES (MISE EN ATTENTE)
  // ============================================

  /**
   * Put current sale on standby (prévente)
   * Saves sale with STANDBY status and resets current sale
   * Emits standbySuccess$ event on success
   */
  putOnStandby(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      console.error('No current sale to put on standby');
      return;
    }

    this.store.setIsSaving(true);
    this.store.setError(null);

    // Determine which endpoint to use based on sale type
    const saleType = this.store.saleType();
    const standbyObservable$ =
      saleType === 'COMPTANT' ? this.apiService.putComptantOnStandby(currentSale) : this.apiService.putAssuranceOnStandby(currentSale);

    standbyObservable$
      .pipe(
        tap(result => {
          if (result?.success) {
            this.store.resetCurrentSale();
            this.standbySuccessSubject.next();
          }
        }),
        catchError(error => {
          console.error('Error putting sale on standby:', error);
          const errorMessage = error?.error?.message || 'Erreur lors de la mise en attente';
          this.notificationService.error(errorMessage);
          this.store.setError(errorMessage);
          return of(null);
        }),
        finalize(() => this.store.setIsSaving(false)),
      )
      .subscribe();
  }

  /**
   * Load pending sales from backend
   */
  loadPendingSales(params: any): void {
    this.store.setLoading(true);

    this.apiService
      .getPendingSales(params)
      .pipe(
        catchError(error => {
          console.error('Error loading pending sales:', error);
          this.store.setError('Erreur lors du chargement des ventes en attente');
          this.store.setLoading(false);
          return of([]);
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sales => {
        this.store.setPendingSales(sales);
      });
  }

  /**
   * Resume a pending sale (load it as current sale)
   * @param saleId - ID of the pending sale to resume
   */
  resumePendingSale(saleId: SaleId): void {
    this.store.setLoading(true);

    this.apiService
      .findSale(saleId)
      .pipe(
        catchError(error => {
          console.error('Error loading pending sale:', error);
          this.notificationService.error('Erreur lors de la reprise de la vente');
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
          this.store.removePendingSale(saleId);
          this.store.setIsEdit(true);

          // Load customer if exists
          if (sale.customer) {
            this.store.setSelectedCustomer(sale.customer);
          }
        }
      });
  }

  /**
   * Delete a pending sale permanently
   * @param saleId - ID of the pending sale to delete
   */
  deletePendingSale(saleId: SaleId): void {
    this.store.setLoading(true);

    this.apiService
      .deleteSale(saleId)
      .pipe(
        catchError(error => {
          console.error('Error deleting pending sale:', error);
          this.notificationService.error('Erreur lors de la suppression de la vente');
          this.store.setLoading(false);
          return of(undefined);
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(() => {
        this.store.removePendingSale(saleId);
      });
  }

  // ============================================
  // PRINTING (IMPRESSION) - Délègue au PrintService
  // ============================================

  /**
   * Print sale invoice (PDF)
   * @param saleId - ID of the sale to print
   */
  printInvoice(saleId: SaleId): void {
    this.printService.printInvoice(saleId);
  }

  /**
   * Print sale receipt (thermal printer format)
   * @param saleId - ID of the sale to print
   */
  printReceipt(saleId: SaleId): void {
    this.printService.printReceipt(saleId).subscribe();
  }

  /**
   * Print current sale (invoice or receipt based on preferences)
   */
  printCurrentSale(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      this.notificationService.warning('Aucune vente à imprimer');
      return;
    }

    const shouldPrintInvoice = this.store.printInvoice();
    const shouldPrintReceipt = this.store.printReceipt();

    if (shouldPrintInvoice) {
      this.printInvoice(currentSale.saleId);
    }

    if (shouldPrintReceipt) {
      this.printReceipt(currentSale.saleId);
    }

    if (!shouldPrintInvoice && !shouldPrintReceipt) {
      // Par défaut, imprimer le ticket
      this.printReceipt(currentSale.saleId);
    }
  }

  // ============================================
  // SIMPLE STATE UPDATES (Pass-through)
  // ============================================

  setCurrentSale = this.store.setCurrentSale.bind(this.store);
  setSelectedCustomer = this.store.setSelectedCustomer.bind(this.store);
  setCashier = this.store.setCashier.bind(this.store);
  setSeller = this.store.setSeller.bind(this.store);
  setSaleType = this.store.setSaleType.bind(this.store);
  setIsEdit = this.store.setIsEdit.bind(this.store);
  setTypePrescription = this.store.setTypePrescription.bind(this.store);
  setPaymentMode = this.store.setPaymentMode.bind(this.store);
  setPrintInvoice = this.store.setPrintInvoice.bind(this.store);
  setPrintReceipt = this.store.setPrintReceipt.bind(this.store);
  addSalesLine = this.store.addSalesLine.bind(this.store);
  updateSalesLine = this.store.updateSalesLine.bind(this.store);
  removeSalesLine = this.store.removeSalesLine.bind(this.store);
  addPendingSale = this.store.addPendingSale.bind(this.store);
  removePendingSale = this.store.removePendingSale.bind(this.store);
  toggleInsuranceDataBar = this.store.toggleInsuranceDataBar.bind(this.store);
  toggleSidebar = this.store.toggleSidebar.bind(this.store);
  clearError = this.store.clearError.bind(this.store);
  reset = this.store.reset.bind(this.store);
  resetCurrentSale = this.store.resetCurrentSale.bind(this.store);
}
