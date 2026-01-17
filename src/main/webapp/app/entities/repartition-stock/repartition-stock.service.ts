import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from '../../core/config/application-config.service';
import { IRepartitionSearchQuery, IRepartitionStockProduit, ISuggestionReassort } from './repartition-stock.model';
import { createRequestOptions } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<IRepartitionStockProduit>;
type EntityArrayResponseType = HttpResponse<IRepartitionStockProduit[]>;
type SuggestionResponseType = HttpResponse<ISuggestionReassort>;

@Injectable({ providedIn: 'root' })
export class RepartitionStockService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/repartition-stock');
  private readonly suggestionUrl = this.applicationConfigService.getEndpointFor('api/suggestion-reassort');

  /**
   * Fetch repartition stock history with filters
   */
  query(req?: IRepartitionSearchQuery): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IRepartitionStockProduit[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  /**
   * Get current open suggestions by type
   */
  getOpenSuggestions(typeReassort?: string): Observable<HttpResponse<ISuggestionReassort[]>> {
    const params = typeReassort ? new HttpParams().set('typeReassort', typeReassort) : new HttpParams();
    return this.http.get<ISuggestionReassort[]>(`${this.suggestionUrl}/open`, { params, observe: 'response' });
  }

  /**
   * Update ligne reassort quantity
   */
  updateLigneQuantity(ligneId: number, quantity: number): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.suggestionUrl}/ligne/${ligneId}`, { quantity }, { observe: 'response' });
  }

  /**
   * Delete ligne reassort
   */
  deleteLigne(ligneId: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.suggestionUrl}/ligne/${ligneId}`, { observe: 'response' });
  }

  /**
   * Validate suggestion reassort (execute stock movement)
   */
  validateSuggestion(suggestionId: number): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.suggestionUrl}/${suggestionId}/validate`, {}, { observe: 'response' });
  }

  /**
   * Delete entire suggestion reassort
   */
  deleteSuggestion(suggestionId: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.suggestionUrl}/${suggestionId}`, { observe: 'response' });
  }

  /**
   * Process manual stock repartition (single or multiple)
   */
  processManualRepartition(
    requests:
      | { stockSourceId: number; stockDestinationId: number | null; quantity: number; seuilMini?: number }
      | { stockSourceId: number; stockDestinationId: number | null; quantity: number; seuilMini?: number }[],
  ): Observable<HttpResponse<void>> {
    const requestsArray = Array.isArray(requests) ? requests : [requests];
    return this.http.post<void>(`${this.resourceUrl}/manual`, requestsArray, { observe: 'response' });
  }

  /**
   * Search stock produits for repartition
   */
  searchStockProduitsForRepartition(storageId: number, searchTerm: string): Observable<HttpResponse<any[]>> {
    const params = new HttpParams().set('storageId', storageId.toString()).set('searchTerm', searchTerm);
    return this.http.get<any[]>(this.applicationConfigService.getEndpointFor('api/stock-produit/search-for-repartition'), {
      params,
      observe: 'response',
    });
  }

  /**
   * Export repartition stock history to PDF
   */
  exportToPdf(req: IRepartitionSearchQuery): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/export/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }
}
