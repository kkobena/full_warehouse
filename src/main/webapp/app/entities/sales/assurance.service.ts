import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { ISales } from 'app/shared/model/sales.model';
import { UpdateSale } from './customer-edit-modal/update-sale.model';

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
    return this.http.get<ISales[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
}
