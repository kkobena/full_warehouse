import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import {
  IInvoicePaymentItem,
  IInvoicePaymentParam,
  IPaymentId,
  IReglement,
  IReglementParams,
  IResponseReglement,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ReglementApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/reglements';
  private readonly http = inject(HttpClient);

  find(paymentId: IPaymentId): Observable<HttpResponse<IReglement>> {
    return this.http.get<IReglement>(
      `${this.resourceUrl}/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  query(params: IInvoicePaymentParam): Observable<HttpResponse<IReglement[]>> {
    return this.http.get<IReglement[]>(this.resourceUrl, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  doReglement(params: IReglementParams): Observable<HttpResponse<IResponseReglement>> {
    return this.http.post<IResponseReglement>(
      SERVER_API_URL + 'api/reglement-factures-tp',
      params,
      { observe: 'response' },
    );
  }

  getItems(paymentId: IPaymentId): Observable<HttpResponse<IInvoicePaymentItem[]>> {
    return this.http.get<IInvoicePaymentItem[]>(
      `${this.resourceUrl}/items/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  getGroupItems(paymentId: IPaymentId): Observable<HttpResponse<IReglement[]>> {
    return this.http.get<IReglement[]>(
      `${this.resourceUrl}/group/items/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  printReceipt(paymentId: IPaymentId): Observable<HttpResponse<{}>> {
    return this.http.get(
      `${this.resourceUrl}/print-receipt/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  getEscPosReceiptForTauri(paymentId: IPaymentId): Observable<ArrayBuffer> {
    return this.http.get(
      `${this.resourceUrl}/print-tauri/${paymentId.id}/${paymentId.transactionDate}`,
      { responseType: 'arraybuffer' },
    );
  }

  delete(paymentId: IPaymentId): Observable<HttpResponse<{}>> {
    return this.http.delete(
      `${this.resourceUrl}/${paymentId.id}/${paymentId.transactionDate}`,
      { observe: 'response' },
    );
  }

  deleteAll(params: { ids: number[] }): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/all`, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  exportToPdf(params: IInvoicePaymentParam): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }

  exportExcel(params: IInvoicePaymentParam): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/export`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }
}
