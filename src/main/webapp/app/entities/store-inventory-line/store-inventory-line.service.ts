import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IStoreInventoryLine } from 'app/shared/model/store-inventory-line.model';

type EntityResponseType = HttpResponse<IStoreInventoryLine>;
@Injectable({ providedIn: 'root' })
export class StoreInventoryLineService {
  public resourceUrl = SERVER_API_URL + 'api/store-inventory-lines';

  constructor(protected http: HttpClient) {}
  update(storeInventoryLine: IStoreInventoryLine): Observable<EntityResponseType> {
    return this.http.put<IStoreInventoryLine>(this.resourceUrl, storeInventoryLine, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IStoreInventoryLine>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
