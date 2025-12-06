import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IStockAlert, StockAlertType } from 'app/shared/model/report/stock-alert.model';

type EntityArrayResponseType = HttpResponse<IStockAlert[]>;
type AlertCountResponseType = HttpResponse<Record<StockAlertType, number>>;

@Injectable({ providedIn: 'root' })
export class StockAlertReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/stock/alerts';
  private readonly http = inject(HttpClient);

  /**
   * Get stock alerts with optional filtering by alert types
   */
  getStockAlerts(types?: StockAlertType[]): Observable<EntityArrayResponseType> {
    let params = new HttpParams();
    if (types && types.length > 0) {
      types.forEach(type => {
        params = params.append('types', type);
      });
    }
    return this.http.get<IStockAlert[]>(this.resourceUrl, { params, observe: 'response' });
  }

  /**
   * Get count of stock alerts by type
   */
  getStockAlertsCount(): Observable<AlertCountResponseType> {
    return this.http.get<Record<StockAlertType, number>>(`${this.resourceUrl}/count`, { observe: 'response' });
  }

  /**
   * Export stock alerts as PDF
   */
  exportStockAlertsToPdf(types?: StockAlertType[]): Observable<HttpResponse<Blob>> {
    let params = new HttpParams();
    if (types && types.length > 0) {
      types.forEach(type => {
        params = params.append('types', type);
      });
    }
    return this.http.get(`${this.resourceUrl}/export`, {
      params,
      responseType: 'blob',
      observe: 'response',
    });
  }
}
