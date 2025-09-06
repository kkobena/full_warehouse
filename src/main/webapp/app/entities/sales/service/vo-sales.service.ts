import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { FinalyseSale, ISales, SaleId, UpdateSaleInfo } from 'app/shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../../shared/model/sales-line.model';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { UtilisationCleSecurite } from '../../action-autorisation/utilisation-cle-securite.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class VoSalesService {
  public resourceUrl = SERVER_API_URL + 'api/sales';
  protected http = inject(HttpClient);

  find(id: SaleId): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  findForEdit(id: SaleId): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/edit/${id.id}/${id.saleDate}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  putCurrentOnStandBy(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    const copy = this.convertDateFromClient(sales);
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/put-on-hold', copy, { observe: 'response' });
  }

  save(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    return this.http.put<FinalyseSale>(this.resourceUrl + '/assurance/save', sales, { observe: 'response' });
  }

  print(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  printReceipt(id: SaleId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/assurance/print/receipt/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  rePrintReceipt(id: SaleId): Observable<{}> {
    return this.http.get(`${this.resourceUrl}/assurance/re-print/receipt/${id.id}/${id.saleDate}`, { observe: 'response' });
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

  deleteItem(id: SaleLineId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/assurance/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  removeThirdPartySaleLineToSales(id: number, saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/remove-tiers-payant/assurance/${id}/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
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

  printInvoice(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id}/${id.saleDate}`, { responseType: 'blob' });
  }

  cancel(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/assurance/${id}/${id.saleDate}`, { observe: 'response' });
  }

  updateTransformedSale(sales: ISales): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/assurance/transform/add-customer', sales, { observe: 'response' });
  }

  changeCustomer(keyValue: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/assurance/change/customer', keyValue, { observe: 'response' });
  }

  authorizeAction(utilisationCleSecurite: UtilisationCleSecurite): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/assurance/authorize-action', utilisationCleSecurite, { observe: 'response' });
  }

  addComplementaireSales(id: SaleId, clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/add-assurance/assurance/${id.id}/${id.saleDate}`, clientTiersPayant, { observe: 'response' });
  }

  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/assurance/add-remise', key, { observe: 'response' });
  }

  removeRemiseFromSale(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assurance/remove-remise/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  private convertDateFromClient(sales: ISales): ISales {
    return Object.assign({}, sales, {
      createdAt: sales.createdAt && sales.createdAt.isValid() ? sales.createdAt.toJSON() : undefined,
      updatedAt: sales.updatedAt && sales.updatedAt.isValid() ? sales.updatedAt.toJSON() : undefined,
    });
  }

  private convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((sales: ISales) => {
        sales.createdAt = sales.createdAt ? moment(sales.createdAt) : undefined;
        sales.updatedAt = sales.updatedAt ? moment(sales.updatedAt) : undefined;
      });
    }
    return res;
  }

  private convertItemDateFromClient(salesLine: ISalesLine): ISalesLine {
    const copy: ISalesLine = Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  private convertItemDateFromServer(res: HttpResponse<ISalesLine>): HttpResponse<ISalesLine> {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }
}
