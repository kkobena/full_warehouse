import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOption } from '../../shared/util/request-util';
import { IAuthority } from '../../shared/model/authority.model';
import { IPrivillegesWrapper } from '../../shared/model/menu.model';

type EntityResponseType = HttpResponse<IAuthority>;
type EntityArrayResponseType = HttpResponse<IAuthority[]>;

@Injectable({
  providedIn: 'root'
})
export class PrivillegeService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/privilleges';
  public resourceAuthorityUrl = SERVER_API_URL + 'api/authorities';

  create(authority: IAuthority): Observable<HttpResponse<IAuthority>> {
    return this.http.post<IAuthority>(`${this.resourceAuthorityUrl}/save`, authority, { observe: 'response' });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IAuthority>(`${this.resourceAuthorityUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IAuthority[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  queryAuthorities(req?: any): Observable<HttpResponse<IAuthority[]>> {
    const options = createRequestOption(req);
    return this.http.get<IAuthority[]>(`${this.resourceAuthorityUrl}/all`, {
      params: options,
      observe: 'response'
    });
  }

  getAllPrivillegesByRole(role?: string): Observable<HttpResponse<IPrivillegesWrapper>> {
    return this.http.get<IPrivillegesWrapper>(`${this.resourceUrl}/${role}`, { observe: 'response' });
  }

  deleteRole(name: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceAuthorityUrl}/delete/${name}`, { observe: 'response' });
  }

  update(authority: IAuthority): Observable<HttpResponse<IAuthority>> {
    return this.http.put<IAuthority>(this.resourceAuthorityUrl + '/associe', authority, { observe: 'response' });
  }
}
