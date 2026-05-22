import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IAjustement } from 'app/shared/model/ajustement.model';
import { IAjust } from '../../shared/model/ajust.model';

type EntityResponseType = HttpResponse<IAjustement>;
type EntityArrayResponseType = HttpResponse<IAjustement[]>;

@Injectable({ providedIn: 'root' })
export class AjustementService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/ajustements';
  toolbarParam: WritableSignal<any> = signal<any>({
    fromDate: new Date(),
    toDate: new Date(),
    search: null,
    userId: null,
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IAjustement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IAjustement[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  queryAjustement(req?: any): Observable<HttpResponse<IAjust[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IAjust[]>(this.resourceUrl + '/ajust', { params: options, observe: 'response' });
  }

  queryAll(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IAjustement[]>(this.resourceUrl + '/all', { params: options, observe: 'response' });
  }

  create(ajustement: IAjust): Observable<HttpResponse<IAjust>> {
    return this.http.post<IAjust>(this.resourceUrl, ajustement, { observe: 'response' });
  }

  saveAjustement(ajustement: IAjust): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/save', ajustement, { observe: 'response' });
  }

  addItem(ajustement: IAjustement): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/item/add', ajustement, { observe: 'response' });
  }

  updateItem(ajustement: IAjustement): Observable<EntityResponseType> {
    return this.http.put<IAjustement>(this.resourceUrl + '/item', ajustement, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteItem(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/item/${id}`, { observe: 'response' });
  }

  deleteItemsByIds(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete/items`, ids, { observe: 'response' });
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${id}`, { responseType: 'blob' });
  }

  updateToolbarParam(param: {}): void {
    this.toolbarParam.set(param);
  }

}
