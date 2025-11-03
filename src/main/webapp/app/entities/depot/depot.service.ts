import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { MagasinService } from '../magasin/magasin.service';
import { IMagasin } from '../../shared/model/magasin.model';
import { SalesService } from '../sales/sales.service';
import { SERVER_API_URL } from '../../app.constants';
import { FinalyseSale, ISales, SaleId, UpdateSaleInfo } from '../../shared/model/sales.model';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import { ISalesLine, SaleLineId } from '../../shared/model/sales-line.model';
import { IUser } from '../../core/user/user.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({
  providedIn: 'root'
})
export class DepotService {
  private readonly resourceUrl = SERVER_API_URL + 'api/vente-depot';
  private readonly http = inject(HttpClient);
  private readonly magasinService = inject(MagasinService);
  private readonly salesService = inject(SalesService);
  selectedDepot: WritableSignal<IMagasin | null> = signal<IMagasin | null>(null);
  depots: WritableSignal<IMagasin[]> = signal<IMagasin[]>([]);
  currentSale: WritableSignal<ISales> = signal<ISales>(null);
  caissier: WritableSignal<IUser> = signal<IUser>(null);
  vendeur: WritableSignal<IUser> = signal<IUser>(null);
  canInvoice: WritableSignal<boolean> = signal<boolean>(false);
  canReceipt: WritableSignal<boolean> = signal<boolean>(true);
  setVendeur(user: IUser): void {
    this.vendeur.set(user);
  }

  setCaissier(user: IUser): void {
    this.caissier.set(user);
  }

  reset(): void {
    this.currentSale.set(null);
    this.selectedDepot.set(null);

  }

  constructor() {
    this.loadAllDepots();
  }

  setCurrentSale(sales: ISales): void {
    this.currentSale.set(sales);
  }

  setSelectedDepot(depot: IMagasin | null): void {
    this.selectedDepot.set(depot);
  }

  loadAllDepots(): void {
    this.magasinService.fetchAllDepots({
      types: ['DEPOT']
    }).subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots.set(res.body || []);
    });
  }

  find(id: SaleId): Observable<EntityResponseType> {
    return this.salesService.find(id);
  }

  findForEdit(id: number): Observable<EntityResponseType> {
    return this.http
      .get<ISales>(`${this.resourceUrl}/edit/${id}`, { observe: 'response' })
      ;
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' })
      ;
  }

  delete(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  save(sales: ISales): Observable<HttpResponse<FinalyseSale>> {

    return this.http.put<FinalyseSale>(this.resourceUrl + '/save', sales, { observe: 'response' });
  }

  print(id: SaleId): Observable<Blob> {
    return this.salesService.print(id);
  }

  printReceipt(id: SaleId): Observable<{}> {
    return this.salesService.printReceipt(id);
  }

  rePrintReceipt(id: SaleId): Observable<{}> {
    return this.salesService.rePrintReceipt(id);
  }

  getEscPosReceiptForTauri(id: SaleId, isEdition: boolean = false): Observable<ArrayBuffer> {
    return this.salesService.getEscPosReceiptForTauri(id, isEdition);
  }

  addItem(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {

    return this.http
      .post<ISalesLine>(`${this.resourceUrl}/add-item`, salesLine, { observe: 'response' });
  }

  create(sales: ISales): Observable<EntityResponseType> {
    return this.http
      .post<ISales>(this.resourceUrl, sales, { observe: 'response' });
  }

  updateItemPrice(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/price`, salesLine, { observe: 'response' })
      ;
  }

  updateItemQtyRequested(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-requested`, salesLine, { observe: 'response' })
      ;
  }

  updateItemQtySold(salesLine: ISalesLine): Observable<HttpResponse<ISalesLine>> {
    return this.http
      .put<ISalesLine>(`${this.resourceUrl}/update-item/quantity-sold`, salesLine, { observe: 'response' })
      ;
  }

  deleteItem(id: SaleLineId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/delete-item/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  deletePrevente(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/prevente/${id.id}/${id.saleDate}`, { observe: 'response' });
  }

  printInvoice(id: SaleId): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/print/invoice/${id.id}/${id.saleDate}`, { responseType: 'blob' });
  }

  cancel(id: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/cancel/${id.id}/${id.saleDate}`, { observe: 'response' });
  }


  removeRemise(saleId: SaleId): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/remove-remise/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
  }

  addRemise(key: UpdateSaleInfo): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl + '/add-remise', key, { observe: 'response' });
  }


}
