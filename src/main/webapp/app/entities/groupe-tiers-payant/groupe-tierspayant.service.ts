import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IGroupeTiersPayant } from '../../shared/model/groupe-tierspayant.model';
import { IResponseDto } from 'app/shared/util/response-dto';

type EntityResponseType = HttpResponse<IGroupeTiersPayant>;
type EntityArrayResponseType = HttpResponse<IGroupeTiersPayant[]>;

@Injectable({ providedIn: 'root' })
export class GroupeTiersPayantService {
  public resourceUrl = SERVER_API_URL + 'api/groupe-tierspayants';

  constructor(protected http: HttpClient) {}

  create(groupeTiersPayant: IGroupeTiersPayant): Observable<EntityResponseType> {
    return this.http.post<IGroupeTiersPayant>(this.resourceUrl, groupeTiersPayant, { observe: 'response' });
  }

  update(groupeTiersPayant: IGroupeTiersPayant): Observable<EntityResponseType> {
    return this.http.put<IGroupeTiersPayant>(this.resourceUrl, groupeTiersPayant, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupeTiersPayant>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IGroupeTiersPayant[]>(this.resourceUrl, {
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

  async queryPromise(req?: any): Promise<IGroupeTiersPayant[]> {
    const options = createRequestOption(req);
    return await firstValueFrom(this.http.get<IGroupeTiersPayant[]>(this.resourceUrl, { params: options }));
  }
}
