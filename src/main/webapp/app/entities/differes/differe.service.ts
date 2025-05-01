import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { ClientDiffere } from './model/client-differe.model';
import { Differe } from './model/differe.model';
import { DiffereParam } from './model/differe-param.model';

@Injectable({
  providedIn: 'root',
})
export class DiffereService {
  differeParams: WritableSignal<DiffereParam> = signal<DiffereParam>(null);

  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/differes';

  query(req?: any): Observable<HttpResponse<Differe[]>> {
    const options = createRequestOptions(req);
    return this.http.get<Differe[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  findClients(): Observable<HttpResponse<ClientDiffere[]>> {
    return this.http.get<ClientDiffere[]>(this.resourceUrl + '/customers', {
      observe: 'response',
    });
  }

  find(id: number): Observable<HttpResponse<Differe>> {
    return this.http.get<Differe>(`${this.resourceUrl}/customers/${id}`, { observe: 'response' });
  }

  setParams(searchParams: DiffereParam): void {
    this.differeParams.set(searchParams);
  }
}
