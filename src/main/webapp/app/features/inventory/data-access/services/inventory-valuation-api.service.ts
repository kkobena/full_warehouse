import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SERVER_API_URL} from '../../../../app.constants';
import {
  IInventoryGlobalSummary,
  IValuationGroup,
  ValuationGroupBy
} from '../../models/inventory-valuation.model';

@Injectable({providedIn: 'root'})
export class InventoryValuationApiService {
  private readonly http = inject(HttpClient);

  private url(inventoryId: number): string {
    return `${SERVER_API_URL}api/inventaires/${inventoryId}`;
  }

  getGlobalSummary(inventoryId: number): Observable<IInventoryGlobalSummary> {
    return this.http.get<IInventoryGlobalSummary>(`${this.url(inventoryId)}/valuation`);
  }

  getSummaryByGroup(inventoryId: number, groupBy: ValuationGroupBy): Observable<IValuationGroup[]> {
    return this.http.get<IValuationGroup[]>(
      `${this.url(inventoryId)}/valuation/by-group`,
      {params: {groupBy}}
    );
  }
}
