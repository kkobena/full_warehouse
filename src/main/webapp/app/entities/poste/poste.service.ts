import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../app.constants';
import { IPoste } from '../../shared/model/poste.model';

type EntityResponseType = HttpResponse<IPoste>;
type EntityArrayResponseType = HttpResponse<IPoste[]>;

@Injectable({
  providedIn: 'root',
})
export class PosteService {
  protected http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/postes';

  create(poste: IPoste): Observable<HttpResponse<void>> {
    return this.http.post<void>(this.resourceUrl, poste, { observe: 'response' });
  }

  fetchAll(): Observable<EntityArrayResponseType> {
    return this.http.get<IPoste[]>(this.resourceUrl, { observe: 'response' });
  }

  getCurrentPoste(): Observable<EntityResponseType> {
    return this.http.get<IPoste>(`${this.resourceUrl}/current`, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
