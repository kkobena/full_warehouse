import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IDemarqueByMotif, IDemarqueKpi } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class DemarqueReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/demarque-report';
  private readonly http = inject(HttpClient);

  getKpi(startDate: string, endDate: string): Observable<HttpResponse<IDemarqueKpi>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IDemarqueKpi>(`${this.resourceUrl}/kpi`, { params, observe: 'response' });
  }

  getByMotif(startDate: string, endDate: string): Observable<HttpResponse<IDemarqueByMotif[]>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IDemarqueByMotif[]>(`${this.resourceUrl}/by-motif`, { params, observe: 'response' });
  }
}
