import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { ISales, SaleId, FinalyseSale, UpdateSaleInfo } from '../../../../shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../../../shared/model/sales-line.model';
import { IClientTiersPayant } from '../../../../shared/model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

/**
 * Sales API Service
 * Handles all HTTP communication with the backend for sales operations
 */
@Injectable({ providedIn: 'root' })
export class SalesApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales';
  private readonly http = inject(HttpClient);

  /**
   * Create a cash sale (vente comptant)
   */
  createComptantSale(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.post<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Update a cash sale
   */
  updateComptantSale(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Close/finalize a cash sale
   */
  closeComptantSale(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<ISales>(`${this.resourceUrl}/comptant/close`, copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Save a cash sale (finalize)
   */
  saveCashSale(sales: ISales): Observable<FinalyseSale> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/comptant/save', copy, { observe: 'response' }).pipe(map(res => res.body!));
  }

  /**
   * Create an insurance sale (vente assurance)
   */
  createAssuranceSale(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.post<ISales>(`${this.resourceUrl}/assurance`, copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Update an insurance sale
   */
  updateAssuranceSale(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<ISales>(`${this.resourceUrl}/assurance`, copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Save an insurance sale (finalize)
   */
  saveAssuranceSale(sales: ISales): Observable<FinalyseSale> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/save', copy, { observe: 'response' }).pipe(map(res => res.body!));
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
    return this.http.get<ISales>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Find a sale by ID for editing
   */
  findSaleForEdit(id: SaleId): Observable<ISales> {
    return this.http.get<ISales>(`${this.resourceUrl}/edit/${id.id}/${id.saleDate}`, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Query sales with filters
   */
  querySales(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  /**
   * Delete a sale
   */
  deleteSale(id: SaleId): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' }).pipe(map((): void => undefined));
  }

  /**
   * Cancel a comptant sale
   */
  cancelComptant(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/comptant/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  /**
   * Cancel an assurance sale
   */
  cancelAssurance(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/assurance/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  /**
   * Update sale date
   */
  updateSaleDate(sales: ISales): Observable<ISales> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<ISales>(this.resourceUrl + '/assurance/date', copy, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
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
    return this.http.get(`${this.resourceUrl}/pdf/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  /**
   * Print sale receipt
   */
  printReceipt__(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/receipt/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }
  printReceipt(id: SaleId): Observable<HttpResponse<void>> {
    return this.http.get<void>(`${this.resourceUrl}/print/receipt/${id.id}/${id.saleDate}`, { observe: 'response' });
  }
  /*** Get ESC/POS receipt data for Tauri integration * Returns raw ESC/POS byte array to be sent directly to printer */
  getEscPosReceiptForTauri(id: SaleId, isEdition = false): Observable<ArrayBuffer> {
    return this.http.get(`${this.resourceUrl}/receipt/tauri/${id.id}/${id.saleDate}`, {
      params: { isEdition },
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
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http.post<ISalesLine>(`${this.resourceUrl}/add-item/comptant`, copy, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Add item to assurance/carnet sale
   * Backend recalculates all amounts and returns updated line
   * Used for both ASSURANCE and CARNET (differentiated by sale type)
   */
  addItemAssurance(salesLine: ISalesLine): Observable<ISalesLine> {
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http.post<ISalesLine>(`${this.resourceUrl}/add-item/assurance`, copy, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Update item quantity sold
   * Backend recalculates all amounts
   */
  updateItemQtySold(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Update item quantity requested (legacy - kept for compatibility)
   * Backend recalculates all amounts
   * @deprecated Use incrementItemQtyRequested or setItemQtyRequested instead
   */
  updateItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * INCREMENT item quantity requested
   * Used when adding product from search with force stock
   * Backend ADDS the quantity to existing quantity
   */
  incrementItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/increment-item/quantity-requested`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
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
      .put<ISalesLine>(`${this.resourceUrl}/increment-item/quantity-requested/assurance`, salesLine, { observe: 'response' })
      .pipe(
        map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
        map(res => res.body!),
      );
  }

  /**
   * SET (REPLACE) item quantity requested
   * Used when editing quantity from table cell
   * Backend REPLACES the quantity (does not increment)
   */
  setItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/set-item/quantity-requested`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * SET (REPLACE) item quantity requested
   * Used when editing quantity from table cell
   * Backend REPLACES the quantity (does not increment)
   */
  setItemQtyRequestedAssurance(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/set-item/quantity-requested/assurance`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Update item price
   * Backend recalculates all amounts
   */
  updateItemPrice(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, { observe: 'response' }).pipe(
      map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)),
      map(res => res.body!),
    );
  }

  /**
   * Delete item from sale
   */
  deleteItem(id: SaleLineId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/delete-item/${id.id}/${id.saleDate}`, { observe: 'response' })
      .pipe(map((): void => undefined));
  }

  /**
   * Delete item from sale
   */
  deleteItemFromAssurance(id: SaleLineId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/delete-item/assurance/${id.id}/${id.saleDate}`, { observe: 'response' })
      .pipe(map((): void => undefined));
  }

  /**
   * Add global remise (discount) to cash sale
   */
  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/comptant/add-remise`, key, { observe: 'response' });
  }

  /**
   * Remove remise from cash sale
   */
  removeRemiseFromCashSale(saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/comptant/remove-remise/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
  }

  // ============================================
  // PENDING SALES (MISE EN ATTENTE)
  // ============================================

  /**
   * Put comptant sale on standby (prévente)
   * Sets sale status to STANDBY without finalizing
   */
  putComptantOnStandby(sales: ISales): Observable<FinalyseSale> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<FinalyseSale>(`${this.resourceUrl}/comptant/put-on-hold`, copy, { observe: 'response' })
      .pipe(map(res => res.body!));
  }

  /**
   * Put assurance/carnet sale on standby (prévente)
   * Sets sale status to STANDBY without finalizing
   */
  putAssuranceOnStandby(sales: ISales): Observable<FinalyseSale> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<FinalyseSale>(`${this.resourceUrl}/assurance/put-on-hold`, copy, { observe: 'response' })
      .pipe(map(res => res.body!));
  }

  /**
   * Get all pending sales
   * Returns list of sales waiting to be completed
   */
  getPendingSales(req: any): Observable<ISales[]> {
    const params = createRequestOptions(req);
    return this.http.get<ISales[]>(`${this.resourceUrl}/prevente`, { params, observe: 'response' }).pipe(
      map((res: EntityArrayResponseType) => {
        if (res.body) {
          res.body.forEach(sale => {
            if (sale.createdAt) sale.createdAt = moment(sale.createdAt);
            if (sale.updatedAt) sale.updatedAt = moment(sale.updatedAt);
          });
        }
        return res.body || [];
      }),
    );
  }

  /**
   * Delete pending sale (prévente) for vno (comptant)
   */
  deletePreventeComptant(saleId: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/${saleId.id}/${saleId.saleDate}`, { observe: 'response' })
      .pipe(map((): void => undefined));
  }
  /**
   * Delete pending sale (prévente) for assurance/carnet
   */
  deletePreventeAssurance(saleId: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/assurance/${saleId.id}/${saleId.saleDate}`, { observe: 'response' })
      .pipe(map((): void => undefined));
  }

  removeThirdPartyFromSales(id: number, saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/remove-tiers-payant/assurance/${id}/${saleId.id}/${saleId.saleDate}`, {
      observe: 'response',
    });
  }

  /**
   * Convert item dates from client format (Moment) to server format (string)
   */
  private convertItemDateFromClient(salesLine: ISalesLine): ISalesLine {
    const copy: ISalesLine = Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  /**
   * Convert item dates from server format (string) to client format (Moment)
   */
  private convertItemDateFromServer(res: HttpResponse<ISalesLine>): HttpResponse<ISalesLine> {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  /**
   * Convert dates from client format (Moment) to server format (string)
   */
  private convertDateFromClient(sales: ISales): ISales {
    const copy: ISales = Object.assign({}, sales, {
      createdAt: sales.createdAt && sales.createdAt.isValid() ? sales.createdAt.toJSON() : undefined,
      updatedAt: sales.updatedAt && sales.updatedAt.isValid() ? sales.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  /**
   * Convert dates from server format (string) to client format (Moment)
   */
  private convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }
}
