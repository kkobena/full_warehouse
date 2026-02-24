import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, Subject, switchMap, tap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISales, SaleId } from '../../../../shared/model/sales.model';
import { IRemise, ISalesLine, ProduitSearch } from '../../../../shared/model';
import { createSalesLineFromProduct } from '../utils/sales-line.utils';
import { extractApiError, handlePlafondVenteWarning } from './sale-facade.utils';

/**
 * Options pour executeAndReloadSale
 */
interface ExecuteAndReloadOptions {
  errorMessage: string;
  successSubject?: Subject<void>;
  clearErrorOnSuccess?: boolean;
}

/**
 * Product Facade — Produits, quantités, prix, remises
 */
@Injectable({ providedIn: 'root' })
export class SaleProductFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);

  // ── Subjects ───────────────────────────────────────────────
  private readonly productAddedSuccessSubject = new Subject<void>();
  readonly productAddedSuccess$ = this.productAddedSuccessSubject.asObservable();

  private readonly lineUpdatedSuccessSubject = new Subject<void>();
  readonly lineUpdatedSuccess$ = this.lineUpdatedSuccessSubject.asObservable();

  private readonly lineRemovedSuccessSubject = new Subject<void>();
  readonly lineRemovedSuccess$ = this.lineRemovedSuccessSubject.asObservable();

  private readonly remiseUpdatedSuccessSubject = new Subject<void>();
  readonly remiseUpdatedSuccess$ = this.remiseUpdatedSuccessSubject.asObservable();

  // ── Public methods ─────────────────────────────────────────

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

    const newLine: ISalesLine = createSalesLineFromProduct(product, quantity, currentSale);

    this.store.setLoading(true);

    this.apiService
      .addItemComptant(newLine)
      .pipe(
        switchMap(() => this.apiService.findSale(currentSale.saleId!)),
        catchError(error => {
          console.error('Error adding product:', error);
          const { errorMessage, errorKey } = extractApiError(error, "Erreur lors de l'ajout du produit");

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
        this.setSelectedProduct(null);
      });
  }

  /**
   * Add product to existing sale (matches original onAddProduit)
   */
  onAddProduit(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemComptant(salesLine));
  }

  /**
   * Add product to existing CARNET sale
   * Uses /add-item/assurance endpoint (shared by ASSURANCE and CARNET)
   */
  onAddProduitCarnet(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemAssurance(salesLine));
  }

  /**
   * Add product to existing DEVIS sale
   * Uses /add-item/comptant endpoint (same as COMPTANT)
   */
  onAddProduitDevis(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemComptant(salesLine));
  }

  /**
   * Update item quantity with force stock (matches original processQtyRequested)
   * Uses INCREMENT endpoint to add to existing quantity
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
   */
  removeLine(saleLineId: any): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      return;
    }
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
   * Update line quantity sold
   * Envoie au backend pour recalcul des montants, puis recharge la vente
   */
  updateLineQuantitySold(lineId: number, newQuantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }

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
   */
  updateLineQuantityRequested(lineId: number, newQuantity: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }

    const updatedLine: ISalesLine = {
      ...line,
      quantityRequested: newQuantity,
      saleCompositeId: currentSale.saleId,
    };

    this.store.setLoading(true);

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

          const { errorMessage, errorKey } = extractApiError(error, 'Erreur lors de la mise à jour de la quantité demandée');

          if (errorKey === 'stock') {
            if (!updatedLine.saleCompositeId && currentSale.saleId) {
              updatedLine.saleCompositeId = {
                id: currentSale.saleId.id,
                saleDate: currentSale.saleId.saleDate,
              };
            }

            this.store.setError(errorMessage);
            this.store.setLastErrorDetails({
              errorKey,
              originalError: error,
              attemptedLine: updatedLine,
              isFromTableCellEdit: true,
            });
            this.store.setLoading(false);
            return of(null);
          } else {
            if (errorKey !== 'stockChInsufisant') {
              this.notificationService.error(errorMessage);
            }

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
          this.store.clearError();
          this.lineUpdatedSuccessSubject.next();
        }
        this.store.setLoading(false);
      });
  }

  /**
   * Update line unit price
   */
  updateLinePrice(lineId: number, newPrice: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }

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
   */
  applyLineDiscount(lineId: number, discountAmount: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }

    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }

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

  // ── Private helpers ────────────────────────────────────────

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
          const { errorMessage, errorKey } = extractApiError(error, "Erreur lors de l'ajout du produit");

          if (errorKey === 'stock') {
            return this.apiService.findSale(currentSale.saleId!).pipe(
              tap(reloadedSale => {
                if (reloadedSale) {
                  this.store.setCurrentSale(reloadedSale);

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
          if (errorKey === 'customerInsuranceCreditLimit') {
            handlePlafondVenteWarning(this.store, this.notificationService, errorMessage);
            return this.apiService.findSale(currentSale.saleId!);
          }

          this.store.setError(errorMessage);
          this.store.setLastErrorDetails({
            errorKey,
            originalError: error,
            attemptedLine: salesLine,
            isFromTableCellEdit: false,
          });
          if (errorKey !== 'stockChInsufisant') {
            this.notificationService.error(errorMessage);
          }
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
          const { errorMessage, errorKey } = extractApiError(error, 'Erreur lors de la mise à jour du produit');

          if (errorKey === 'customerInsuranceCreditLimit') {
            handlePlafondVenteWarning(this.store, this.notificationService, errorMessage);
            return this.apiService.findSale(currentSale.saleId!);
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
}
