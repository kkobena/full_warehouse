import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';

type EntityResponseType = HttpResponse<IGroupeFournisseur>;
type EntityArrayResponseType = HttpResponse<IGroupeFournisseur[]>;

@Injectable({
  providedIn: 'root'
})
export class GroupeFournisseurService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/groupe-fournisseurs';

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  create(groupeFournisseur: IGroupeFournisseur): Observable<EntityResponseType> {
    return this.http.post<IGroupeFournisseur>(this.resourceUrl, groupeFournisseur, { observe: 'response' });
  }

  update(groupeFournisseur: IGroupeFournisseur): Observable<EntityResponseType> {
    return this.http.put<IGroupeFournisseur>(this.resourceUrl, groupeFournisseur, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupeFournisseur>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IGroupeFournisseur[]>(this.resourceUrl, {
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
