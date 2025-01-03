import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { InvoicePaymentItem, Reglement, ReglementParams, ResponseReglement } from './model/reglement.model';
import { createRequestOptions } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<Reglement>;
type EntityArrayResponseType = HttpResponse<Reglement[]>;

@Injectable({
  providedIn: 'root',
})
export class ReglementService {
  public resourceUrl = SERVER_API_URL + 'api/reglements';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<Reglement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
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

  getItems(id: number): Observable<HttpResponse<InvoicePaymentItem[]>> {
    return this.http.get<InvoicePaymentItem[]>(`${this.resourceUrl}/items/${id}`, { observe: 'response' });
  }

  getGroupItems(id: number): Observable<HttpResponse<Reglement[]>> {
    return this.http.get<Reglement[]>(`${this.resourceUrl}/group/items/${id}`, { observe: 'response' });
  }

  printReceipt(id: number): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/print-receipt/${id}`, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
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
