import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { ICommande } from 'app/shared/model/commande.model';
import { IOrderLine } from '../../shared/model/order-line.model';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { ICommandeResponse } from '../../shared/model/commande-response.model';

type EntityResponseType = HttpResponse<ICommande>;
type EntityArrayResponseType = HttpResponse<ICommande[]>;

@Injectable({ providedIn: 'root' })
export class CommandeService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/commandes';

  create(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .post<ICommande>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .put<ICommande>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ICommande>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  rollback(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/rollback/${id}`, { observe: 'response' });
  }

  rollbackCommandes(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/rollback`, ids, { observe: 'response' });
  }

  createOrUpdateOrderLine(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .post<ICommande>(this.resourceUrl + '/add-order-line', orderLine, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateQuantityRequested(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + '/update-order-line-quantity-requested', orderLine, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateOrderCostAmount(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + '/update-order-line-cost-amount', orderLine, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateOrderUnitPrice(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(this.resourceUrl + '/update-order-line-unit-price', orderLine, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  deleteOrderLineById(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/order-line/${id}`, { observe: 'response' });
  }

  deleteOrderLinesByIds(commandeId: number, ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete/order-lines/${commandeId}`, ids, { observe: 'response' });
  }

  updateCip(orderLine: IOrderLine): Observable<HttpResponse<{}>> {
    return this.http.put<IOrderLine>(this.resourceUrl + '/update-provisional-cip', orderLine, { observe: 'response' });
  }

  closeCommandeEnCours(commandeId: number): Observable<HttpResponse<{}>> {
    return this.http.get(`${this.resourceUrl}/close-commande-en-cours/${commandeId}`, { observe: 'response' });
  }

  exportToCsv(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${commandeId}`, { responseType: 'blob' });
  }

  exportToPdf(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${commandeId}`, { responseType: 'blob' });
  }

  filterCommandeLines(req?: any): Observable<HttpResponse<IOrderLine[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IOrderLine[]>(`${this.resourceUrl}/filter-order-lines`, {
      params: options,
      observe: 'response',
    });
  }

  fetchOrderLinesByCommandeId(commandeId: number): Observable<HttpResponse<IOrderLine[]>> {
    return this.http.get<IOrderLine[]>(`${this.resourceUrl}/pageable-order-lines/${commandeId}`, {
      params: { size: '99999', sort: 'fournisseurProduit_produit_libelle' },
      observe: 'response',
    });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<ICommande[]>(this.resourceUrl + '/commandes-without-order-lines', {
        params: options,
        observe: 'response',
      })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  fusionner(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/fusionner`, ids, { observe: 'response' });
  }

  deleteSelectedCommandes(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete-commandes`, ids, { observe: 'response' });
  }

  importerReponseCommande(commandeId: number, file: any): Observable<HttpResponse<IResponseCommande>> {
    return this.http.post<IResponseCommande>(`${this.resourceUrl}/verification-commande-en-cours/${commandeId}`, file, {
      observe: 'response',
    });
  }

  uploadNewCommande(fournisseurId: number, model: string, file: any): Observable<HttpResponse<ICommandeResponse>> {
    return this.http.post<ICommandeResponse>(`${this.resourceUrl}/upload-new-commande/${fournisseurId}/${model}`, file, {
      observe: 'response',
    });
  }

  getRuptureCsv(reference: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/rupture-csv/${reference}`, { responseType: 'blob' });
  }

  updateQuantityReceived(orderLine: IOrderLine): Observable<{}> {
    return this.http.put(this.resourceUrl + '/update-order-line-quantity-received', orderLine, {
      observe: 'response',
    });
  }

  updateQuantityUG(orderLine: IOrderLine): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/update-order-line-quantity-ug', orderLine, { observe: 'response' });
  }

  updateOrderUnitPriceOnStockEntry(orderLine: IOrderLine): Observable<{}> {
    return this.http.put<IOrderLine>(this.resourceUrl + '/update-order-line-unit-price', orderLine, { observe: 'response' });
  }

  test(req: any): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/test-csv`, { params: options, observe: 'response' });
  }

  sauvegarderSaisieEntreeStock(commande: ICommande): Observable<EntityResponseType> {
    return this.http
      .put<ICommande>(`${this.resourceUrl}/entree-stock/save`, commande, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  changeGrossiste(commande: ICommande): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/change-grossiste`, commande, { observe: 'response' });
  }

  finalizeSaisieEntreeStock(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .put<ICommande>(`${this.resourceUrl}/entree-stock/finalize`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  findSaisieEntreeStock(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ICommande>(`${this.resourceUrl}/entree-stock/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  private convertDateFromClient(commande: ICommande): ICommande {
    return Object.assign({}, commande, {
      createdAt: commande.createdAt && commande.createdAt.isValid() ? commande.createdAt.toJSON() : undefined,
      updatedAt: commande.updatedAt && commande.updatedAt.isValid() ? commande.updatedAt.toJSON() : undefined,
    });
  }

  private convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((commande: ICommande) => {
        commande.createdAt = commande.createdAt ? moment(commande.createdAt) : undefined;
        commande.updatedAt = commande.updatedAt ? moment(commande.updatedAt) : undefined;
      });
    }
    return res;
  }
}
