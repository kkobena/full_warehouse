import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOption, createRequestOptions } from 'app/shared/util/request-util';
import { IStatistiqueProduit } from 'app/shared/model/statistique-produit.model';
import { VenteRecordWrapper } from '../shared/model/vente-record.model';
import { VenteRecordParam } from '../shared/model/vente-record-param.model';
import { AchatRecordParam } from '../shared/model/achat-record-param.model';
import { AchatRecord } from '../shared/model/achat-record.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  public resourceUrl = SERVER_API_URL + 'api/dashboard';

  constructor(protected http: HttpClient) {}

  fetchCa(venteRecordParam: VenteRecordParam): Observable<HttpResponse<VenteRecordWrapper>> {
    const options = createRequestOptions(venteRecordParam);
    return this.http.get<VenteRecordWrapper>(`${this.resourceUrl}/ca`, {
      params: options,
      observe: 'response',
    });
  }

  fetchCaAchat(achatRecordParam: AchatRecordParam): Observable<HttpResponse<AchatRecord>> {
    const options = createRequestOptions(achatRecordParam);
    return this.http.get<AchatRecord>(`${this.resourceUrl}/ca-achats`, {
      params: options,
      observe: 'response',
    });
  }

  yearlyQuantity(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/yearly-quantity`, {
      params: options,
      observe: 'response',
    });
  }

  yearlyAmount(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/yearly-amount`, {
      params: options,
      observe: 'response',
    });
  }

  monthlyQuantity(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/monthly-quantity`, {
      params: options,
      observe: 'response',
    });
  }

  monthlyAmount(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/monthly-amount`, {
      params: options,
      observe: 'response',
    });
  }
}
