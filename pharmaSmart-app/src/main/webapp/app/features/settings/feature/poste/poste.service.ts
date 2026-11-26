import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { expectErrorStatuses } from 'app/core/interceptor/expected-error-statuses';
import { IPoste } from "../../../../shared/model/poste.model";


type EntityResponseType = HttpResponse<IPoste>;
type EntityArrayResponseType = HttpResponse<IPoste[]>;

@Injectable({
  providedIn: 'root',
})
export class PosteService {
  private readonly  http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/postes';

  create(poste: IPoste): Observable<EntityResponseType> {
    return this.http.post<IPoste>(this.resourceUrl, poste, { observe: 'response' });
  }

  fetchAll(): Observable<EntityArrayResponseType> {
    return this.http.get<IPoste[]>(this.resourceUrl, { observe: 'response' });
  }

  getCurrentPoste(): Observable<EntityResponseType> {
    // 404 = poste non enregistré : réponse prévue, pas une erreur à afficher.
    return this.http.get<IPoste>(`${this.resourceUrl}/current`, { observe: 'response', context: expectErrorStatuses(404) });
  }

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
