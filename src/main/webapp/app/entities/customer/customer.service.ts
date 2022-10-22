import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ICustomer } from 'app/shared/model/customer.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';

type EntityResponseType = HttpResponse<ICustomer>;
type EntityArrayResponseType = HttpResponse<ICustomer[]>;

@Injectable({ providedIn: 'root' })
export class CustomerService {
  public resourceUrl = SERVER_API_URL + 'api/customers';

  constructor(protected http: HttpClient) {}

  create(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .post<ICustomer>(this.resourceUrl + '/assured', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .put<ICustomer>(this.resourceUrl + '/assured', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ICustomer>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ICustomer[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteAssuredCustomer(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assured/${id}`, { observe: 'response' });
  }

  lock(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/lock/${id}`, { observe: 'response' });
  }

  createUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .post<ICustomer>(`${this.resourceUrl}/uninsured`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .put<ICustomer>(`${this.resourceUrl}/uninsured`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  queryUninsuredCustomers(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ICustomer[]>(`${this.resourceUrl}/uninsured`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  createAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .post<ICustomer>(this.resourceUrl + '/ayant-droit', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(customer);
    return this.http
      .put<ICustomer>(this.resourceUrl + '/ayant-droit', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  uploadJsonData(file: any): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/importjson`, file, {
      observe: 'response',
      headers: new HttpHeaders({ timeout: `${7600000}` }),
    });
  }

  purchases(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ICustomer[]>(`${this.resourceUrl}/purchases`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  queryAssuredCustomer(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ICustomer[]>(`${this.resourceUrl}/assured`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  queryVente(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ICustomer[]>(`${this.resourceUrl}/ventes`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  fetchCustomersTiersPayant(id: number): Observable<HttpResponse<IClientTiersPayant[]>> {
    return this.http.get<IClientTiersPayant[]>(`${this.resourceUrl}/tiers-payants/${id}`, { observe: 'response' });
  }

  queryAyantDroits(id: number): Observable<EntityArrayResponseType> {
    return this.http
      .get<ICustomer[]>(`${this.resourceUrl}/ayant-droits/${id}`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  protected convertDateFromClient(customer: ICustomer): ICustomer {
    const copy: ICustomer = Object.assign({}, customer, {
      createdAt: customer.createdAt && customer.createdAt.isValid() ? customer.createdAt.toJSON() : undefined,
      updatedAt: customer.updatedAt && customer.updatedAt.isValid() ? customer.updatedAt.toJSON() : undefined,
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
      res.body.forEach((customer: ICustomer) => {
        customer.createdAt = customer.createdAt ? moment(customer.createdAt) : undefined;
        customer.updatedAt = customer.updatedAt ? moment(customer.updatedAt) : undefined;
      });
    }
    return res;
  }
}
