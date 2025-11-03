import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

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
    const copy = this.convertDateFromClient(salesLine);
    return this.http
      .post<ISalesLine>(`${this.saleUrl}/add-item`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }



  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<ISalesLine[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  queryBySale(saleId?: SaleId): Observable<EntityArrayResponseType> {
    return this.http
      .get<ISalesLine[]>(`${this.resourceUrl}/${saleId.id}/${saleId.saleDate}`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  private convertDateFromClient(salesLine: ISalesLine): ISalesLine {
    return Object.assign({}, salesLine, {
      createdAt: salesLine.createdAt && salesLine.createdAt.isValid() ? salesLine.createdAt.toJSON() : undefined,
      updatedAt: salesLine.updatedAt && salesLine.updatedAt.isValid() ? salesLine.updatedAt.toJSON() : undefined,
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
      res.body.forEach((salesLine: ISalesLine) => {
        salesLine.createdAt = salesLine.createdAt ? moment(salesLine.createdAt) : undefined;
        salesLine.updatedAt = salesLine.updatedAt ? moment(salesLine.updatedAt) : undefined;
      });
    }
    return res;
  }
}
