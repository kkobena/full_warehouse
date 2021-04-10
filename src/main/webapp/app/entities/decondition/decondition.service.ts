import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IDecondition } from 'app/shared/model/decondition.model';

type EntityResponseType = HttpResponse<IDecondition>;
type EntityArrayResponseType = HttpResponse<IDecondition[]>;

@Injectable({ providedIn: 'root' })
export class DeconditionService {
  public resourceUrl = SERVER_API_URL + 'api/deconditions';

  constructor(protected http: HttpClient) {}

  create(decondition: IDecondition): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(decondition);
    return this.http
      .post<IDecondition>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(decondition: IDecondition): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(decondition);
    return this.http
      .put<IDecondition>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IDecondition>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IDecondition[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(decondition: IDecondition): IDecondition {
    const copy: IDecondition = Object.assign({}, decondition, {
      dateMtv: decondition.dateMtv && decondition.dateMtv.isValid() ? decondition.dateMtv.toJSON() : undefined,
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
      res.body.forEach((decondition: IDecondition) => {
        decondition.dateMtv = decondition.dateMtv ? moment(decondition.dateMtv) : undefined;
      });
    }
    return res;
  }
}
