import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { AllowedWidget, DateRange, FRONT_ONLY_PARAMS as FRONT_ONLY, WidgetData } from '../../models/dashboard.model';

const FRONT_ONLY_PARAMS = new Set<string>(FRONT_ONLY);

@Injectable({ providedIn: 'root' })
export class DashboardWidgetApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/dashboard-widgets';
  private readonly http = inject(HttpClient);

  /** Widgets que l'utilisateur a le droit d'ajouter ; `licensed = false` : hors abonnement. */
  allowed(): Observable<AllowedWidget[]> {
    return this.http.get<AllowedWidget[]>(`${this.resourceUrl}/allowed`);
  }

  /** Données d'un widget ; les droits sont vérifiés par le serveur, qui refuse en 400 avec un message. */
  load(key: string, range: DateRange | null, params: Record<string, string> = {}): Observable<WidgetData> {
    let httpParams = new HttpParams();
    if (range) {
      httpParams = httpParams.set('startDate', range.start).set('endDate', range.end);
    }
    for (const [name, value] of Object.entries(params)) {
      if (!FRONT_ONLY_PARAMS.has(name) && value != null && value !== '') {
        httpParams = httpParams.set(name, value);
      }
    }
    return this.http.get<WidgetData>(`${this.resourceUrl}/${encodeURIComponent(key)}`, { params: httpParams });
  }
}
