import { Injectable } from '@angular/core';

import { HttpClient, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { IFamilleProduit } from '../../shared/model/famille-produit.model';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';

type EntityResponseType = HttpResponse<IFamilleProduit>;
type EntityArrayResponseType = HttpResponse<IFamilleProduit[]>;

@Injectable({
  providedIn: 'root',
})
export class FamilleProduitService {
  public resourceUrl = SERVER_API_URL + 'api/famille-produits';

  constructor(protected http: HttpClient) {}

  create(familleProduit: IFamilleProduit): Observable<EntityResponseType> {
    return this.http.post<IFamilleProduit>(this.resourceUrl, familleProduit, { observe: 'response' });
  }

  update(familleProduit: IFamilleProduit): Observable<EntityResponseType> {
    return this.http.put<IFamilleProduit>(this.resourceUrl, familleProduit, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IFamilleProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFamilleProduit[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  queryPromise(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFamilleProduit[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
