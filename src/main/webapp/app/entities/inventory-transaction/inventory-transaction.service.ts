import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';

type EntityResponseType = HttpResponse<IInventoryTransaction>;
type EntityArrayResponseType = HttpResponse<IInventoryTransaction[]>;

@Injectable({ providedIn: 'root' })
export class InventoryTransactionService {
  public resourceUrl = SERVER_API_URL + 'api/inventory-transactions';

  constructor(protected http: HttpClient) {}

  create(inventoryTransaction: IInventoryTransaction): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(inventoryTransaction);
    return this.http
      .post<IInventoryTransaction>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(inventoryTransaction: IInventoryTransaction): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(inventoryTransaction);
    return this.http
      .put<IInventoryTransaction>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IInventoryTransaction>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IInventoryTransaction[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(inventoryTransaction: IInventoryTransaction): IInventoryTransaction {
    const copy: IInventoryTransaction = Object.assign({}, inventoryTransaction, {
      createdAt:
        inventoryTransaction.createdAt && inventoryTransaction.createdAt.isValid() ? inventoryTransaction.createdAt.toJSON() : undefined,
      updatedAt:
        inventoryTransaction.updatedAt && inventoryTransaction.updatedAt.isValid() ? inventoryTransaction.updatedAt.toJSON() : undefined,
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
      res.body.forEach((inventoryTransaction: IInventoryTransaction) => {
        inventoryTransaction.createdAt = inventoryTransaction.createdAt ? moment(inventoryTransaction.createdAt) : undefined;
        inventoryTransaction.updatedAt = inventoryTransaction.updatedAt ? moment(inventoryTransaction.updatedAt) : undefined;
      });
    }
    return res;
  }
}
