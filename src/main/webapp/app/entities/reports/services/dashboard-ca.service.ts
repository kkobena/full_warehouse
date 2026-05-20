import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IBasketEvolution, IDailyCA, IDashboardCASummary, IDashboardCAEvolution, IPaymentMethodCA, IProductFamilyCA } from 'app/shared/model/report';
import { ITopProduct } from 'app/shared/model/report/top-product.model';

type EntityArrayResponseType = HttpResponse<IDailyCA[]>;
type PaymentMethodResponseType = HttpResponse<IPaymentMethodCA[]>;
type ProductFamilyResponseType = HttpResponse<IProductFamilyCA[]>;
type TopProductsResponseType = HttpResponse<ITopProduct[]>;
type SummaryResponseType = HttpResponse<IDashboardCASummary>;
type EvolutionResponseType = HttpResponse<IDashboardCAEvolution>;

/**
 * Service for Dashboard CA (Chiffre d'Affaires)
 */
@Injectable({ providedIn: 'root' })
export class DashboardCAService {
  private readonly resourceUrl = SERVER_API_URL + 'api/dashboard-ca';
  private readonly http = inject(HttpClient);

  /**
   * Get daily CA summary for a date range
   */
  getDailySummary(startDate: string, endDate: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IDailyCA[]>(`${this.resourceUrl}/daily`, { params, observe: 'response' });
  }

  /**
   * Get overall summary with KPIs (today, week, month, year)
   */
  getOverallSummary(): Observable<SummaryResponseType> {
    return this.http.get<IDashboardCASummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Get evolution data for charts
   * @param period 'daily', 'weekly', 'monthly'
   * @param startDate start date in ISO format
   * @param endDate end date in ISO format
   */
  getEvolutionData(period: string, startDate: string, endDate: string): Observable<EvolutionResponseType> {
    const params = new HttpParams().set('period', period).set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IDashboardCAEvolution>(`${this.resourceUrl}/evolution`, { params, observe: 'response' });
  }

  /**
   * Get CA distribution by payment method
   */
  getPaymentMethodDistribution(startDate: string, endDate: string): Observable<PaymentMethodResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IPaymentMethodCA[]>(`${this.resourceUrl}/payment-methods`, { params, observe: 'response' });
  }

  /**
   * Get CA distribution by product family
   */
  getProductFamilyDistribution(startDate: string, endDate: string): Observable<ProductFamilyResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IProductFamilyCA[]>(`${this.resourceUrl}/product-families`, { params, observe: 'response' });
  }

  /**
   * Get top products by CA
   */
  getTopProducts(startDate: string, endDate: string, limit = 10): Observable<TopProductsResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate).set('limit', limit.toString());
    return this.http.get<ITopProduct[]>(`${this.resourceUrl}/top-products`, { params, observe: 'response' });
  }

  /**
   * Export dashboard to PDF
   */
  exportDashboardToPdf(startDate: string, endDate: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${this.resourceUrl}/export`, { params, observe: 'response', responseType: 'blob' });
  }

  /**
   * Export daily summary to Excel
   */
  exportDailySummaryToExcel(startDate: string, endDate: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${this.resourceUrl}/export/excel`, { params, observe: 'response', responseType: 'blob' });
  }

  /**
   * Export daily summary to CSV
   */
  exportDailySummaryToCsv(startDate: string, endDate: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${this.resourceUrl}/export/csv`, { params, observe: 'response', responseType: 'blob' });
  }

  /**
   * Export top products to Excel
   */
  exportTopProductsToExcel(startDate: string, endDate: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${this.resourceUrl}/export/top-products/excel`, { params, observe: 'response', responseType: 'blob' });
  }

  /**
   * Export top products to CSV
   */
  exportTopProductsToCsv(startDate: string, endDate: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${this.resourceUrl}/export/top-products/csv`, { params, observe: 'response', responseType: 'blob' });
  }

  /**
   * Get average basket evolution over the last 12 months — GAP-C2
   */
  getBasketEvolution(): Observable<HttpResponse<IBasketEvolution>> {
    return this.http.get<IBasketEvolution>(`${this.resourceUrl}/basket-evolution`, { observe: 'response' });
  }

  /**
   * Refresh materialized views
   */
  refreshViews(): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/refresh`, null, { observe: 'response' });
  }
}
