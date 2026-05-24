import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

import {SERVER_API_URL} from '../../../../app.constants';
import {createRequestOptions} from '../../../../shared/util/request-util';
import {FinalyseSale, ISales, SaleId, UpdateSaleInfo} from '../../../../shared/model/sales.model';
import {ISalesLine, SaleLineId} from '../../../../shared/model/sales-line.model';
import {IClientTiersPayant} from '../../../../shared/model';

/**
 * Sales API Service
 * Handles all HTTP communication with the backend for sales operations
 */
@Injectable({providedIn: 'root'})
export class SalesApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales';
  private readonly http = inject(HttpClient);

  /**
   * Create a cash sale (vente comptant)
   */
  createComptantSale(sales: ISales): Observable<ISales> {
    return this.http.post<ISales>(`${this.resourceUrl}/comptant`, sales, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Update a cash sale
   */
  updateComptantSale(sales: ISales): Observable<ISales> {
    return this.http.put<ISales>(`${this.resourceUrl}/comptant`, sales, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Save a cash sale (finalize)
   */
  saveCashSale(sales: ISales): Observable<FinalyseSale> {
    return this.http.put<FinalyseSale>(this.resourceUrl + '/comptant/save', sales, {observe: 'response'}).pipe(map(res => res.body!));
  }

  /**
   * Create an insurance sale (vente assurance)
   */
  createAssuranceSale(sales: ISales): Observable<ISales> {
    return this.http.post<ISales>(`${this.resourceUrl}/assurance`, sales, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Update an insurance sale
   */
  updateAssuranceSale(sales: ISales): Observable<ISales> {
    return this.http.put<ISales>(`${this.resourceUrl}/assurance`, sales, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Save an insurance sale (finalize)
   */
  saveAssuranceSale(sales: ISales): Observable<FinalyseSale> {
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/save', sales, {observe: 'response'}).pipe(map(res => res.body!));
  }

  /**
   * Finalize a presale (comptant)
   */
  finalizePresaleComptant(sales: ISales, transform: boolean = true): Observable<void> {
    if (transform) {
      return this.http.put<void>(`${this.resourceUrl}/comptant/finalize-prevente-and-transform`, sales, {observe: 'response'}).pipe(map((): void => undefined));
    }
    return this.http.put<void>(`${this.resourceUrl}/comptant/finalize-prevente`, sales, {observe: 'response'}).pipe(map((): void => undefined));
  }

  /**
   * Finalize a presale (assurance/carnet)
   */
  finalizePresaleAssurance(sales: ISales, transform: boolean = true): Observable<void> {
    if (transform) {
      return this.http.put<void>(`${this.resourceUrl}/assurance/finalize-prevente-and-transform`, sales, {observe: 'response'}).pipe(map((): void => undefined));
    }
    return this.http.put<void>(`${this.resourceUrl}/assurance/finalize-prevente`, sales, {observe: 'response'}).pipe(map((): void => undefined));
  }

  /**
   * Note: CARNET utilise les mêmes endpoints que ASSURANCE
   * La différenciation se fait via sale.type = 'CARNET' ou 'ASSURANCE'
   * Utiliser createAssuranceSale() et updateAssuranceSale() pour CARNET également
   */

  /**
   * Find a sale by ID
   * Find current sale
   */
  findSale(id: SaleId): Observable<ISales> {
    return this.http.get<ISales>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  querySales(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ISales[]>(this.resourceUrl, {params: options, observe: 'response'});
  }

  querySalesTotalAmount(req?: any): Observable<number> {
    const options = createRequestOptions(req);
    return this.http.get<number>(`${this.resourceUrl}/total-amount`, {params: options});
  }

  /**
   * Export filtered journal to Excel
   */
  exportJournal(req?: any): Observable<HttpResponse<Blob>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/export`, {params: options, responseType: 'blob', observe: 'response'});
  }

  /**
   * Delete a sale
   */
  deleteSale(id: SaleId): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, {observe: 'response'}).pipe(map((): void => undefined));
  }

  /**
   * Cancel a comptant sale
   */
  cancelComptant(id: SaleId, cancelComment: string): Observable<HttpResponse<{}>> {

    return this.http.delete(`${this.resourceUrl}/cancel/comptant/${id.id}/${id.saleDate}`, {
      params: {cancelComment},
      observe: 'response'
    });
  }

  /**
   * Cancel an assurance sale
   */
  cancelAssurance(id: SaleId, cancelComment: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/assurance/${id.id}/${id.saleDate}`, {
      params: {cancelComment},
      observe: 'response'
    });
  }

  /**
   * Update sale date
   */
  updateSaleDate(sales: ISales): Observable<ISales> {
    return this.http.put<ISales>(this.resourceUrl + '/assurance/date', sales, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Add complementary third-party payer (tiers payant complémentaire) to an insurance sale
   * @param saleId
   * @param clientTiersPayant
   */
  addTiersPayantComplementaire(saleId: SaleId, clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/add-assurance/assurance/${saleId.id}/${saleId.saleDate}`, clientTiersPayant, {
      observe: 'response',
    });
  }

  /**
   * Print sale invoice
   */
  printInvoice(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id.id}/${id.saleDate}`, {responseType: 'blob'});
  }

  countPendingSales(req?: any): Observable<HttpResponse<number>> {
    const options = createRequestOptions(req);
    return this.http.get<number>(this.resourceUrl + '/vente-en-attente-count', {
      params: options,
      observe: 'response'
    });
  }

  printReceipt(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.get<void>(`${this.resourceUrl}/print/receipt/${id.id}/${id.saleDate}`, {observe: 'response'});
  }

  /*** Get ESC/POS receipt data for Tauri integration * Returns raw ESC/POS byte array to be sent directly to printer */
  getEscPosReceiptForTauri(id: SaleId, isEdition = false): Observable<ArrayBuffer> {
    return this.http.get(`${this.resourceUrl}/receipt/tauri/${id.id}/${id.saleDate}`, {
      params: {isEdition},
      responseType: 'arraybuffer',
    });
  }

  // ============================================
  // SALES LINE OPERATIONS
  // ============================================

  /**
   * Add item to comptant sale
   * Backend recalculates all amounts and returns updated line
   */
  addItemComptant(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.post<ISalesLine>(`${this.resourceUrl}/add-item/comptant`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Add item to assurance/carnet sale
   * Backend recalculates all amounts and returns updated line
   * Used for both ASSURANCE and CARNET (differentiated by sale type)
   */
  addItemAssurance(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.post<ISalesLine>(`${this.resourceUrl}/add-item/assurance`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Update item quantity sold
   * Backend recalculates all amounts
   */
  updateItemQtySold(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * INCREMENT item quantity requested
   * Used when adding product from search with force stock
   * Backend ADDS the quantity to existing quantity
   */
  incrementItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/increment-item/quantity-requested`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * INCREMENT item quantity requested
   * Used when adding product from search with force stock
   * Backend ADDS the quantity to existing quantity
   */
  incrementItemQtyRequestedAssurance(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/increment-item/quantity-requested/assurance`, salesLine, {observe: 'response'})
      .pipe(map(res => res.body!));
  }

  /**
   * SET (REPLACE) item quantity requested
   * Used when editing quantity from table cell
   * Backend REPLACES the quantity (does not increment)
   */
  setItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/set-item/quantity-requested`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * SET (REPLACE) item quantity requested
   * Used when editing quantity from table cell
   * Backend REPLACES the quantity (does not increment)
   */
  setItemQtyRequestedAssurance(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/set-item/quantity-requested/assurance`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Update item price
   * Backend recalculates all amounts
   */
  updateItemPrice(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }

  /**
   * Delete item from sale
   */
  deleteItem(id: SaleLineId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/delete-item/${id.id}/${id.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

  /**
   * Delete item from sale
   */
  deleteItemFromAssurance(id: SaleLineId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/delete-item/assurance/${id.id}/${id.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

  /**
   * Add global remise (discount) to cash sale
   */
  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/comptant/add-remise`, key, {observe: 'response'});
  }

  /*
   * Add global remise (discount) to assurance/carnet sale
   */
  addAssuranceRemise(key: UpdateSaleInfo): Observable<HttpResponse<void>> {
    return this.http.put<void>(this.resourceUrl + '/assurance/add-remise', key, {observe: 'response'});
  }

  /**
   * Remove remise from cash sale
   */
  removeRemiseFromCashSale(saleId: SaleId): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/comptant/remove-remise/${saleId.id}/${saleId.saleDate}`, {observe: 'response'});
  }

  /**
   * Remove remise from assurance/carnet sale
   * @param saleId
   */
  removeRemiseFromAssuranceSale(saleId: SaleId): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/assurance/remove-remise/${saleId.id}/${saleId.saleDate}`, {observe: 'response'});
  }

  // ============================================
  // PENDING SALES (MISE EN ATTENTE)
  // ============================================

  /**
   * Put comptant sale on standby (prévente)
   * Sets sale status to STANDBY without finalizing
   */
  putComptantOnStandby(sales: ISales): Observable<FinalyseSale> {
    return this.http
      .put<FinalyseSale>(`${this.resourceUrl}/comptant/put-on-hold`, sales, {observe: 'response'})
      .pipe(map(res => res.body!));
  }

  /**
   * Put assurance/carnet sale on standby (prévente)
   * Sets sale status to STANDBY without finalizing
   */
  putAssuranceOnStandby(sales: ISales): Observable<FinalyseSale> {
    return this.http
      .put<FinalyseSale>(`${this.resourceUrl}/assurance/put-on-hold`, sales, {observe: 'response'})
      .pipe(map(res => res.body!));
  }

  /**
   * Get all pending sales
   * Returns list of sales waiting to be completed
   */
  getPendingSales(req: any): Observable<ISales[]> {
    const params = createRequestOptions(req);
    return this.http.get<ISales[]>(`${this.resourceUrl}/prevente`, {
      params,
      observe: 'response'
    }).pipe(map(res => res.body || []));
  }

  /**
   * Delete pending sale (prévente) for vno (comptant)
   */
  deletePreventeComptant(saleId: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/${saleId.id}/${saleId.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

  /**
   * Delete pending sale (prévente) for assurance/carnet
   */
  deletePreventeAssurance(saleId: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/assurance/${saleId.id}/${saleId.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

  /**
   * Transform a comptant sale to VO (ASSURANCE or CARNET)
   * Returns the SaleId of the transformed sale
   */
  transformSale(natureVente: string, saleId: SaleId): Observable<SaleId> {
    const params = {natureVente, saleId: saleId.id, sale_date: saleId.saleDate};
    return this.http
      .get<SaleId>(this.resourceUrl + '/assurance/transform', {
        params: createRequestOptions(params),
        observe: 'response',
      })
      .pipe(map(res => res.body!));
  }

  removeThirdPartyFromSales(id: number, saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/remove-tiers-payant/assurance/${id}/${saleId.id}/${saleId.saleDate}`, {
      observe: 'response',
    });
  }

  /**
   * Add customer to cash sale (comptant)
   * @param keyValue
   */
  addCustommerToCashSale(keyValue: UpdateSaleInfo): Observable<HttpResponse<void>> {
    return this.http.put<void>(this.resourceUrl + '/comptant/add-customer', keyValue, {observe: 'response'});
  }

  /**
   * Change customer of an insurance/carnet sale
   * @param keyValue
   */
  changeAssuranceCustomer(keyValue: UpdateSaleInfo): Observable<HttpResponse<void>> {
    return this.http.put<void>(this.resourceUrl + '/assurance/change/customer', keyValue, {observe: 'response'});
  }

  changeAyantDroit(updateSaleInfo: UpdateSaleInfo): Observable<HttpResponse<void>> {
    return this.http.put<void>(this.resourceUrl + '/assurance/ayant-droit', updateSaleInfo, {observe: 'response'});
  }

  // ============================================
  // MANAGEMENT — Journal, préventes, devis
  // ============================================

  /**
   * Re-print receipt for a comptant sale (VNO)
   */
  reprintReceiptComptant(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.get<void>(`${this.resourceUrl}/re-print/receipt/${id.id}/${id.saleDate}`, {observe: 'response'});
  }

  /**
   * Re-print receipt for an assurance/carnet sale (VO)
   */
  reprintReceiptAssurance(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.get<void>(`${this.resourceUrl}/assurance/re-print/receipt/${id.id}/${id.saleDate}`, {observe: 'response'});
  }

  /**
   * Copy a closed sale to a new editable sale
   */
  copyToEdit(id: SaleId): Observable<HttpResponse<SaleId>> {
    return this.http.put<SaleId>(`${this.resourceUrl}/assurance/copier`, id, {observe: 'response'});
  }

  /**
   * Query sales for management views (préventes, ventes en cours)
   * Filters by statut array (PROCESSING, PENDING, ACTIVE…)
   */
  queryManagement(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ISales[]>(`${this.resourceUrl}/prevente`, {params: options, observe: 'response'});
  }

  /**
   * Query devis/proformas
   */
  queryDevis(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ISales[]>(`${this.resourceUrl}/devis`, {params: options, observe: 'response'});
  }

  /**
   * Delete a prévente (same endpoint regardless of type VNO/VO)
   */
  deletePreventeById(id: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/${id.id}/${id.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

  /**
   * Transform a prévente to a finalized comptant sale
   */
  transformPreventeToSaleComptant(id: SaleId): Observable<HttpResponse<SaleId>> {
    return this.http.put<SaleId>(`${this.resourceUrl}/comptant/transform`, id, {observe: 'response'});
  }

  /**
   * Transform a prévente to a finalized assurance/carnet sale
   */
  transformPreventeToSaleAssurance(id: SaleId): Observable<HttpResponse<SaleId>> {
    return this.http.put<SaleId>(`${this.resourceUrl}/assurance/transform`, id, {observe: 'response'});
  }

  /**
   * Clone a comptant devis
   */
  cloneDevisComptant(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/comptant/clone-devis`, id, {observe: 'response'});
  }

  /**
   * Clone an assurance/carnet devis
   */
  cloneDevisAssurance(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/assurance/clone-devis`, id, {observe: 'response'});
  }
}
