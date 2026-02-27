import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { FinalyseSale, ISales, SaleId, UpdateSaleInfo } from '../../../../shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../../../shared/model/sales-line.model';
import {map} from "rxjs/operators";

/**
 * Service HTTP dédié aux ventes dépôt (/api/vente-depot/*)
 * Ne contient aucune logique métier — uniquement des appels HTTP.
 */
@Injectable({ providedIn: 'root' })
export class VenteDepotApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/vente-depot';
  private readonly http = inject(HttpClient);

  create(sales: ISales): Observable<HttpResponse<ISales>> {
    return this.http.post<ISales>(this.resourceUrl, sales, { observe: 'response' });
  }

  addItem(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http.post<ISalesLine>(`${this.resourceUrl}/add-item`, salesLine, { observe: 'response' });
  }

  updateItemQtyRequested(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested`, salesLine, { observe: 'response' });
  }

  updateItemQtySold(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, { observe: 'response' });
  }

  updateItemPrice(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, { observe: 'response' });
  }

  deleteItem(id: SaleLineId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  save(sales: ISales): Observable<HttpResponse<FinalyseSale>> {
    return this.http.put<FinalyseSale>(`${this.resourceUrl}/save`, sales, { observe: 'response' });
  }

  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/add-remise`, key, { observe: 'response' });
  }

  removeRemise(saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/remove-remise/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
  }

  changeDepot(id: SaleId, depotId: number): Observable<HttpResponse<SaleId>> {
    const options = createRequestOptions({ depotId, saleId: id.id, saleDate: id.saleDate });
    return this.http.get<SaleId>(`${this.resourceUrl}/change-depot`, { params: options, observe: 'response' });
  }
  incrementItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/increment-item/quantity-requested`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }
  setItemQtyRequested(salesLine: ISalesLine): Observable<ISalesLine> {
    return this.http.put<ISalesLine>(`${this.resourceUrl}/set-item/quantity-requested`, salesLine, {observe: 'response'}).pipe(
      map(res => res.body!),
    );
  }
  printInvoice(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  deletePrevente(saleId: SaleId): Observable<void> {
    return this.http
      .delete<void>(`${this.resourceUrl}/prevente/${saleId.id}/${saleId.saleDate}`, {observe: 'response'})
      .pipe(map((): void => undefined));
  }

}
