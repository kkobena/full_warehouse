import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { firstValueFrom, Observable } from 'rxjs';
import { createRequestOption } from '../../shared/util/request-util';
import { ITva } from '../../shared/model/tva.model';

type EntityResponseType = HttpResponse<ITva>;
type EntityArrayResponseType = HttpResponse<ITva[]>;

@Injectable({
  providedIn: 'root',
})
export class TvaService {
  protected http = inject(HttpClient);

  public resourceUrl = SERVER_API_URL + 'api/tvas';

  create(tva: ITva): Observable<EntityResponseType> {
    return this.http.post<ITva>(this.resourceUrl, tva, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ITva>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ITva[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  async queryPromise(req?: any): Promise<ITva[]> {
    const options = createRequestOption(req);
    return await firstValueFrom(this.http.get<ITva[]>(this.resourceUrl, { params: options }));
  }
}
