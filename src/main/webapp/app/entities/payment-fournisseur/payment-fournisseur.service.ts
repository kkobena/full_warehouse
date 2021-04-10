import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';

type EntityResponseType = HttpResponse<IPaymentFournisseur>;
type EntityArrayResponseType = HttpResponse<IPaymentFournisseur[]>;

@Injectable({ providedIn: 'root' })
export class PaymentFournisseurService {
  public resourceUrl = SERVER_API_URL + 'api/payment-fournisseurs';

  constructor(protected http: HttpClient) {}

  create(paymentFournisseur: IPaymentFournisseur): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(paymentFournisseur);
    return this.http
      .post<IPaymentFournisseur>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(paymentFournisseur: IPaymentFournisseur): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(paymentFournisseur);
    return this.http
      .put<IPaymentFournisseur>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IPaymentFournisseur>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IPaymentFournisseur[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(paymentFournisseur: IPaymentFournisseur): IPaymentFournisseur {
    const copy: IPaymentFournisseur = Object.assign({}, paymentFournisseur, {
      createdAt: paymentFournisseur.createdAt && paymentFournisseur.createdAt.isValid() ? paymentFournisseur.createdAt.toJSON() : undefined,
      updatedAt: paymentFournisseur.updatedAt && paymentFournisseur.updatedAt.isValid() ? paymentFournisseur.updatedAt.toJSON() : undefined,
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
      res.body.forEach((paymentFournisseur: IPaymentFournisseur) => {
        paymentFournisseur.createdAt = paymentFournisseur.createdAt ? moment(paymentFournisseur.createdAt) : undefined;
        paymentFournisseur.updatedAt = paymentFournisseur.updatedAt ? moment(paymentFournisseur.updatedAt) : undefined;
      });
    }
    return res;
  }
}
