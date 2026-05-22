import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';

type EntityArrayResponseType = HttpResponse<IInventoryTransaction[]>;

@Injectable({ providedIn: 'root' })
export class InventoryTransactionService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/inventory-transactions';

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IInventoryTransaction[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
}
