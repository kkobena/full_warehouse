import { inject, Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  InvoicePaymentItem,
  PaymentId,
  Reglement,
  ReglementParams,
  ResponseReglement
} from './model/reglement.model';
import { createRequestOptions } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<Reglement>;
type EntityArrayResponseType = HttpResponse<Reglement[]>;

@Injectable({
  providedIn: 'root',
})
export class ReglementService {
  public resourceUrl = SERVER_API_URL + 'api/reglements';
  protected http = inject(HttpClient);

  find(paymentId: PaymentId): Observable<EntityResponseType> {
    return this.http.get<Reglement>(`${this.resourceUrl}/${paymentId.id}/${paymentId.transactionDate}`, { observe: 'response' });
  }

  doReglement(reglementParams: ReglementParams): Observable<HttpResponse<ResponseReglement>> {
    return this.http.post<ResponseReglement>(SERVER_API_URL + 'api/reglement-factures-tp', reglementParams, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<Reglement[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  getItems(paymentId: PaymentId): Observable<HttpResponse<InvoicePaymentItem[]>> {
    return this.http.get<InvoicePaymentItem[]>(`${this.resourceUrl}/items/${paymentId.id}/${paymentId.transactionDate}`, {
      observe: 'response',
    });
  }

  getGroupItems(paymentId: PaymentId): Observable<HttpResponse<Reglement[]>> {
    return this.http.get<Reglement[]>(`${this.resourceUrl}/group/items/${paymentId.id}/${paymentId.transactionDate}`, {
      observe: 'response',
    });
  }

  printReceipt(paymentId: PaymentId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/print-receipt/${paymentId.id}/${paymentId.transactionDate}`, { observe: 'response' });
  }

  delete(paymentId: PaymentId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${paymentId.id}/${paymentId.transactionDate}`, { observe: 'response' });
  }

  deleteAll(req?: any): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.delete(`${this.resourceUrl}/all`, { params: options, observe: 'response' });
  }

  onPrintPdf(req?: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }
}
