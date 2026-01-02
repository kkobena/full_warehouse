import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IProductAssociation, IMarketBasketSummary } from 'app/shared/model/report/market-basket.model';

type EntityArrayResponseType = HttpResponse<IProductAssociation[]>;
type EntityResponseType = HttpResponse<IMarketBasketSummary>;

/**
 * Service for Market Basket Analysis
 */
@Injectable({ providedIn: 'root' })
export class MarketBasketService {
  private readonly resourceUrl = SERVER_API_URL + 'api/market-basket';
  private readonly http = inject(HttpClient);

  /**
   * Get product associations
   */
  getProductAssociations(
    startDate: string,
    endDate: string,
    minSupport = 1.0,
    minConfidence = 10.0,
    limit = 50,
  ): Observable<EntityArrayResponseType> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('minSupport', minSupport.toString())
      .set('minConfidence', minConfidence.toString())
      .set('limit', limit.toString());

    return this.http.get<IProductAssociation[]>(`${this.resourceUrl}/associations`, {
      params,
      observe: 'response',
    });
  }

  /**
   * Get associations for a specific product
   */
  getAssociationsForProduct(productId: number, startDate: string, endDate: string, limit = 20): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate).set('limit', limit.toString());

    return this.http.get<IProductAssociation[]>(`${this.resourceUrl}/associations/${productId}`, {
      params,
      observe: 'response',
    });
  }

  /**
   * Get market basket summary
   */
  getSummary(startDate: string, endDate: string): Observable<EntityResponseType> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);

    return this.http.get<IMarketBasketSummary>(`${this.resourceUrl}/summary`, {
      params,
      observe: 'response',
    });
  }

  /**
   * Get cross-sell recommendations for a product
   */
  getRecommendations(productId: number): Observable<EntityArrayResponseType> {
    return this.http.get<IProductAssociation[]>(`${this.resourceUrl}/recommendations/${productId}`, {
      observe: 'response',
    });
  }
}
