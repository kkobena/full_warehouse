import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';

type EntityResponseType = HttpResponse<IFournisseur>;
type EntityArrayResponseType = HttpResponse<IFournisseur[]>;

@Injectable({
  providedIn: 'root'
})
export class FournisseurService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/fournisseurs';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  create(fournisseur: IFournisseur): Observable<EntityResponseType> {
    return this.http.post<IFournisseur>(this.resourceUrl, fournisseur, { observe: 'response' });
  }

  update(fournisseur: IFournisseur): Observable<EntityResponseType> {
    return this.http.put<IFournisseur>(this.resourceUrl, fournisseur, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IFournisseur>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFournisseur[]>(this.resourceUrl, {
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
}
