import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { FinancialTransaction, MvtCaisse, MvtCaisseWrapper } from '../cash-register/model/cash-register.model';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { Pair } from '../../shared/model/configuration.model';

type EntityResponseType = HttpResponse<FinancialTransaction>;
type EntityArrayResponseType = HttpResponse<FinancialTransaction[]>;

@Injectable({
  providedIn: 'root',
})
export class MvtCaisseServiceService {
  public resourceUrl = SERVER_API_URL + 'api/payment-transactions';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<FinancialTransaction>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  create(entity: FinancialTransaction): Observable<EntityResponseType> {
    return this.http.post<FinancialTransaction>(this.resourceUrl, entity, { observe: 'response' });
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

  findAllMvtsTypes(): Observable<HttpResponse<Pair[]>> {
    return this.http.get<Pair[]>(this.resourceUrl + '/types', { observe: 'response' });
  }
}
