import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IProduit } from 'app/shared/model/produit.model';
import { ISubstitut } from 'app/shared/model/substitut.model';
import { IProduitIndicateurs } from '../../models/produit-indicateurs.model';
import { IVenteMois } from '../../models/vente-mois.model';

export interface ILotPeremption {
  id?: number;
  numLot?: string;
  datePeremption?: string;
  quantity?: number;
  peremptionStatut?: { libelle: string; days: number; mouths: number; years: number };
}

@Injectable({ providedIn: 'root' })
export class ProductsApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/produits';

  query(req?: any): Observable<HttpResponse<IProduit[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IProduit[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  getById(id: number): Observable<IProduit> {
    return this.http
      .get<IProduit>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => res.body!));
  }

  getIndicateurs(id: number): Observable<IProduitIndicateurs> {
    return this.http
      .get<IProduitIndicateurs>(`${this.resourceUrl}/${id}/indicateurs`, { observe: 'response' })
      .pipe(map(res => res.body!));
  }

  getVentesMensuelles(id: number, nbMois = 12): Observable<IVenteMois[]> {
    return this.http
      .get<IVenteMois[]>(`${this.resourceUrl}/${id}/ventes-mensuelles`, {
        params: { nbMois: nbMois.toString() },
        observe: 'response',
      })
      .pipe(map(res => res.body ?? []));
  }

  getGeneriques(id: number): Observable<ISubstitut[]> {
    return this.http
      .get<ISubstitut[]>(`${this.resourceUrl}/${id}/generiques`, { observe: 'response' })
      .pipe(map(res => res.body ?? []));
  }

  getEtiquettes(id: number, qty = 1, startAt = 1): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes`, {
      params: { qty: qty.toString(), startAt: startAt.toString() },
      responseType: 'blob',
    });
  }

  patchStatus(id: number, status: 'ENABLE' | 'DISABLE'): Observable<void> {
    return this.http
      .patch<void>(`${this.resourceUrl}/${id}/status`, null, {
        params: { status },
        observe: 'body',
      });
  }

  getLots(produitId: number, dayCount = 180): Observable<ILotPeremption[]> {
    return this.http
      .get<ILotPeremption[]>(`${SERVER_API_URL}api/lot`, {
        params: { produitId: produitId.toString(), dayCount: dayCount.toString() },
        observe: 'response',
      })
      .pipe(map(res => res.body ?? []));
  }
}
