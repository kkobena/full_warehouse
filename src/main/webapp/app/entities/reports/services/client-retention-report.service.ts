import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IClientRetentionKpi, IClientRetentionRow } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class ClientRetentionReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/client-retention';
  private readonly http = inject(HttpClient);

  getKpi(): Observable<HttpResponse<IClientRetentionKpi>> {
    return this.http.get<IClientRetentionKpi>(`${this.resourceUrl}/kpi`, { observe: 'response' });
  }

  getClientList(limit = 200): Observable<HttpResponse<IClientRetentionRow[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<IClientRetentionRow[]>(`${this.resourceUrl}/list`, { params, observe: 'response' });
  }
}
