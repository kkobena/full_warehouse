import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { Facture } from './facture.model';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { EditionSearchParams } from './edition-search-params.model';
import { DossierFacture } from './dossier-facture.model';
import { TiersPayantDossierFacture } from './tiers-payant-dossier-facture.model';
import { FactureEditionResponse } from './facture-edition-response';

type EntityResponseType = HttpResponse<Facture>;
type EntityArrayResponseType = HttpResponse<Facture[]>;

@Injectable({
  providedIn: 'root',
})
export class FactureService {
  public resourceUrl = SERVER_API_URL + 'api/edition-factures';

  constructor(protected http: HttpClient) {}

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<Facture>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(searchParams?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(searchParams);
    let url = this.resourceUrl;
    if (searchParams.factureGroupees) {
      url += '/groupes';
    }
    return this.http.get<Facture[]>(url, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  queryBons(editionSearchParams: any): Observable<HttpResponse<DossierFacture[]>> {
    const options = createRequestOptions(editionSearchParams);
    return this.http.get<DossierFacture[]>(this.resourceUrl + '/bons', {
      params: options,
      observe: 'response',
    });
  }

  queryEditionData(editionSearchParams: any): Observable<HttpResponse<TiersPayantDossierFacture[]>> {
    const options = createRequestOptions(editionSearchParams);
    return this.http.get<TiersPayantDossierFacture[]>(this.resourceUrl + '/data', {
      params: options,
      observe: 'response',
    });
  }

  editInvoices(editionSearchParams: EditionSearchParams): Observable<HttpResponse<FactureEditionResponse>> {
    return this.http.post<FactureEditionResponse>(`${this.resourceUrl}/edit`, editionSearchParams, { observe: 'response' });
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf//${id}`, {
      responseType: 'blob',
    });
  }

  exportAllInvoices(editionResponse: FactureEditionResponse): Observable<Blob> {
    const options = createRequestOptions(editionResponse);
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }
}
