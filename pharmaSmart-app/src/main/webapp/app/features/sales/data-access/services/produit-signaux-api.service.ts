import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SERVER_API_URL} from '../../../../app.constants';

/** Ce que le comptoir doit savoir d'un produit du panier (mêmes règles que la recherche produit). */
export interface ProduitSignaux {
  produitId: number;
  statutLegal: string | null;
  typeGenerique: string | null;
  peremptionLot: string | null;
  peremptionDate: string | null;
  /** AAAA-MM-JJ : au-delà, un lot n'est plus « proche de sa péremption » (seuil de configuration). */
  dateLimitePeremption: string;
  /** Stock restant (rayon + réserve) et seuil mini du produit ; seuil 0 : pas d'alerte. */
  stockRestant: number;
  seuilMini: number;
}

@Injectable({providedIn: 'root'})
export class ProduitSignauxApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/sales/produits-signaux';

  chargerSignaux(produitIds: number[]): Observable<ProduitSignaux[]> {
    return this.http.get<ProduitSignaux[]>(this.resourceUrl, {params: new HttpParams().set('ids', produitIds.join(','))});
  }
}
