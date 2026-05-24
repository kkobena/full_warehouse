import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IDsoOrganisme, IEncoursMensuel, IVieillissementGlobal } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class VieillissementCreancesService {
  private readonly resourceUrl = SERVER_API_URL + 'api/vieillissement-creances';
  private readonly http = inject(HttpClient);

  getAgingGlobal(): Observable<HttpResponse<IVieillissementGlobal>> {
    return this.http.get<IVieillissementGlobal>(`${this.resourceUrl}/global`, { observe: 'response' });
  }

  getDsoByOrganisme(page = 0, size = 15): Observable<HttpResponse<IDsoOrganisme[]>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<IDsoOrganisme[]>(`${this.resourceUrl}/dso-organisme`, { params, observe: 'response' });
  }

  getEncoursMensuelEvolution(): Observable<HttpResponse<IEncoursMensuel>> {
    return this.http.get<IEncoursMensuel>(`${this.resourceUrl}/encours-evolution`, { observe: 'response' });
  }
}
