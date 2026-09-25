import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import {
  ISupplierEvolution,
  ISupplierPerformance,
  ISupplierPerformanceSummary,
  ISupplierPurchase,
} from 'app/shared/model/report/supplier-performance.model';

type EntityArrayResponseType = HttpResponse<ISupplierPerformance[]>;
type EntityResponseType = HttpResponse<ISupplierPerformance>;
type SummaryResponseType = HttpResponse<ISupplierPerformanceSummary>;

@Injectable({ providedIn: 'root' })
export class SupplierPerformanceReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/supplier-performance';
  private readonly http = inject(HttpClient);

  /**
   * Get all supplier performance data
   */
  getAllSupplierPerformance(): Observable<EntityArrayResponseType> {
    return this.http.get<ISupplierPerformance[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get supplier performance for a specific supplier
   */
  getSupplierPerformance(fournisseurId: number): Observable<EntityResponseType> {
    return this.http.get<ISupplierPerformance>(`${this.resourceUrl}/${fournisseurId}`, { observe: 'response' });
  }

  /**
   * Get top suppliers by purchase volume
   */
  getTopSuppliersByVolume(limit = 10): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ISupplierPerformance[]>(`${this.resourceUrl}/top`, { params, observe: 'response' });
  }

  /**
   * Top fournisseurs par montant d'achat sur une fenetre. Contrairement a `getTopSuppliersByVolume`,
   * dont les fenetres 30 jours et 12 mois sont figees dans la vue materialisee, celle-ci suit la
   * periode demandee.
   */
  getTopSuppliersByPeriode(fromDate: string, toDate: string, limit = 10): Observable<HttpResponse<ISupplierPurchase[]>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate).set('limit', limit.toString());
    return this.http.get<ISupplierPurchase[]>(`${this.resourceUrl}/achats`, { params, observe: 'response' });
  }

  /**
   * Get suppliers by performance score
   */
  getSuppliersByPerformanceScore(minScore = 70): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('minScore', minScore.toString());
    return this.http.get<ISupplierPerformance[]>(`${this.resourceUrl}/score`, { params, observe: 'response' });
  }

  /**
   * Get suppliers with delivery issues
   */
  getSuppliersWithDeliveryIssues(): Observable<EntityArrayResponseType> {
    return this.http.get<ISupplierPerformance[]>(`${this.resourceUrl}/delivery-issues`, { observe: 'response' });
  }

  /**
   * Get aggregated supplier performance summary
   */
  getSupplierPerformanceSummary(): Observable<SummaryResponseType> {
    return this.http.get<ISupplierPerformanceSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Get monthly N vs N-1 evolution (rolling 12 months)
   */
  getEvolution(): Observable<HttpResponse<ISupplierEvolution>> {
    return this.http.get<ISupplierEvolution>(`${this.resourceUrl}/evolution`, { observe: 'response' });
  }

  /**
   * Export supplier performance report as PDF
   */
  exportSupplierPerformanceToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
