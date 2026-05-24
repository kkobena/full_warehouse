import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IHistoriqueCertificationFne, IHistoriquePlanification, IPlanification, IPlanificationFne } from '../models';

@Injectable({ providedIn: 'root' })
export class PlanificationApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/planifications-facturation';
  private readonly http = inject(HttpClient);

  getAll(): Observable<HttpResponse<IPlanification[]>> {
    return this.http.get<IPlanification[]>(this.resourceUrl, { observe: 'response' });
  }

  create(dto: IPlanification): Observable<HttpResponse<IPlanification>> {
    return this.http.post<IPlanification>(this.resourceUrl, dto, { observe: 'response' });
  }

  update(id: number, dto: IPlanification): Observable<HttpResponse<IPlanification>> {
    return this.http.put<IPlanification>(`${this.resourceUrl}/${id}`, dto, { observe: 'response' });
  }

  toggleActif(id: number): Observable<HttpResponse<{}>> {
    return this.http.patch(`${this.resourceUrl}/${id}/toggle-actif`, {}, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  executerMaintenant(id: number): Observable<HttpResponse<any>> {
    return this.http.post(`${this.resourceUrl}/${id}/executer-maintenant`, {}, { observe: 'response' });
  }

  getHistorique(id: number, pageable: any): Observable<HttpResponse<IHistoriquePlanification[]>> {
    return this.http.get<IHistoriquePlanification[]>(`${this.resourceUrl}/${id}/historique`, {
      params: createRequestOptions(pageable),
      observe: 'response',
    });
  }

  // ── FNE planification ────────────────────────────────────────────────
  private readonly fneUrl = SERVER_API_URL + 'api/planification-certification-fne';

  getFnePlanification(): Observable<HttpResponse<IPlanificationFne>> {
    return this.http.get<IPlanificationFne>(`${this.fneUrl}/first`, { observe: 'response' });
  }

  toggleActifFne(id: number): Observable<HttpResponse<{}>> {
    return this.http.patch(`${this.fneUrl}/${id}/toggle-actif`, {}, { observe: 'response' });
  }

  executerMaintenantFne(id: number): Observable<HttpResponse<any>> {
    return this.http.post(`${this.fneUrl}/${id}/executer-maintenant`, {}, { observe: 'response' });
  }

  getHistoriqueFne(id: number, pageable: any): Observable<HttpResponse<IHistoriqueCertificationFne[]>> {
    return this.http.get<IHistoriqueCertificationFne[]>(`${this.fneUrl}/${id}/historique`, {
      params: createRequestOptions(pageable),
      observe: 'response',
    });
  }
}
