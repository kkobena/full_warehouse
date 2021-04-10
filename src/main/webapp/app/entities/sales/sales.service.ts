import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ISales } from 'app/shared/model/sales.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class SalesService {
  public resourceUrl = SERVER_API_URL + 'api/sales';

  constructor(protected http: HttpClient) {}

  create(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(sales: ISales): ISales {
    const copy: ISales = Object.assign({}, sales, {
      createdAt: sales.createdAt && sales.createdAt.isValid() ? sales.createdAt.toJSON() : undefined,
      updatedAt: sales.updatedAt && sales.updatedAt.isValid() ? sales.updatedAt.toJSON() : undefined,
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
      res.body.forEach((sales: ISales) => {
        sales.createdAt = sales.createdAt ? moment(sales.createdAt) : undefined;
        sales.updatedAt = sales.updatedAt ? moment(sales.updatedAt) : undefined;
      });
    }
    return res;
  }
  save(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(this.resourceUrl + '/save', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  print(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/${id}`, { responseType: 'blob' });
  }
}
