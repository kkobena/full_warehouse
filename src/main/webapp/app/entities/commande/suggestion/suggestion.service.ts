import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../../shared/util/request-util';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../../app.constants';
import { Suggestion } from './model/suggestion.model';
import { Keys } from '../../../shared/model/keys.model';
import { SuggestionLine } from './model/suggestion-line.model';
import { FournisseurSuggestionSummary } from '../../../features/commande/feature/suggestion/data-access/suggestion-enrichie.model';
import { IFournisseurProduit } from "../../../shared/model";
import { CommandeId } from '../../../shared/model/abstract-commande.model';

type EntityArrayResponseType = HttpResponse<Suggestion[]>;

@Injectable({
  providedIn: 'root',
})
export class SuggestionService {
  private readonly  http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/suggestions';

  queryParFournisseur(statut?: 'GENEREE' | 'VALIDEE'): Observable<FournisseurSuggestionSummary[]> {
    const params: Record<string, string> = {};
    if (statut) params['statut'] = statut;
    return this.http.get<FournisseurSuggestionSummary[]>(this.resourceUrl + '/par-fournisseur', { params });
  }

  countByStatut(statut: 'GENEREE' | 'VALIDEE'): Observable<number> {
    return this.http.get<number>(`${this.resourceUrl}/count-by-statut`, { params: { statut } });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<Suggestion[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  queryItems(req?: any): Observable<HttpResponse<SuggestionLine[]>> {
    const { suggestionId, ...rest } = req ?? {};
    const options = createRequestOptions(rest);
    return this.http.get<SuggestionLine[]>(`${this.resourceUrl}/${suggestionId}/lines`, {
      params: options,
      observe: 'response',
    });
  }

  /** Charge toutes les lignes sans pagination — pour le composant d'édition. */
  queryAllLines(id: number, search?: string, niveauUrgence?: string): Observable<SuggestionLine[]> {
    const params: Record<string, string> = {};
    if (search) params['search'] = search;
    if (niveauUrgence) params['niveauUrgence'] = niveauUrgence;
    return this.http.get<SuggestionLine[]>(`${this.resourceUrl}/${id}/all-lines`, { params });
  }



  find(id: number): Observable<HttpResponse<Suggestion>> {
    return this.http.get<Suggestion>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  delete(ids: Keys): Observable<HttpResponse<void>> {
    return this.http.post<void>(this.resourceUrl + '/delete', ids, { observe: 'response' });
  }

  deleteItem(ids: Keys): Observable<HttpResponse<void>> {
    return this.http.post<void>(this.resourceUrl + '/delete/lines', ids, { observe: 'response' });
  }

  fusionner(ids: Keys): Observable<HttpResponse<void>> {
    return this.http.post<void>(this.resourceUrl + '/fusionner', ids, { observe: 'response' });
  }

  /** Nettoie (sanitize) une suggestion — supprime les lignes inutiles. */
  sanitize(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/sanitize/${id}`);
  }

  exportToCsv(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/export-csv`, { responseType: 'blob' });
  }

  createOrUpdateItem(item: SuggestionLine, id: number): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/add-item/${id}`, item, { observe: 'response' });
  }

  updateQuantity(item: SuggestionLine): Observable<HttpResponse<void>> {
    return this.http.put<void>(this.resourceUrl + '/lines/quantity', item, { observe: 'response' });
  }

  /** Réinitialise le flag quantiteModifieeManuel — le batch peut à nouveau calculer la qté. */
  resetQuantiteManuelle(id: number): Observable<void> {
    return this.http.put<void>(`${this.resourceUrl}/lines/${id}/reset-quantite`, {});
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/export-pdf`, { responseType: 'blob' });
  }

  /** Commande toute la suggestion (toutes les lignes). Retourne le CommandeId de la commande créée.
   * @param fournisseurId Fournisseur cible (optionnel, null = fournisseur de la suggestion).
   */
  commander(id: number, fournisseurId?: number): Observable<CommandeId> {
    const params: Record<string, string> = {};
    if (fournisseurId != null) params['fournisseurId'] = fournisseurId.toString();
    return this.http.post<CommandeId>(`${this.resourceUrl}/${id}/commander`, {}, { params });
  }

  /** Commande une sélection de lignes. Retourne le CommandeId de la commande créée. */
  commanderSelection(dto: { suggestionId: number; lignes: { suggestionLineId: number; quantite: number }[]; fournisseurId?: number }): Observable<CommandeId> {
    return this.http.post<CommandeId>(`${this.resourceUrl}/commander-selection`, dto);
  }

  getBudget(): Observable<BudgetCommande> {
    return this.http.get<BudgetCommande>(`${this.resourceUrl}/budget-commande`);
  }

  getSemoisFraicheur(): Observable<SemoisFraicheur> {
    return this.http.get<SemoisFraicheur>(`${SERVER_API_URL}api/semois/freshness`);
  }

  recalculerSemois(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${SERVER_API_URL}api/semois/recalculate`, {});
  }

  valider(id: number): Observable<void> {
    return this.http.put<void>(`${this.resourceUrl}/${id}/valider`, {});
  }

  /** Rejette (supprime) une suggestion. */
  rejeter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
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
