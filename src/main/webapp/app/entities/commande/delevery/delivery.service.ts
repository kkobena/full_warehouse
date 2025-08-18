import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IDelivery } from '../../../shared/model/delevery.model';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { IDeliveryItem } from '../../../shared/model/delivery-item';

type EntityResponseType = HttpResponse<IDelivery>;
type EntityArrayResponseType = HttpResponse<IDelivery[]>;

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  deliveryPreviousActiveNav: WritableSignal<string> = signal<string>('pending');
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/commandes/data/entree-stock';
  private readonly resourceUrl2 = SERVER_API_URL + 'api/commandes/entree-stock/create';
  private readonly resourceFinalyse = SERVER_API_URL + 'api/commandes/entree-stock/finalize';
  private readonly resourceUrlTransac = SERVER_API_URL + 'api/commandes/entree-stock';

  updateCommandPreviousActiveNav(nav: string): void {
    this.deliveryPreviousActiveNav.set(nav);
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDelivery>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  create(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.post<IDelivery>(this.resourceUrl2, entity, { observe: 'response' });
  }

  finalizeSaisieEntreeStock(delivery: IDelivery): Observable<EntityResponseType> {
    return this.http.put<IDelivery>(this.resourceFinalyse, delivery, { observe: 'response' });
  }

  update(entity: IDelivery): Observable<EntityResponseType> {
    return this.http.put<IDelivery>(this.resourceUrl2, entity, { observe: 'response' });
  }

  exportToCsv(entityId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${entityId}`, { responseType: 'blob' });
  }

  exportToPdf(entityId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${entityId}`, { responseType: 'blob' });
  }

  printEtiquette(id: number, req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/etiquettes/${id}`, {
      params: options,
      responseType: 'blob'
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IDelivery[]>(this.resourceUrl + '/list', {
      params: options,
      observe: 'response'
    });
  }

  uploadNew(data: any): Observable<HttpResponse<ICommandeResponse>> {
    return this.http.post<ICommandeResponse>(`${this.resourceUrlTransac}/upload-new`, data, {
      observe: 'response'
    });
  }

  filterItems(req?: any): Observable<HttpResponse<IDeliveryItem[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IDeliveryItem[]>(`${this.resourceUrl}/filter-items`, {
      params: options,
      observe: 'response'
    });
  }

  updateQuantityReceived(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + '/update-order-line-quantity-received',
      this.resetdatePeremption(deliveryItem),
      {
        observe: 'response'
      }
    );
  }

  updateQuantityUG(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + '/update-order-line-quantity-ug',
      this.resetdatePeremption(deliveryItem),
      {
        observe: 'response'
      }
    );
  }

  updateCip(deliveryItem: IDeliveryItem): Observable<HttpResponse<{}>> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '//update-provisional-cip', this.resetdatePeremption(deliveryItem), {
      observe: 'response'
    });
  }

  updateOrderUnitPriceOnStockEntry(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-unit-price', this.resetdatePeremption(deliveryItem), {
      observe: 'response'
    });
  }

  updateOrderCostAmount(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + '/update-order-line-cost-amount',
      this.resetdatePeremption(deliveryItem),
      {
        observe: 'response'
      }
    );
  }

  updateDatePeremption(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(
      this.resourceUrlTransac + '/update-order-line-date-peremption',
      this.resetdatePeremption(deliveryItem),
      {
        observe: 'response'
      }
    );
  }

  updateTva(deliveryItem: IDeliveryItem): Observable<{}> {
    return this.http.put<IDeliveryItem>(this.resourceUrlTransac + '/update-order-line-tva', this.resetdatePeremption(deliveryItem), {
      observe: 'response'
    });
  }

  private resetdatePeremption(deliveryItem: IDeliveryItem): IDeliveryItem {
    deliveryItem.datePeremption = null;
    return deliveryItem;
  }
}
