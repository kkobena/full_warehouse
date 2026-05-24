import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IPnlEvolution, IPnlFamille, IPnlSegment } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class PnlAnalytiqueService {
  private readonly resourceUrl = SERVER_API_URL + 'api/pnl-analytique';
  private readonly http = inject(HttpClient);

  getSnapshotBySegment(year?: number): Observable<HttpResponse<IPnlSegment[]>> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    return this.http.get<IPnlSegment[]>(`${this.resourceUrl}/segment`, { params, observe: 'response' });
  }

  getSnapshotByFamille(year?: number): Observable<HttpResponse<IPnlFamille[]>> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    return this.http.get<IPnlFamille[]>(`${this.resourceUrl}/famille`, { params, observe: 'response' });
  }

  getEvolutionByFamille(): Observable<HttpResponse<IPnlEvolution>> {
    return this.http.get<IPnlEvolution>(`${this.resourceUrl}/evolution`, { observe: 'response' });
  }

  getEvolutionBySegment(): Observable<HttpResponse<IPnlEvolution>> {
    return this.http.get<IPnlEvolution>(`${this.resourceUrl}/evolution-segment`, { observe: 'response' });
  }
}
