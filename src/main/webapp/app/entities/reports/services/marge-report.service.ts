import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IMargeDTO, IMargeSummary } from 'app/shared/model/report';
import { createRequestOptions } from 'app/shared/util/request-util';

@Injectable({ providedIn: 'root' })
export class MargeReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/marges-profitability';
  private readonly http = inject(HttpClient);

  /**
   * Liste paginée des marges avec filtres optionnels.
   */
  getMarges(req?: {
    page?: number;
    size?: number;
    sort?: string[];
    familleProduitId?: number;
    search?: string;
  }): Observable<HttpResponse<IMargeDTO[]>> {
    const params = createRequestOptions(req);
    return this.http.get<IMargeDTO[]>(this.resourceUrl, { params, observe: 'response' });
  }

  /**
   * Produits dont le taux de marge est inférieur au seuil.
   */
  getFaibleMarge(seuil = 10, req?: { page?: number; size?: number }): Observable<HttpResponse<IMargeDTO[]>> {
    const params = createRequestOptions({ ...req, seuil });
    return this.http.get<IMargeDTO[]>(`${this.resourceUrl}/faible-marge`, { params, observe: 'response' });
  }

  /**
   * Top N produits par marge brute décroissante.
   */
  getTopMarges(limit = 20, req?: { page?: number; size?: number }): Observable<HttpResponse<IMargeDTO[]>> {
    const params = createRequestOptions({ ...req, limit });
    return this.http.get<IMargeDTO[]>(`${this.resourceUrl}/top`, { params, observe: 'response' });
  }

  /**
   * Résumé global des marges avec seuils configurables.
   */
  getMargeSummary(req?: {
    familleProduitId?: number;
    seuilBas?: number;
    seuilHaut?: number;
  }): Observable<HttpResponse<IMargeSummary>> {
    const params = createRequestOptions(req);
    return this.http.get<IMargeSummary>(`${this.resourceUrl}/summary`, { params, observe: 'response' });
  }
}

