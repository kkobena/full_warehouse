import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import moment from 'moment';

import {DATE_FORMAT} from 'app/shared/constants/input.constants';
import {SERVER_API_URL} from 'app/app.constants';
import {createRequestOptions} from 'app/shared/util/request-util';
import {ICommande} from 'app/shared/model/commande.model';
import {IProduit} from 'app/shared/model/produit.model';
import {IOrderLine} from '../../shared/model/order-line.model';
import {IResponseCommande} from '../../shared/model/response-commande.model';
import {ICommandeResponse} from '../../shared/model/commande-response.model';

type EntityResponseType = HttpResponse<ICommande>;
type EntityArrayResponseType = HttpResponse<ICommande[]>;

@Injectable({providedIn: 'root'})
export class CommandeService {
  public resourceUrl = SERVER_API_URL + 'api/commandes';

  constructor(protected http: HttpClient) {
  }

  create(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .post<ICommande>(this.resourceUrl, copy, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .put<ICommande>(this.resourceUrl, copy, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ICommande>(`${this.resourceUrl}/${id}`, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, {observe: 'response'});
  }

  createOrUpdateOrderLine(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .post<IOrderLine>(this.resourceUrl + '/add-order-line', orderLine, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateQuantityRequested(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/update-order-line-quantity-requested', orderLine, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateOrderCostAmount(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/update-order-line-cost-amount', orderLine, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  updateOrderUnitPrice(orderLine: IOrderLine): Observable<EntityResponseType> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/update-order-line-unit-price', orderLine, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  deleteOrderLineById(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/order-line/${id}`, {observe: 'response'});
  }

  deleteOrderLinesByIds(commandeId: number, ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete/order-lines/${commandeId}`, ids, {observe: 'response'});
  }

  updateCip(orderLine: IOrderLine): Observable<HttpResponse<{}>> {
    return this.http.put<IOrderLine>(this.resourceUrl + '/update-provisional-cip', orderLine, {observe: 'response'});
  }

  closeCommandeEnCours(commandeId: number): Observable<HttpResponse<{}>> {
    return this.http.get(`${this.resourceUrl}/close-commande-en-cours/${commandeId}`, {observe: 'response'});
  }

  exportToCsv(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/csv/${commandeId}`, {responseType: 'blob'});
  }

  exportToPdf(commandeId: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf/${commandeId}`, {responseType: 'blob'});
  }

  filterCommandeLines(req?: any): Observable<HttpResponse<IOrderLine[]>> {
    const options = createRequestOptions(req);
    return this.http
      .get<IOrderLine[]>(`${this.resourceUrl}/filter-order-lines`, {params: options, observe: 'response'})
      .pipe(map((res: HttpResponse<IOrderLine[]>) => this.convertOrderLineDateArrayFromServer(res)));
  }

  fetchOrderLinesByCommandeId(commandeId: number): Observable<HttpResponse<IOrderLine[]>> {
    return this.http
      .get<IOrderLine[]>(`${this.resourceUrl}/pageable-order-lines/${commandeId}`, {
        params: {size: '99999', sort: 'fournisseurProduit_produit_libelle'},
        observe: 'response'
      })
      .pipe(map((res: HttpResponse<IOrderLine[]>) => this.convertOrderLineDateArrayFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<ICommande[]>(this.resourceUrl + '/commandes-without-order-lines', {params: options, observe: 'response'})
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  fusionner(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/fusionner`, ids, {observe: 'response'});
  }

  deleteSelectedCommandes(ids: number[]): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/delete-commandes`, ids, {observe: 'response'});
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
    return this.http.get(`${this.resourceUrl}/rupture-csv/${reference}`, {responseType: 'blob'});
  }

  updateQuantityReceived(orderLine: IOrderLine): Observable<{}> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/entree-stock/update-order-line-quantity-received', orderLine, {observe: 'response'});
  }

  updateQuantityUG(orderLine: IOrderLine): Observable<{}> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/entree-stock/update-order-line-quantity-ug', orderLine, {observe: 'response'});
  }

  updateOrderUnitPriceOnStockEntry(orderLine: IOrderLine): Observable<{}> {
    return this.http
      .put<IOrderLine>(this.resourceUrl + '/entree-stock/update-order-line-unit-price', orderLine, {observe: 'response'});
  }

  test(req: any): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/test-csv`, {params: options, observe: 'response'});
  }

  createNewCommand(produits: IProduit[]): Observable<HttpResponse<IProduit[]>> {
    return this.http.post<IProduit[]>(this.resourceUrl + '/set', produits, {observe: 'response'});
  }


  sauvegarderSaisieEntreeStock(commande: ICommande): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(commande);
    return this.http
      .put<ICommande>(`${this.resourceUrl}/entree-stock/save`, copy, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  findSaisieEntreeStock(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ICommande>(`${this.resourceUrl}/entree-stock/${id}`, {observe: 'response'})
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  protected convertDateFromClient(commande: ICommande): ICommande {
    const copy: ICommande = Object.assign({}, commande, {
      receiptDate: commande.receiptDate && commande.receiptDate.isValid() ? commande.receiptDate.format(DATE_FORMAT) : undefined,
      createdAt: commande.createdAt && commande.createdAt.isValid() ? commande.createdAt.toJSON() : undefined,
      updatedAt: commande.updatedAt && commande.updatedAt.isValid() ? commande.updatedAt.toJSON() : undefined,
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.receiptDate = res.body.receiptDate ? moment(res.body.receiptDate) : undefined;
      res.body.createdAt = res.body.createdAt ? moment(res.body.createdAt) : undefined;
      res.body.updatedAt = res.body.updatedAt ? moment(res.body.updatedAt) : undefined;
    }
    return res;
  }

  protected convertOrderLineDateArrayFromServer(res: HttpResponse<IOrderLine[]>): HttpResponse<IOrderLine[]> {
    if (res.body) {
      res.body.forEach((orderLine: IOrderLine) => {
        orderLine.receiptDate = orderLine.receiptDate ? moment(orderLine.receiptDate) : undefined;
        orderLine.createdAt = orderLine.createdAt ? moment(orderLine.createdAt) : undefined;
        orderLine.updatedAt = orderLine.updatedAt ? moment(orderLine.updatedAt) : undefined;
      });
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((commande: ICommande) => {
        commande.receiptDate = commande.receiptDate ? moment(commande.receiptDate) : undefined;
        commande.createdAt = commande.createdAt ? moment(commande.createdAt) : undefined;
        commande.updatedAt = commande.updatedAt ? moment(commande.updatedAt) : undefined;
      });
    }
    return res;
  }
}
