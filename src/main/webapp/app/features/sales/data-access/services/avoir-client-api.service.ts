import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';

export interface IAvoirClient {
  saleId?: number;
  saleDate?: string;
  numberTransaction?: string;
  customerName?: string;
  sellerName?: string;
  salesLineId?: number;
  produitLibelle?: string;
  codeCip?: string;
  quantityAvoir?: number;
  regularUnitPrice?: number;
  netUnitPrice?: number;
  montantAvoir?: number;
  updatedAt?: string;
}

export type AvoirClientStatut = 'OUVERT' | 'CLOTURE' | 'ANNULE' | 'EXPIRE';
export type ModeClotureAvoir =
  | 'REMBOURSEMENT_ESPECES'
  | 'REMBOURSEMENT_CB'
  | 'BON_AVOIR'
  | 'RETOUR_PRODUIT'
  | 'COMPENSATION_VENTE';

export interface IAvoirClientDocument {
  id?: number;
  reference?: string;
  createdAt?: string;
  clotureLe?: string;
  statut?: AvoirClientStatut;
  modeCloture?: ModeClotureAvoir;
  quantite?: number;
  montant?: number;
  montantUtilise?: number;
  montantRestant?: number;
  dateExpiration?: string;
  procheExpiration?: boolean;
  commentaire?: string;
  customerName?: string;
  produitLibelle?: string;
  codeCip?: string;
  salesLineId?: number;
  salesLineDate?: string;
  numberTransaction?: string;
  commandeReference?: string;
  closedByName?: string;
}

export interface CloturerAvoirRequest {
  modeCloture: ModeClotureAvoir;
  commentaire?: string;
  montantUtilise?: number;
}

@Injectable({ providedIn: 'root' })
export class AvoirClientApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales/avoirs';
  private readonly http = inject(HttpClient);

  query(req?: any): Observable<HttpResponse<IAvoirClient[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IAvoirClient[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  queryDocuments(req?: any): Observable<HttpResponse<IAvoirClientDocument[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IAvoirClientDocument[]>(`${this.resourceUrl}/documents`, {
      params: options,
      observe: 'response',
    });
  }

  cloturerAvoir(id: number, request: CloturerAvoirRequest): Observable<HttpResponse<IAvoirClientDocument>> {
    return this.http.post<IAvoirClientDocument>(
      `${this.resourceUrl}/documents/${id}/cloturer`,
      request,
      { observe: 'response' }
    );
  }
}
