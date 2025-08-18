import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';

import { PrixReference } from './model/prix-reference.model';
import { Observable } from 'rxjs';

type EntityResponseType = HttpResponse<PrixReference>;
type EntityArrayResponseType = HttpResponse<PrixReference[]>;

@Injectable({
  providedIn: 'root'
})
export class PrixReferenceService {
  private http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/prix-reference';

  create(entity: PrixReference): Observable<EntityResponseType> {
    return this.http.post<PrixReference>(this.resourceUrl, entity, { observe: 'response' });
  }

  update(entity: PrixReference): Observable<EntityResponseType> {
    return this.http.put<PrixReference>(this.resourceUrl, entity, { observe: 'response' });
  }

  query(produitId: number): Observable<EntityArrayResponseType> {
    return this.http.get<PrixReference[]>(`${this.resourceUrl}/${produitId}`, { observe: 'response' });
  }

  queryByTiersPayantId(tiersPayantId: number): Observable<EntityArrayResponseType> {
    return this.http.get<PrixReference[]>(`${this.resourceUrl}/tiers-payant/${tiersPayantId}`, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
