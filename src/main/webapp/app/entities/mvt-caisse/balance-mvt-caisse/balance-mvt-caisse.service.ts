import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../../shared/util/request-util';
import { BalanceCaisseWrapper } from './balance-caisse.model';

@Injectable({
  providedIn: 'root',
})
export class BalanceMvtCaisseService {
  public resourceUrl = SERVER_API_URL + 'api/balances';

  constructor(protected http: HttpClient) {}

  query(req?: any): Observable<HttpResponse<BalanceCaisseWrapper>> {
    const options = createRequestOptions(req);
    return this.http.get<BalanceCaisseWrapper>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }
}
