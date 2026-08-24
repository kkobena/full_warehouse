import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';

import {SERVER_API_URL} from 'app/app.constants';
import {createRequestOptions} from 'app/shared/util/request-util';
import {IProduit} from 'app/shared/model/produit.model';
import {IResponseDto} from '../../../shared/util/response-dto';
import {ISales, SaleId} from '../../../shared/model/sales.model';
import {ISalesLine} from '../../../shared/model';

type EntityArrayResponseType = HttpResponse<IProduit[]>;

@Injectable({providedIn: 'root'})
export class StockDepotService {
  private http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/stock-depots';
  private importationResourceUrl = SERVER_API_URL + 'api/importation';

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IProduit[]>(this.resourceUrl, {params: options, observe: 'response'});
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.importationResourceUrl}/importcsv`, file, {observe: 'response'});
  }

  findImortation(): Observable<HttpResponse<IResponseDto>> {
    return this.http.get<IResponseDto>(`${this.importationResourceUrl}/result`, {observe: 'response'});
  }

  /**
   * Ventes dépôt paginées, **sans les lignes** : le serveur ne renseigne que `itemCount`.
   * Le détail se charge au dépliage, via {@link findSaleLines}.
   */
  fetchSales(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ISales[]>(this.resourceUrl + '/sales', {
      params: options,
      observe: 'response'
    });
  }

  /** Lignes d'une vente, chargées à la demande quand l'utilisateur déplie sa ligne. */
  findSaleLines(saleId: SaleId): Observable<ISalesLine[]> {
    return this.http.get<ISalesLine[]>(`${SERVER_API_URL}api/sales-lines/${saleId.id}/${saleId.saleDate}`);
  }

  export(format: string, saleId: SaleId): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export/${saleId.id}/${saleId.saleDate}/${format}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  exportToExcel(saleId: SaleId): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export/${saleId.id}/${saleId.saleDate}/excel`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  exportToCsv(saleId: SaleId): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export/${saleId.id}/${saleId.saleDate}/csv`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
