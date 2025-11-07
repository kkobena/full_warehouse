import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IProduit } from 'app/shared/model/produit.model';
import { IResponseDto } from '../../../shared/util/response-dto';
import { ISales } from '../../../shared/model/sales.model';


type EntityArrayResponseType = HttpResponse<IProduit[]>;

@Injectable({ providedIn: 'root' })
export class StockDepotService {
  private http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/stock-depots';
  private importationResourceUrl = SERVER_API_URL + 'api/importation';


  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IProduit[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.importationResourceUrl}/importcsv`, file, { observe: 'response' });
  }

  findImortation(): Observable<HttpResponse<IResponseDto>> {
    return this.http.get<IResponseDto>(`${this.importationResourceUrl}/result`, { observe: 'response' });
  }
  fetchSales(req?: any): Observable< HttpResponse<ISales[]>> {
    const options = createRequestOptions(req);
    return this.http
      .get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
}
