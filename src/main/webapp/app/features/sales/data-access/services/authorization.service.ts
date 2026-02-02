import { Injectable, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, from } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthorizationModalComponent } from '../../ui/authorization-modal/authorization-modal.component';
import { HasAuthorityService } from '../../../../entities/sales/service/has-authority.service';
import { Authority } from '../../../../shared/constants/authority.constants';

/**
 * AuthorizationService
 * 
 * Service for requesting authorizations for protected actions in sales
 * Opens a modal to request security key from authorized user
 * 
 * @example
 * this.authService.requestAuthorization(saleId, 'PR_SUPPRIME_PRODUIT_VENTE')
 *   .subscribe(authorized => {
 *     if (authorized) {
 *       // Proceed with protected action
 *     }
 *   });
 */
@Injectable({ providedIn: 'root' })
export class AuthorizationService {
  private readonly modalService = inject(NgbModal);
  private readonly hasAuthorityService = inject(HasAuthorityService);

  /**
   * Request authorization for a protected action
   * @param saleId - ID of the sale
   * @param privilege - Required privilege code
   * @param saleType - Type of sale (COMPTANT, ASSURANCE, CARNET)
   * @returns Observable<boolean> - true if authorized, false if cancelled
   */
  requestAuthorization(
    saleId: number | undefined,
    privilege: string,
    saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT'
  ): Observable<boolean> {
    const modalRef = this.modalService.open(AuthorizationModalComponent, {
      backdrop: 'static',
      centered: true,
      size: 'md',
    });

    modalRef.componentInstance.saleId = saleId;
    modalRef.componentInstance.privilege = privilege;
    modalRef.componentInstance.saleType = saleType;

    return from(modalRef.result).pipe(
      map(result => result === true),
      catchError(() => from([false]))
    );
  }

  /**
   * Request authorization to delete a product from sale
   */
  requestDeleteProductAuthorization(saleId: number | undefined, saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT'): Observable<boolean> {
    return this.requestAuthorization(saleId, 'PR_SUPPRIME_PRODUIT_VENTE', saleType);
  }

  /**
   * Request authorization to apply discount
   */
  requestDiscountAuthorization(saleId: number | undefined, saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT'): Observable<boolean> {
    return this.requestAuthorization(saleId, 'PR_AJOUTER_REMISE_VENTE', saleType);
  }

  /**
   * Request authorization to modify price
   */
  requestPriceModificationAuthorization(saleId: number | undefined, saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT'): Observable<boolean> {
    return this.requestAuthorization(saleId, 'PR_MODIFIER_PRIX_VENTE', saleType);
  }

  /**
   * Request authorization to cancel sale
   */
  requestCancelSaleAuthorization(saleId: number | undefined, saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT'): Observable<boolean> {
    return this.requestAuthorization(saleId, 'PR_ANNULER_VENTE', saleType);
  }

  /**
   * Check if current user can apply discount without authorization
   */
  canApplyDiscount(): boolean {
    return this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE);
  }

  /**
   * Check if current user can delete products without authorization
   */
  canDeleteProduct(): boolean {
    return this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE);
  }

  /**
   * Check if current user can modify price without authorization
   */
  canModifyPrice(): boolean {
    return this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFIER_PRIX);
  }

  /**
   * Check if current user can force stock (add product even if stock insufficient)
   */
  canForceStock(): boolean {
    return this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);
  }
}