import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IResponseDto } from 'app/shared/util/response-dto';
import { ITiersPayant, ModelFacture, OrdreTrisFacture } from '../../shared/model/tierspayant.model';
import { TiersPayantAchat } from './model/tiers-payant-achat.model';

type EntityResponseType = HttpResponse<ITiersPayant>;
type EntityArrayResponseType = HttpResponse<ITiersPayant[]>;

@Injectable({ providedIn: 'root' })
export class TiersPayantService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/tiers-payants';

  create(tiersPayant: ITiersPayant): Observable<EntityResponseType> {
    return this.http.post<ITiersPayant>(this.resourceUrl, tiersPayant, { observe: 'response' });
  }

  update(tiersPayant: ITiersPayant): Observable<EntityResponseType> {
    return this.http.put<ITiersPayant>(this.resourceUrl, tiersPayant, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ITiersPayant>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ITiersPayant[]>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }

  uploadJsonData(file: any): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/importjson`, file, { observe: 'response' });
  }

  desable(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/desable/${id}`, { observe: 'response' });
  }

  getModelFacture(): Observable<HttpResponse<ModelFacture[]>> {
    return this.http.get<ModelFacture[]>(this.resourceUrl + '/models-facture', {
      observe: 'response'
    });
  }

  getOrdreTrisFacture(): Observable<HttpResponse<OrdreTrisFacture[]>> {
    return this.http.get<OrdreTrisFacture[]>(this.resourceUrl + '/order-tris-facture', {
      observe: 'response'
    });
  }

  fetchAchatTiersPayant(req?: any): Observable<HttpResponse<TiersPayantAchat[]>> {
    const options = createRequestOptions(req);
    return this.http.get<TiersPayantAchat[]>(this.resourceUrl + '/achats-summary', {
      params: options,
      observe: 'response'
    });
  }
}
