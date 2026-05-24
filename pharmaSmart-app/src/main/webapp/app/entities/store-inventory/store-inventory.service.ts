import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IStoreInventory, ItemsCountRecord, StoreInventoryExportRecord } from 'app/shared/model/store-inventory.model';
import { ICategorie } from '../../shared/model/categorie.model';

type EntityResponseType = HttpResponse<IStoreInventory>;
type EntityArrayResponseType = HttpResponse<IStoreInventory[]>;

@Injectable({ providedIn: 'root' })
export class StoreInventoryService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/store-inventories';

  create(storeInventory: IStoreInventory): Observable<EntityResponseType> {
    return this.http.post<IStoreInventory>(this.resourceUrl, storeInventory, { observe: 'response' });
  }

  update(storeInventory: ICategorie): Observable<EntityResponseType> {
    return this.http.put<IStoreInventory>(this.resourceUrl, storeInventory, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IStoreInventory>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IStoreInventory[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  init(): Observable<HttpResponse<{}>> {
    return this.http.get<{}>(`${this.resourceUrl}/init`, { observe: 'response' });
  }

  close(id: number): Observable<HttpResponse<ItemsCountRecord>> {
    return this.http.get<ItemsCountRecord>(`${this.resourceUrl}/close/${id}`, { observe: 'response' });
  }

  exportToPdf(inventoryExportRecord: StoreInventoryExportRecord): Observable<Blob> {
    return this.http.post(`${this.resourceUrl}/pdf`, inventoryExportRecord, { responseType: 'blob' });
  }

}
