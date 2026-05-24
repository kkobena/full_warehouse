import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ICustomer } from 'app/shared/model/customer.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { IAvoirClientDocument } from 'app/shared/model/avoir-client-document.model';

type EntityResponseType = HttpResponse<ICustomer>;
type EntityArrayResponseType = HttpResponse<ICustomer[]>;

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/customers';

  create(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(this.resourceUrl + '/assured', customer, { observe: 'response' });
  }

  update(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(this.resourceUrl + '/assured', customer, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ICustomer>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteAssuredCustomer(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assured/${id}`, { observe: 'response' });
  }

  deleteTiersPayant(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assured/tiers-payants/${id}`, { observe: 'response' });
  }

  lock(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/lock/${id}`, { observe: 'response' });
  }

  createUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(`${this.resourceUrl}/uninsured`, customer, { observe: 'response' });
  }

  updateUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(`${this.resourceUrl}/uninsured`, customer, { observe: 'response' });
  }

  queryUninsuredCustomers(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/uninsured`, { params: options, observe: 'response' });
  }

  createAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(this.resourceUrl + '/ayant-droit', customer, { observe: 'response' });
  }

  updateAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(this.resourceUrl + '/ayant-droit', customer, { observe: 'response' });
  }

  uploadJsonData(file: any): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/importjson`, file, {
      observe: 'response',
      headers: new HttpHeaders({ timeout: `7600000` }),
    });
  }

  purchases(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/purchases`, { params: options, observe: 'response' });
  }

  queryAssuredCustomer(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/assured`, { params: options, observe: 'response' });
  }

  addTiersPayant(clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<ICustomer>> {
    return this.http.post<ICustomer>(`${this.resourceUrl}/assured/tiers-payants`, clientTiersPayant, { observe: 'response' });
  }

  updateTiersPayant(clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<ICustomer>> {
    return this.http.put<ICustomer>(`${this.resourceUrl}/assured/tiers-payants`, clientTiersPayant, { observe: 'response' });
  }

  queryVente(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/ventes`, { params: options, observe: 'response' });
  }

  fetchCustomersTiersPayant(id: number): Observable<HttpResponse<IClientTiersPayant[]>> {
    return this.http.get<IClientTiersPayant[]>(`${this.resourceUrl}/tiers-payants/${id}`, { observe: 'response' });
  }

  queryAyantDroits(id: number): Observable<EntityArrayResponseType> {
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/ayant-droits/${id}`, { observe: 'response' });
  }

  avoirsByCustomer(customerId: number): Observable<IAvoirClientDocument[]> {
    return this.http.get<IAvoirClientDocument[]>(`${SERVER_API_URL}api/sales/avoirs/documents/by-customer/${customerId}`);
  }
}
