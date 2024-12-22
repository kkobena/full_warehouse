import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Reglement } from './model/reglement.model';

type EntityResponseType = HttpResponse<Reglement>;
type EntityArrayResponseType = HttpResponse<Reglement[]>;

@Injectable({
  providedIn: 'root',
})
export class ReglementService {
  public resourceUrl = SERVER_API_URL + 'api/reglements';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<Reglement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
