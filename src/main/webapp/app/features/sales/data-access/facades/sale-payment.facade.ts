import { inject, Injectable } from '@angular/core';
import { catchError, finalize, map, Observable, of, Subject, tap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { PrintService } from '../services/print.service';
import { ISales, SaleId } from '../../../../shared/model/sales.model';
import { SalesStatut } from '../../../../shared/model';
import { extractApiError } from './sale-facade.utils';

/**
 * Payment Facade — Save, standby, presale, devis, pending, print
 */
@Injectable({ providedIn: 'root' })
export class SalePaymentFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly printService = inject(PrintService);

  // ── Subjects ───────────────────────────────────────────────
  private readonly standbySuccessSubject = new Subject<void>();
  readonly standbySuccess$ = this.standbySuccessSubject.asObservable();

  // ── Public methods ─────────────────────────────────────────

  /**
   * Save current sale (finalize)
   * Returns an Observable that emits the saved sale on success, null on error
   */
  saveSale(): Observable<ISales | null> {
    this.store.setIsSaving(true);
    this.store.clearError();

    const currentSale = this.store.currentSale();
    if (!currentSale) {
      const error = 'Aucune vente en cours';
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    this.calculateSaleAmounts(currentSale);

    currentSale.avoir = this.store.isAvoir();

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
      saveObservable = this.apiService.saveAssuranceSale(currentSale);
    } else {
      const error = 'Type de vente non supporté';
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    return saveObservable.pipe(
      tap(result => {
        const printInvoice = this.store.printInvoice();
        const printReceipt = this.store.printReceipt();

        if (printInvoice && result.saleId) {
          this.printService.printInvoice(result.saleId);
        }

        if (printReceipt && result.saleId) {
          this.printService.printReceipt(result.saleId).subscribe();
        }

        this.store.resetCurrentSale();
        this.store.setIsSaving(false);
      }),
      catchError(error => {
        console.error('Error saving sale:', error);
        const { errorMessage } = extractApiError(error, "Erreur lors de l'enregistrement de la vente");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        return of(null);
      }),
    );
  }

  /**
   * Save assurance sale with payment modes and tiers payants
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

    currentSale.payments = paymentModes;

    return this.saveSale();
  }

  /**
   * Put current sale on standby (prévente)
   */
  putOnStandby(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      console.error('No current sale to put on standby');
      return;
    }

    this.store.setIsSaving(true);
    this.store.clearError();

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
          const { errorMessage } = extractApiError(error, 'Erreur lors de la mise en attente');
          this.notificationService.error(errorMessage);
          this.store.setError(errorMessage);
          return of(null);
        }),
        finalize(() => this.store.setIsSaving(false)),
      )
      .subscribe();
  }

  /**
   * Finalize a presale
   */
  finalizePresale(sale: ISales, transform: boolean = true): Observable<boolean | null> {
    this.store.setIsSaving(true);
    this.store.clearError();

    const saleType = this.store.saleType();
    const apiCall$ =
      saleType === 'COMPTANT'
        ? this.apiService.finalizePresaleComptant(sale, transform)
        : this.apiService.finalizePresaleAssurance(sale, transform);

    return apiCall$.pipe(
      map(() => true as boolean),
      tap(() => {
        this.store.resetCurrentSale();
        this.store.setIsSaving(false);
      }),
      catchError(error => {
        console.error('Error finalizing presale:', error);
        const { errorMessage } = extractApiError(error, 'Erreur lors de la finalisation de la prevente');
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        return of(null);
      }),
    );
  }

  /**
   * Save a devis (like presale, no payment)
   */
  saveDevis(sale: ISales): Observable<boolean | null> {
    this.store.setIsSaving(true);
    this.store.clearError();

    if (!sale.customerId && !this.store.selectedCustomer()?.id) {
      const error = 'Un client est obligatoire pour un devis';
      this.notificationService.error(error);
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    sale.statut = SalesStatut.DEVIS;

    return this.apiService.finalizePresaleComptant(sale, false).pipe(
      map(() => true as boolean),
      tap(() => {
        this.store.resetCurrentSale();
        this.store.setIsSaving(false);
      }),
      catchError(error => {
        console.error('Error saving devis:', error);
        const { errorMessage } = extractApiError(error, "Erreur lors de l'enregistrement du devis");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        return of(null);
      }),
    );
  }

  /**
   * Save a devis carnet (CARNET with DEVIS status)
   */
  saveDevisCarnet(sale: ISales): Observable<boolean | null> {
    this.store.setIsSaving(true);
    this.store.clearError();

    if (!sale.customerId && !this.store.selectedCustomer()?.id) {
      const error = 'Un client est obligatoire pour un devis carnet';
      this.notificationService.error(error);
      this.store.setError(error);
      this.store.setIsSaving(false);
      return of(null);
    }

    sale.statut = SalesStatut.DEVIS;

    return this.apiService.finalizePresaleAssurance(sale).pipe(
      map(() => true as boolean),
      tap(() => {
        this.store.resetCurrentSale();
        this.store.setIsSaving(false);
      }),
      catchError(error => {
        console.error('Error saving devis carnet:', error);
        const { errorMessage } = extractApiError(error, "Erreur lors de l'enregistrement du devis carnet");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        return of(null);
      }),
    );
  }

  /**
   * Load pending sales from backend
   */
  loadPendingSales(params: any): void {
    this.store.setPendingSalesLoading(true);

    this.apiService
      .getPendingSales(params)
      .pipe(
        catchError(error => {
          console.error('Error loading pending sales:', error);
          this.store.setError('Erreur lors du chargement des ventes en attente');
          this.store.setPendingSalesLoading(false);
          return of([]);
        }),
        finalize(() => this.store.setPendingSalesLoading(false)),
      )
      .subscribe(sales => {
        this.store.setPendingSales(sales);
      });
  }

  /**
   * Delete a pending sale permanently
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

  /**
   * Print sale invoice (PDF)
   */
  printInvoice(saleId: SaleId): void {
    this.printService.printInvoice(saleId);
  }

  /**
   * Print sale receipt (thermal printer format)
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
      this.printReceipt(currentSale.saleId);
    }
  }

  // ── Private helpers ────────────────────────────────────────

  private calculateSaleAmounts(sale: ISales): void {
    const montantVerse = Number(sale.montantVerse) || 0;
    const amountToBePaid = Number(sale.amountToBePaid) || 0;
    const restToPay = amountToBePaid - montantVerse;

    sale.payrollAmount = restToPay <= 0 ? amountToBePaid : montantVerse;
    sale.restToPay = Math.max(restToPay, 0);

    if (sale.montantRendu === undefined || sale.montantRendu === null) {
      sale.montantRendu = restToPay < 0 ? Math.abs(restToPay) : 0;
    }
  }
}
