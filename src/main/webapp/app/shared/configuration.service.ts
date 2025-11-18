import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { IConfiguration } from './model/configuration.model';
import { SERVER_API_URL } from '../app.constants';
import { Observable } from 'rxjs';

import { createRequestOption } from './util/request-util';
import { SessionStorageService } from 'ngx-webstorage';

type EntityResponseType = HttpResponse<IConfiguration>;
type EntityArrayResponseType = HttpResponse<IConfiguration[]>;

@Injectable({
  providedIn: 'root',
})
export class ConfigurationService {
  protected http = inject(HttpClient);
  private sessionStorageService = inject(SessionStorageService);

  public resourceUrl = SERVER_API_URL + 'api/app';

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
    return this.http.put(this.resourceUrl, app, { observe: 'response' });
  }




  getSimpleSaleConfig(): Observable<boolean> {
    return new Observable<boolean>(observer => {
      this.find('use-simple-sale').subscribe({
        next: res => {
          if (res.body) {
            this.storeParamByKey('use-simple-sale', res.body);
            observer.next(Number(res.body.value) ==1);
          } else {
            observer.next(false);
          }
          observer.complete();
        },
        error: () => {
          observer.next(false);
          observer.complete();
        },
      });
    });
  }
}
