import { Injectable, inject } from '@angular/core';
import { SERVER_API_URL } from '../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { createRequestOption } from '../../shared/util/request-util';
import { Storage } from './storage.model';

type EntityResponseType = HttpResponse<Storage>;
type EntityArrayResponseType = HttpResponse<Storage[]>;

@Injectable({
  providedIn: 'root',
})
export class StorageService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/storages';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<Storage>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<Storage[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
}
