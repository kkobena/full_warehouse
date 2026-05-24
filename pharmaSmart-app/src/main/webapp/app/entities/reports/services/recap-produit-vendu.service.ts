import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import {
  IRecapProduitVendu,
  IRecapProduitVenduRequestParam,
  IRecapProduitVenduSummary,
} from 'app/shared/model/report/recap-produit-vendu.model';

import { createRequestOptions } from '../../../shared/util/request-util';

type EntityArrayResponseType = HttpResponse<IRecapProduitVendu[]>;
type SummaryResponseType = HttpResponse<IRecapProduitVenduSummary>;

@Injectable({ providedIn: 'root' })
export class RecapProduitVenduService {
  private readonly resourceUrl = SERVER_API_URL + 'api/recap-produit-vendu';
  private readonly http = inject(HttpClient);

  /**
   * Get paginated recap produit vendu report
   */
  getRecapProduitVenduReport(requestParam: IRecapProduitVenduRequestParam): Observable<EntityArrayResponseType> {
    const params = createRequestOptions(requestParam);
    // Add filter parameters

    return this.http.get<IRecapProduitVendu[]>(this.resourceUrl, { params, observe: 'response' });
  }

  /**
   * Get paginated recap produit invendu report
   */
  getRecapProduitInvenduReport(requestParam: IRecapProduitVenduRequestParam): Observable<EntityArrayResponseType> {
    const params = createRequestOptions(requestParam);
    return this.http.get<IRecapProduitVendu[]>(`${this.resourceUrl}/invendus`, { params, observe: 'response' });
  }

  /**
   * Get summary for unsold products
   */
  getRecapProduitInvenduSummary(requestParam: IRecapProduitVenduRequestParam): Observable<SummaryResponseType> {
    const params = createRequestOptions(requestParam);
    return this.http.get<IRecapProduitVenduSummary>(`${this.resourceUrl}/invendus/summary`, { params, observe: 'response' });
  }

  /**
   * Export to PDF
   */
  exportToPdf(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);
    return this.http.get(`${this.resourceUrl}/pdf`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Export to Excel
   */
  exportToExcel(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);
    return this.http.get(`${this.resourceUrl}/excel`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Export to CSV
   */
  exportToCsv(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);

    return this.http.get(`${this.resourceUrl}/csv`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Export unsold products to PDF
   */
  exportInvenduToPdf(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);
    return this.http.get(`${this.resourceUrl}/invendus/pdf`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Export unsold products to Excel
   */
  exportInvenduToExcel(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);
    return this.http.get(`${this.resourceUrl}/invendus/excel`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Export unsold products to CSV
   */
  exportInvenduToCsv(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<Blob>> {
    const params = createRequestOptions(requestParam);
    return this.http.get(`${this.resourceUrl}/invendus/csv`, { params, responseType: 'blob', observe: 'response' });
  }

  /**
   * Create suggestion from recap
   */
  createSuggestionFromRecap(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<number>> {
    const params = createRequestOptions(requestParam);
    return this.http.post<number>(`${this.resourceUrl}/create-suggestion`, null, { params, observe: 'response' });
  }

  /**
   * Create inventory from recap
   */
  createInventoryFromRecap(requestParam: IRecapProduitVenduRequestParam): Observable<HttpResponse<number>> {
    const params = createRequestOptions(requestParam);
    return this.http.post<number>(`${this.resourceUrl}/create-inventory`, null, { params, observe: 'response' });
  }
  getRecapProduitVenduSummary(requestParam: IRecapProduitVenduRequestParam): Observable<SummaryResponseType> {
    const params = createRequestOptions(requestParam);
    return this.http.get<IRecapProduitVenduSummary>(`${this.resourceUrl}/summary`, { params, observe: 'response' });
  }
}
