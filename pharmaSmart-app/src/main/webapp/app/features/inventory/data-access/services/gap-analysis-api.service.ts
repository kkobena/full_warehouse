import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IGapEntry, IGapLine, IGapSummary } from '../../models/gap-analysis.model';

@Injectable({ providedIn: 'root' })
export class GapAnalysisApiService {
  private readonly http = inject(HttpClient);

  private url(inventoryId: number): string {
    return `${SERVER_API_URL}api/inventaires/${inventoryId}`;
  }

  /**
   * Page des lignes en écart. La réponse est observée entière : le total vit dans
   * l'en-tête `X-Total-Count`, il alimente le paginateur et le compteur du bandeau.
   */
  getGapLines(inventoryId: number, page: number, size: number): Observable<HttpResponse<IGapLine[]>> {
    return this.http.get<IGapLine[]>(`${this.url(inventoryId)}/gap-lines`, {
      params: new HttpParams().set('page', page).set('size', size),
      observe: 'response',
    });
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
