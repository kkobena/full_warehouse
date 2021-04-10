import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IPaymentMode } from 'app/shared/model/payment-mode.model';

type EntityResponseType = HttpResponse<IPaymentMode>;
type EntityArrayResponseType = HttpResponse<IPaymentMode[]>;

@Injectable({ providedIn: 'root' })
export class PaymentModeService {
  public resourceUrl = SERVER_API_URL + 'api/payment-modes';

  constructor(protected http: HttpClient) {}

  create(paymentMode: IPaymentMode): Observable<EntityResponseType> {
    return this.http.post<IPaymentMode>(this.resourceUrl, paymentMode, { observe: 'response' });
  }

  update(paymentMode: IPaymentMode): Observable<EntityResponseType> {
    return this.http.put<IPaymentMode>(this.resourceUrl, paymentMode, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IPaymentMode>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IPaymentMode[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
