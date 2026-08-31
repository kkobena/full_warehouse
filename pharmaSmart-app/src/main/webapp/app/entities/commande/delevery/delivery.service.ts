import { inject, Injectable, signal, WritableSignal } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { SERVER_API_URL } from "app/app.constants";
import { createRequestOptions } from "app/shared/util/request-util";
import { IDelivery } from "../../../shared/model/delevery.model";
import { ICommandeResponse } from "../../../shared/model/commande-response.model";
import { IDeliveryItem } from "../../../shared/model/delivery-item";
import { CommandeId } from "../../../shared/model/abstract-commande.model";
import { IStockEntryResult } from "../../../shared/model/stock-entry-result.model";
import { IReceptionScanResult } from "../../../shared/model/reception-scan-result.model";

export interface IDeliveryTotals {
  count: number;
  totalGrossAmount: number;
  totalNetAmount: number;
  totalTaxAmount: number;
}

type EntityResponseType = HttpResponse<IDelivery>;
type EntityArrayResponseType = HttpResponse<IDelivery[]>;

@Injectable({ providedIn: "root" })
export class DeliveryService {
  deliveryPreviousActiveNav: WritableSignal<string> = signal<string>("pending");
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + "api/commandes/data/entree-stock";
  private readonly resourceUrlQuery = SERVER_API_URL + "api/commandes";
  private readonly resourceUrl2 = SERVER_API_URL + "api/commandes/entree-stock/create";
  private readonly resourceFinalyse = SERVER_API_URL + "api/commandes/entree-stock/finalize";
  private readonly resourceUrlTransac = SERVER_API_URL + "api/commandes/entree-stock";

  updateCommandPreviousActiveNav(nav: string): void {
    this.deliveryPreviousActiveNav.set(nav);
  }

  find(commandeId: CommandeId): Observable<EntityResponseType> {
    return this.http.get<IDelivery>(`${this.resourceUrl}/${commandeId.id}/${commandeId.orderDate}`, { observe: "response" });
  }

  create(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.post<IDelivery>(this.resourceUrl2, entity, { observe: "response" });
  }

  finalizeSaisieEntreeStock(delivery: IDelivery): Observable<HttpResponse<IStockEntryResult>> {
    return this.http.put<IStockEntryResult>(this.resourceFinalyse, delivery, { observe: "response" });
  }

  update(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.put<IDelivery>(this.resourceUrl2, entity, { observe: "response" });
  }

  exportToCsv(commandeId: CommandeId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${commandeId.id}/${commandeId.orderDate}`, { responseType: "blob" });
  }

  exportToPdf(commandeId: CommandeId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${commandeId.id}/${commandeId.orderDate}`, { responseType: "blob" });
  }

  printEtiquette(commandeId: CommandeId, req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/etiquettes/${commandeId.id}/${commandeId.orderDate}`, {
      params: options,
      responseType: "blob"
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IDelivery[]>(this.resourceUrlQuery, {
      params: options,
      observe: "response"
    });
  }

  countByStatut(statut: string): Observable<number> {
    return this.http.get<number>(`${this.resourceUrlQuery}/count`, { params: { statut } });
  }

  fetchTotals(req?: any): Observable<HttpResponse<IDeliveryTotals>> {
    const options = createRequestOptions(req);
    return this.http.get<IDeliveryTotals>(`${this.resourceUrlQuery}/totaux`, {
      params: options,
      observe: "response"
    });
  }

  cancelReceived(commandeId: CommandeId): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrlQuery}/cancel/${commandeId.id}/${commandeId.orderDate}`, { observe: "response" });
  }

  queryWithoutDetail(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IDelivery[]>(this.resourceUrl + "/list-bon-livraison", {
      params: options,
      observe: "response"
    });
  }

  uploadNew(data: any): Observable<HttpResponse<ICommandeResponse>> {
    return this.http.post<ICommandeResponse>(`${this.resourceUrlTransac}/upload-new`, data, {
      observe: "response"
    });
  }

  filterItems(commandeId: CommandeId): Observable<HttpResponse<IDeliveryItem[]>> {
    return this.http.get<IDeliveryItem[]>(`${this.resourceUrl}/filter-items/${commandeId.id}/${commandeId.orderDate}`, {
      observe: "response"
    });
  }

  updateQuantityReceived(deliveryItem: IDeliveryItem): Observable<void> {
    return this.http.put<void>(
      this.resourceUrlTransac + "/update-order-line-quantity-received",
      this.versLiteDTO(deliveryItem)
    );
  }

  /**
   * « Tout valider » : pose la quantité reçue sur plusieurs lignes d'un coup.
   *
   * Le corps est RÉDUIT aux champs que le serveur lit (`DeliveryReceiptItemLiteDTO`, dont le
   * service n'exploite que l'identifiant de ligne et la quantité). Poster la ligne entière
   * échouait en HTTP 400 « Failed to read request » : la ligne rapportée par le serveur porte
   * un `tva` OBJET (`{id, taux, tva}`), quand le DTO d'écriture attend un entier — et Jackson
   * refuse tout le lot pour ce seul champ.
   */
  batchUpdateQuantityReceived(deliveryItems: IDeliveryItem[]): Observable<void> {
    const payload = deliveryItems.map(item => this.versLiteDTO(item));
    return this.http.put<void>(this.resourceUrlTransac + "/batch-quantity-received", payload);
  }

  updateQuantityUG(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + "/update-order-line-quantity-ug",
      this.versLiteDTO(deliveryItem),
      {
        observe: "response"
      }
    );
  }

  /** Attend un `OrderLineDTO` complet — dont le code CIP, que le DTO Lite ne porte pas. */
  updateCip(deliveryItem: IDeliveryItem): Observable<HttpResponse<{}>> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + "//update-provisional-cip", this.resetdatePeremption(deliveryItem), {
      observe: "response"
    });
  }

  updateOrderUnitPriceOnStockEntry(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + "/update-order-line-unit-price", this.versLiteDTO(deliveryItem), {
      observe: "response"
    });
  }

  /** Attend un `OrderLineDTO` complet — dont le coût d'achat qu'il vient écrire. */
  updateOrderCostAmount(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + "/update-order-line-cost-amount",
      this.resetdatePeremption(deliveryItem),
      {
        observe: "response"
      }
    );
  }

  updateDatePeremption(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + "/update-order-line-date-peremption",
      this.versLiteDTO(deliveryItem),
      {
        observe: "response"
      }
    );
  }

  updateTva(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + "/update-order-line-tva", this.versLiteDTO(deliveryItem), {
      observe: "response"
    });
  }

  scanReception(commandeId: number, rawScan: string): Observable<HttpResponse<IReceptionScanResult>> {
    return this.http.post<IReceptionScanResult>(
      `${this.resourceUrlTransac}/scan-reception?commandeId=${commandeId}`,
      rawScan,
      { observe: "response", headers: { "Content-Type": "text/plain" } }
    );
  }

  /**
   * Réduit une ligne de réception au corps que le serveur sait lire.
   *
   * Les endpoints d'écriture attendent un `DeliveryReceiptItemLiteDTO`, pas la ligne
   * complète telle que la lecture la rapporte. La différence n'est pas une question de
   * poids : la ligne lue porte un `tva` OBJET (`{id, taux, tva}`) là où le DTO d'écriture
   * déclare un entier, et Jackson refuse alors TOUT le corps — HTTP 400 « Failed to read
   * request », sans qu'aucune valeur ne soit enregistrée. La saisie de la quantité reçue,
   * les unités gratuites, la TVA, la péremption : tout passait par là.
   *
   * `datePeremption` est délibérément vidée : la péremption qui compte est celle du lot en
   * cours de saisie (`datePeremptionTmp`), et la valeur lue est une date sérialisée que le
   * serveur ne saurait pas relire.
   */
  private resetdatePeremption(deliveryItem: IDeliveryItem): IDeliveryItem {
    deliveryItem.datePeremption = null;
    return deliveryItem;
  }

  private versLiteDTO(deliveryItem: IDeliveryItem): Record<string, unknown> {
    const tva = deliveryItem.tva as unknown;
    const tauxTva =
      typeof tva === 'number' ? tva : typeof tva === 'object' && tva !== null ? (tva as any).taux : undefined;
    return {
      id: deliveryItem.id,
      orderLineId: deliveryItem.orderLineId,
      quantityReceived: deliveryItem.quantityReceived,
      quantityReceivedTmp: deliveryItem.quantityReceivedTmp,
      quantityRequested: deliveryItem.quantityRequested,
      quantityReturned: deliveryItem.quantityReturned,
      quantityUG: deliveryItem.freeQty,
      orderUnitPrice: deliveryItem.orderUnitPrice,
      tva: tauxTva,
      tvaId: (deliveryItem as any).tvaId ?? (typeof tva === 'object' && tva !== null ? (tva as any).id : undefined),
      lots: deliveryItem.lots,
      datePeremption: null,
      datePeremptionTmp: deliveryItem.datePeremptionTmp,
    };
  }
}
