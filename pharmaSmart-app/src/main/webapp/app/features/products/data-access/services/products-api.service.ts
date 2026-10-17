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
import { IProduitMergePreview, IProduitMergeRequest, IProduitMergeResult } from "../../models/produit-merge.model";
import { IProductResolveResult, IProduitCodeAlias } from "app/shared/model/reception-scan-result.model";

export interface IDeconditionRecord {
  id?: number;
  qtyMvt?: number;
  dateMtv?: string;
  stockBefore?: number;
  stockAfter?: number;
  user?: { firstName?: string; lastName?: string; login?: string };
}

/** Un format de planche du catalogue serveur. */
export type SupportEtiquette = "PLANCHE_A4" | "ROULEAU";

/** Étiquette de préparation magistrale, saisie au moment de l'impression. */
export interface IEtiquettePreparation {
  libelle: string;
  composition: string;
  patient?: string | null;
  prescripteur?: string | null;
  posologie?: string | null;
  datePreparation?: string | null;
  peremption?: string | null;
  mention?: string | null;
}

export interface IFormatEtiquette {
  code: string;
  libelle: string;
  reference: string;
  /** Une planche a une grille et une case de depart ; un rouleau, ni l'une ni l'autre. */
  support: SupportEtiquette;
  supportLibelle: string;
  largeurMm: number;
  hauteurMm: number;
  colonnes: number;
  rangees: number;
  capacite: number;
  parDefaut: boolean;
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

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: "response" });
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

  /** Codes alternatifs d'un produit, du plus récent au plus ancien. */
  getCodesAlias(id: number): Observable<IProduitCodeAlias[]> {
    return this.http
      .get<IProduitCodeAlias[]>(`${this.resourceUrl}/${id}/codes-alias`, { observe: "response" })
      .pipe(map(res => res.body ?? []));
  }

  /**
   * Mémorise un code scanné pour ce produit : les scans suivants le résoudront sans
   * intervention. Le type du code est déduit côté serveur.
   */
  createCodeAlias(id: number, code: string): Observable<IProduitCodeAlias> {
    return this.http
      .post<IProduitCodeAlias>(`${this.resourceUrl}/${id}/codes-alias`, { code }, { observe: "response" })
      .pipe(map(res => res.body!));
  }

  deleteCodeAlias(aliasId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/codes-alias/${aliasId}`, { observe: "response" })
      .pipe(map((): void => undefined));
  }

  /** Produits portant ce code, codes alternatifs compris. Vide = aucune correspondance. */
  resoudreCode(code: string): Observable<IProductResolveResult[]> {
    return this.http
      .get<IProductResolveResult[]>(`${this.resourceUrl}/resoudre-code`, {
        params: { code },
        observe: "response"
      })
      .pipe(map(res => res.body ?? []));
  }

  getEtiquettes(id: number, qty = 1, startAt = 1, format?: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes`, {
      params: { qty: qty.toString(), startAt: startAt.toString(), ...(format ? { format } : {}) },
      responseType: "blob"
    });
  }

  /** Catalogue des formats de planche. Lu au serveur, jamais recopié dans le bundle. */
  /** La même étiquette en ZPL : du texte, envoyé tel quel à l'imprimante à rouleau. */
  getEtiquettesZpl(id: number, qty = 1, format?: string, resolution?: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes/zpl`, {
      params: { qty: qty.toString(), ...(format ? { format } : {}), ...(resolution ? { resolution } : {}) },
      responseType: "text"
    });
  }

  /** Étiquette de lot en ZPL : le GS1 DataMatrix est tracé par l'imprimante. */
  getEtiquettesLotZpl(id: number, lotId: number, qty = 1, format?: string, resolution?: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/${id}/lots/${lotId}/etiquettes/zpl`, {
      params: { qty: qty.toString(), ...(format ? { format } : {}), ...(resolution ? { resolution } : {}) },
      responseType: "text"
    });
  }

  /** Réglette de gondole : le prix en gros, le code en petit — elle se colle sur l'étagère. */
  getReglettes(id: number, qty = 1, startAt = 1, format?: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes/reglette`, {
      params: { qty: qty.toString(), startAt: startAt.toString(), ...(format ? { format } : {}) },
      responseType: "blob"
    });
  }

  getReglettesZpl(id: number, qty = 1, format?: string, resolution?: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes/reglette/zpl`, {
      params: { qty: qty.toString(), ...(format ? { format } : {}), ...(resolution ? { resolution } : {}) },
      responseType: "text"
    });
  }

  /** Préparation magistrale : le contenu est saisi à l'impression, rien n'est conservé. */
  imprimerPreparation(saisie: IEtiquettePreparation, qty = 1, startAt = 1, format?: string): Observable<Blob> {
    return this.http.post(`${SERVER_API_URL}api/etiquettes/preparation`, saisie, {
      params: { qty: qty.toString(), startAt: startAt.toString(), ...(format ? { format } : {}) },
      responseType: "blob"
    });
  }

  imprimerPreparationZpl(saisie: IEtiquettePreparation, qty = 1, format?: string, resolution?: string): Observable<string> {
    return this.http.post(`${SERVER_API_URL}api/etiquettes/preparation/zpl`, saisie, {
      params: { qty: qty.toString(), ...(format ? { format } : {}), ...(resolution ? { resolution } : {}) },
      responseType: "text"
    });
  }

  listerFormatsEtiquette(): Observable<IFormatEtiquette[]> {
    return this.http.get<IFormatEtiquette[]>(`${SERVER_API_URL}api/etiquettes/formats`);
  }

  /** Second type d'étiquette : celle d'un lot, avec sa péremption et un GS1 DataMatrix. */
  getEtiquettesLot(id: number, lotId: number, qty = 1, startAt = 1, format?: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/lots/${lotId}/etiquettes`, {
      params: { qty: qty.toString(), startAt: startAt.toString(), ...(format ? { format } : {}) },
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

  patchFlag(id: number, flag: 'THERMOSENSIBLE' | 'MEDICAMENT_ESSENTIEL' | 'PRODUIT_GARDE' | 'CLASSIFICATION_OVERRIDDEN', value: boolean): Observable<void> {
    return this.http.patch<void>(`${this.resourceUrl}/${id}/flags`, null, {
      params: { flag, value: String(value) }
    });
  }

  previewMerge(targetId: number, sourceIds: number[]): Observable<IProduitMergePreview> {
    return this.http
      .post<IProduitMergePreview>(`${this.resourceUrl}/merge/preview`, sourceIds, {
        params: { targetId: targetId.toString() },
        observe: "response"
      })
      .pipe(map(res => res.body!));
  }

  confirmMerge(request: IProduitMergeRequest): Observable<IProduitMergeResult> {
    return this.http
      .post<IProduitMergeResult>(`${this.resourceUrl}/merge`, request, { observe: "response" })
      .pipe(map(res => res.body!));
  }
}
