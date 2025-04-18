import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { VenteRecordParam } from '../../../shared/model/vente-record-param.model';
import {
  HistoriqueProduitAchats,
  HistoriqueProduitVDonneesMensuelles,
  HistoriqueProduitVente,
  ProductStatRecord,
  ProduitAuditingParam,
  ProduitAuditingState,
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

  fetch20x80(venteRecordParam: VenteRecordParam): Observable<HttpResponse<ProductStatRecord[]>> {
    const options = createRequestOptions(venteRecordParam);
    return this.http.get<ProductStatRecord[]>(`${this.resourceUrl}/vingt-quantre-vingt`, {
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

  exportToPdf(produitAuditingParam: ProduitAuditingParam): Observable<Blob> {
    return this.http.post(`${this.resourceUrl}/transactions/pdf`, produitAuditingParam, { responseType: 'blob' });
  }

  getProduitHistoriqueVente(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<HistoriqueProduitVente[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVente[]>(`${this.resourceUrl}/historique-vente`, {
      params: options,
      observe: 'response',
    });
  }

  getProduitHistoriqueAchat(produitAuditingParam: ProduitAuditingParam): Observable<HttpResponse<HistoriqueProduitAchats[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitAchats[]>(`${this.resourceUrl}/historique-achat`, {
      params: options,
      observe: 'response',
    });
  }

  getProduitHistoriqueAchatMensuelle(
    produitAuditingParam: ProduitAuditingParam,
  ): Observable<HttpResponse<HistoriqueProduitVDonneesMensuelles[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVDonneesMensuelles[]>(`${this.resourceUrl}/historique-achat-mensuelle`, {
      params: options,
      observe: 'response',
    });
  }
  getProduitHistoriqueVenteMensuelle(
    produitAuditingParam: ProduitAuditingParam,
  ): Observable<HttpResponse<HistoriqueProduitVDonneesMensuelles[]>> {
    const options = createRequestOptions(produitAuditingParam);
    return this.http.get<HistoriqueProduitVDonneesMensuelles[]>(`${this.resourceUrl}/historique-vente-mensuelle`, {
      params: options,
      observe: 'response',
    });
  }
}
