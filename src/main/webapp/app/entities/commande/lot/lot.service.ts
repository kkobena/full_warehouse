import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { ILot } from '../../../shared/model/lot.model';
import { createRequestOptions } from '../../../shared/util/request-util';
import { LotFilterParam, LotPerimes, LotPerimeValeurSum } from '../../gestion-peremption/model/lot-perimes';

type EntityResponseType = HttpResponse<ILot>;
type EntityArrayResponseType = HttpResponse<ILot[]>;

@Injectable({ providedIn: 'root' })
export class LotService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/lot';

  create(lot: ILot): Observable<EntityResponseType> {
    return this.http.post<ILot>(this.resourceUrl + '/add-to-commande', lot, { observe: 'response' });
  }

  addLot(lot: ILot): Observable<EntityResponseType> {
    return this.http.post<ILot>(this.resourceUrl + '/add', lot, { observe: 'response' });
  }

  editLot(lot: ILot): Observable<EntityResponseType> {
    return this.http.post<ILot>(this.resourceUrl + '/edit', lot, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ILot>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  delete(lot: ILot): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/remove-to-commande', lot, { observe: 'response' });
  }

  remove(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ILot[]>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }

  getSum(req: LotFilterParam): Observable<HttpResponse<LotPerimeValeurSum>> {
    const options = createRequestOptions(req);
    return this.http.get<LotPerimeValeurSum>(this.resourceUrl + '/sum', {
      params: options,
      observe: 'response'
    });
  }

  fetchLotPerimes(req: LotFilterParam): Observable<HttpResponse<LotPerimes[]>> {
    const options = createRequestOptions(req);
    return this.http.get<LotPerimes[]>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }

  export(format: string, req: LotFilterParam): Observable<HttpResponse<Blob>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/export/${format}`, {
      params: options,
      observe: 'response',
      responseType: 'blob'
    });
  }

  exportToPdf(req: LotFilterParam): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: options,
      responseType: 'blob'
    });
  }
}
