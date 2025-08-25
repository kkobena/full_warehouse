import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { VenteRecordParam } from '../../../shared/model/vente-record-param.model';
import {
  HistoriqueProduitAchats,
  HistoriqueProduitAchatsSummary,
  HistoriqueProduitDonneesMensuelles,
  HistoriqueProduitVente,
  HistoriqueProduitVenteMensuelleSummary,
  HistoriqueProduitVenteSummary,
  ProductStatParetoRecord,
  ProductStatRecord,
  ProduitAuditingParam,
  ProduitAuditingState,
  ProduitAuditingSum,
} from '../../../shared/model/produit-record.model';

@Injectable({ providedIn: 'root' })
export class ProduitStatService {
  private readonly resourceUrl = SERVER_API_URL + 'api/produits/stat';
  private readonly http = inject(HttpClient);

  fetchPoduitCa(venteRecordParam: VenteRecordParam): Observable<HttpResponse<ProductStatRecord[]>> {
    const options = createRequestOptions(venteRecordParam);
    return this.http.get<ProductStatRecord[]>(`${this.resourceUrl}/ca`, {
      params: options,
      observe: 'response',
    });
  }

  fetch20x80(venteRecordParam: VenteRecordParam): Observable<HttpResponse<ProductStatParetoRecord[]>> {
    const options = createRequestOptions(venteRecordParam);
    return this.http.get<ProductStatParetoRecord[]>(`${this.resourceUrl}/vingt-quantre-vingt`, {
      params: options,
      observe: 'response',
    });
  }

  fetchTransactions(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<ProduitAuditingState[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<ProduitAuditingState[]>(`${this.resourceUrl}/transactions`, {
      params: options,
      observe: 'response',
    });
  }

  fetchTransactionsSum(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<ProduitAuditingSum[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<ProduitAuditingSum[]>(`${this.resourceUrl}/transactions/sum`, {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    return this.http.post(`${this.resourceUrl}/transactions/pdf`, produitAuditingParam, { responseType: 'blob' });
  }

  getProduitHistoriqueVente(produitAuditingParam: any): Observable<HttpResponse<HistoriqueProduitVente[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVente[]>(`${this.resourceUrl}/historique-vente`, {
      params: options,
      observe: 'response',
    });
  }

  getProduitHistoriqueAchat(produitAuditingParam: any): Observable<HttpResponse<HistoriqueProduitAchats[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitAchats[]>(`${this.resourceUrl}/historique-achat`, {
      params: options,
      observe: 'response',
    });
  }

  getProduitHistoriqueAchatMensuelle(produitAuditingParam: any): Observable<HttpResponse<HistoriqueProduitDonneesMensuelles[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitDonneesMensuelles[]>(`${this.resourceUrl}/historique-achat-mensuelle`, {
      params: options,
      observe: 'response',
    });
  }

  getProduitHistoriqueVenteMensuelle(produitAuditingParam: any): Observable<HttpResponse<HistoriqueProduitDonneesMensuelles[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitDonneesMensuelles[]>(`${this.resourceUrl}/historique-vente-mensuelle`, {
      params: options,
      observe: 'response',
    });
  }

  getHistoriqueAchatSummary(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<HistoriqueProduitAchatsSummary>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitAchatsSummary>(`${this.resourceUrl}/historique-achat-summary`, {
      params: options,
      observe: 'response',
    });
  }

  getHistoriqueVenteSummary(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<HistoriqueProduitVenteSummary>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVenteSummary>(`${this.resourceUrl}/historique-vente-summary`, {
      params: options,
      observe: 'response',
    });
  }

  getHistoriqueVenteMensuelleSummary(
    produitAuditingParam: ProduitAuditingParam,
  ): Observable<HttpResponse<HistoriqueProduitVenteMensuelleSummary>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVenteMensuelleSummary>(`${this.resourceUrl}/historique-vente-mensuelle-summary`, {
      params: options,
      observe: 'response',
    });
  }

  exportHistoriqueVenteToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get(`${this.resourceUrl}/historique-vente/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }

  exportHistoriqueAchatToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get(`${this.resourceUrl}/historique-achat/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }

  exportHistoriqueVenteMensuelleToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get(`${this.resourceUrl}/historique-vente-mensuelle/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }

  exportHistoriqueAchatMensuelToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get(`${this.resourceUrl}/historique-achat-mensuelle/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }
}
