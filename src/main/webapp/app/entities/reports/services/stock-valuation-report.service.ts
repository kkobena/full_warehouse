import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IStockValuation, IStockValuationSummary } from 'app/shared/model/report/stock-valuation.model';

type EntityArrayResponseType = HttpResponse<IStockValuation[]>;
type EntityResponseType = HttpResponse<IStockValuationSummary>;

@Injectable({ providedIn: 'root' })
export class StockValuationReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/stock/valuation';
  private readonly http = inject(HttpClient);

  /**
   * Get all stock valuation data
   */
  getAllStockValuation(): Observable<EntityArrayResponseType> {
    return this.http.get<IStockValuation[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get stock valuation filtered by category
   */
  getStockValuationByCategory(categorie: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('categorie', categorie);
    return this.http.get<IStockValuation[]>(`${this.resourceUrl}/category`, { params, observe: 'response' });
  }

  /**
   * Get stock valuation filtered by storage location
   */
  getStockValuationByStorage(storageLocation: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('storageLocation', storageLocation);
    return this.http.get<IStockValuation[]>(`${this.resourceUrl}/storage`, { params, observe: 'response' });
  }

  /**
   * Get aggregated stock valuation summary
   */
  getStockValuationSummary(): Observable<EntityResponseType> {
    return this.http.get<IStockValuationSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  /**
   * Export stock valuation report as PDF
   */
  exportStockValuationToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
