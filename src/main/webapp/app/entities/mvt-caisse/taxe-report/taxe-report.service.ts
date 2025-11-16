import { inject, Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { createRequestOptions } from '../../../shared/util/request-util';
import { TaxeWrapper } from './taxe-report.model';

@Injectable({
  providedIn: 'root',
})
export class TaxeReportService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/taxe-report';

  query(req?: any): Observable<HttpResponse<TaxeWrapper>> {
    const options = createRequestOptions(req);
    return this.http.get<TaxeWrapper>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }
}
