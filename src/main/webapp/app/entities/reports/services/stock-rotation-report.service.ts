import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IStockRotation, CategorieABC } from 'app/shared/model/report/stock-rotation.model';

type EntityArrayResponseType = HttpResponse<IStockRotation[]>;
type CountResponseType = HttpResponse<Record<CategorieABC, number>>;

@Injectable({ providedIn: 'root' })
export class StockRotationReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/stock/rotation';
  private readonly http = inject(HttpClient);

  /**
   * Get all stock rotation data
   */
  getAllStockRotation(): Observable<EntityArrayResponseType> {
    return this.http.get<IStockRotation[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get stock rotation filtered by product category (famille)
   */
  getStockRotationByCategory(categorie: string): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('categorie', categorie);
    return this.http.get<IStockRotation[]>(`${this.resourceUrl}/category`, { params, observe: 'response' });
  }

  /**
   * Get stock rotation filtered by ABC classification
   */
  getStockRotationByABCClassification(categorieABC: CategorieABC): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('categorieABC', categorieABC);
    return this.http.get<IStockRotation[]>(`${this.resourceUrl}/abc`, { params, observe: 'response' });
  }

  /**
   * Get count of products by ABC classification
   */
  getStockRotationCountByABCClassification(): Observable<CountResponseType> {
    return this.http.get<Record<CategorieABC, number>>(`${this.resourceUrl}/count`, { observe: 'response' });
  }

  /**
   * Get slow moving products (Category C)
   */
  getSlowMovingProducts(): Observable<EntityArrayResponseType> {
    return this.http.get<IStockRotation[]>(`${this.resourceUrl}/slow`, { observe: 'response' });
  }

  /**
   * Export stock rotation report as PDF
   */
  exportStockRotationToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
