import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { FinancialTransaction, MvtCaisse, MvtCaisseWrapper } from '../cash-register/model/cash-register.model';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { Pair } from '../../shared/model/configuration.model';
import { PaymentId } from '../reglement/model/reglement.model';

type EntityResponseType = HttpResponse<FinancialTransaction>;
type EntityArrayResponseType = HttpResponse<FinancialTransaction[]>;

@Injectable({
  providedIn: 'root',
})
export class MvtCaisseServiceService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/payment-transactions';

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<FinancialTransaction>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  create(entity: FinancialTransaction): Observable<HttpResponse<PaymentId>> {
    return this.http.post<PaymentId>(this.resourceUrl, entity, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<FinancialTransaction[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  findAllMvts(req?: any): Observable<HttpResponse<MvtCaisse[]>> {
    const options = createRequestOptions(req);
    return this.http.get<MvtCaisse[]>(this.resourceUrl + '/mvt-caisses', {
      params: options,
      observe: 'response',
    });
  }

  findAllMvtsSum(req?: any): Observable<HttpResponse<MvtCaisseWrapper>> {
    const options = createRequestOptions(req);
    return this.http.get<MvtCaisseWrapper>(this.resourceUrl + '/mvt-caisses/sum', {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }

  findAllMvtsTypes(): Observable<HttpResponse<Pair[]>> {
    return this.http.get<Pair[]>(this.resourceUrl + '/types', { observe: 'response' });
  }

  printReceipt(paymentId: PaymentId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/print-receipt/${paymentId.id}/${paymentId.transactionDate}`, { observe: 'response' });
  }

  getEscPosReceiptForTauri(paymentId: PaymentId): Observable<ArrayBuffer> {
    return this.http.get(`${this.resourceUrl}/print-tauri/${paymentId.id}/${paymentId.transactionDate}`, {
      responseType: 'arraybuffer',
    });
  }
}
