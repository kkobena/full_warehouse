import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';

export interface ExportComptableParams {
  startDate: string;
  endDate: string;
  format: string;
  ventes: boolean;
  achats: boolean;
  mvtCaisse: boolean;
  tiersPayant: boolean;
  differes: boolean;
  tva: boolean;
}

@Injectable({ providedIn: 'root' })
export class ExportComptableApiService {
  private readonly http = inject(HttpClient);

  export(p: ExportComptableParams): Observable<HttpResponse<Blob>> {
    const params = new HttpParams()
      .set('startDate', p.startDate)
      .set('endDate', p.endDate)
      .set('format', p.format)
      .set('ventes', p.ventes)
      .set('achats', p.achats)
      .set('mvtCaisse', p.mvtCaisse)
      .set('tiersPayant', p.tiersPayant)
      .set('differes', p.differes)
      .set('tva', p.tva);

    return this.http.get(`${SERVER_API_URL}api/finances/export-comptable`, {
      params,
      observe: 'response',
      responseType: 'blob',
    });
  }
}
