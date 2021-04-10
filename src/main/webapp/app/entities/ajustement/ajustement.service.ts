import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IAjustement } from 'app/shared/model/ajustement.model';

type EntityResponseType = HttpResponse<IAjustement>;
type EntityArrayResponseType = HttpResponse<IAjustement[]>;

@Injectable({ providedIn: 'root' })
export class AjustementService {
  public resourceUrl = SERVER_API_URL + 'api/ajustements';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IAjustement>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IAjustement[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }
  queryAll(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IAjustement[]>(this.resourceUrl + '/all', { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  protected convertDateFromClient(ajustement: IAjustement): IAjustement {
    const copy: IAjustement = Object.assign({}, ajustement, {
      dateMtv: ajustement.dateMtv && ajustement.dateMtv.isValid() ? ajustement.dateMtv.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.dateMtv = res.body.dateMtv ? moment(res.body.dateMtv) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((ajustement: IAjustement) => {
        ajustement.dateMtv = ajustement.dateMtv ? moment(ajustement.dateMtv) : undefined;
      });
    }
    return res;
  }
  create(ajustement: any): Observable<EntityResponseType> {
    return this.http
      .post<IAjustement>(this.resourceUrl, ajustement, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(ajustement: IAjustement): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(ajustement);
    return this.http
      .put<IAjustement>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }
  save(id: number): Observable<HttpResponse<{}>> {
    return this.http.post<IAjustement>(this.resourceUrl + '/save', { ajustId: id, produitId: 0 }, { observe: 'response' });
  }
}
