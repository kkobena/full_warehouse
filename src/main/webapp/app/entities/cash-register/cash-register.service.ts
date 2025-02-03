import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { CashRegister } from './model/cash-register.model';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOption, createRequestOptions } from '../../shared/util/request-util';
import { Ticketing } from './model/ticketing.model';

type EntityResponseType = HttpResponse<CashRegister>;
type EntityArrayResponseType = HttpResponse<CashRegister[]>;

@Injectable({
  providedIn: 'root',
})
export class CashRegisterService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/cash-registers';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<CashRegister>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<CashRegister[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  getConnectedUserNonClosedCashRegisters(): Observable<EntityArrayResponseType> {
    return this.http.get<CashRegister[]>(this.resourceUrl + '/connected-user-non-closed-cash-registers', {
      observe: 'response',
    });
  }

  doTicketing(ticketing: Ticketing): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/do-ticketing', ticketing, { observe: 'response' });
  }

  openCashRegister(req?: any): Observable<EntityResponseType> {
    const options = createRequestOption(req);
    return this.http.get<CashRegister>(this.resourceUrl + '/open-cash-register', {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }

  closeCashRegister(cashRegisterId: number): Observable<HttpResponse<{}>> {
    return this.http.get(`${this.resourceUrl}/close-cash-register/${cashRegisterId}`, { observe: 'response' });
  }
}
