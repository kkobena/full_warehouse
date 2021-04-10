import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IDailyca } from '../shared/model/dailyca.model';
import { createRequestOption } from 'app/shared/util/request-util';
import { IStatistiqueProduit } from 'app/shared/model/statistique-produit.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  public resourceUrl = SERVER_API_URL + 'api/dashboard';

  constructor(protected http: HttpClient) {}

  daily(): Observable<HttpResponse<IDailyca>> {
    return this.http.get<IDailyca>(`${this.resourceUrl}/daily`, { observe: 'response' });
  }

  weekly(): Observable<HttpResponse<IDailyca>> {
    return this.http.get<IDailyca>(`${this.resourceUrl}/weekly`, { observe: 'response' });
  }

  monthly(): Observable<HttpResponse<IDailyca>> {
    return this.http.get<IDailyca>(`${this.resourceUrl}/monthly`, { observe: 'response' });
  }

  yearly(): Observable<HttpResponse<IDailyca>> {
    return this.http.get<IDailyca>(`${this.resourceUrl}/yearly`, { observe: 'response' });
  }

  yearlyQuantity(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/yearly-quantity`, { params: options, observe: 'response' });
  }

  yearlyAmount(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/yearly-amount`, { params: options, observe: 'response' });
  }

  monthlyQuantity(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/monthly-quantity`, { params: options, observe: 'response' });
  }

  monthlyAmount(req?: any): Observable<HttpResponse<IStatistiqueProduit>> {
    const options = createRequestOption(req);
    return this.http.get<IStatistiqueProduit>(`${this.resourceUrl}/monthly-amount`, { params: options, observe: 'response' });
  }
}
