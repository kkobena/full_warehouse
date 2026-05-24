import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IDailySalesSummary } from 'app/shared/model/report/daily-sales-summary.model';

type EntityArrayResponseType = HttpResponse<IDailySalesSummary[]>;

@Injectable({ providedIn: 'root' })
export class SalesSummaryReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales-summary';
  private readonly http = inject(HttpClient);

  /**
   * Get daily sales summary for a date range
   */
  getDailySalesSummary(startDate: string, endDate: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IDailySalesSummary[]>(this.resourceUrl, { params, observe: 'response' });
  }

  /**
   * Get daily sales summary for a specific date
   */
  getDailySalesSummaryByDate(date: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('date', date);
    return this.http.get<IDailySalesSummary[]>(`${this.resourceUrl}/by-date`, { params, observe: 'response' });
  }

  /**
   * Get daily sales summary filtered by sale type
   */
  getDailySalesSummaryByType(startDate: string, endDate: string, typeVente: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate).set('typeVente', typeVente);
    return this.http.get<IDailySalesSummary[]>(`${this.resourceUrl}/by-type`, { params, observe: 'response' });
  }
}
