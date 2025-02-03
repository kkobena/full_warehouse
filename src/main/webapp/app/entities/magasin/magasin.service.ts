import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IMagasin } from 'app/shared/model/magasin.model';

type EntityResponseType = HttpResponse<IMagasin>;
type EntityArrayResponseType = HttpResponse<IMagasin[]>;

@Injectable({ providedIn: 'root' })
export class MagasinService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/magasins';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  create(magasin: IMagasin): Observable<EntityResponseType> {
    return this.http.post<IMagasin>(this.resourceUrl, magasin, { observe: 'response' });
  }

  update(magasin: IMagasin): Observable<EntityResponseType> {
    return this.http.put<IMagasin>(this.resourceUrl, magasin, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IMagasin>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IMagasin[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  async findPromise(): Promise<IMagasin> {
    return await firstValueFrom(this.http.get<IMagasin>(this.resourceUrl));
  }
}
