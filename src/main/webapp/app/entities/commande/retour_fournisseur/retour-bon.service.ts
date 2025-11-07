import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';

type EntityResponseType = HttpResponse<IRetourBon>;
type EntityArrayResponseType = HttpResponse<IRetourBon[]>;

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

  findByCommande(commandeId: number, orderDate: string): Observable<EntityArrayResponseType> {
    return this.http.get<IRetourBon[]>(`${this.resourceUrl}/by-commande/${commandeId}/${orderDate}`, {
      observe: 'response',
    });
  }

  findByDateRange(startDate: string, endDate: string, req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    options.set('startDate', startDate);
    options.set('endDate', endDate);
    return this.http.get<IRetourBon[]>(`${this.resourceUrl}/by-date-range`, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  validate(id: number): Observable<EntityResponseType> {
    return this.http.put<IRetourBon>(`${this.resourceUrl}/${id}/validate`, {}, { observe: 'response' });
  }
}
