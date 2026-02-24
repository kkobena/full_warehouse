import { inject, Injectable } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, map, Observable, of, pipe, switchMap, tap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISales, SaleId } from '../../../../shared/model/sales.model';
import { ISalesLine, SalesStatut } from '../../../../shared/model';
import { extractApiError, handlePlafondVenteWarning } from './sale-facade.utils';

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
 * Lifecycle Facade — Création, initialisation, chargement, annulation, transformation
 */
@Injectable({ providedIn: 'root' })
export class SaleLifecycleFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);

  // ── Create sale rxMethods ──────────────────────────────────

  createComptantSale = rxMethod<ISalesLine>(
    this.createSalePipeline({
      saleType: 'COMPTANT',
      defaultErrorMessage: 'Erreur lors de la création de la vente',
      apiCall: sale => this.apiService.createComptantSale(sale),
      buildSale: initialLine => ({
        statut: this.store.isPresale() ? SalesStatut.PROCESSING : SalesStatut.ACTIVE,
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

  createDevisSale = rxMethod<ISalesLine>(
    this.createSalePipeline({
      saleType: 'COMPTANT',
      defaultErrorMessage: 'Erreur lors de la création du devis',
      apiCall: sale => this.apiService.createComptantSale(sale),
      buildSale: initialLine => ({
        statut: SalesStatut.DEVIS,
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

  createAssuranceSale = this.createVOSale('ASSURANCE');
  createCarnetSale = this.createVOSale('CARNET');

  // ── Load sale rxMethods ────────────────────────────────────

  loadSaleForEdit = rxMethod<SaleId>(
    this.loadSalePipeline(sale => {
      this.hydrateSaleInStore(sale);
      this.store.setLoading(false);
      this.store.emitEvent('SALE_RELOADED');
    }),
  );

  loadSale = rxMethod<SaleId>(
    this.loadSalePipeline(sale => {
      this.hydrateSaleInStore(sale);
      this.store.setLoading(false);
      this.store.emitEvent('SALE_RELOADED_TO_EDIT');
    }),
  );

  resumePendingSale = rxMethod<SaleId>(
    this.loadSalePipeline(sale => {
      if (sale.statut !== SalesStatut.CLOSED) {
        this.hydrateSaleInStore(sale);
        this.store.emitEvent('RESUME_PENDING_SALE');
      } else {
        this.store.resetCurrentSale();
      }
      this.store.removePendingSale(sale.saleId);
      this.store.setLoading(false);
    }),
  );

  cancelSale = rxMethod<void>(
    pipe(
      tap(() => {
        this.store.setLoading(true);
        this.store.clearError();
      }),
      switchMap(() => {
        const currentSale = this.store.currentSale();
        if (!currentSale || !currentSale.id || !currentSale.saleId) {
          throw new Error('Aucune vente en cours');
        }
        const saleId: SaleId = currentSale.saleId;
        const isVno = this.store.saleType() === 'COMPTANT';
        if (isVno) {
          return this.apiService.deletePreventeComptant(saleId);
        } else {
          return this.apiService.deletePreventeAssurance(saleId);
        }
      }),
      tap({
        next: () => {
          this.store.resetCurrentSale();
          this.store.setLoading(false);
          this.store.emitEvent('CANCEL_SALE');
        },
        error: error => {
          const { errorMessage } = extractApiError(error, "Erreur lors de l'annulation de la vente");
          this.store.setError(errorMessage);
          this.store.setLoading(false);
        },
      }),
      catchError(error => {
        console.error('Error canceling sale:', error);
        return of(null);
      }),
    ),
  );

  // ── Initialize methods ─────────────────────────────────────

  initializeComptantSale(initialLine: ISalesLine): void {
    if (!initialLine) {
      throw new Error('Impossible de créer une vente sans produit');
    }
    this.store.setSaleType('COMPTANT');
    this.createComptantSale(initialLine);
  }

  initializeAssuranceSale(): void {
    this.store.setSaleType('ASSURANCE');
    this.store.setPendingTiersPayants([]);
  }

  initializeCarnetSale(): void {
    this.store.setSaleType('CARNET');
    this.store.setPendingTiersPayants([]);
  }

  initializeDevisSale(): void {
    this.store.setSaleType('COMPTANT');
    this.store.setIsDevis(true);
  }

  initializeDevisCarnetSale(): void {
    this.store.setSaleType('CARNET');
    this.store.setIsDevis(true);
    this.store.setPendingTiersPayants([]);
  }

  // ── Transform methods ──────────────────────────────────────

  transformCashSaleToAssurance(): void {
    this.doTransformCashSale('ASSURANCE');
  }

  transformCashSaleToCarnet(): void {
    this.doTransformCashSale('CARNET');
  }

  // ── Private helpers ────────────────────────────────────────

  private createVOSale(natureVente: 'ASSURANCE' | 'CARNET') {
    const saleType = natureVente;
    return rxMethod<ISalesLine>(
      this.createSalePipeline({
        saleType,
        defaultErrorMessage: `Erreur lors de la création de la vente ${natureVente.toLowerCase()}`,
        apiCall: sale => this.apiService.createAssuranceSale(sale),
        buildSale: initialLine => ({
          statut: this.store.isDevis() ? SalesStatut.DEVIS : this.store.isPresale() ? SalesStatut.PROCESSING : SalesStatut.ACTIVE,
          salesLines: [initialLine],
          customerId: this.store.selectedCustomer()?.id,
          natureVente,
          typePrescription: this.store.typePrescription() || 'PRESCRIPTION',
          cassierId: this.store.cashier()?.id,
          sellerId: this.store.seller()?.id,
          type: 'VO',
          categorie: 'VO',
          tiersPayants: this.store.pendingTiersPayants(),
        }),
      }),
    );
  }

  private createSalePipeline(config: CreateSaleConfig) {
    return pipe(
      tap(() => {
        this.store.setSaleType(config.saleType);
        this.store.clearError();
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
            this.store.clearError();
            this.store.emitEvent('PRODUCT_ADDED');
          }
          this.store.setLoading(false);
        },
      }),
    );
  }

  private handleCreateSaleError(
    error: any,
    initialLine: ISalesLine,
    defaultMessage: string,
  ): Observable<{ createdSale: ISales; initialLine: ISalesLine } | null> {
    console.error('Error creating sale:', error);
    const { errorMessage, errorKey } = extractApiError(error, defaultMessage);

    if (errorKey === 'stock') {
      this.store.setError(errorMessage);
      this.store.setLastErrorDetails({
        errorKey,
        originalError: error,
        attemptedLine: initialLine,
        isFromTableCellEdit: false,
      });
    } else if (errorKey === 'stockChInsufisant') {
      this.store.setError(errorMessage);
      this.store.setLastErrorDetails({
        errorKey,
        originalError: error,
        attemptedLine: initialLine,
        isFromTableCellEdit: false,
      });
    } else if (errorKey === 'customerInsuranceCreditLimit') {
      handlePlafondVenteWarning(this.store, this.notificationService, errorMessage);
      const saleId: SaleId | null = error.error?.payload?.saleId ?? null;
      if (saleId) {
        return this.apiService.findSale(saleId).pipe(
          map(reloadedSale => ({ createdSale: reloadedSale, initialLine })),
          catchError(() => of(null)),
        );
      }
    } else {
      this.store.setError(errorMessage);
      this.notificationService.error(errorMessage);
    }

    this.store.setLoading(false);
    return of(null);
  }

  private loadSalePipeline(onSuccess: (sale: ISales) => void) {
    return pipe(
      tap((_: SaleId) => {
        this.store.setLoading(true);
        this.store.clearError();
      }),
      switchMap((saleId: SaleId) => this.apiService.findSale(saleId)),
      tap({
        next: (sale: ISales) => onSuccess(sale),
        error: (error: any) => {
          const { errorMessage } = extractApiError(error, 'Erreur lors du chargement de la vente');
          this.store.setError(errorMessage);
          this.store.setLoading(false);
        },
      }),
      catchError(error => {
        console.error('Error loading sale:', error);
        return of(null);
      }),
    );
  }

  private hydrateSaleInStore(sale: ISales): void {
    this.store.setCurrentSale(sale);

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
  }

  private doTransformCashSale(natureVente: 'ASSURANCE' | 'CARNET'): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      return;
    }

    this.store.setSelectedCustomer(null);
    this.store.setLoading(true);

    this.apiService
      .transformSale(natureVente, currentSale.saleId)
      .pipe(
        switchMap(saleId => this.apiService.findSale(saleId)),
        tap(sale => {
          this.store.setVoFromCashSale(true);
          this.store.setCurrentSale(sale);
          this.store.setSaleType(natureVente);
          if (sale.customer) {
            this.store.setSelectedCustomer(sale.customer);
          }
          this.store.setLoading(false);
        }),
        catchError(err => {
          const { errorMessage } = extractApiError(err, 'Erreur lors de la transformation de la vente');
          this.notificationService.error(errorMessage);
          this.store.setLoading(false);
          return of(null);
        }),
      )
      .subscribe();
  }
}
