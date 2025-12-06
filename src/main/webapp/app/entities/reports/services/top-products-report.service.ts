import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { ITopProduct } from 'app/shared/model/report/top-product.model';

type EntityArrayResponseType = HttpResponse<ITopProduct[]>;

@Injectable({ providedIn: 'root' })
export class TopProductsReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/top-products';
  private readonly http = inject(HttpClient);

  /**
   * Get top N products by revenue for a specific month
   */
  getTopProductsByRevenue(month: string, limit: number = 20): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('month', month).set('limit', limit.toString());
    return this.http.get<ITopProduct[]>(`${this.resourceUrl}/by-revenue`, { params, observe: 'response' });
  }

  /**
   * Get top N products by quantity sold for a specific month
   */
  getTopProductsByQuantity(month: string, limit: number = 20): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('month', month).set('limit', limit.toString());
    return this.http.get<ITopProduct[]>(`${this.resourceUrl}/by-quantity`, { params, observe: 'response' });
  }

  /**
   * Get all products stats for a specific month
   */
  getAllProductsForMonth(month: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('month', month);
    return this.http.get<ITopProduct[]>(`${this.resourceUrl}/all`, { params, observe: 'response' });
  }

  /**
   * Get monthly evolution for a specific product
   */
  getProductMonthlyEvolution(produitId: number, nbMonths: number = 6): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('nbMonths', nbMonths.toString());
    return this.http.get<ITopProduct[]>(`${this.resourceUrl}/evolution/${produitId}`, { params, observe: 'response' });
  }
}
