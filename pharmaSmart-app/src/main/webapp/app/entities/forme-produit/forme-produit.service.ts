import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { IFormProduit } from '../../shared/model/form-produit.model';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<IFormProduit>;
type EntityArrayResponseType = HttpResponse<IFormProduit[]>;

@Injectable({
  providedIn: 'root',
})
export class FormeProduitService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/form-produits';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  create(formProduit: IFormProduit): Observable<EntityResponseType> {
    return this.http.post<IFormProduit>(this.resourceUrl, formProduit, { observe: 'response' });
  }

  update(formProduit: IFormProduit): Observable<EntityResponseType> {
    return this.http.put<IFormProduit>(this.resourceUrl, formProduit, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IFormProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFormProduit[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
