import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { ClientDiffere } from './model/client-differe.model';
import { Differe } from './model/differe.model';
import { DiffereParam } from './model/differe-param.model';
import { ReglementDiffere } from './model/reglement-differe.model';
import { NewReglementDiffere, PaymentId, ReglementDiffereResponse } from './model/new-reglement-differe.model';
import { DiffereSummary } from './model/differe-summary.model';
import { ReglementDiffereSummary } from './model/reglement-differe-summary.model';

@Injectable({
  providedIn: 'root',
})
export class DiffereService {
  differeParams: WritableSignal<DiffereParam> = signal<DiffereParam>(null);

  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/differes';

  query(req?: any): Observable<HttpResponse<Differe[]>> {
    const options = createRequestOptions(req);
    return this.http.get<Differe[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  findClients(): Observable<HttpResponse<ClientDiffere[]>> {
    return this.http.get<ClientDiffere[]>(this.resourceUrl + '/customers', {
      observe: 'response',
    });
  }

  find(id: number): Observable<HttpResponse<Differe>> {
    return this.http.get<Differe>(`${this.resourceUrl}/customers/${id}`, { observe: 'response' });
  }

  setParams(searchParams: DiffereParam): void {
    this.differeParams.set(searchParams);
  }

  exportListToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }

  getReglementsDifferes(req?: any): Observable<HttpResponse<ReglementDiffere[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ReglementDiffere[]>(this.resourceUrl + '/reglements', {
      params: options,
      observe: 'response',
    });
  }

  printReceipt(paymentId: PaymentId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/print-receipt/${paymentId.id}/${paymentId.transactionDate}`, { observe: 'response' });
  }

  getEscPosReceiptForTauri(paymentId: PaymentId): Observable<ArrayBuffer> {
    return this.http.get(`${this.resourceUrl}/print-tauri/${paymentId.id}/${paymentId.transactionDate}`, {
      responseType: 'arraybuffer',
    });
  }

  getDiffereSummary(req?: any): Observable<HttpResponse<DiffereSummary>> {
    const options = createRequestOptions(req);
    return this.http.get<DiffereSummary>(this.resourceUrl + '/summary', {
      params: options,
      observe: 'response',
    });
  }

  doReglement(reglementParams: NewReglementDiffere): Observable<HttpResponse<ReglementDiffereResponse>> {
    return this.http.post<ReglementDiffereResponse>(this.resourceUrl + '/do-reglement', reglementParams, { observe: 'response' });
  }

  getReglementDiffereSummary(req?: any): Observable<HttpResponse<ReglementDiffereSummary>> {
    const options = createRequestOptions(req);
    return this.http.get<ReglementDiffereSummary>(this.resourceUrl + '/reglements/summary', {
      params: options,
      observe: 'response',
    });
  }

  exportReglementsToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/reglements/pdf`, { params: options, responseType: 'blob' });
  }
}
