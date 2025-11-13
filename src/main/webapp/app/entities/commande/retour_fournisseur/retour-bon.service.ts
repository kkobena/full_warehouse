import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { IReponseRetourBon } from 'app/shared/model/reponse-retour-bon.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';

type EntityResponseType = HttpResponse<IRetourBon>;
type EntityArrayResponseType = HttpResponse<IRetourBon[]>;
type ResponseEntityResponseType = HttpResponse<IReponseRetourBon>;

@Injectable({ providedIn: 'root' })
export class RetourBonService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/retour-bons';

  create(retourBon: IRetourBon): Observable<EntityResponseType> {
    return this.http.post<IRetourBon>(this.resourceUrl, retourBon, { observe: 'response' });
  }

  update(retourBon: IRetourBon): Observable<EntityResponseType> {
    return this.http.put<IRetourBon>(this.resourceUrl, retourBon, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IRetourBon>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IRetourBon[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  queryByStatut(statut: RetourBonStatut, req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    options.set('statut', statut.toString());
    return this.http.get<IRetourBon[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  createSupplierResponse(reponseRetourBon: IReponseRetourBon): Observable<ResponseEntityResponseType> {
    return this.http.post<IReponseRetourBon>(`${this.resourceUrl}/supplier-response`, reponseRetourBon, { observe: 'response' });
  }
}
