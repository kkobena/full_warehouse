import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IConcentrationEvolution, IConcentrationOrganisme, IConcentrationSummary } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class ConcentrationPayersService {
  private readonly resourceUrl = SERVER_API_URL + 'api/concentration-payers';
  private readonly http = inject(HttpClient);

  getSummary(periode: string, topN: number = 10): Observable<HttpResponse<IConcentrationSummary>> {
    const params = new HttpParams().set('periode', periode).set('topN', topN);
    return this.http.get<IConcentrationSummary>(`${this.resourceUrl}/summary`, { params, observe: 'response' });
  }

  getOrganismes(periode: string, topN: number = 10): Observable<HttpResponse<IConcentrationOrganisme[]>> {
    const params = new HttpParams().set('periode', periode).set('topN', topN);
    return this.http.get<IConcentrationOrganisme[]>(`${this.resourceUrl}/organismes`, { params, observe: 'response' });
  }

  getEvolution(topN: number = 10): Observable<HttpResponse<IConcentrationEvolution>> {
    const params = new HttpParams().set('topN', topN);
    return this.http.get<IConcentrationEvolution>(`${this.resourceUrl}/evolution`, { params, observe: 'response' });
  }
}
