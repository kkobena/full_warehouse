import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { IConfiguration } from './model/configuration.model';
import { SERVER_API_URL } from '../app.constants';
import { Observable } from 'rxjs';

import { createRequestOption } from './util/request-util';

type EntityResponseType = HttpResponse<IConfiguration>;
type EntityArrayResponseType = HttpResponse<IConfiguration[]>;

@Injectable({
  providedIn: 'root',
})
export class ConfigurationService {
  public resourceUrl = SERVER_API_URL + 'api/app';

  constructor(protected http: HttpClient) {}

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IConfiguration[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  findStockConfig(): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/param-gestion-stock`, { observe: 'response' });
  }
}
