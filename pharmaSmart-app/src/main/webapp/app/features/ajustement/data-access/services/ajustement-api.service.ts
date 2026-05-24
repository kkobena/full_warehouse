import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IAjust } from '../../../../shared/model/ajust.model';
import { IAjustement } from '../../../../shared/model/ajustement.model';
import { IMotifAjustement } from '../../../../shared/model/motif-ajustement.model';
import { ILotItem } from '../../models';

@Injectable({ providedIn: 'root' })
export class AjustementApiService {
  private readonly http = inject(HttpClient);

  private readonly ajustUrl = SERVER_API_URL + 'api/ajustements';
  private readonly motifUrl = SERVER_API_URL + 'api/motif-ajsutements';
  private readonly lotUrl  = SERVER_API_URL + 'api/lot';

  // ── Ajust (en-tête) ──────────────────────────────────────────────────────

  listHistory(params: Record<string, unknown>): Observable<HttpResponse<IAjust[]>> {
    return this.http.get<IAjust[]>(`${this.ajustUrl}/ajust`, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  create(payload: IAjust): Observable<HttpResponse<IAjust>> {
    return this.http.post<IAjust>(this.ajustUrl, payload, { observe: 'response' });
  }

  finalise(payload: IAjust): Observable<HttpResponse<unknown>> {
    return this.http.post(`${this.ajustUrl}/save`, payload, { observe: 'response' });
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.ajustUrl}/pdf/${id}`, { responseType: 'blob' });
  }

  // ── Ajustement lines ─────────────────────────────────────────────────────

  getLines(ajustId: number, search?: string): Observable<HttpResponse<IAjustement[]>> {
    const params: Record<string, unknown> = { ajustementId: ajustId };
    if (search) params['search'] = search;
    return this.http.get<IAjustement[]>(this.ajustUrl, {
      params: createRequestOptions(params),
      observe: 'response',
    });
  }

  addLine(line: IAjustement): Observable<HttpResponse<unknown>> {
    return this.http.post(`${this.ajustUrl}/item/add`, line, { observe: 'response' });
  }

  updateLine(line: IAjustement): Observable<HttpResponse<IAjustement>> {
    return this.http.put<IAjustement>(`${this.ajustUrl}/item`, line, { observe: 'response' });
  }

  deleteLine(id: number): Observable<HttpResponse<unknown>> {
    return this.http.delete(`${this.ajustUrl}/item/${id}`, { observe: 'response' });
  }

  deleteLines(ids: number[]): Observable<HttpResponse<unknown>> {
    return this.http.put(`${this.ajustUrl}/delete/items`, ids, { observe: 'response' });
  }

  // ── Motifs ───────────────────────────────────────────────────────────────

  listMotifs(): Observable<IMotifAjustement[]> {
    return this.http.get<IMotifAjustement[]>(this.motifUrl, {
      params: createRequestOptions({ page: 0, size: 9999 }),
    });
  }

  // ── Lots ─────────────────────────────────────────────────────────────────

  getLotsForProduit(produitId: number): Observable<ILotItem[]> {
    return this.http.get<ILotItem[]>(`${this.lotUrl}/produit/${produitId}`);
  }

}
