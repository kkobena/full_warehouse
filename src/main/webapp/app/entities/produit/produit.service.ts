import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IProduit } from 'app/shared/model/produit.model';

type EntityResponseType = HttpResponse<IProduit>;
type EntityArrayResponseType = HttpResponse<IProduit[]>;

@Injectable({ providedIn: 'root' })
export class ProduitService {
  public resourceUrl = SERVER_API_URL + 'api/produits';

  constructor(protected http: HttpClient) {}

  create(produit: IProduit): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(produit);
    return this.http
      .post<IProduit>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(produit: IProduit): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(produit);
    return this.http
      .put<IProduit>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IProduit[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(produit: IProduit): IProduit {
    const copy: IProduit = Object.assign({}, produit, {
      createdAt: produit.createdAt && produit.createdAt.isValid() ? produit.createdAt.toJSON() : undefined,
      updatedAt: produit.updatedAt && produit.updatedAt.isValid() ? produit.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((produit: IProduit) => {
        produit.createdAt = produit.createdAt ? moment(produit.createdAt) : undefined;
        produit.updatedAt = produit.updatedAt ? moment(produit.updatedAt) : undefined;
      });
    }
    return res;
  }
  uploadFile(file: any, id: number): Observable<HttpResponse<IProduit>> {
    return this.http.post<IProduit>(`${this.resourceUrl}/${id}/img`, file, { observe: 'response' });
  }
}
