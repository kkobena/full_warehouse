import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption, createRequestOptions } from 'app/shared/util/request-util';
import { FinalyseSale, ISales, SaleId, UpdateSaleInfo } from 'app/shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../shared/model/sales-line.model';
import { IResponseDto } from '../../shared/util/response-dto';
import { UtilisationCleSecurite } from '../action-autorisation/utilisation-cle-securite.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales';
  private readonly  http = inject(HttpClient);

  createComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  authorizeAction(utilisationCleSecurite: UtilisationCleSecurite): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/comptant/authorize-action', utilisationCleSecurite, { observe: 'response' });
  }

  updateDate(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(this.resourceUrl + '/assurance/date', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  closeComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(`${this.resourceUrl}/comptant/close`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: SaleId): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  findForEdit(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/edit/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  saveComptant(sales: ISales): Observable<HttpResponse<IResponseDto>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<IResponseDto>(this.resourceUrl + '/comptant/save', copy, { observe: 'response' });
  }

  saveCash(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/comptant/save', copy, { observe: 'response' });
  }

  print(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  printReceipt(id: SaleId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/print/receipt/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  rePrintReceipt(id: SaleId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/re-print/receipt/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  /**
   * Get receipt as byte arrays for Tauri printing
   * Returns a list of PNG images (as byte arrays) representing each page of the receipt
   */
  getReceiptForTauri(id: SaleId): Observable<string[]> {
    return this.http.get<string[]>(`${this.resourceUrl}/receipt/tauri/${id.id}/${id.saleDate}`);
  }

  /**
   * Get assurance receipt as byte arrays for Tauri printing
   * Returns a list of PNG images (as byte arrays) representing each page of the receipt
   */
  getAssuranceReceiptForTauri(id: SaleId): Observable<string[]> {
    return this.http.get<string[]>(`${this.resourceUrl}/assurance/receipt/tauri/${id.id}/${id.saleDate}`);
  }


  getEscPosReceiptForTauri(id: SaleId, isEdition: boolean = false): Observable<ArrayBuffer> {
    return this.http.get(`${this.resourceUrl}/receipt/tauri/${id.id}/${id.saleDate}`, {
      params: { isEdition: isEdition},
      responseType: 'arraybuffer'
    });
  }

  addItemComptant(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(`${this.resourceUrl}/add-item/comptant`, copy, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  create(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateItemPrice(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtyRequested(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtySold(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  deleteItem(id: SaleLineId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  putCurrentCashSaleOnStandBy(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/comptant/put-on-hold', copy, { observe: 'response' });
  }

  addCustommerToCashSale(keyValue: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/comptant/add-customer', keyValue, { observe: 'response' });
  }

  removeCustommerToCashSale(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/comptant/remove-customer/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  queryPrevente(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ISales[]>(`${this.resourceUrl}/prevente`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  deletePrevente(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/prevente/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  printInvoice(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  cancelComptant(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/comptant/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  cancelAssurance(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/assurance/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  transform(req?: any): Observable<HttpResponse<SaleId>> {
    const options = createRequestOption(req);
    return this.http.get<SaleId>(this.resourceUrl + '/assurance/transform', {
      params: options,
      observe: 'response'
    });
  }

  removeRemiseFromCashSale(saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/comptant/remove-remise/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
  }

  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/comptant/add-remise', key, { observe: 'response' });
  }

  private convertDateFromClient(sales: ISales): ISales {
    return Object.assign({}, sales, {
      createdAt: sales.createdAt && sales.createdAt.isValid() ? sales.createdAt.toJSON() : undefined,
      updatedAt: sales.updatedAt && sales.updatedAt.isValid() ? sales.updatedAt.toJSON() : undefined
    });
  }

  private convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((sales: ISales) => {
        sales.createdAt = sales.createdAt ? moment(sales.createdAt) : undefined;
        sales.updatedAt = sales.updatedAt ? moment(sales.updatedAt) : undefined;
      });
    }
    return res;
  }

  private convertItemDateFromClient(salesLine: ISalesLine): ISalesLine {
    const copy: ISalesLine = Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined
    });
    return copy;
  }

  private convertItemDateFromServer(res: HttpResponse<ISalesLine>): HttpResponse<ISalesLine> {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }
}
