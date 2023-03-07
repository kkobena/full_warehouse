import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IDelivery } from '../../../shared/model/delevery.model';

type EntityResponseType = HttpResponse<IDelivery>;
type EntityArrayResponseType = HttpResponse<IDelivery[]>;

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  public resourceUrl = SERVER_API_URL + 'api/commandes/data/entree-stock';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IDelivery>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  exportToCsv(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${commandeId}`, { responseType: 'blob' });
  }

  exportToPdf(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${commandeId}`, { responseType: 'blob' });
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
