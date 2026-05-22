import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { SaleId } from '../../shared/model/sales.model';

type EntityResponseType = HttpResponse<ISalesLine>;
type EntityArrayResponseType = HttpResponse<ISalesLine[]>;

@Injectable({ providedIn: 'root' })
export class SalesLineService {
  protected http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/sales-lines';
  private readonly saleUrl = SERVER_API_URL + 'api/sales';

  create(salesLine: ISalesLine): Observable<EntityResponseType> {
    return this.http.post<ISalesLine>(`${this.saleUrl}/add-item`, salesLine, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ISalesLine[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  queryBySale(saleId?: SaleId): Observable<EntityArrayResponseType> {
    return this.http.get<ISalesLine[]>(`${this.resourceUrl}/${saleId.id}/${saleId.saleDate}`, { observe: 'response' });
  }
}
