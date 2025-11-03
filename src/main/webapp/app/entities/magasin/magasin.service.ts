import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IMagasin } from 'app/shared/model/magasin.model';
import { createRequestOptions } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<IMagasin>;
type EntityArrayResponseType = HttpResponse<IMagasin[]>;

@Injectable({ providedIn: 'root' })
export class MagasinService {
  private http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/magasins';

  create(magasin: IMagasin): Observable<EntityResponseType> {
    return this.http.post<IMagasin>(this.resourceUrl, magasin, { observe: 'response' });
  }

  update(magasin: IMagasin): Observable<EntityResponseType> {
    return this.http.put<IMagasin>(this.resourceUrl, magasin, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IMagasin>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  fetchAll(): Observable<EntityArrayResponseType> {
    return this.http.get<IMagasin[]>(this.resourceUrl, { observe: 'response' });
  }
  fetchAllDepots(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IMagasin[]>(this.resourceUrl+'/depots', { params: options, observe: 'response' } );
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  async findCurrentUserMagasin(): Promise<IMagasin> {
    return await firstValueFrom(this.http.get<IMagasin>(this.resourceUrl + '/current-user-magasin'));
  }
}
