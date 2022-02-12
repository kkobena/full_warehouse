import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ISales } from 'app/shared/model/sales.model';
import { ISalesLine } from '../../shared/model/sales-line.model';
import { IResponseDto } from '../../shared/util/response-dto';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class SalesService {
  public resourceUrl = SERVER_API_URL + 'api/sales';

  constructor(protected http: HttpClient) {}

  createComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(`${this.resourceUrl}/comptant`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  closeComptant(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .put<ISales>(`${this.resourceUrl}/comptant/close`, copy, { observe: 'response' })
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

  saveComptant(sales: ISales): Observable<HttpResponse<IResponseDto>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<IResponseDto>(this.resourceUrl + 'comptant/save', copy, { observe: 'response' });
  }

  print(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/${id}`, { responseType: 'blob' });
  }

  addItemComptant(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(`${this.resourceUrl}/add-item/comptant`, copy, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  create(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateItemPrice(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtyRequested(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtySold(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  addItemAssurance(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(`${this.resourceUrl}/add-item/assurance`, copy, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemAssurance(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/assurance`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  deleteItem(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/${id}`, { observe: 'response' });
  }
  putCurrentCashSaleOnHold(sales: ISales): Observable<HttpResponse<IResponseDto>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<IResponseDto>(this.resourceUrl + '/comptant/put-on-hold', copy, { observe: 'response' });
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

  protected convertItemDateFromClient(salesLine: ISalesLine): ISalesLine {
    const copy: ISalesLine = Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  protected convertItemDateFromServer(res: HttpResponse<ISalesLine>): HttpResponse<ISalesLine> {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  protected convertItemDateArrayFromServer(res: HttpResponse<ISalesLine[]>): HttpResponse<ISalesLine[]> {
    if (res.body) {
      res.body.forEach((salesLine: ISalesLine) => {
        salesLine.createdAt = salesLine.createdAt ? moment(salesLine.createdAt) : undefined;
        salesLine.updatedAt = salesLine.updatedAt ? moment(salesLine.updatedAt) : undefined;
      });
    }
    return res;
  }
}
