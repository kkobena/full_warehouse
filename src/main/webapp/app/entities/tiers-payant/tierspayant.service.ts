import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IResponseDto } from 'app/shared/util/response-dto';
import { ITiersPayant } from '../../shared/model/tierspayant.model';

type EntityResponseType = HttpResponse<ITiersPayant>;
type EntityArrayResponseType = HttpResponse<ITiersPayant[]>;

@Injectable({ providedIn: 'root' })
export class TiersPayantService {
  public resourceUrl = SERVER_API_URL + 'api/tiers-payants';

  constructor(protected http: HttpClient) {}

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
    const options = createRequestOption(req);
    return this.http.get<ITiersPayant[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
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
}
