import { inject, Injectable } from '@angular/core';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { SalesApiService } from '../services/sales-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { UpdateSaleInfo } from '../../../../shared/model/sales.model';
import { IClientTiersPayant, ICustomer, ISales } from '../../../../shared/model';

/**
 * Customer Facade — Client, tiers payants
 */
@Injectable({ providedIn: 'root' })
export class SaleCustomerFacade {
  private readonly store = inject(SalesStore);
  private readonly apiService = inject(SalesApiService);
  private readonly notificationService = inject(NotificationService);

  // ── Public methods ─────────────────────────────────────────

  setCustomer(customer: ICustomer): void {
    const currentSale = this.store.currentSale();

    if (!currentSale?.saleId) {
      this.store.setSelectedCustomer(customer);
      return;
    }

    this.store.setLoading(true);
    this.store.setSelectedCustomer(customer);

    const updateSaleInfo: UpdateSaleInfo = { id: currentSale.saleId, value: customer.id! };
    const updateObservable$ =
      currentSale.type === 'VNO'
        ? this.apiService.addCustommerToCashSale(updateSaleInfo)
        : this.apiService.changeAssuranceCustomer(updateSaleInfo);

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
          this.store.setCurrentSale({
            ...sale,
            differe: currentSale.differe,
            avoir: currentSale.avoir,
          });
          this.store.emitEvent('CUSTOMER_SET');
        }
      });
  }

  removeCustomer(): void {
    const currentSale = this.store.currentSale();
    if (!currentSale?.saleId) {
      return;
    }

    this.store.setLoading(true);
    this.store.setSelectedCustomer(null);

    const updatedSale: ISales = {
      ...currentSale,
      customerId: undefined,
    };

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
          this.store.emitEvent('CUSTOMER_REMOVED');
        }
      });
  }

  updateSaleTiersPayants(tiersPayants: IClientTiersPayant[]): void {
    const currentSale = this.store.currentSale();
    if (currentSale?.saleId) {
      this.store.setCurrentSale({
        ...currentSale,
        tiersPayants: tiersPayants,
      });
    } else {
      this.store.setPendingTiersPayants(tiersPayants);
    }
  }

  removeTiersPayantFromSale(tiersPayant: IClientTiersPayant, onSuccess?: () => void): void {
    const currentSale = this.store.currentSale();
    if (!currentSale) {
      return;
    }

    const updatedTiersPayants = (currentSale.tiersPayants || []).filter(tp => tp.id !== tiersPayant.id);

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
      this.store.setCurrentSale({
        ...currentSale,
        tiersPayants: updatedTiersPayants,
      });
      if (onSuccess) {
        onSuccess();
      }
    }
  }

  addTiersPayantToSale(tiersPayant: IClientTiersPayant): void {
    const currentSale = this.store.currentSale();

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
            this.store.emitEvent('TIERS_PAYANT_ADDED', tiersPayant);
          }
        });
    } else {
      if (currentSale) {
        const existingTiersPayants = currentSale.tiersPayants || [];
        const updatedTiersPayants = [...existingTiersPayants, tiersPayant];
        this.store.setCurrentSale({
          ...currentSale,
          tiersPayants: updatedTiersPayants,
        });
      }
      this.store.emitEvent('TIERS_PAYANT_ADDED', tiersPayant);
    }
  }
}
