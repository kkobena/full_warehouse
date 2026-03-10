import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

import {SERVER_API_URL} from 'app/app.constants';
import {IStockValuation, IStockValuationSummary} from 'app/shared/model/report/stock-valuation.model';
import {createRequestOptions} from "../../../shared/util/request-util";

type EntityArrayResponseType = HttpResponse<IStockValuation[]>;
type EntityResponseType = HttpResponse<IStockValuationSummary>;

@Injectable({providedIn: 'root'})
export class StockValuationReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/stock/valuation';
  private readonly http = inject(HttpClient);

  /**
   * Get all stock valuation data
   */
  getAllStockValuation(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IStockValuation[]>(this.resourceUrl, {params: options, observe: 'response'});
  }

  /**
   * Get aggregated stock valuation summary
   */
  getStockValuationSummary(req?: any): Observable<EntityResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IStockValuationSummary>(`${this.resourceUrl}/summary`, {params: options, observe: 'response'});
  }

  /**
   * Export stock valuation report as PDF
   */
  exportStockValuationToPdf(req?: any): Observable<HttpResponse<Blob>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/export`, {params: options, observe: 'response', responseType: 'blob'});
  }
}
