import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IProductProfitability, IProfitabilitySummary } from 'app/shared/model/report';
import { BCGCategory } from 'app/shared/model/report/bcg-category.enum';

type EntityArrayResponseType = HttpResponse<IProductProfitability[]>;
type EntityResponseType = HttpResponse<IProfitabilitySummary>;

@Injectable({ providedIn: 'root' })
export class ProfitabilityReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/profitability';
  private readonly http = inject(HttpClient);

  /**
   * Get all product profitability data
   */
  getAllProductProfitability(): Observable<EntityArrayResponseType> {
    return this.http.get<IProductProfitability[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get product profitability filtered by category
   */
  getProductProfitabilityByCategory(categorie: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('categorie', categorie);
    return this.http.get<IProductProfitability[]>(`${this.resourceUrl}/category`, { params, observe: 'response' });
  }

  /**
   * Get product profitability filtered by BCG classification
   */
  getProductProfitabilityByBCGCategory(bcgCategory: BCGCategory): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('bcgCategory', bcgCategory);
    return this.http.get<IProductProfitability[]>(`${this.resourceUrl}/bcg`, { params, observe: 'response' });
  }

  /**
   * Get top N most profitable products
   */
  getTopProfitableProducts(limit = 20): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<IProductProfitability[]>(`${this.resourceUrl}/top`, { params, observe: 'response' });
  }

  /**
   * Get low margin products (< 10%)
   */
  getLowMarginProducts(): Observable<EntityArrayResponseType> {
    return this.http.get<IProductProfitability[]>(`${this.resourceUrl}/low-margin`, { observe: 'response' });
  }

  /**
   * Get aggregated profitability summary
   */
  getProfitabilitySummary(): Observable<EntityResponseType> {
    return this.http.get<IProfitabilitySummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Export profitability report as PDF
   */
  exportProfitabilityToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
