import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { ISalesForecast, IForecastSummary } from 'app/shared/model/report/sales-forecast.model';

type ForecastResponseType = HttpResponse<ISalesForecast[]>;
type SummaryResponseType = HttpResponse<IForecastSummary>;

/**
 * Service for Sales Forecasting
 */
@Injectable({ providedIn: 'root' })
export class SalesForecastService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales-forecast';
  private readonly http = inject(HttpClient);

  /**
   * Get sales forecast
   */
  getForecast(monthsAhead: number, method: string): Observable<ForecastResponseType> {
    const params = new HttpParams().set('monthsAhead', monthsAhead.toString()).set('method', method);
    return this.http.get<ISalesForecast[]>(this.resourceUrl, { params, observe: 'response' });
  }

  /**
   * Get forecast summary
   */
  getSummary(): Observable<SummaryResponseType> {
    return this.http.get<IForecastSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Get historical vs forecast comparison
   */
  getHistoricalVsForecast(startDate: string, endDate: string, monthsAhead: number): Observable<ForecastResponseType> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('monthsAhead', monthsAhead.toString());
    return this.http.get<ISalesForecast[]>(`${this.resourceUrl}/historical`, { params, observe: 'response' });
  }

  /**
   * Detect seasonality
   */
  detectSeasonality(): Observable<HttpResponse<boolean>> {
    return this.http.get<boolean>(`${this.resourceUrl}/seasonality`, { observe: 'response' });
  }
}
