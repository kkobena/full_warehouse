import { Injectable, inject } from '@angular/core';

import { HttpClient, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';

type EntityResponseType = HttpResponse<IMotifAjustement>;
type EntityArrayResponseType = HttpResponse<IMotifAjustement[]>;

@Injectable({
  providedIn: 'root',
})
export class ModifAjustementService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/motif-ajsutements';

  create(modifAjustement: IMotifAjustement): Observable<EntityResponseType> {
    return this.http.post<IMotifAjustement>(this.resourceUrl, modifAjustement, { observe: 'response' });
  }

  update(modifAjustement: IMotifAjustement): Observable<EntityResponseType> {
    return this.http.put<IMotifAjustement>(this.resourceUrl, modifAjustement, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IMotifAjustement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IMotifAjustement[]>(this.resourceUrl, {
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
}
