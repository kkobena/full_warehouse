import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';

export type MotifRetourClient = 'ERREUR_DISPENSATION' | 'PRODUIT_DEFECTUEUX' | 'ERREUR_QUANTITE' | 'INSATISFACTION' | 'AUTRE';

export type ModeReglementRetour = 'REMBOURSEMENT_ESPECES' | 'REMBOURSEMENT_CB' | 'AVOIR_CLIENT';

export type StatutLegal = 'SANS_LISTE' | 'LISTE_I' | 'LISTE_II' | 'STUPEFIANTS' | 'PSO';

export interface ISaleLineForRetour {
  salesLineId?: number;
  salesLineDate?: string;
  produitLibelle?: string;
  codeCip?: string;
  quantitySold?: number;
  netUnitPrice?: number;
  statutLegal?: StatutLegal;
  retourInterdit?: boolean;
  thermosensible?: boolean;
  /** Pour toute la quantité vendue ; une ligne en promotion, remise déduite. */
  montantRemboursableClient?: number;
  montantTp?: number;
  /** Une ligne en promotion se retourne en entier ou pas du tout. */
  promotionId?: number | null;
  promotionLibelle?: string | null;
  /** Un pack se retourne avec tous ses articles. */
  pack?: boolean;
  /** Pourquoi la ligne en promotion ne se retourne pas (vente à crédit, cumul) : annuler la vente et la ressaisir. */
  motifRefus?: string | null;
  quantiteRetour?: number;
}

/** Ni interdite par son statut légal, ni refusée parce qu'en promotion. */
export function estRetournable(l: ISaleLineForRetour): boolean {
  return !l.retourInterdit && !l.motifRefus;
}

/** Ce que rend la quantité retournée : le montant remboursable vaut pour toute la quantité vendue. */
export function montantRetour(l: ISaleLineForRetour): number {
  const vendue = l.quantitySold ?? 0;
  const retour = l.quantiteRetour ?? 0;
  if (!vendue || !retour) {
    return 0;
  }
  const ligne = l.montantRemboursableClient ?? (l.netUnitPrice ?? 0) * vendue;
  return Math.round((ligne * retour) / vendue);
}

export interface ISaleForRetour {
  saleId?: number;
  saleDate?: string;
  numberTransaction?: string;
  customerName?: string;
  ancienneteJours?: number;
  depasseDelai?: boolean;
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
  montantTpTotal?: number;
  customerName?: string;
  originalSaleRef?: string;
  originalSaleDate?: string;
  createdByName?: string;
  lines?: IRetourClientLine[];
  avecEchange?: boolean;
  echangeSaleRef?: string;
}

export interface IEchangeContext {
  customerId?: number;
  customerName?: string;
  montantCredit?: number;
  retourId?: number;
  retourReference?: string;
  avoirReferences?: string[];
}

export interface IRetourLigneRejetee {
  produitLibelle?: string;
  codeCip?: string;
  quantite?: number;
  statutLegal?: StatutLegal;
  raison?: string;
}

export interface IRetourClientResult {
  retour?: IRetourClient;
  lignesRejetees?: IRetourLigneRejetee[];
  lignesNonRestockees?: IRetourLigneRejetee[];
  partiel?: boolean;
  echangeContext?: IEchangeContext;
}

export interface RetourLineRequest {
  salesLineId: number;
  salesLineDate: string;
  quantite: number;
  emballageIntact?: boolean;
  numLotLisible?: boolean;
  datePeremptionValide?: boolean;
}

export interface RetourClientRequest {
  saleId: number;
  saleDate: string;
  motif: MotifRetourClient;
  modeReglement: ModeReglementRetour;
  avecEchange?: boolean;
  commentaire?: string;
  lines: RetourLineRequest[];
}

@Injectable({ providedIn: 'root' })
export class RetourClientApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/sales/retours';
  private readonly http = inject(HttpClient);

  findSaleByRef(ref: string): Observable<ISaleForRetour> {
    return this.http.get<ISaleForRetour>(`${this.resourceUrl}/sale`, { params: { ref } });
  }

  findSaleById(id: number, saleDate: string): Observable<ISaleForRetour> {
    return this.http.get<ISaleForRetour>(`${this.resourceUrl}/sale/${id}`, { params: { saleDate } });
  }

  query(req?: any): Observable<HttpResponse<IRetourClient[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IRetourClient[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  validerRetour(request: RetourClientRequest): Observable<IRetourClientResult> {
    return this.http.post<IRetourClientResult>(this.resourceUrl, request);
  }

  lierVenteEchange(retourId: number, saleRef: string): Observable<IRetourClient> {
    return this.http.patch<IRetourClient>(`${this.resourceUrl}/${retourId}/echange-sale`, { saleRef });
  }
}
