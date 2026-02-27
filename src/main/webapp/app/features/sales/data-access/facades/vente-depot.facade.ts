import { inject, Injectable, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, finalize, map, switchMap } from 'rxjs/operators';

import { VenteDepotStore } from '../store/vente-depot.store';
import { VenteDepotApiService } from '../services/vente-depot-api.service';
import { SalesApiService } from '../services/sales-api.service';
import { MagasinService } from '../../../../entities/magasin/magasin.service';
import { ConfigurationService } from '../../../../shared/configuration.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { FinalyseSale, ISales, SaleId, Sales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../../../shared/model/sales-line.model';
import { IRemise, ProduitSearch } from '../../../../shared/model';
import { IMagasin } from '../../../../shared/model';
import { IUser } from '../../../../core/user/user.model';

/**
 * Façade orchestrant les ventes dépôt dans le module features/sales.
 *
 * Délègue les appels HTTP à VenteDepotApiService et SalesApiService.
 * Centralise l'état dans VenteDepotStore (ngrx/signals).
 * Expose des Observables typés via toObservable(store.lastEvent) que
 * le composant consomme pour réagir aux résultats des opérations.
 *
 * Expose aussi les méthodes/signals nécessaires pour la compatibilité
 * avec les mixins (product-handling, force-stock, deconditionnement).
 */
@Injectable({ providedIn: 'root' })
export class VenteDepotFacade {
  private readonly store = inject(VenteDepotStore);
  private readonly api = inject(VenteDepotApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly magasinService = inject(MagasinService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  // ── Délégation état Store ────────────────────────────────────
  readonly currentSale = this.store.currentSale;
  readonly selectedDepot = this.store.selectedDepot;
  readonly depots = this.store.depots;
  readonly quantityMax = this.store.quantityMax;
  readonly canReceipt = this.store.canReceipt;
  readonly canInvoice = this.store.canInvoice;
  readonly cashier = this.store.cashier;
  readonly seller = this.store.seller;
  readonly loading = this.store.loading;
  readonly error = this.store.error;
  readonly isAvoir = this.store.isAvoir;
  readonly isEmpty = this.store.isEmpty;
  readonly canSave = this.store.canSave;
  readonly salesLines = this.store.salesLines;
  readonly selectedProduct = this.store.selectedProduct;
  readonly errorDetails = this.store.errorDetails;

  /** Alias pour compatibilité avec le mixin force-stock (attend `lastError`). */
  readonly lastError = this.store.error;

  /** Pas de client pour les ventes dépôt — signal toujours null. */
  readonly selectedCustomer = signal(null).asReadonly();

  // ── Observables typés (remplacent les Subjects) ──────────────

  readonly productOpResult$ = toObservable(this.store.lastEvent).pipe(
    filter(e => e?.type === 'PRODUCT_ADDED'),
    map(e => e!.payload as SaveResponse),
  );

  /** Observable filtré sur les ajouts réussis (pour product-handling mixin). */
  readonly productAddedSuccess$ = this.productOpResult$.pipe(filter(r => r.success));

  /** Observable filtré sur les mises à jour de lignes réussies. */
  readonly lineUpdatedSuccess$ = toObservable(this.store.lastEvent).pipe(
    filter(e => e?.type === 'LINE_UPDATED'),
    map(e => e!.payload as SaveResponse),
    filter(r => r.success),
  );

  /** Observable émis après un rechargement de vente (pour force-stock cancel en mode editCell). */
  readonly saleReloadedSuccess$ = toObservable(this.store.lastEvent).pipe(
    filter(e => e?.type === 'SALE_RELOADED' as any),
    map((): void => undefined),
  );

  readonly saleFinalized$ = toObservable(this.store.lastEvent).pipe(
    filter(e => e?.type === 'SALE_FINALIZED'),
    map(e => e!.payload as FinalyseSale),
  );

  // ── Initialisation ───────────────────────────────────────────

  init(): void {
    this.loadDepots();
    this.loadQuantityMax();
  }

  // ── Mutations état ───────────────────────────────────────────

  resetForNewSession(): void {
    this.store.resetForNewSession();
  }

  reset(): void {
    this.store.reset();
  }

  setSeller(user: IUser | null): void {
    this.store.setSeller(user);
  }

  setCashier(user: IUser | null): void {
    this.store.setCashier(user);
  }

  setSelectedDepot(depot: IMagasin | null): void {
    this.store.setSelectedDepot(depot);
  }

  setSelectedProduct(product: ProduitSearch | null): void {
    this.store.setSelectedProduct(product);
  }

  clearError(): void {
    this.store.clearError();
  }

  // ── Opérations métier ────────────────────────────────────────

  create(salesLine: ISalesLine): void {
    this.store.setLoading(true);
    this.api
      .create(this.buildNewSale(salesLine))
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: (res: HttpResponse<ISales>) => {
          this.store.setCurrentSale(res.body);
          this.store.emitEvent('PRODUCT_ADDED', { success: true });
        },
        error: err => this.handleError(err, salesLine),
      });
  }

  addItemToSale(salesLine: ISalesLine): void {
    const saleId = this.store.currentSale()!.saleId;
    this.handleSaleUpdate(
      this.api.addItem({ ...salesLine, saleCompositeId: saleId }).pipe(switchMap(() => this.salesApi.findSale(saleId))),
      salesLine,
    );
  }

  addOrIncrementProduct(salesLine: ISalesLine): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines) {
      this.addItemToSale(salesLine);
      return;
    }
    const existingLine = currentSale.salesLines.find(l => l.produitId === salesLine.produitId);
    if (existingLine) {
      this.updateItemQtyRequested({ ...existingLine, quantityRequested: salesLine.quantityRequested });
    } else {
      this.addItemToSale(salesLine);
    }
  }

  removeItemFromSale(id: SaleLineId): void {
    const saleId = this.store.currentSale()!.saleId;
    this.handleSaleUpdate(this.api.deleteItem(id).pipe(switchMap(() => this.salesApi.findSale(saleId))));
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    const saleId = this.store.currentSale()!.saleId;
    this.handleSaleUpdate(
      this.api.incrementItemQtyRequested({ ...salesLine, saleCompositeId: saleId }).pipe(switchMap(() => this.salesApi.findSale(saleId))),
      salesLine,
    );
  }



  updateLineQuantityRequested(lineId: number, newQty: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }
    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }
    const updatedLine: ISalesLine = { ...line, quantityRequested: newQty, saleCompositeId: currentSale.saleId };
    this.store.setLoading(true);
    this.api
      .setItemQtyRequested(updatedLine)
      .pipe(
        switchMap(() => this.salesApi.findSale(currentSale.saleId)),
        finalize(() => this.store.setLoading(false)),
      )
      .subscribe({
        next: (sale: ISales) => {
          this.store.setCurrentSale(sale);
          this.store.clearError();
          this.store.emitEvent('LINE_UPDATED', { success: true });
        },
        error: err => {
          const errorKey = err?.error?.errorKey ?? null;
          this.store.setError(errorKey);
          this.store.setErrorDetails({
            errorKey,
            originalError: err,
            attemptedLine: updatedLine,
            isFromTableCellEdit: true,
          });
          this.store.emitEvent('PRODUCT_ADDED', { success: false, error: err, payload: updatedLine });
        },
      });
  }

  updateLineQuantitySold(lineId: number, newQty: number): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.salesLines || !currentSale.saleId) {
      return;
    }
    const line = currentSale.salesLines.find(l => l.id === lineId);
    if (!line) {
      return;
    }
    const updatedLine: ISalesLine = { ...line, quantitySold: newQty, saleCompositeId: currentSale.saleId };
    this.handleSaleUpdate(
      this.api.updateItemQtySold(updatedLine).pipe(switchMap(() => this.salesApi.findSale(currentSale.saleId))),
    );
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
    const updatedLine: ISalesLine = { ...line, regularUnitPrice: newPrice, saleCompositeId: currentSale.saleId };
    this.handleSaleUpdate(
      this.api.updateItemPrice(updatedLine).pipe(switchMap(() => this.salesApi.findSale(currentSale.saleId))),
    );
  }

  updateRemise(remise?: IRemise): void {
    const saleId = this.store.currentSale()!.saleId;
    const action$ = remise ? this.api.addRemise({ id: saleId, value: remise.id }) : this.api.removeRemise(saleId);
    this.handleSaleUpdate(action$.pipe(switchMap(() => this.salesApi.findSale(saleId))));
  }

  changeDepot(saleId: SaleId, depotId: number): Observable<HttpResponse<SaleId>> {
    return this.api.changeDepot(saleId, depotId);
  }

  cancelSale(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      this.store.resetForNewSession();
      return;
    }
    this.store.setLoading(true);
    this.api
      .deletePrevente(currentSale.saleId)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: () => this.store.resetForNewSession(),
        error: err => this.handleError(err),
      });
  }

  finalizeSale(): void {
    const currentSale = this.store.currentSale()!;
    currentSale.payments = [];
    currentSale.type = 'DEPOT';
    currentSale.avoir = this.store.isAvoir();
    this.save(currentSale);
  }

  printInvoice(saleId: SaleId): void {
    this.store.setLoading(true);
    this.api
      .printInvoice(saleId)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe(blob => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl);
      });
  }

  printReceipt(saleId: SaleId): void {
    this.store.setLoading(true);
    this.salesApi
      .printReceipt(saleId)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe();
  }

  printReceiptForTauri(saleId: SaleId, isEdition = false): void {
    this.store.setLoading(true);
    this.salesApi
      .getEscPosReceiptForTauri(saleId, isEdition)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: async (data: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(data);
          } catch (err) {
            this.store.emitEvent('PRODUCT_ADDED', { success: false, error: err });
          }
        },
        error: err => this.store.emitEvent('PRODUCT_ADDED', { success: false, error: err }),
      });
  }

  // ── Privé ─────────────────────────────────────────────────────

  private loadDepots(): void {
    this.magasinService.fetchAllDepots({ types: ['DEPOT'] }).subscribe((res: HttpResponse<IMagasin[]>) => {
      this.store.setDepots(res.body ?? []);
    });
  }

  private loadQuantityMax(): void {
    this.configurationService.find('APP_QTY_MAX').subscribe({
      next: res => this.store.setQuantityMax(Number(res.body?.value ?? 10000)),
      error: () => this.store.setQuantityMax(10000),
    });
  }

  private save(currentSale: ISales): void {
    this.store.setLoading(true);
    currentSale.payrollAmount = currentSale.amountToBePaid;
    currentSale.restToPay = currentSale.amountToBePaid;
    this.api
      .save(currentSale)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) =>
          this.store.emitEvent('SALE_FINALIZED', new FinalyseSale(true, undefined, res.body?.saleId)),
        error: err => this.store.emitEvent('SALE_FINALIZED', new FinalyseSale(false, err)),
      });
  }

  private handleSaleUpdate(observable: Observable<ISales>, attemptedLine: ISalesLine | null = null): void {
    this.store.setLoading(true);
    observable.pipe(finalize(() => this.store.setLoading(false))).subscribe({
      next: (sale: ISales) => {
        this.store.setCurrentSale(sale);
        this.store.clearError();
        this.store.emitEvent('PRODUCT_ADDED', { success: true });
      },
      error: err => this.handleError(err, attemptedLine),
    });
  }

  /**
   * Gère les erreurs HTTP en mettant à jour errorDetails dans le store
   * pour que les mixins force-stock et déconditionnement puissent réagir.
   */
  private handleError(err: any, attemptedLine: ISalesLine | null = null): void {
    const errorKey = err?.error?.errorKey ?? null;
    this.store.setError(errorKey);
    this.store.setErrorDetails({
      errorKey,
      originalError: err,
      attemptedLine: attemptedLine ?? undefined,
    });
    this.store.emitEvent('PRODUCT_ADDED', { success: false, error: err, payload: attemptedLine });
  }

  private buildNewSale(salesLine: ISalesLine): ISales {
    return {
      ...new Sales(),
      salesLines: [salesLine],
      natureVente: 'ASSURANCE',
      magasin: { id: this.store.selectedDepot()?.id },
      typePrescription: 'DEPOT',
      cassierId: this.store.cashier()?.id,
      sellerId: this.store.seller()?.id,
      type: 'DEPOT',
      categorie: 'VO',
    };
  }
}
