import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import {
  IDossierFacture,
  IEditionSearchParams,
  IFacture,
  IFactureEditionResponse,
  IFactureId,
  IFacturationKpi,
  IInvoiceSearchParams,
  ITiersPayantDossierFacture,
} from '../models';
import { IDossierFactureProjection, IReglementFactureDossier } from '../models';

@Injectable({ providedIn: 'root' })
export class FactureApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/edition-factures';
  private readonly http = inject(HttpClient);

  find(factureId: IFactureId): Observable<HttpResponse<IFacture>> {
    return this.http.get<IFacture>(
      `${this.resourceUrl}/${factureId.id}/${factureId.invoiceDate}`,
      { observe: 'response' },
    );
  }

  query(searchParams: IInvoiceSearchParams): Observable<HttpResponse<IFacture[]>> {
    const options = createRequestOptions(searchParams);
    const url = searchParams.factureGroupees ? `${this.resourceUrl}/groupes` : this.resourceUrl;
    return this.http.get<IFacture[]>(url, { params: options, observe: 'response' });
  }

  delete(factureId: IFactureId): Observable<HttpResponse<{}>> {
    return this.http.delete(
      `${this.resourceUrl}/${factureId.id}/${factureId.invoiceDate}`,
      { observe: 'response' },
    );
  }

  queryBons(params: any): Observable<HttpResponse<IDossierFacture[]>> {
    return this.http.get<IDossierFacture[]>(
      `${this.resourceUrl}/bons`,
      { params: createRequestOptions(params), observe: 'response' },
    );
  }

  queryEditionData(params: any): Observable<HttpResponse<ITiersPayantDossierFacture[]>> {
    return this.http.get<ITiersPayantDossierFacture[]>(
      `${this.resourceUrl}/data`,
      { params: createRequestOptions(params), observe: 'response' },
    );
  }

  editInvoices(params: IEditionSearchParams): Observable<HttpResponse<IFactureEditionResponse>> {
    return this.http.post<IFactureEditionResponse>(
      `${this.resourceUrl}/edit`,
      params,
      { observe: 'response' },
    );
  }

  exportToPdf(factureId: IFactureId): Observable<Blob> {
    return this.http.get(
      `${this.resourceUrl}/pdf/${factureId.id}/${factureId.invoiceDate}`,
      { responseType: 'blob' },
    );
  }

  exportAllInvoices(editionResponse: IFactureEditionResponse): Observable<Blob> {
    return this.http.get(
      `${this.resourceUrl}/pdf`,
      { params: createRequestOptions(editionResponse), responseType: 'blob' },
    );
  }

  exportExcel(searchParams: IInvoiceSearchParams): Observable<Blob> {
    const url = searchParams.factureGroupees
      ? `${this.resourceUrl}/groupes/export`
      : `${this.resourceUrl}/export`;
    return this.http.get(url, {
      params: createRequestOptions(searchParams),
      responseType: 'blob',
    });
  }

  getKpi(params: { fromDate?: string; toDate?: string; organismeId?: number }): Observable<HttpResponse<IFacturationKpi>> {
    return this.http.get<IFacturationKpi>(
      `${this.resourceUrl}/kpi`,
      { params: createRequestOptions(params), observe: 'response' },
    );
  }

  findDossierReglement(
    factureId: IFactureId,
    typeFacture: 'groupes' | 'individuelle',
    query: any,
  ): Observable<HttpResponse<IReglementFactureDossier[]>> {
    const segment = typeFacture === 'groupes' ? 'groupes' : 'single';
    return this.http.get<IReglementFactureDossier[]>(
      `${this.resourceUrl}/reglement/${segment}/${factureId.id}/${factureId.invoiceDate}`,
      { params: createRequestOptions(query), observe: 'response' },
    );
  }

  findDossierFactureProjection(
    factureId: IFactureId,
    query: { isGroup: boolean },
  ): Observable<HttpResponse<IDossierFactureProjection>> {
    return this.http.get<IDossierFactureProjection>(
      `${this.resourceUrl}/reglement/${factureId.id}/${factureId.invoiceDate}`,
      { params: createRequestOptions(query), observe: 'response' },
    );
  }
}
