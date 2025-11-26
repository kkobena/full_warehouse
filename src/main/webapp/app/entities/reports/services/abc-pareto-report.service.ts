import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IABCPareto, IABCParetoSummary } from 'app/shared/model/report';
import { ClassePareto } from 'app/shared/model/report/classe-pareto.enum';

type EntityArrayResponseType = HttpResponse<IABCPareto[]>;
type EntityResponseType = HttpResponse<IABCParetoSummary>;

@Injectable({ providedIn: 'root' })
export class ABCParetoReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/abc-pareto';
  private readonly http = inject(HttpClient);

  /**
   * Get all ABC Pareto analysis data
   */
  getAllABCParetoAnalysis(): Observable<EntityArrayResponseType> {
    return this.http.get<IABCPareto[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get ABC Pareto analysis filtered by category
   */
  getABCParetoByCategory(categorie: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('categorie', categorie);
    return this.http.get<IABCPareto[]>(`${this.resourceUrl}/category`, { params, observe: 'response' });
  }

  /**
   * Get ABC Pareto analysis filtered by Pareto class
   */
  getABCParetoByClass(classePareto: ClassePareto): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('classePareto', classePareto);
    return this.http.get<IABCPareto[]>(`${this.resourceUrl}/class`, { params, observe: 'response' });
  }

  /**
   * Get top N products by revenue contribution
   */
  getTopRevenueContributors(limit: number = 20): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<IABCPareto[]>(`${this.resourceUrl}/top`, { params, observe: 'response' });
  }

  /**
   * Get aggregated ABC Pareto summary
   */
  getABCParetoSummary(): Observable<EntityResponseType> {
    return this.http.get<IABCParetoSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Export ABC Pareto report as PDF
   */
  exportABCParetoToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
