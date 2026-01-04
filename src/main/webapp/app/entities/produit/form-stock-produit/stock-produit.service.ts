import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from '../../../core/config/application-config.service';
import { IStockProduit } from '../../../shared/model/stock-produit.model';

type EntityResponseType = HttpResponse<IStockProduit>;

@Injectable({ providedIn: 'root' })
export class StockProduitService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/stock-produit');

  /**
   * Create a new stock produit (reserve)
   */
  create(stockProduit: IStockProduit): Observable<EntityResponseType> {
    return this.http.post<IStockProduit>(this.resourceUrl, stockProduit, { observe: 'response' });
  }

  /**
   * Update an existing stock produit
   */
  update(stockProduit: IStockProduit): Observable<EntityResponseType> {
    return this.http.put<IStockProduit>(`${this.resourceUrl}/${stockProduit.id}`, stockProduit, { observe: 'response' });
  }

  /**
   * Get a stock produit by ID
   */
  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IStockProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
