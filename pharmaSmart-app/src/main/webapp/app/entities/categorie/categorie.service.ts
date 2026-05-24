import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { ICategorie } from 'app/shared/model/categorie.model';

type EntityResponseType = HttpResponse<ICategorie>;
type EntityArrayResponseType = HttpResponse<ICategorie[]>;

@Injectable({ providedIn: 'root' })
export class CategorieService {
  public resourceUrl = SERVER_API_URL + 'api/categories';
  protected http = inject(HttpClient);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  create(categorie: ICategorie): Observable<EntityResponseType> {
    return this.http.post<ICategorie>(this.resourceUrl, categorie, { observe: 'response' });
  }

  update(categorie: ICategorie): Observable<EntityResponseType> {
    return this.http.put<ICategorie>(this.resourceUrl, categorie, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ICategorie>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ICategorie[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
