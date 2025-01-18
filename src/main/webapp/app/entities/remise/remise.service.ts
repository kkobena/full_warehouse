import { Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { IRemise } from '../../shared/model/remise.model';
import { Observable } from 'rxjs';
import { createRequestOption } from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<IRemise>;
type EntityArrayResponseType = HttpResponse<IRemise[]>;

@Injectable({
  providedIn: 'root',
})
export class RemiseService {
  public resourceUrl = SERVER_API_URL + 'api/remises';

  constructor(protected http: HttpClient) {}

  create(remise: IRemise): Observable<EntityResponseType> {
    return this.http.post<IRemise>(this.resourceUrl, remise, { observe: 'response' });
  }

  update(remise: IRemise): Observable<EntityResponseType> {
    return this.http.put<IRemise>(this.resourceUrl, remise, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IRemise>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IRemise[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  associer(id: number, prouitIds: number[]): Observable<{}> {
    return this.http.put<{}>(`${this.resourceUrl}/associer/${id}`, prouitIds, { observe: 'response' });
  }

  dissocier(prouitIds: number[]): Observable<{}> {
    return this.http.put<{}>(`${this.resourceUrl}/dissocier`, prouitIds, { observe: 'response' });
  }

  changeStatus(remise: IRemise): Observable<EntityResponseType> {
    return this.http.put<IRemise>(`${this.resourceUrl}/change-status`, remise, { observe: 'response' });
  }
}
