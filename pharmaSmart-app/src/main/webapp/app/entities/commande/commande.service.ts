import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { SERVER_API_URL } from "app/app.constants";
import { createRequestOptions } from "app/shared/util/request-util";
import { ICommande } from "app/shared/model/commande.model";
import { IOrderLine } from "../../shared/model/order-line.model";
import { IResponseCommande } from "../../shared/model/response-commande.model";
import { ICommandeResponse } from "../../shared/model/commande-response.model";
import { CommandeId } from "../../shared/model/abstract-commande.model";
import { OrderLineId } from "../../shared/model/abstract-order-item.model";

export interface IPriceHistory {
  id: number;
  oldPrixAchat: number;
  newPrixAchat: number;
  oldPrixUni: number;
  newPrixUni: number;
  changedAt: string;
  receiptReference: string | null;
  changedBy: string | null;
}

export interface IPutawayPreviewItem {
  produitId: number;
  produitLibelle: string;
  codeCip: string;
  qtyRayon: number;
  stockMaxiRayon: number;
  qtyOverflow: number;
  qtyReserveActuelle: number;
  classePareto?: string;
}

export interface ICommandeResumee {
  id: number;
  orderDate: string;
  orderReference: string | null;
  fournisseurLibelle: string;
  grossAmount: number;
  orderStatus: string;
  reliquatDeCommandeId: number | null;
}

export interface IPharmaMlEnvoiResumee {
  id: number;
  commandeId: number;
  commandeOrderDate: string;
  commandeRef: string | null;
  fournisseurLibelle: string;
  statut: string;
  createdAt: string;
}

export interface ICommandeLiteDTO {
  id: number;
  orderReference: string | null;
}

export interface IFournisseurStatsService {
  tauxService: number;
  delaiMoyen: number;
  periodeJours: number;
}

export interface ICommandeDashboard {
  totalRequested: number;
  totalReceived: number;
  totalPharmamlPending: number;
  commandesRequested: ICommandeResumee[];
  commandesReceived: ICommandeResumee[];
  envoisPending: IPharmaMlEnvoiResumee[];
}

type EntityResponseType = HttpResponse<ICommande>;
type EntityArrayResponseType = HttpResponse<ICommande[]>;

@Injectable({ providedIn: "root" })
export class CommandeService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + "api/commandes";

  createCommandeRapide(totalQuantity: number, fournisseurProduitId: number, quantityRequested: number): Observable<HttpResponse<ICommande>> {
    return this.http.post<ICommande>(`${this.resourceUrl}/rapide`, {
      totalQuantity,
      fournisseurProduitId,
      quantityRequested
    }, { observe: "response" });
  }

  create(commande: ICommande): Observable<EntityResponseType> {
    return this.http.post<ICommande>(this.resourceUrl, commande, { observe: "response" });
  }

  update(commande: ICommande): Observable<EntityResponseType> {
    return this.http.put<ICommande>(this.resourceUrl, commande, { observe: "response" });
  }

  find(commandeId: CommandeId): Observable<EntityResponseType> {
    return this.http.get<ICommande>(`${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}`, { observe: "response" });
  }

  delete(commandeId: CommandeId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}`, { observe: "response" });
  }

  createOrUpdateOrderLine(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .post<ICommande>(this.resourceUrl + "/add-order-line", orderLine, { observe: "response" })
      ;
  }

  updateQuantityRequested(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + "/update-order-line-quantity-requested", orderLine, { observe: "response" })
      ;
  }

  updateOrderCostAmount(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + "/update-order-line-cost-amount", orderLine, { observe: "response" })
      ;
  }

  updateOrderUnitPrice(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + "/update-order-line-unit-price", orderLine, { observe: "response" })
      ;
  }

  deleteOrderLineById(orderLineId: OrderLineId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/order-line/${orderLineId.id}/${orderLineId.orderDate}`, { observe: "response" });
  }

  deleteOrderLinesByIds(commandeId: CommandeId, ids: OrderLineId[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete/order-lines/${commandeId.id}/${commandeId.orderDate}`, ids, { observe: "response" });
  }

  updateCip(orderLine: IOrderLine): Observable<HttpResponse<{}>> {
    return this.http.put<IOrderLine>(this.resourceUrl + "/update-provisional-cip", orderLine, { observe: "response" });
  }

  exportToCsv(commandeId: CommandeId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${commandeId.id}/${commandeId.orderDate}`, { responseType: "blob" });
  }

  exportToPdf(commandeId: CommandeId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${commandeId.id}/${commandeId.orderDate}`, { responseType: "blob" });
  }

  filterCommandeLines(req?: any): Observable<HttpResponse<IOrderLine[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IOrderLine[]>(`${this.resourceUrl}/filter-order-lines`, {
      params: options,
      observe: "response"
    });
  }

  fetchOrderLinesByCommandeId(commandeId: CommandeId): Observable<HttpResponse<IOrderLine[]>> {
    return this.http.get<IOrderLine[]>(`${this.resourceUrl}/pageable-order-lines/${commandeId.id}/${commandeId.orderDate}`, {
      params: { size: "99999", sort: "fournisseurProduit_produit_libelle" },
      observe: "response"
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ICommande[]>(this.resourceUrl + "/commandes-without-order-lines", {
      params: options,
      observe: "response"
    });
  }

  fusionner(ids: CommandeId[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/fusionner`, ids, { observe: "response" });
  }

  deleteSelectedCommandes(ids: CommandeId[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete-commandes`, ids, { observe: "response" });
  }

  importerReponseCommande(commandeId: CommandeId, model: string, file: any): Observable<HttpResponse<IResponseCommande>> {
    return this.http.post<IResponseCommande>(
      `${this.resourceUrl}/verification-commande-en-cours/${commandeId.id}/${commandeId.orderDate}/${model}`,
      file,
      { observe: 'response' }
    );
  }

  uploadNewCommande(fournisseurId: number, model: string, file: any): Observable<HttpResponse<ICommandeResponse>> {
    return this.http.post<ICommandeResponse>(`${this.resourceUrl}/upload-new-commande/${fournisseurId}/${model}`, file, {
      observe: "response"
    });
  }

  getRuptureCsv(reference: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/rupture-csv/${reference}`, { responseType: "blob" });
  }

  updateQuantityReceived(orderLine: IOrderLine): Observable<{}> {
    return this.http.put(this.resourceUrl + "/update-order-line-quantity-received", orderLine, {
      observe: "response"
    });
  }

  updateQuantityUG(orderLine: IOrderLine): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + "/update-order-line-quantity-ug", orderLine, { observe: "response" });
  }

  updateOrderUnitPriceOnStockEntry(orderLine: IOrderLine): Observable<{}> {
    return this.http.put<IOrderLine>(this.resourceUrl + "/update-order-line-unit-price", orderLine, { observe: "response" });
  }

  test(req: any): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/test-csv`, { params: options, observe: "response" });
  }

  sauvegarderSaisieEntreeStock(commande: ICommande): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(`${this.resourceUrl}/entree-stock/save`, commande, { observe: "response" })
      ;
  }

  changeGrossiste(commande: ICommande): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/change-grossiste`, commande, { observe: "response" });
  }

  /**
   * Vérifie côté serveur les lignes dont la variation de prix dépasse le seuil configuré
   * (APP_SEUIL_VARIATION_PRIX). Opère sur TOUTES les lignes, sans dépendre de la pagination frontend.
   */
  getPriceHistory(fournisseurProduitId: number): Observable<IPriceHistory[]> {
    return this.http.get<IPriceHistory[]>(`${this.resourceUrl}/entree-stock/prix-historique/${fournisseurProduitId}`);
  }

  checkPriceVariation(commandeId: CommandeId): Observable<HttpResponse<IOrderLine[]>> {
    return this.http.get<IOrderLine[]>(`${this.resourceUrl}/entree-stock/check-price-variation`, {
      params: { commandeId: commandeId.id, orderDate: commandeId.orderDate },
      observe: "response"
    });
  }

  getPutawayPreview(commandeId: CommandeId): Observable<IPutawayPreviewItem[]> {
    return this.http.get<IPutawayPreviewItem[]>(`${this.resourceUrl}/entree-stock/putaway-preview/${commandeId.id}/${commandeId.orderDate}`);
  }

  getDashboard(): Observable<ICommandeDashboard> {
    return this.http.get<ICommandeDashboard>(`${this.resourceUrl}/dashboard`);
  }

  getStatsService(fournisseurId: number, periodeJours = 30): Observable<IFournisseurStatsService> {
    return this.http.get<IFournisseurStatsService>(
      `${this.resourceUrl}/fournisseurs/${fournisseurId}/stats-service`,
      { params: { periodeJours: String(periodeJours) } }
    );
  }

  createReliquat(commandeId: CommandeId): Observable<HttpResponse<ICommandeLiteDTO>> {
    return this.http.post<ICommandeLiteDTO>(
      `${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}/reliquat`,
      null,
      { observe: "response" }
    );
  }

  importSuggestionIntoCommande(commandeId: CommandeId, suggestionId: number): Observable<HttpResponse<{}>> {
    return this.http.post(
      `${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}/import-suggestion`,
      null,
      { observe: "response", params: { suggestionId } }
    );
  }

  importSuggestionLinesIntoCommande(commandeId: CommandeId, suggestionId: number, lineIds: number[]): Observable<HttpResponse<{}>> {
    return this.http.post(
      `${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}/import-suggestion-lines`,
      lineIds,
      { observe: "response", params: { suggestionId } }
    );
  }


  findSaisieEntreeStock(commandeId: CommandeId): Observable<EntityResponseType> {
    return this.http
      .get<ICommande>(`${this.resourceUrl}/entree-stock/${commandeId.id}/${commandeId.orderDate}`, { observe: "response" })
      ;
  }

}
