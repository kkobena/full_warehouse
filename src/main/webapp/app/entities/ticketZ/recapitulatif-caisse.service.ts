import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { Ticket } from './model/ticket.model';
import { createRequestOptions } from '../../shared/util/request-util';
import { RecapParam } from './model/recap-param.model';

@Injectable({
  providedIn: 'root'
})
export class RecapitulatifCaisseService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/ticketz';

  query(req: RecapParam): Observable<HttpResponse<Ticket>> {
    const options = createRequestOptions(req);
    return this.http.get<Ticket>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }

  exportToPdf(req: RecapParam): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/pdf`, { params: options, responseType: 'blob' });
  }

  print(req: RecapParam): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/print`, { params: options, observe: 'response' });
  }

  sendMail(req: RecapParam): Observable<HttpResponse<{}>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/email`, { params: options, observe: 'response' });
  }
}
