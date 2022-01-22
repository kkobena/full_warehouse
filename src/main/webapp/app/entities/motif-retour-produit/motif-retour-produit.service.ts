import { Injectable } from '@angular/core';

import { HttpResponse, HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';
import { IMotifRetourProduit } from '../../shared/model/motif-retour-produit.model';

type EntityResponseType = HttpResponse<IMotifRetourProduit>;
type EntityArrayResponseType = HttpResponse<IMotifRetourProduit[]>;
@Injectable({
  providedIn: 'root',
})
export class ModifRetourProduitService {
  public resourceUrl = SERVER_API_URL + 'api/modif-retour-produits';
  constructor(protected http: HttpClient) {}

  create(modifAjustement: IMotifRetourProduit): Observable<EntityResponseType> {
    return this.http.post<IMotifRetourProduit>(this.resourceUrl, modifAjustement, { observe: 'response' });
  }

  update(modifAjustement: IMotifRetourProduit): Observable<EntityResponseType> {
    return this.http.put<IMotifRetourProduit>(this.resourceUrl, modifAjustement, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IMotifRetourProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IMotifRetourProduit[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
  async queryPromise(req?: any): Promise<IMotifRetourProduit[]> {
    const options = createRequestOption(req);
    return await this.http
      .get<IMotifRetourProduit[]>(this.resourceUrl, { params: options })
      .toPromise();
  }
  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
