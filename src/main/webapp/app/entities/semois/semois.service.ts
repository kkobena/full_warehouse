import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  ISemoisSuggestion,
  ISemoisConfiguration,
  ISemoisClasseConfig,
  IInitConfigurationRequest,
  IImportHistoricalRequest,
  IAggregationStatus,
  IMessageResponse,
  IInitAllResponse,
  IReapproDashboard,
} from 'app/shared/model/semois';
import { ClasseCriticite } from 'app/shared/model/semois/classe-criticite.model';
import { createRequestOption } from 'app/core/request/request-util';
import { createRequestOptions } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<ISemoisSuggestion>;
type EntityArrayResponseType = HttpResponse<ISemoisSuggestion[]>;
type ConfigResponseType = HttpResponse<ISemoisConfiguration>;
type ConfigArrayResponseType = HttpResponse<ISemoisConfiguration[]>;

@Injectable({ providedIn: 'root' })
export class SemoisService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/semois');

  /**
   * Récupère toutes les suggestions SEMOIS avec filtres optionnels et pagination
   * @param req Paramètres de pagination (page, size, sort)
   * @param search Texte de recherche (libellé ou code CIP)
   * @param classeCriticite Filtre par classe de criticité
   * @returns Page de suggestions paginées
   */
  getSuggestions(req?: any, search?: string, classeCriticite?: ClasseCriticite): Observable<EntityArrayResponseType> {
    const options = createRequestOptions({
      ...req,
      search,
      classeCriticite,
    });
    return this.http.get<ISemoisSuggestion[]>(`${this.resourceUrl}/suggestions`, {
      params: options,
      observe: 'response',
    });
  }

  /**
   * Récupère la suggestion SEMOIS pour un produit spécifique
   * @param produitId ID du produit
   * @returns Suggestion SEMOIS
   */
  getSuggestionForProduct(produitId: number): Observable<EntityResponseType> {
    return this.http.get<ISemoisSuggestion>(`${this.resourceUrl}/suggestions/${produitId}`, { observe: 'response' });
  }

  /**
   * Récupère la configuration SEMOIS d'un produit
   * @param produitId ID du produit
   * @returns Configuration SEMOIS
   */
  getConfiguration(produitId: number): Observable<ConfigResponseType> {
    return this.http.get<ISemoisConfiguration>(`${this.resourceUrl}/configuration/${produitId}`, { observe: 'response' });
  }

  /**
   * Initialise la configuration SEMOIS pour un produit
   * @param request Request avec produitId et classe optionnelle
   * @returns Configuration créée
   */
  initializeConfiguration(request: IInitConfigurationRequest): Observable<ConfigResponseType> {
    return this.http.post<ISemoisConfiguration>(`${this.resourceUrl}/configuration`, request, { observe: 'response' });
  }

  /**
   * Met à jour la configuration SEMOIS d'un produit
   * @param produitId ID du produit
   * @param config Configuration mise à jour
   * @returns Configuration mise à jour
   */
  updateConfiguration(produitId: number, config: ISemoisConfiguration): Observable<ConfigResponseType> {
    return this.http.put<ISemoisConfiguration>(`${this.resourceUrl}/configuration/${produitId}`, config, { observe: 'response' });
  }

  /**
   * Initialise toutes les configurations SEMOIS manquantes (admin only)
   * @returns Nombre de configurations créées
   */
  initializeAllConfigurations(): Observable<HttpResponse<IInitAllResponse>> {
    return this.http.post<IInitAllResponse>(`${this.resourceUrl}/init-all`, null, { observe: 'response' });
  }

  /**
   * Déclenche un recalcul manuel SEMOIS (admin only)
   * @returns Message de succès
   */
  triggerRecalculation(): Observable<HttpResponse<IMessageResponse>> {
    return this.http.post<IMessageResponse>(`${this.resourceUrl}/recalculate`, null, { observe: 'response' });
  }

  /**
   * Importe les données historiques sur N mois (admin only)
   * @param request Request avec nombre de mois
   * @returns Message de succès
   */
  importHistoricalData(request: IImportHistoricalRequest): Observable<HttpResponse<IMessageResponse>> {
    return this.http.post<IMessageResponse>(`${this.resourceUrl}/import-historical`, request, { observe: 'response' });
  }

  /**
   * Récupère le statut de l'agrégation mensuelle
   * @returns Statut de l'agrégation
   */
  getAggregationStatus(): Observable<HttpResponse<IAggregationStatus>> {
    return this.http.get<IAggregationStatus>(`${this.resourceUrl}/aggregation/status`, { observe: 'response' });
  }

  /**
   * Dégèle un mois pour corrections exceptionnelles (admin only)
   */
  unfreezeMonth(anneeMois: string, reason: string): Observable<HttpResponse<IMessageResponse>> {
    return this.http.post<IMessageResponse>(`${this.resourceUrl}/aggregation/unfreeze`, { anneeMois, reason }, { observe: 'response' });
  }

  /**
   * Récupère les 5 configurations de classe de criticité SEMOIS
   */
  getClasseConfigs(): Observable<HttpResponse<ISemoisClasseConfig[]>> {
    return this.http.get<ISemoisClasseConfig[]>(`${this.resourceUrl}/classe-configs`, { observe: 'response' });
  }

  /**
   * Met à jour la configuration d'une classe de criticité
   */
  updateClasseConfig(classeCriticite: string, config: ISemoisClasseConfig): Observable<HttpResponse<ISemoisClasseConfig>> {
    return this.http.put<ISemoisClasseConfig>(`${this.resourceUrl}/classe-configs/${classeCriticite}`, config, { observe: 'response' });
  }

  /**
   * Récupère le tableau de bord réapprovisionnement SEMOIS temps réel (Axe 6).
   * Consolide les indicateurs : compteurs par urgence, répartition par classe, top produits urgents.
   */
  getDashboard(): Observable<HttpResponse<IReapproDashboard>> {
    return this.http.get<IReapproDashboard>(`${this.resourceUrl}/dashboard`, { observe: 'response' });
  }
}
