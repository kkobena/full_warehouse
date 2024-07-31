import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { FinalyseSale, ISales, KeyValue } from 'app/shared/model/sales.model';
import { ISalesLine } from '../../../shared/model/sales-line.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class VoSalesService {
  public resourceUrl = SERVER_API_URL + 'api/sales';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  findForEdit(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/edit/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  putCurrentOnStandBy(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/put-on-hold', copy, { observe: 'response' });
  }

  save(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/save', copy, { observe: 'response' });
  }

  print(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/${id}`, { responseType: 'blob' });
  }

  printReceipt(id: number): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/assurance/print/receipt/${id}`, { observe: 'response' });
  }

  create(sales: ISales): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sales);
    return this.http
      .post<ISales>(this.resourceUrl + '/assurance', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateItemPrice(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/price/assurance`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtyRequested(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested/assurance`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItemQtySold(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold/assurance`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  addItem(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    const copy = this.convertItemDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(`${this.resourceUrl}/add-item/assurance`, copy, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  updateItem(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/assurance`, salesLine, { observe: 'response' })
      .pipe(map((res: HttpResponse<ISalesLine>) => this.convertItemDateFromServer(res)));
  }

  deleteItem(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/${id}`, { observe: 'response' });
  }

  queryPrevente(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ISales[]>(`${this.resourceUrl}/prevente`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  deletePrevente(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/prevente/${id}`, { observe: 'response' });
  }

  printInvoice(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id}`, { responseType: 'blob' });
  }

  cancel(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/assurance/${id}`, { observe: 'response' });
  }

  changeCustommer(keyValue: KeyValue): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/assurance/customer', keyValue, { observe: 'response' });
  }

  protected convertDateFromClient(sales: ISales): ISales {
    return Object.assign({}, sales, {
      createdAt: sales.createdAt && sales.createdAt.isValid() ? sales.createdAt.toJSON() : undefined,
      updatedAt: sales.updatedAt && sales.updatedAt.isValid() ? sales.updatedAt.toJSON() : undefined,
    });
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
}
