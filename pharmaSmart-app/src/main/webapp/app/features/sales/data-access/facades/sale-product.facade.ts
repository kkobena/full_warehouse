import { inject, Injectable } from '@angular/core';
import { catchError, EMPTY, finalize, Observable, switchMap, tap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { SaleId } from '../../../../shared/model/sales.model';
import { IRemise, ISalesLine, ProduitSearch } from '../../../../shared/model';
import { createSalesLineFromProduct } from '../utils/sales-line.utils';
import { extractApiError, handlePlafondVenteWarning } from './sale-facade.utils';

/**
 * Options pour executeAndReloadSale
 */
interface ExecuteAndReloadOptions {
  errorMessage: string;
  successEvent?: 'LINE_UPDATED' | 'LINE_REMOVED' | 'REMISE_UPDATED';
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

  // ── Public methods ─────────────────────────────────────────

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
          this.store.setLastErrorDetails({ errorKey, originalError: error, attemptedLine: newLine, reserveInfo: error.error?.payload });
          this.notificationService.error(errorMessage);
          return EMPTY;
        }),
        finalize(() => {
          this.store.setLoading(false);
          this.setSelectedProduct(null);
        }),
      )
      .subscribe(sale => {
        this.store.setCurrentSale(sale);
        this.store.emitEvent('PRODUCT_ADDED');
      });
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemComptant(salesLine));
  }

  onAddProduitCarnet(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemAssurance(salesLine));
  }

  onAddProduitDevis(salesLine: ISalesLine): void {
    this.addProductWithStockHandling(salesLine, this.apiService.addItemComptant(salesLine));
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    const saleType = this.store.saleType();
    const apiCall =
      saleType === 'ASSURANCE' || saleType === 'CARNET'
        ? this.apiService.incrementItemQtyRequestedAssurance(salesLine)
        : this.apiService.incrementItemQtyRequested(salesLine);

    this.executeQtyUpdate(true, apiCall);
  }

  updateItemQtyRequestedWithSet(salesLine: ISalesLine): void {
    const saleType = this.store.saleType();
    const apiCall =
      saleType === 'ASSURANCE' || saleType === 'CARNET'
        ? this.apiService.setItemQtyRequestedAssurance(salesLine)
        : this.apiService.setItemQtyRequested(salesLine);

    this.executeQtyUpdate(false, apiCall);
  }

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
        successEvent: 'LINE_REMOVED',
      },
    );
  }

  setSelectedProduct(product: any | null): void {
    this.store.setSelectedProductData(product);
  }

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
      successEvent: 'LINE_UPDATED',
    });
  }

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
          } else if (errorKey !== 'stockChInsufisant') {
            this.notificationService.error(errorMessage);
          }

          this.store.setError(errorMessage);
          this.store.setLastErrorDetails({
            errorKey: errorKey || null,
            originalError: error,
            attemptedLine: updatedLine,
            isFromTableCellEdit: true,
            reserveInfo: error.error?.payload,
          });
          return EMPTY;
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        this.store.setCurrentSale(sale);
        this.store.clearError();
        this.store.emitEvent('LINE_UPDATED');
      });
  }

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
      successEvent: 'REMISE_UPDATED',
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
                  reserveInfo: error.error?.payload,
                });
              }),
              catchError(reloadError => {
                console.error('Error reloading sale after stock error:', reloadError);
                return EMPTY;
              }),
              switchMap(() => EMPTY),
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
            reserveInfo: error.error?.payload,
          });
          if (errorKey !== 'stockChInsufisant') {
            this.notificationService.error(errorMessage);
          }
          return EMPTY;
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        this.store.setCurrentSale(sale);
        this.store.clearError();
        this.store.emitEvent('PRODUCT_ADDED');
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
          return EMPTY;
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        this.store.setCurrentSale(sale);
        this.store.clearError();
        this.store.emitEvent(isAjoutProduit ? 'PRODUCT_ADDED' : 'LINE_UPDATED');
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
          return EMPTY;
        }),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe(sale => {
        this.store.setCurrentSale(sale);
        if (options.clearErrorOnSuccess) {
          this.store.clearError();
        }
        if (options.successEvent) {
          this.store.emitEvent(options.successEvent);
        }
      });
  }
}
