import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';

export type MotifRetourClient =
  | 'ERREUR_DISPENSATION'
  | 'PRODUIT_DEFECTUEUX'
  | 'ERREUR_QUANTITE'
  | 'INSATISFACTION'
  | 'AUTRE';

export type ModeReglementRetour =
  | 'REMBOURSEMENT_ESPECES'
  | 'REMBOURSEMENT_CB'
  | 'AVOIR_CLIENT';

export interface ISaleLineForRetour {
  salesLineId?: number;
  salesLineDate?: string;
  produitLibelle?: string;
  codeCip?: string;
  quantitySold?: number;
  netUnitPrice?: number;
  quantiteRetour?: number;
}

export interface ISaleForRetour {
  saleId?: number;
  saleDate?: string;
  numberTransaction?: string;
  customerName?: string;
  lines?: ISaleLineForRetour[];
}

export interface IRetourClientLine {
  id?: number;
  produitLibelle?: string;
  codeCip?: string;
  quantite?: number;
  prixUnitaire?: number;
  montant?: number;
}

export interface IRetourClient {
  id?: number;
  reference?: string;
  createdAt?: string;
  motif?: MotifRetourClient;
  modeReglement?: ModeReglementRetour;
  commentaire?: string;
  montantTotal?: number;
  customerName?: string;
  originalSaleRef?: string;
  originalSaleDate?: string;
  createdByName?: string;
  lines?: IRetourClientLine[];
}

export interface RetourClientRequest {
  saleId: number;
  saleDate: string;
  motif: MotifRetourClient;
  modeReglement: ModeReglementRetour;
  commentaire?: string;
  lines: { salesLineId: number; salesLineDate: string; quantite: number }[];
}

@Injectable({ providedIn: 'root' })
export class RetourClientApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales/retours';
  private readonly http = inject(HttpClient);

  findSaleByRef(ref: string): Observable<ISaleForRetour> {
    return this.http.get<ISaleForRetour>(`${this.resourceUrl}/sale`, { params: { ref } });
  }

  query(req?: any): Observable<HttpResponse<IRetourClient[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IRetourClient[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  validerRetour(request: RetourClientRequest): Observable<IRetourClient> {
    return this.http.post<IRetourClient>(this.resourceUrl, request);
  }
}
