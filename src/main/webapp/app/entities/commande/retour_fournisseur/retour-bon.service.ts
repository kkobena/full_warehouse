import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { IRetourBonGroupe } from 'app/shared/model/retour-bon-groupe.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';
import {
  RetourBonBatchResult,
  RetourBonLotResolution,
  RetourFournisseurBatchRequest,
  RetourFournisseurRequest,
} from 'app/entities/gestion-peremption/model/retour-fournisseur-request';

type EntityResponseType = HttpResponse<IRetourBon>;
type EntityArrayResponseType = HttpResponse<IRetourBon[]>;

@Injectable({ providedIn: 'root' })
export class RetourBonService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/retour-bons';

  create(retourBon: IRetourBon): Observable<EntityResponseType> {
    return this.http.post<IRetourBon>(this.resourceUrl, retourBon, { observe: 'response' });
  }

  update(id: number, retourBon: IRetourBon): Observable<EntityResponseType> {
    return this.http.put<IRetourBon>(`${this.resourceUrl}/${id}`, retourBon, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
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

  getPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/pdf`, { responseType: 'blob' });
  }

  markAsProcessing(id: number): Observable<EntityResponseType> {
    return this.http.patch<IRetourBon>(`${this.resourceUrl}/${id}/processing`, null, { observe: 'response' });
  }

  sendEdi(id: number): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/${id}/send-edi`, null, { observe: 'response' });
  }

  createFromExpiredLot(request: RetourFournisseurRequest): Observable<HttpResponse<IRetourBon>> {
    return this.http.post<IRetourBon>(`${this.resourceUrl}/from-expired-lot`, request, { observe: 'response' });
  }

  createFromExpiredLots(request: RetourFournisseurBatchRequest): Observable<HttpResponse<RetourBonBatchResult>> {
    return this.http.post<RetourBonBatchResult>(`${this.resourceUrl}/from-expired-lots`, request, { observe: 'response' });
  }

  /**
   * Pré-résout un lot pour déterminer l'état initial du formulaire "Retour fournisseur".
   * @param lotId l'identifiant du lot
   */
  resolveLot(lotId: number): Observable<HttpResponse<RetourBonLotResolution>> {
    return this.http.get<RetourBonLotResolution>(`${this.resourceUrl}/resolution-lot`, {
      params: { lotId: lotId.toString() },
      observe: 'response',
    });
  }

  closeManually(id: number): Observable<EntityResponseType> {
    return this.http.patch<IRetourBon>(`${this.resourceUrl}/${id}/close-manually`, null, { observe: 'response' });
  }

  retourCompletCommande(request: { commandeId: number; commandeOrderDate: string; motifRetourId: number; commentaire?: string }): Observable<EntityResponseType> {
    return this.http.post<IRetourBon>(`${this.resourceUrl}/retour-complet`, request, { observe: 'response' });
  }

  getGroupedByFournisseur(): Observable<HttpResponse<IRetourBonGroupe[]>> {
    return this.http.get<IRetourBonGroupe[]>(`${this.resourceUrl}/grouped-by-fournisseur`, { observe: 'response' });
  }

  exportGroupe(ids: number[]): Observable<Blob> {
    return this.http.post(`${this.resourceUrl}/export-groupe`, ids, { responseType: 'blob' });
  }

  /**
   * Exporte les bons de retour en Excel ou CSV selon les filtres actifs.
   * @param format 'excel' ou 'csv'
   * @param params filtres : statut, dtStart, dtEnd, search
   */
  export(format: 'excel' | 'csv', params: Record<string, string>): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/export`, {
      params: { format, ...params },
      responseType: 'blob',
    });
  }
}


