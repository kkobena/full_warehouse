import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IComparativeByFamily, IComparativeByFournisseur, IComparativeCA, IComparativeByType, IComparativeSummary } from 'app/shared/model/report/comparative-report.model';

type ComparativeCAResponseType = HttpResponse<IComparativeCA[]>;
type ComparativeByTypeResponseType = HttpResponse<IComparativeByType[]>;
type ComparativeSummaryResponseType = HttpResponse<IComparativeSummary>;

/**
 * Service for Comparative Reports (Tableaux Comparatifs)
 */
@Injectable({ providedIn: 'root' })
export class ComparativeReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/comparative-reports';
  private readonly http = inject(HttpClient);

  /**
   * Get monthly CA comparison
   */
  getMonthlyComparison(year?: number): Observable<ComparativeCAResponseType> {
    const params = year ? new HttpParams().set('year', year.toString()) : new HttpParams();
    return this.http.get<IComparativeCA[]>(`${this.resourceUrl}/monthly`, { params, observe: 'response' });
  }

  /**
   * Get quarterly CA comparison
   */
  getQuarterlyComparison(year?: number): Observable<ComparativeCAResponseType> {
    const params = year ? new HttpParams().set('year', year.toString()) : new HttpParams();
    return this.http.get<IComparativeCA[]>(`${this.resourceUrl}/quarterly`, { params, observe: 'response' });
  }

  /**
   * Get yearly CA comparison
   */
  getYearlyComparison(startDate: string, endDate: string): Observable<ComparativeCAResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<IComparativeCA[]>(`${this.resourceUrl}/yearly`, { params, observe: 'response' });
  }

  /**
   * Get comparison by sales type
   */
  getComparisonBySalesType(currentYear?: number, previousYear?: number): Observable<ComparativeByTypeResponseType> {
    let params = new HttpParams();
    if (currentYear) params = params.set('currentYear', currentYear.toString());
    if (previousYear) params = params.set('previousYear', previousYear.toString());
    return this.http.get<IComparativeByType[]>(`${this.resourceUrl}/by-sales-type`, { params, observe: 'response' });
  }

  /**
   * Get overall comparative summary
   */
  getComparativeSummary(): Observable<ComparativeSummaryResponseType> {
    return this.http.get<IComparativeSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Get comparison by product family (N vs N-1)
   */
  getComparisonByFamily(currentYear?: number, previousYear?: number): Observable<HttpResponse<IComparativeByFamily[]>> {
    let params = new HttpParams();
    if (currentYear) params = params.set('currentYear', currentYear.toString());
    if (previousYear) params = params.set('previousYear', previousYear.toString());
    return this.http.get<IComparativeByFamily[]>(`${this.resourceUrl}/by-family`, { params, observe: 'response' });
  }

  /**
   * Get comparison by supplier (N vs N-1)
   */
  getComparisonByFournisseur(currentYear?: number, previousYear?: number): Observable<HttpResponse<IComparativeByFournisseur[]>> {
    let params = new HttpParams();
    if (currentYear) params = params.set('currentYear', currentYear.toString());
    if (previousYear) params = params.set('previousYear', previousYear.toString());
    return this.http.get<IComparativeByFournisseur[]>(`${this.resourceUrl}/by-fournisseur`, { params, observe: 'response' });
  }

  /**
   * Export comparative report to PDF
   */
  exportToPdf(comparisonType: string, year?: number): Observable<HttpResponse<Blob>> {
    let params = new HttpParams().set('comparisonType', comparisonType);
    if (year) params = params.set('year', year.toString());
    return this.http.get(`${this.resourceUrl}/export`, { params, observe: 'response', responseType: 'blob' });
  }
}
