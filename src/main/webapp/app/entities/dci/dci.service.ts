import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOption } from '../../shared/util/request-util';
import { Dci } from '../../shared/model/produit.model';

@Injectable({
  providedIn: 'root'
})
export class DciService {
  private http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/dci';

  query(req?: any): Observable<HttpResponse<Dci[]>> {
    const options = createRequestOption(req);
    return this.http.get<Dci[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  queryUnpaged(req?: any): Observable<HttpResponse<Dci[]>> {
    const options = createRequestOption(req);
    return this.http.get<Dci[]>(this.resourceUrl + '/unpaged', { params: options, observe: 'response' });
  }
}
