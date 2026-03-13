import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IStoreInventory, ItemsCountRecord } from '../../../../shared/model/store-inventory.model';
import { createRequestOptions } from '../../../../shared/util/request-util';
import {
  BatchSyncResultRecord,
  IInventoryLine,
  ImportResultRecord,
  InventoryProgressRecord,
  StoreInventoryCreateRecord,
} from '../../models';

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/store-inventories';
  private readonly linesUrl = SERVER_API_URL + 'api/store-inventory-lines';

  list(params: any): Observable<HttpResponse<IStoreInventory[]>> {
    const options = createRequestOptions(params);
    return this.http.get<IStoreInventory[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  create(record: StoreInventoryCreateRecord): Observable<HttpResponse<IStoreInventory>> {
    return this.http.post<IStoreInventory>(this.resourceUrl, record, { observe: 'response' });
  }

  get(id: number): Observable<HttpResponse<IStoreInventory>> {
    return this.http.get<IStoreInventory>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  close(id: number): Observable<HttpResponse<ItemsCountRecord>> {
    return this.http.get<ItemsCountRecord>(`${this.resourceUrl}/close/${id}`, { observe: 'response' });
  }

  getProgress(id: number): Observable<HttpResponse<InventoryProgressRecord>> {
    return this.http.get<InventoryProgressRecord>(`${this.resourceUrl}/${id}/progress`, { observe: 'response' });
  }

  importCsv(id: number, file: File): Observable<HttpResponse<ImportResultRecord>> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<ImportResultRecord>(`${this.resourceUrl}/${id}/import`, formData, { observe: 'response' });
  }

  getLines(params: any): Observable<HttpResponse<IInventoryLine[]>> {
    const options = createRequestOptions(params);
    return this.http.get<IInventoryLine[]>(`${this.linesUrl}/v2`, { params: options, observe: 'response' });
  }

  updateLine(line: IInventoryLine): Observable<HttpResponse<IInventoryLine>> {
    return this.http.put<IInventoryLine>(this.linesUrl, line, { observe: 'response' });
  }

  batchSave(lines: { id: number; quantityOnHand: number }[]): Observable<HttpResponse<BatchSyncResultRecord>> {
    return this.http.put<BatchSyncResultRecord>(`${this.linesUrl}/batch`, lines, { observe: 'response' });
  }

  exportToPdf(params: any): Observable<Blob> {
    const options = createRequestOptions(params);
    return this.http.get(`${this.resourceUrl}/export-pdf`, { params: options, responseType: 'blob' });
  }
}
