import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ISalesLine } from 'app/shared/model/sales-line.model';

type EntityResponseType = HttpResponse<ISalesLine>;
type EntityArrayResponseType = HttpResponse<ISalesLine[]>;

@Injectable({ providedIn: 'root' })
export class SalesLineService {
  public resourceUrl = SERVER_API_URL + 'api/sales-lines';

  constructor(protected http: HttpClient) {}

  create(salesLine: ISalesLine): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(salesLine: ISalesLine): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(salesLine);
    return this.http
      .put<ISalesLine>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISalesLine>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ISalesLine[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(salesLine: ISalesLine): ISalesLine {
    const copy: ISalesLine = Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined,
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
      res.body.forEach((salesLine: ISalesLine) => {
        salesLine.createdAt = salesLine.createdAt ? moment(salesLine.createdAt) : undefined;
        salesLine.updatedAt = salesLine.updatedAt ? moment(salesLine.updatedAt) : undefined;
      });
    }
    return res;
  }

  queryBySale(id?: number): Observable<EntityArrayResponseType> {
    return this.http
      .get<ISalesLine[]>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }
}
