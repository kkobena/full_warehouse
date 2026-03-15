import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IGapEntry, IGapLine, IGapSummary } from '../../models/gap-analysis.model';

@Injectable({ providedIn: 'root' })
export class GapAnalysisApiService {
  private readonly http = inject(HttpClient);

  private url(inventoryId: number): string {
    return `${SERVER_API_URL}api/inventaires/${inventoryId}`;
  }

  getGapLines(inventoryId: number): Observable<IGapLine[]> {
    return this.http.get<IGapLine[]>(`${this.url(inventoryId)}/gap-lines`);
  }

  saveAnalysis(inventoryId: number, entries: IGapEntry[]): Observable<void> {
    return this.http.post<void>(`${this.url(inventoryId)}/gap-analysis`, entries);
  }

  getSummary(inventoryId: number): Observable<IGapSummary[]> {
    return this.http.get<IGapSummary[]>(`${this.url(inventoryId)}/gap-summary`);
  }

  hasAnalysis(inventoryId: number): Observable<{ exists: boolean }> {
    return this.http.get<{ exists: boolean }>(`${this.url(inventoryId)}/gap-analysis/exists`);
  }
}
