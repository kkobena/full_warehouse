import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { SERVER_API_URL } from "app/app.constants";
import { createRequestOptions } from "app/shared/util/request-util";
import { IProduit } from "app/shared/model/produit.model";
import { ISubstitut } from "app/shared/model/substitut.model";
import { IProduitIndicateurs } from "../../models/produit-indicateurs.model";
import { IVenteMois } from "../../models/vente-mois.model";
import { ILot } from "app/shared/model/lot.model";

export interface IDeconditionRecord {
  id?: number;
  qtyMvt?: number;
  dateMtv?: string;
  stockBefore?: number;
  stockAfter?: number;
  user?: { firstName?: string; lastName?: string; login?: string };
}

export interface ILotProduit {
  freeQty?: number;
  ugQuantityReceived?: number;
  quantityReceived?: number;
  currentQuantity?: number;
  createdDate?: Date;
  manufacturingDate?: Date;
  expiryDate?: Date;
  serialNumber?: string;
  id?: number;
  numLot?: string;
  quantity?: number;
  peremptionStatut?: { libelle: string; days: number; mouths: number; years: number };
}

export interface ILotPeremption {
  id?: number;
  numLot?: string;
  datePeremption?: string;
  quantity?: number;
  peremptionStatut?: { libelle: string; days: number; mouths: number; years: number };
}

@Injectable({ providedIn: "root" })
export class ProductsApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + "api/produits";

  query(req?: any): Observable<HttpResponse<IProduit[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IProduit[]>(this.resourceUrl, { params: options, observe: "response" });
  }

  getById(id: number): Observable<IProduit> {
    return this.http
      .get<IProduit>(`${this.resourceUrl}/${id}`, { observe: "response" })
      .pipe(map(res => res.body!));
  }

  getIndicateurs(id: number): Observable<IProduitIndicateurs> {
    return this.http
      .get<IProduitIndicateurs>(`${this.resourceUrl}/${id}/indicateurs`, { observe: "response" })
      .pipe(map(res => res.body!));
  }

  getVentesMensuelles(id: number, nbMois = 12): Observable<IVenteMois[]> {
    return this.http
      .get<IVenteMois[]>(`${this.resourceUrl}/${id}/ventes-mensuelles`, {
        params: { nbMois: nbMois.toString() },
        observe: "response"
      })
      .pipe(map(res => res.body ?? []));
  }

  getGeneriques(id: number): Observable<ISubstitut[]> {
    return this.http
      .get<ISubstitut[]>(`${this.resourceUrl}/${id}/generiques`, { observe: "response" })
      .pipe(map(res => res.body ?? []));
  }

  getEtiquettes(id: number, qty = 1, startAt = 1): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes`, {
      params: { qty: qty.toString(), startAt: startAt.toString() },
      responseType: "blob"
    });
  }

  patchStatus(id: number, status: "ENABLE" | "DISABLE"): Observable<void> {
    return this.http
      .patch<void>(`${this.resourceUrl}/${id}/status`, null, {
        params: { status },
        observe: "body"
      });
  }

  createProduitDetail(produit: IProduit): Observable<HttpResponse<IProduit>> {
    return this.http.post<IProduit>(this.resourceUrl, produit, { observe: "response" });
  }

  updateProduitDetail(produit: IProduit): Observable<HttpResponse<IProduit>> {
    return this.http.put<IProduit>(`${this.resourceUrl}/detail`, produit, { observe: "response" });
  }

  createDecondition(decondition: { qtyMvt: number; produitId: number }): Observable<HttpResponse<unknown>> {
    return this.http.post<unknown>(`${SERVER_API_URL}api/deconditions`, decondition, { observe: "response" });
  }

  getDeconditions(produitId: number): Observable<IDeconditionRecord[]> {
    return this.http
      .get<IDeconditionRecord[]>(`${SERVER_API_URL}api/deconditions`, {
        params: { produitId: produitId.toString() },
        observe: "response"
      })
      .pipe(map(res => res.body ?? []));
  }

  getLots(produitId: number): Observable<ILotProduit[]> {
    return this.http
      .get<ILotProduit[]>(`${SERVER_API_URL}api/lot/produit/${produitId}`, {
        observe: "response"
      })
      .pipe(map(res => res.body ?? []));
  }

  addLotHorsCommande(lot: ILot): Observable<HttpResponse<ILot>> {
    return this.http.post<ILot>(`${SERVER_API_URL}api/lot/add-sur-produit`, lot, { observe: "response" });
  }

  patchGestionLot(id: number, active: boolean): Observable<void> {
    return this.http.patch<void>(`${this.resourceUrl}/${id}/gestion-lot`, null, {
      params: { active: String(active) }
    });
  }
}
