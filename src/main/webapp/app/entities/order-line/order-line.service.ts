import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { SERVER_API_URL } from 'app/app.constants';
import { IOrderLine } from 'app/shared/model/order-line.model';

type EntityResponseType = HttpResponse<IOrderLine>;
type EntityArrayResponseType = HttpResponse<IOrderLine[]>;

@Injectable({ providedIn: 'root' })
export class OrderLineService {
  public resourceUrl = SERVER_API_URL + 'api/order-lines';

  constructor(protected http: HttpClient) {}

  create(orderLine: IOrderLine): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(orderLine);
    return this.http
      .post<IOrderLine>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(orderLine: IOrderLine): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(orderLine);
    return this.http
      .put<IOrderLine>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IOrderLine>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(commandeId?: number): Observable<EntityArrayResponseType> {
    return this.http
      .get<IOrderLine[]>(`${this.resourceUrl}/${commandeId}`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(orderLine: IOrderLine): IOrderLine {
    const copy: IOrderLine = Object.assign({}, orderLine, {
      receiptDate: orderLine.receiptDate && orderLine.receiptDate.isValid() ? orderLine.receiptDate.format(DATE_FORMAT) : undefined,
      createdAt: orderLine.createdAt && orderLine.createdAt.isValid() ? orderLine.createdAt.toJSON() : undefined,
      updatedAt: orderLine.updatedAt && orderLine.updatedAt.isValid() ? orderLine.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.receiptDate = res.body.receiptDate ? moment(res.body.receiptDate) : undefined;
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((orderLine: IOrderLine) => {
        orderLine.receiptDate = orderLine.receiptDate ? moment(orderLine.receiptDate) : undefined;
        orderLine.createdAt = orderLine.createdAt ? moment(orderLine.createdAt) : undefined;
        orderLine.updatedAt = orderLine.updatedAt ? moment(orderLine.updatedAt) : undefined;
      });
    }
    return res;
  }
}
