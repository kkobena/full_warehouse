import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SERVER_API_URL} from '../../app.constants';
import {ITypeEtiquette} from '../../shared/model/type-etiquette.model';
import {createRequestOption} from '../../shared/util/request-util';

type EntityResponseType = HttpResponse<ITypeEtiquette>;
type EntityArrayResponseType = HttpResponse<ITypeEtiquette[]>;

@Injectable({
  providedIn: 'root',
})
export class TypeEtiquetteService {
  public resourceUrl = SERVER_API_URL + 'api/type-etiquettes';

  constructor(protected http: HttpClient) {
  }

  create(typeEtiquette: ITypeEtiquette): Observable<EntityResponseType> {
    return this.http.post<ITypeEtiquette>(this.resourceUrl, typeEtiquette, {observe: 'response'});
  }

  update(typeEtiquette: ITypeEtiquette): Observable<EntityResponseType> {
    return this.http.put<ITypeEtiquette>(this.resourceUrl, typeEtiquette, {observe: 'response'});
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ITypeEtiquette>(`${this.resourceUrl}/${id}`, {observe: 'response'});
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ITypeEtiquette[]>(this.resourceUrl, {params: options, observe: 'response'});
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {observe: 'response'});
  }

  async queryPromise(req?: any): Promise<ITypeEtiquette[] | undefined> {
    const options = createRequestOption(req);
    
    return await this.http
      .get<ITypeEtiquette[]>(this.resourceUrl, {params: options})
      .toPromise();
  }
}
