import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../../shared/util/request-util';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../../app.constants';
import { Suggestion } from './model/suggestion.model';
import { Keys } from '../../../shared/model/keys.model';
import { SuggestionLine } from './model/suggestion-line.model';
import { FournisseurSuggestionSummary } from '../../../features/commande/feature/suggestion/data-access/suggestion-enrichie.model';
import { IFournisseurProduit } from '../../../shared/model/fournisseur-produit.model';

type EntityArrayResponseType = HttpResponse<Suggestion[]>;

@Injectable({
  providedIn: 'root',
})
export class SuggestionService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/suggestions';

  queryParFournisseur(): Observable<FournisseurSuggestionSummary[]> {
    return this.http.get<FournisseurSuggestionSummary[]>(this.resourceUrl + '/par-fournisseur');
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<Suggestion[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  queryItems(req?: any): Observable<HttpResponse<SuggestionLine[]>> {
    const options = createRequestOptions(req);
    return this.http.get<SuggestionLine[]>(this.resourceUrl + '/items', {
      params: options,
      observe: 'response',
    });
  }



  find(id: number): Observable<HttpResponse<Suggestion>> {
    return this.http.get<Suggestion>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  delete(ids: Keys): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/delete', ids, { observe: 'response' });
  }

  deleteItem(ids: Keys): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/delete/items', ids, { observe: 'response' });
  }

  fusionner(ids: Keys): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/fusionner', ids, { observe: 'response' });
  }

  sanitize(id: number): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/sanitize/${id}`);
  }

  exportToCsv(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${id}`, { responseType: 'blob' });
  }

  createOrUpdateItem(item: SuggestionLine, id: number): Observable<HttpResponse<{}>> {
    return this.http.post(`${this.resourceUrl}/add-item/${id}`, item, { observe: 'response' });
  }

  updateQuantity(item: SuggestionLine): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/update-quantity', item, { observe: 'response' });
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${id}`, { responseType: 'blob' });
  }

  commander(id: number): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/commander/${id}`);
  }

  commanderSelection(dto: { suggestionId: number; lignes: { suggestionLineId: number; quantite: number }[] }): Observable<{}> {
    return this.http.post(`${this.resourceUrl}/commander-selection`, dto);
  }

  getBudget(): Observable<BudgetCommande> {
    return this.http.get<BudgetCommande>(`${this.resourceUrl}/budget`);
  }

  getSemoisFraicheur(): Observable<SemoisFraicheur> {
    return this.http.get<SemoisFraicheur>(`${SERVER_API_URL}api/semois/freshness`);
  }

  recalculerSemois(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${SERVER_API_URL}api/semois/recalculate`, {});
  }

  valider(id: number): Observable<{}> {
    return this.http.put(`${this.resourceUrl}/${id}/valider`, {});
  }

  rejeter(id: number): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/${id}/rejeter`);
  }

  getFournisseursProduit(produitId: number): Observable<IFournisseurProduit[]> {
    return this.http.get<IFournisseurProduit[]>(`${SERVER_API_URL}api/fournisseur-produits/by-produit/${produitId}`);
  }
}

export interface SemoisFraicheur {
  dernierCalcul: string | null; // ISO datetime
  calculeRecent: boolean;
  nbProduitsConfigures: number;
}

export interface BudgetCommande {
  budgetMensuel: number;
  montantEstime: number;
  montantCommande: number;
  budgetRestant: number;
  enDepassement: boolean;
  budgetIllimite: boolean;
}
