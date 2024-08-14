import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { IConfiguration } from './model/configuration.model';
import { SERVER_API_URL } from '../app.constants';
import { Observable } from 'rxjs';

import { createRequestOption } from './util/request-util';
import { SessionStorageService } from 'ngx-webstorage';
import { ISales } from './model/sales.model';
import { map } from 'rxjs/operators';

type EntityResponseType = HttpResponse<IConfiguration>;
type EntityArrayResponseType = HttpResponse<IConfiguration[]>;

@Injectable({
  providedIn: 'root',
})
export class ConfigurationService {
  public resourceUrl = SERVER_API_URL + 'api/app';

  constructor(
    protected http: HttpClient,
    private sessionStorageService: SessionStorageService,
  ) {}

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IConfiguration[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  findStockConfig(): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/param-gestion-stock`, { observe: 'response' });
  }

  storeParamByKey(key: string, value?: IConfiguration): void {
    this.sessionStorageService.store(key, value);
  }

  getParamByKey(key: string): IConfiguration | null {
    const param = this.sessionStorageService.retrieve(key) as IConfiguration | null;
    if (param) {
      return param;
    }
    this.find(key).subscribe(res => this.storeParamByKey(key, res.body));
    return this.sessionStorageService.retrieve(key) as IConfiguration | null;
  }

  clearParam(key: string): void {
    this.sessionStorageService.clear(key);
  }

  update(app: IConfiguration): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}`, app, { observe: 'response' });
  }
}
