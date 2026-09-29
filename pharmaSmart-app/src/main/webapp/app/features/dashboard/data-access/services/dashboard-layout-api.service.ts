import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';

type EntityResponseType = HttpResponse<IDashboardLayout>;
type EntityArrayResponseType = HttpResponse<IDashboardLayout[]>;

/** Layouts de dashboard : CRUD, accueil personnel et accueil par rôle. */
@Injectable({ providedIn: 'root' })
export class DashboardLayoutApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/dashboard-layouts';
  private readonly http = inject(HttpClient);

  create(dashboardLayout: IDashboardLayout): Observable<EntityResponseType> {
    return this.http.post<IDashboardLayout>(this.resourceUrl, dashboardLayout, { observe: 'response' });
  }

  update(id: number, dashboardLayout: IDashboardLayout): Observable<EntityResponseType> {
    return this.http.put<IDashboardLayout>(`${this.resourceUrl}/${id}`, dashboardLayout, { observe: 'response' });
  }

  /** Layouts de l'utilisateur et layouts publics des autres (hors layouts système). */
  query(): Observable<EntityArrayResponseType> {
    return this.http.get<IDashboardLayout[]>(this.resourceUrl, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDashboardLayout>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /** Accueil personnel de l'utilisateur ; 204 s'il n'en a pas. */
  getDefault(): Observable<EntityResponseType> {
    return this.http.get<IDashboardLayout>(`${this.resourceUrl}/default`, { observe: 'response' });
  }

  /**
   * Résout le layout effectif pour l'utilisateur connecté.
   *
   * Ordre de priorité (côté backend) :
   *  1. Layout personnel (user isDefault=true)
   *  2. Layout par rôle  (authority isDefault=true)
   *  3. 204 No Content   → body null → HomeComponent → DefaultDashboard
   *
   * Si isRoute=true  : name contient la route Angular → router.navigate([name])
   * Si isRoute=false : componentKey choisit le composant (CUSTOM = grille de widgets)
   */
  getResolved(): Observable<EntityResponseType> {
    return this.http.get<IDashboardLayout>(`${this.resourceUrl}/resolved`, { observe: 'response' });
  }

  setAsDefault(id: number): Observable<EntityResponseType> {
    return this.http.put<IDashboardLayout>(`${this.resourceUrl}/${id}/set-default`, null, { observe: 'response' });
  }

  /** Réservé à l'administrateur. */
  setAsDefaultForRole(id: number, authorityName: string): Observable<EntityResponseType> {
    return this.http.put<IDashboardLayout>(`${this.resourceUrl}/${id}/set-default-for-role`, null, {
      observe: 'response',
      params: { authorityName },
    });
  }

  clone(id: number, newName: string): Observable<EntityResponseType> {
    return this.http.post<IDashboardLayout>(`${this.resourceUrl}/${id}/clone`, null, { observe: 'response', params: { newName } });
  }

  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
