import { computed, inject, Injectable } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap, catchError, of, debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISales, Sales, SaleId } from '../../../../shared/model/sales.model';
import { createSalesLineFromProduct } from '../utils/sales-line.utils';
import { ISalesLine, SalesLine } from '../../../../shared/model/sales-line.model';
import { ICustomer } from '../../../../shared/model/customer.model';
import { ProduitSearch } from '../../../../shared/model/produit.model';
import { IRemise } from '../../../../shared/model/remise.model';

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

  /** Is edit mode */
  readonly isEdit = this.store.isEdit;

  /** Is saving */
  readonly isSaving = this.store.isSaving;

  /** Loading state */
  readonly loading = this.store.loading;

  /** Error message */
  readonly error = this.store.error;

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

  // Computed amounts - expose from store
  readonly totalAmount = computed(() => this.currentSale()?.salesAmount || 0);
  readonly discountAmount = computed(() => this.currentSale()?.discountAmount || 0);
  readonly taxAmount = computed(() => this.currentSale()?.taxAmount || 0);
  readonly netAmount = computed(() => this.currentSale()?.netAmount || 0);
  readonly amountToBePaid = computed(() => this.currentSale()?.amountToBePaid || 0);
  readonly canSaveSale = this.canSave;

  // ============================================
  // BUSINESS ACTIONS (High-level)
  // ============================================

  /**
   * Create a new comptant sale
   * @param initialLine - REQUIRED first product to add to the sale (cannot create empty sale)
   */
  createComptantSale = rxMethod<ISalesLine>(
    pipe(
      tap(() => {
        this.store.setSaleType('COMPTANT');
        this.store.setError(null);
      }),
      switchMap((initialLine) => {
        if (!initialLine) {
          throw new Error('Impossible de créer une vente sans produit');
        }

        const sale: ISales = {
          type: 'COMPTANT',
          cassierId: this.store.cashier()?.id,
          sellerId: this.store.seller()?.id,
          customerId: this.store.selectedCustomer()?.id,
          salesLines: [initialLine],
        };

        this.store.setLoading(true);
        return this.apiService.createComptantSale(sale);
      }),
      tap({
        next: (sale) => {
          this.store.setCurrentSale(sale);
          this.store.setIsEdit(true);
          this.store.setLoading(false);
        },
        error: (error) => {
          this.store.setError(error.message || 'Erreur lors de la création de la vente');
          this.store.setLoading(false);
        },
      }),
      catchError((error) => {
        console.error('Error creating comptant sale:', error);
        return of(null);
      })
    )
  );

  /**
   * Create a new insurance sale
   * @param initialLine - REQUIRED first product to add to the sale (cannot create empty sale)
   */
  createAssuranceSale = rxMethod<ISalesLine>(
    pipe(
      tap(() => {
        this.store.setSaleType('ASSURANCE');
        this.store.setError(null);
      }),
      switchMap((initialLine) => {
        if (!initialLine) {
          throw new Error('Impossible de créer une vente sans produit');
        }

        const sale: ISales = {
          type: 'ASSURANCE',
          cassierId: this.store.cashier()?.id,
          sellerId: this.store.seller()?.id,
          customerId: this.store.selectedCustomer()?.id,
          salesLines: [initialLine],
        };

        this.store.setLoading(true);
        return this.apiService.createAssuranceSale(sale);
      }),
      tap({
        next: (sale) => {
          this.store.setCurrentSale(sale);
          this.store.setIsEdit(true);
          this.store.setLoading(false);
        },
        error: (error) => {
          this.store.setError(error.message || 'Erreur lors de la création de la vente assurance');
          this.store.setLoading(false);
        },
      }),
      catchError((error) => {
        console.error('Error creating assurance sale:', error);
        return of(null);
      })
    )
  );

  /**
   * Save current sale (finalize)
   */
  saveSale = rxMethod<void>(
    pipe(
      tap(() => { this.store.setIsSaving(true); this.store.setError(null); }),
      switchMap(() => {
        const currentSale = this.store.currentSale();
        if (!currentSale) {
          throw new Error('Aucune vente en cours');
        }

        // Calculer les montants avant sauvegarde
        this.calculateSaleAmounts(currentSale);

        // Appliquer le flag avoir automatiquement
        currentSale.avoir = this.store.isAvoir();

        // Vérifier si client obligatoire pour avoir
        if (currentSale.avoir && !currentSale.customerId) {
          throw new Error('Un client est obligatoire pour une vente avec avoir (livraison partielle)');
        }

        const saleType = this.store.saleType();
        
        if (saleType === 'COMPTANT') {
          return this.apiService.saveCashSale(currentSale);
        } else if (saleType === 'ASSURANCE') {
          return this.apiService.saveAssuranceSale(currentSale);
        } else {
          throw new Error('Type de vente non supporté');
        }
      }),
      tap({
        next: (result) => {
          // Print if needed
          const printInvoice = this.store.printInvoice();
          const printReceipt = this.store.printReceipt();
          
          if (printInvoice && result.saleId) {
            this.apiService.printInvoice(result.saleId).subscribe();
          }
          
          if (printReceipt && result.saleId) {
            this.apiService.printReceipt(result.saleId).subscribe();
          }

          // Reset after successful save
          this.store.resetCurrentSale();
          this.store.setIsSaving(false);
        },
        error: (error) => {
          this.store.setError(error.message || 'Erreur lors de l\'enregistrement de la vente');
          this.store.setIsSaving(false);
        },
      }),
      catchError((error) => {
        console.error('Error saving sale:', error);
        return of(null);
      })
    )
  );

  /**
   * Calculate sale amounts before saving
   */
  private calculateSaleAmounts(sale: ISales): void {
    const montantVerse = Number(sale.montantVerse) || 0;
    const amountToBePaid = Number(sale.amountToBePaid) || 0;
    const restToPay = amountToBePaid - montantVerse;

    // Montant payé = minimum entre montant versé et montant à payer
    sale.payrollAmount = restToPay <= 0 ? amountToBePaid : montantVerse;
    
    // Reste à payer = maximum entre 0 et le reste
    sale.restToPay = Math.max(restToPay, 0);
    
    // Monnaie rendue = si montant versé > montant dû
    sale.montantRendu = restToPay < 0 ? Math.abs(restToPay) : 0;
  }

  /**
   * Load sale for editing
   */
  loadSaleForEdit = rxMethod<SaleId>(
    pipe(
      tap(() => { this.store.setLoading(true); this.store.setError(null); }),
      switchMap((saleId) => this.apiService.findSaleForEdit(saleId)),
      tap({
        next: (sale) => {
          this.store.setCurrentSale(sale);
          this.store.setIsEdit(true);
          
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
        },
        error: (error) => {
          this.store.setError(error.message || 'Erreur lors du chargement de la vente');
          this.store.setLoading(false);
        },
      }),
      catchError((error) => {
        console.error('Error loading sale:', error);
        return of(null);
      })
    )
  );

  /**
   * Cancel current sale
   */
  cancelSale = rxMethod<void>(
    pipe(
      tap(() => { this.store.setLoading(true); this.store.setError(null); }),
      switchMap(() => {
        const currentSale = this.store.currentSale();
        if (!currentSale) {
          throw new Error('Aucune vente en cours');
        }
        return this.apiService.cancelSale(currentSale);
      }),
      tap({
        next: () => {
          this.store.resetCurrentSale();
          this.store.setLoading(false);
        },
        error: (error) => {
          this.store.setError(error.message || 'Erreur lors de l\'annulation de la vente');
          this.store.setLoading(false);
        },
      }),
      catchError((error) => {
        console.error('Error canceling sale:', error);
        return of(null);
      })
    )
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
    this.store.resetCurrentSale();
    this.createComptantSale(initialLine);
  }

  /**
   * Initialize a new assurance sale (without initial product - customer required first)
   */
  initializeAssuranceSale(): void {
    this.store.setSaleType('ASSURANCE');
    this.store.resetCurrentSale();
    // Ne pas créer la vente backend tant qu'il n'y a pas de client + produit
  }

  /**
   * Save assurance sale with payment modes and tiers payants
   * @param paymentModes - Payment modes used
   */
  saveAssuranceSale(paymentModes: any[]): void {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      this.notificationService.error('Erreur', 'Aucune vente en cours');
      return;
    }

    if (!currentSale.customerId) {
      this.notificationService.error('Erreur', 'Client obligatoire pour vente ASSURANCE');
      return;
    }

    // TODO: Ajouter les payment modes et tiers payants à la vente
    currentSale.payments = paymentModes;
    
    this.saveSale();
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
    const currentSale = this.store.currentSale();
    if (!currentSale || !currentSale.saleId) {
      console.error('No current sale to add product to');
      return;
    }

    this.store.setLoading(true);

    this.apiService.addItemComptant(salesLine)
      .pipe(
        switchMap(() => this.apiService.findSaleForEdit(currentSale.saleId!)),
        catchError(error => {
          console.error('Error adding product:', error);
          this.store.setError('Erreur lors de l\'ajout du produit');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Remove line by saleLineId (matches original removeLine)
   * @param saleLineId - Composite ID of the line to remove
   */
  removeLine(saleLineId: any): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) return;

    this.store.setLoading(true);

    this.apiService.deleteItem(saleLineId)
      .pipe(
        switchMap(() => this.apiService.findSaleForEdit(currentSale.saleId!)),
        catchError(error => {
          console.error('Error removing line:', error);
          this.store.setError('Erreur lors de la suppression de la ligne');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
      });
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
    this.apiService.addItemComptant(newLine)
      .pipe(
        switchMap(() => {
          // Recharger la vente complète avec tous les montants recalculés
          return this.apiService.findSaleForEdit(currentSale.saleId!);
        }),
        catchError(error => {
          console.error('Error adding product:', error);
          this.notificationService.error('Erreur lors de l\'ajout du produit');
          this.store.setError('Erreur lors de l\'ajout du produit');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
        this.setSelectedProduct(null); // Clear selection
      });
  }

  /**
   * Update line quantity
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  updateLineQuantity(lineId: number, newQuantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) return;

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) return;

    const updatedLine: ISalesLine = {
      ...line,
      quantitySold: newQuantity,
      saleCompositeId: currentSale.saleId,
    };

    this.store.setLoading(true);

    // Backend recalcule tous les montants
    this.apiService.updateItemQtySold(updatedLine)
      .pipe(
        switchMap(() => {
          // Recharger la vente complète
          return this.apiService.findSaleForEdit(currentSale.saleId!);
        }),
        catchError(error => {
          console.error('Error updating quantity:', error);
          this.notificationService.error('Erreur lors de la mise à jour de la quantité');
          this.store.setError('Erreur lors de la mise à jour de la quantité');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Update line quantity requested
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

    // Backend recalcule tous les montants
    this.apiService.updateItemQtyRequested(updatedLine)
      .pipe(
        switchMap(() => {
          // Recharger la vente complète
          return this.apiService.findSaleForEdit(currentSale.saleId!);
        }),
        catchError(error => {
          console.error('Error updating quantity requested:', error);
          this.notificationService.error('Erreur lors de la mise à jour de la quantité demandée');
          this.store.setError('Erreur lors de la mise à jour de la quantité demandée');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
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

    this.store.setLoading(true);

    // Backend recalcule tous les montants
    this.apiService.updateItemPrice(updatedLine)
      .pipe(
        switchMap(() => {
          // Recharger la vente complète
          return this.apiService.findSaleForEdit(currentSale.saleId!);
        }),
        catchError(error => {
          console.error('Error applying discount:', error);
          this.notificationService.error('Erreur lors de l\'application de la remise');
          this.store.setError('Erreur lors de l\'application de la remise');
          this.store.setLoading(false);
          return of(null);
        })
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Update global remise (discount) on current sale
   * @param remise - Remise to apply, or undefined to remove remise
   */
  updateRemise(remise?: IRemise): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      this.notificationService.error('Aucune vente en cours');
      return;
    }

    this.store.setLoading(true);

    const action$ = remise
      ? this.apiService.addRemise({ id: currentSale.saleId, value: remise.id! })
      : this.apiService.removeRemiseFromCashSale(currentSale.saleId);

    action$.pipe(
      switchMap(() => {
        // Recharger la vente complète avec les montants recalculés
        return this.apiService.findSaleForEdit(currentSale.saleId!);
      }),
      catchError(error => {
        console.error('Error updating remise:', error);
        this.notificationService.error('Erreur lors de la mise à jour de la remise');
        this.store.setError('Erreur lors de la mise à jour de la remise');
        this.store.setLoading(false);
        return of(null);
      })
    ).subscribe(sale => {
      if (sale) {
        this.store.setCurrentSale(sale);
      }
      this.store.setLoading(false);
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
    if (!currentSale?.saleId) {
      return;
    }

    this.store.setLoading(true);
    this.store.setSelectedCustomer(customer);

    // Update sale with customer ID
    const updatedSale: ISales = {
      ...currentSale,
      customerId: customer.id,
    };

    // Determine which endpoint to use based on sale type
    const updateObservable$ =
      currentSale.natureVente === 'VO'
        ? this.apiService.updateComptantSale(updatedSale)
        : this.apiService.updateAssuranceSale(updatedSale);

    updateObservable$
      .pipe(
        switchMap(() => this.apiService.findSaleForEdit(currentSale.saleId!)),
        catchError(error => {
          console.error('Error setting customer:', error);
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
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
      currentSale.natureVente === 'VO'
        ? this.apiService.updateComptantSale(updatedSale)
        : this.apiService.updateAssuranceSale(updatedSale);

    updateObservable$
      .pipe(
        switchMap(() => this.apiService.findSaleForEdit(currentSale.saleId!)),
        catchError(error => {
          console.error('Error removing customer:', error);
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(sale => {
        if (sale) {
          this.store.setCurrentSale(sale);
        }
      });
  }

  // ============================================
  // PENDING SALES (MISE EN ATTENTE)
  // ============================================

  /**
   * Put current sale on standby (prévente)
   * Saves sale with STANDBY status and resets current sale
   */
  putOnStandby(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      console.error('No current sale to put on standby');
      return;
    }

    this.store.setLoading(true);

    // Determine which endpoint to use based on sale type
    const standbyObservable$ =
      currentSale.natureVente === 'VO'
        ? this.apiService.putComptantOnStandby(currentSale.saleId)
        : this.apiService.putAssuranceOnStandby(currentSale.saleId);

    standbyObservable$
      .pipe(
        catchError(error => {
          console.error('Error putting sale on standby:', error);
          this.notificationService.error('Erreur lors de la mise en attente');
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(sale => {
        if (sale) {
          this.store.addPendingSale(sale);
          this.resetCurrentSale();
          this.notificationService.success('Vente mise en attente avec succès');
        }
      });
  }

  /**
   * Load pending sales from backend
   */
  loadPendingSales(): void {
    this.store.setLoading(true);

    this.apiService.getPendingSales()
      .pipe(
        catchError(error => {
          console.error('Error loading pending sales:', error);
          this.store.setError('Erreur lors du chargement des ventes en attente');
          this.store.setLoading(false);
          return of([]);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(sales => {
        sales.forEach(sale => this.store.addPendingSale(sale));
      });
  }

  /**
   * Resume a pending sale (load it as current sale)
   * @param saleId - ID of the pending sale to resume
   */
  resumePendingSale(saleId: SaleId): void {
    this.store.setLoading(true);

    this.apiService.findSaleForEdit(saleId)
      .pipe(
        catchError(error => {
          console.error('Error loading pending sale:', error);
          this.notificationService.error('Erreur lors de la reprise de la vente');
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
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

    this.apiService.deleteSale(saleId)
      .pipe(
        catchError(error => {
          console.error('Error deleting pending sale:', error);
          this.notificationService.error('Erreur lors de la suppression de la vente');
          this.store.setLoading(false);
          return of(undefined);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(() => {
        this.store.removePendingSale(saleId);
        this.notificationService.success('Vente supprimée avec succès');
      });
  }

  // ============================================
  // PRINTING (IMPRESSION)
  // ============================================

  /**
   * Print sale invoice (PDF)
   * Opens invoice in new window or downloads it
   * @param saleId - ID of the sale to print
   */
  printInvoice(saleId: SaleId): void {
    this.store.setLoading(true);

    this.apiService.printInvoice(saleId)
      .pipe(
        catchError(error => {
          console.error('Error printing invoice:', error);
          this.notificationService.error('Erreur lors de l\'impression de la facture');
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(blob => {
        if (blob) {
          this.downloadOrOpenBlob(blob, `facture_${saleId.id}.pdf`, 'application/pdf');
        }
      });
  }

  /**
   * Print sale receipt (thermal printer format)
   * Opens receipt in new window or downloads it
   * @param saleId - ID of the sale to print
   */
  printReceipt(saleId: SaleId): void {
    this.store.setLoading(true);

    this.apiService.printReceipt(saleId)
      .pipe(
        catchError(error => {
          console.error('Error printing receipt:', error);
          this.notificationService.error('Erreur lors de l\'impression du ticket');
          this.store.setLoading(false);
          return of(null);
        }),
        finalize(() => this.store.setLoading(false))
      )
      .subscribe(blob => {
        if (blob) {
          this.downloadOrOpenBlob(blob, `ticket_${saleId.id}.pdf`, 'application/pdf');
        }
      });
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

  /**
   * Helper to download or open blob in new window
   * @param blob - The blob to handle
   * @param filename - Default filename for download
   * @param mimeType - MIME type of the blob
   */
  private downloadOrOpenBlob(blob: Blob, filename: string, mimeType: string): void {
    const url = window.URL.createObjectURL(new Blob([blob], { type: mimeType }));
    
    // Try to open in new window first
    const printWindow = window.open(url, '_blank');
    
    if (printWindow) {
      // Window opened successfully
      printWindow.onload = () => {
        printWindow.focus();
        printWindow.print();
      };
    } else {
      // Popup blocked, fallback to download
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      window.URL.revokeObjectURL(url);
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
  reset = this.store.reset.bind(this.store);
  resetCurrentSale = this.store.resetCurrentSale.bind(this.store);
}

