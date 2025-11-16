import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption, createRequestOptions } from 'app/shared/util/request-util';
import { ISales } from 'app/shared/model/sales.model';
import { ISalesLine } from '../../shared/model/sales-line.model';
import { IResponseDto } from '../../shared/util/response-dto';
import { IClientTiersPayant } from '../../shared/model/client-tiers-payant.model';
import { UpdateSale } from './customer-edit-modal/update-sale.model';

type EntityResponseType = HttpResponse<ISales>;
type EntityArrayResponseType = HttpResponse<ISales[]>;

@Injectable({ providedIn: 'root' })
export class AssuranceService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/sales';

  updateCustomerInformation(updateSale: UpdateSale): Observable<HttpResponse<{}>> {
    return this.http.put<{}>(`${this.resourceUrl}/assurance/update-customer-information`, updateSale, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http
      .get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((sales: ISales) => {
        sales.createdAt = sales.createdAt ? moment(sales.createdAt) : undefined;
        sales.updatedAt = sales.updatedAt ? moment(sales.updatedAt) : undefined;
      });
    }
    return res;
  }
}
