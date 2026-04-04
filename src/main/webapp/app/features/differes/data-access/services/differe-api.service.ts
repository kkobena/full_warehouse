import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import {
  IClientDiffere,
  IDiffere,
  IDiffereSummary,
  IDiffereSearchParams,
  INewReglementDiffere,
  IPaymentIdDiffere,
  IReglementDiffere,
  IReglementDiffereResponse,
  IReglementDiffereSummary,
} from '../models';

@Injectable({ providedIn: 'root' })
export class DiffereApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/differes';
  private readonly http = inject(HttpClient);

  query(params: IDiffereSearchParams): Observable<HttpResponse<IDiffere[]>> {
    return this.http.get<IDiffere[]>(this.resourceUrl, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  findClients(): Observable<HttpResponse<IClientDiffere[]>> {
    return this.http.get<IClientDiffere[]>(`${this.resourceUrl}/customers`, { observe: 'response' });
  }

  findByCustomer(customerId: number): Observable<HttpResponse<IDiffere>> {
    return this.http.get<IDiffere>(`${this.resourceUrl}/customers/${customerId}`, { observe: 'response' });
  }

  getDiffereSummary(params: IDiffereSearchParams): Observable<HttpResponse<IDiffereSummary>> {
    return this.http.get<IDiffereSummary>(`${this.resourceUrl}/summary`, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  getReglementsDifferes(params: IDiffereSearchParams): Observable<HttpResponse<IReglementDiffere[]>> {
    return this.http.get<IReglementDiffere[]>(`${this.resourceUrl}/reglements`, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  getReglementDiffereSummary(params: IDiffereSearchParams): Observable<HttpResponse<IReglementDiffereSummary>> {
    return this.http.get<IReglementDiffereSummary>(`${this.resourceUrl}/reglements/summary`, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  doReglement(params: INewReglementDiffere): Observable<HttpResponse<IReglementDiffereResponse>> {
    return this.http.post<IReglementDiffereResponse>(`${this.resourceUrl}/do-reglement`, params, { observe: 'response' });
  }

  printReceipt(paymentId: IPaymentIdDiffere): Observable<HttpResponse<{}>> {
    return this.http.get(
      `${this.resourceUrl}/print-receipt/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  getEscPosReceiptForTauri(paymentId: IPaymentIdDiffere): Observable<ArrayBuffer> {
    return this.http.get(
      `${this.resourceUrl}/print-tauri/${paymentId.id}/${paymentId.transactionDate}`,
      { responseType: 'arraybuffer' },
    );
  }

  exportListToPdf(params: IDiffereSearchParams): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }

  exportReglementsToPdf(params: IDiffereSearchParams): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/reglements/pdf`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }

  exportToExcel(params: Pick<IDiffereSearchParams, 'customerId'>): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/export`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }
}
