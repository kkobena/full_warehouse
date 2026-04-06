import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IRecapitulatifMensuelDto, IRecapitulatifParams } from '../models';

@Injectable({ providedIn: 'root' })
export class RecapitulatifApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/edition-factures/recapitulatif';
  private readonly http = inject(HttpClient);

  query(params: IRecapitulatifParams, pageable?: any): Observable<HttpResponse<IRecapitulatifMensuelDto[]>> {
    return this.http.get<IRecapitulatifMensuelDto[]>(this.resourceUrl, {
      params: createRequestOptions({ ...params, ...pageable }),
      observe: 'response',
    });
  }

  exportPdf(params: IRecapitulatifParams): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }

  exportExcel(params: IRecapitulatifParams): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/excel`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }
}
