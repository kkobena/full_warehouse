import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IAvoir, IAvoirCommand } from '../models';

@Injectable({ providedIn: 'root' })
export class AvoirApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/avoirs';
  private readonly http = inject(HttpClient);

  query(params: any, pageable?: any): Observable<HttpResponse<IAvoir[]>> {
    return this.http.get<IAvoir[]>(this.resourceUrl, {
      params: createRequestOptions({ ...params, ...pageable }),
      observe: 'response',
    });
  }

  create(command: IAvoirCommand): Observable<HttpResponse<IAvoir>> {
    return this.http.post<IAvoir>(this.resourceUrl, command, { observe: 'response' });
  }

  emettre(id: number): Observable<HttpResponse<IAvoir>> {
    return this.http.post<IAvoir>(`${this.resourceUrl}/${id}/emettre`, {}, { observe: 'response' });
  }

  imputer(id: number, factureId: number, factureDate: string): Observable<HttpResponse<{}>> {
    return this.http.post(
      `${this.resourceUrl}/${id}/imputer`,
      { factureId, factureDate },
      { observe: 'response' },
    );
  }

  annuler(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  exportPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/pdf`, { responseType: 'blob' });
  }
}
