import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IDelivery } from '../../../shared/model/delevery.model';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { IDeliveryItem } from '../../../shared/model/delivery-item';

type EntityResponseType = HttpResponse<IDelivery>;
type EntityArrayResponseType = HttpResponse<IDelivery[]>;

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  public resourceUrl = SERVER_API_URL + 'api/commandes/data/entree-stock';
  public resourceUrl2 = SERVER_API_URL + 'api/commandes/entree-stock/create';
  public resourceFinalyse = SERVER_API_URL + 'api/commandes/entree-stock/finalize';
  public resourceUrlTransac = SERVER_API_URL + 'api/commandes/entree-stock';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IDelivery>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  create(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.post<IDelivery>(this.resourceUrl2, entity, { observe: 'response' });
  }

  finalizeSaisieEntreeStock(delivery: IDelivery): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(delivery);
    return this.http
      .put<IDelivery>(this.resourceFinalyse, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.put<IDelivery>(this.resourceUrl2, entity, { observe: 'response' });
  }

  findByOrderReference(orderReference: string): Observable<EntityResponseType> {
    return this.http
      .get<IDelivery>(`${this.resourceUrl}/by-order-reference/${orderReference}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  exportToCsv(entityId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${entityId}`, { responseType: 'blob' });
  }

  exportToPdf(entityId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${entityId}`, { responseType: 'blob' });
  }

  printEtiquette(id: number, req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/etiquettes/${id}`, {
      params: options,
      responseType: 'blob',
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<IDelivery[]>(this.resourceUrl + '/list', {
        params: options,
        observe: 'response',
      })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  uploadNew(data: any): Observable<HttpResponse<ICommandeResponse>> {
    return this.http.post<ICommandeResponse>(`${this.resourceUrlTransac}/upload-new`, data, {
      observe: 'response',
    });
  }

  filterItems(req?: any): Observable<HttpResponse<IDeliveryItem[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IDeliveryItem[]>(`${this.resourceUrl}/filter-items`, {
      params: options,
      observe: 'response',
    });
  }

  updateQuantityReceived(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-quantity-received', deliveryItem, {
      observe: 'response',
    });
  }

  updateQuantityUG(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-quantity-ug', deliveryItem, {
      observe: 'response',
    });
  }

  updateCip(deliveryItem: IDeliveryItem): Observable<HttpResponse<{}>> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '//update-provisional-cip', deliveryItem, { observe: 'response' });
  }

  updateOrderUnitPriceOnStockEntry(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-unit-price', deliveryItem, {
      observe: 'response',
    });
  }

  updateOrderCostAmount(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-cost-amount', deliveryItem, {
      observe: 'response',
    });
  }

  protected convertDateFromClient(delivery: IDelivery): IDelivery {
    const copy: IDelivery = Object.assign({}, delivery, {
      receiptDate: delivery.receiptDate && delivery.receiptDate.isValid() ? delivery.receiptDate.format(DATE_FORMAT) : undefined,
      createdAt: delivery.createdDate && delivery.createdDate.isValid() ? delivery.createdDate.toJSON() : undefined,
      updatedAt: delivery.modifiedDate && delivery.modifiedDate.isValid() ? delivery.modifiedDate.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.receiptDate = res.body.receiptDate ? moment(res.body.receiptDate) : undefined;
      res.body.createdDate = res.body.receiptDate ? moment(res.body.createdDate) : undefined;
      res.body.modifiedDate = res.body.modifiedDate ? moment(res.body.modifiedDate) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((delivery: IDelivery) => {
        delivery.receiptDate = delivery.receiptDate ? moment(delivery.receiptDate) : undefined;
        delivery.createdDate = delivery.createdDate ? moment(delivery.createdDate) : undefined;
        delivery.modifiedDate = delivery.modifiedDate ? moment(delivery.modifiedDate) : undefined;
      });
    }
    return res;
  }
}
