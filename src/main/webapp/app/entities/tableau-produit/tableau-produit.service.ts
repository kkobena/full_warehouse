import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITableau } from '../../shared/model/tableau.model';

type EntityResponseType = HttpResponse<ITableau>;
type EntityArrayResponseType = HttpResponse<ITableau[]>;

@Injectable({
  providedIn: 'root',
})
export class TableauProduitService {
  public resourceUrl = SERVER_API_URL + 'api/tableaux';

  constructor(protected http: HttpClient) {}

  create(tableau: ITableau): Observable<EntityResponseType> {
    return this.http.post<ITableau>(this.resourceUrl, tableau, { observe: 'response' });
  }

  update(tableau: ITableau): Observable<EntityResponseType> {
    return this.http.put<ITableau>(this.resourceUrl, tableau, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ITableau>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ITableau[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
