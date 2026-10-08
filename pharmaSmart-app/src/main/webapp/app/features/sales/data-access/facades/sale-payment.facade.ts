import {inject, Injectable} from '@angular/core';
import {catchError, EMPTY, finalize, from, map, Observable, of, switchMap, tap} from 'rxjs';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {DepassementLimiteCredit, LimiteCreditModalComponent} from '../../ui/limite-credit-modal/limite-credit-modal.component';
import {OrdonnanceRequise, OrdonnanceRequiseModalComponent} from '../../ui/ordonnance-requise-modal/ordonnance-requise-modal.component';
import {SalesStore} from '../store/sales.store';
import {SalesApiService} from '../services/sales-api.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {PrintService} from '../services/print.service';
import {ISales, SaleId} from '../../../../shared/model/sales.model';
import {SalesStatut} from '../../../../shared/model';
import {extractApiError} from './sale-facade.utils';

/**
 * Payment Facade — Save, standby, presale, devis, pending, print
 */
@Injectable({providedIn: 'root'})
export class SalePaymentFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly printService = inject(PrintService);
  private readonly modalService = inject(NgbModal);

  // ── Public methods ─────────────────────────────────────────

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
        // Vente à crédit au-delà de la limite de l'officine : dérogation tracée, puis nouvelle finalisation.
        if (error?.error?.errorKey === 'limiteCreditDepassee' && error.error.payload) {
          this.store.setIsSaving(false);
          return this.demanderDerogationLimiteCredit(error.error.payload).pipe(switchMap(autorise => (autorise ? this.saveSale() : of(null))));
        }
        // Produit sur ordonnance : ordonnance ou prescripteur obligatoire, puis nouvelle finalisation.
        if (error?.error?.errorKey === 'ordonnanceRequise' && error.error.payload) {
          this.store.setIsSaving(false);
          return this.demanderOrdonnance(error.error.payload).pipe(switchMap(associee => (associee ? this.saveSale() : of(null))));
        }
        console.error('Error saving sale:', error);
        const {errorMessage} = extractApiError(error, "Erreur lors de l'enregistrement de la vente");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        this.reloadAfterConflict(error, currentSale.saleId);
        return of(null);
      }),
    );
  }

  private demanderOrdonnance(requise: OrdonnanceRequise): Observable<boolean> {
    const modalRef = this.modalService.open(OrdonnanceRequiseModalComponent, {backdrop: 'static', centered: true, size: 'md'});
    modalRef.componentInstance.requise = requise;
    return from(modalRef.result).pipe(
      map(result => result === true),
      catchError(() => of(false)),
    );
  }

  private demanderDerogationLimiteCredit(depassement: DepassementLimiteCredit): Observable<boolean> {
    const modalRef = this.modalService.open(LimiteCreditModalComponent, {backdrop: 'static', centered: true, size: 'md'});
    modalRef.componentInstance.depassement = depassement;
    return from(modalRef.result).pipe(
      map(result => result === true),
      catchError(() => of(false)),
    );
  }

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
           this.store.emitEvent('STANDBY_SUCCESS');
          }
        }),
        catchError(error => {
          console.error('Error putting sale on standby:', error);
          const {errorMessage} = extractApiError(error, 'Erreur lors de la mise en attente');
          this.notificationService.error(errorMessage);
          this.store.setError(errorMessage);
          this.reloadAfterConflict(error, currentSale.saleId);
          return EMPTY;
        }),
        finalize(() => this.store.setIsSaving(false)),
      )
      .subscribe();
  }

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
        const {errorMessage} = extractApiError(error, 'Erreur lors de la finalisation de la prevente');
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        this.reloadAfterConflict(error, sale.saleId);
        return of(null);
      }),
    );
  }

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
        const {errorMessage} = extractApiError(error, "Erreur lors de l'enregistrement du devis");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        this.reloadAfterConflict(error, sale.saleId);
        return of(null);
      }),
    );
  }

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
        const {errorMessage} = extractApiError(error, "Erreur lors de l'enregistrement du devis carnet");
        this.notificationService.error(errorMessage);
        this.store.setError(errorMessage);
        this.store.setIsSaving(false);
        this.reloadAfterConflict(error, sale.saleId);
        return of(null);
      }),
    );
  }

  loadPendingSales(params: any): void {
    //  this.store.setPendingSalesLoading(true);

    this.apiService
      .getPendingSales(params)
      .pipe(
        catchError(error => {
          console.error('Error loading pending sales:', error);
          this.store.setError('Erreur lors du chargement des ventes en attente');
          return of([]);
        }),
        finalize(() => this.store.setPendingSalesLoading(false)),
      )
      .subscribe(sales => {
        this.store.setPendingSales(sales);
      });
  }


  printInvoice(saleId: SaleId): void {
    this.printService.printInvoice(saleId);
  }

  printReceipt(saleId: SaleId): void {
    this.printService.printReceipt(saleId).subscribe();
  }

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

  /**
   * Après un 409 (vente modifiée ailleurs depuis son affichage), l'écran montre l'état réel de la
   * vente relu sur le serveur — jamais une fusion avec ce que le poste affichait. Une vente déjà
   * finalisée ou annulée ailleurs quitte l'écran : il n'y a plus rien à y encaisser.
   */
  private reloadAfterConflict(error: any, saleId?: SaleId): void {
    if (error?.status !== 409 || !saleId) {
      return;
    }
    this.apiService
      .findSale(saleId)
      .pipe(
        catchError(() => {
          // Introuvable : supprimée ou transformée sur un autre poste.
          this.store.resetCurrentSale();
          return EMPTY;
        }),
      )
      .subscribe(sale => {
        if (sale.statut === SalesStatut.CLOSED || sale.canceled) {
          this.store.resetCurrentSale();
          this.notificationService.warning('Cette vente a déjà été finalisée sur un autre poste.');
          return;
        }
        this.store.setCurrentSale(sale);
        this.store.setSelectedCustomer(sale.customer ?? null);
        this.store.emitEvent('SALE_RELOADED');
      });
  }

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
