import { Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IAjustement } from 'app/shared/model/ajustement.model';
import { IAjust } from '../../shared/model/ajust.model';

type EntityResponseType = HttpResponse<IAjustement>;
type EntityArrayResponseType = HttpResponse<IAjustement[]>;

@Injectable({ providedIn: 'root' })
export class AjustementService {
  public resourceUrl = SERVER_API_URL + 'api/ajustements';
  toolbarParam: WritableSignal<any> = signal<any>({
    fromDate: new Date(),
    toDate: new Date(),
    search: null,
    userId: null,
  });

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IAjustement>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<IAjustement[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  queryAjustement(req?: any): Observable<HttpResponse<IAjust[]>> {
    const options = createRequestOptions(req);
    return this.http
      .get<IAjust[]>(this.resourceUrl + '/ajust', { params: options, observe: 'response' })
      .pipe(map((res: HttpResponse<IAjust[]>) => this.convertAjustDateArrayFromServer(res)));
  }

  queryAll(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<IAjustement[]>(this.resourceUrl + '/all', { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
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
    const copy = this.convertDateFromClient(ajustement);
    return this.http
      .put<IAjustement>(this.resourceUrl + '/item', copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
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

  protected convertDateFromClient(ajustement: IAjustement): IAjustement {
    const copy: IAjustement = Object.assign({}, ajustement, {
      dateMtv: ajustement.dateMtv && ajustement.dateMtv.isValid() ? ajustement.dateMtv.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.dateMtv = res.body.dateMtv ? moment(res.body.dateMtv) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((ajustement: IAjustement) => {
        ajustement.dateMtv = ajustement.dateMtv ? moment(ajustement.dateMtv) : undefined;
      });
    }
    return res;
  }

  protected convertAjustDateArrayFromServer(res: HttpResponse<IAjust[]>): HttpResponse<IAjust[]> {
    if (res.body) {
      res.body.forEach((ajustement: IAjust) => {
        ajustement.dateMtv = ajustement.dateMtv ? moment(ajustement.dateMtv) : undefined;
      });
    }
    return res;
  }
}
