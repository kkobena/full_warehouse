import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { Facture, FactureId } from './facture.model';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { EditionSearchParams } from './edition-search-params.model';
import { DossierFacture } from './dossier-facture.model';
import { TiersPayantDossierFacture } from './tiers-payant-dossier-facture.model';
import { FactureEditionResponse } from './facture-edition-response';
import { DossierFactureProjection, ReglementFactureDossier } from '../reglement/model/reglement-facture-dossier.model';

type EntityResponseType = HttpResponse<Facture>;
type EntityArrayResponseType = HttpResponse<Facture[]>;

@Injectable({
  providedIn: 'root',
})
export class FactureService {
  public resourceUrl = SERVER_API_URL + 'api/edition-factures';
  protected http = inject(HttpClient);

  find(factureId: FactureId): Observable<EntityResponseType> {
    return this.http.get<Facture>(`${this.resourceUrl}/${factureId.id}/${factureId.invoiceDate}`, { observe: 'response' });
  }

  query(searchParams?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(searchParams);
    let url = this.resourceUrl;
    if (searchParams.factureGroupees) {
      url += '/groupes';
    }
    return this.http.get<Facture[]>(url, { params: options, observe: 'response' });
  }

  delete(factureId: FactureId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${factureId.id}/${factureId.invoiceDate}`, { observe: 'response' });
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

  exportToPdf(factureId: FactureId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${factureId.id}/${factureId.invoiceDate}`, {
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

  findDossierReglement(factureId: FactureId, typeFacture: string, query: any): Observable<HttpResponse<ReglementFactureDossier[]>> {
    const options = createRequestOptions(query);
    if (typeFacture === 'groupes') {
      return this.http.get<ReglementFactureDossier[]>(`${this.resourceUrl}/reglement/groupes/${factureId.id}/${factureId.invoiceDate}`, {
        params: options,
        observe: 'response',
      });
    }
    return this.http.get<ReglementFactureDossier[]>(`${this.resourceUrl}/reglement/single/${factureId.id}/${factureId.invoiceDate}`, {
      params: options,
      observe: 'response',
    });
  }

  findDossierFactureProjection(factureId: FactureId, query: any): Observable<HttpResponse<DossierFactureProjection>> {
    const options = createRequestOptions(query);
    return this.http.get<DossierFactureProjection>(`${this.resourceUrl}/reglement/${factureId.id}/${factureId.invoiceDate}`, {
      params: options,
      observe: 'response',
    });
  }
}
