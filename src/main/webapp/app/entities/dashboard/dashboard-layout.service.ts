import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IDashboardLayout, IDashboardLayoutParsed } from 'app/shared/model/dashboard-layout.model';

type EntityResponseType = HttpResponse<IDashboardLayout>;
type EntityArrayResponseType = HttpResponse<IDashboardLayout[]>;

/**
 * Service for managing Dashboard Layouts
 */
@Injectable({ providedIn: 'root' })
export class DashboardLayoutService {
  private readonly resourceUrl = SERVER_API_URL + 'api/dashboard-layouts';
  private readonly http = inject(HttpClient);

  /**
   * Create a new dashboard layout
   */
  create(dashboardLayout: IDashboardLayout): Observable<EntityResponseType> {
    return this.http.post<IDashboardLayout>(this.resourceUrl, dashboardLayout, { observe: 'response' });
  }

  /**
   * Update a dashboard layout
   */
  update(id: number, dashboardLayout: IDashboardLayout): Observable<EntityResponseType> {
    return this.http.put<IDashboardLayout>(`${this.resourceUrl}/${id}`, dashboardLayout, { observe: 'response' });
  }

  /**
   * Get all layouts for current user
   */
  query(): Observable<EntityArrayResponseType> {
    return this.http.get<IDashboardLayout[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get all public layouts
   */
  queryPublic(): Observable<EntityArrayResponseType> {
    return this.http.get<IDashboardLayout[]>(`${this.resourceUrl}/public`, { observe: 'response' });
  }

  /**
   * Get a specific layout
   */
  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDashboardLayout>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /**
   * Get current user's default layout
   */
  getDefault(): Observable<EntityResponseType> {
    return this.http.get<IDashboardLayout>(`${this.resourceUrl}/default`, { observe: 'response' });
  }

  /**
   * Set layout as default
   */
  setAsDefault(id: number): Observable<EntityResponseType> {
    return this.http.put<IDashboardLayout>(`${this.resourceUrl}/${id}/set-default`, null, { observe: 'response' });
  }

  /**
   * Clone a layout
   */
  clone(id: number, newName: string): Observable<EntityResponseType> {
    return this.http.post<IDashboardLayout>(`${this.resourceUrl}/${id}/clone?newName=${encodeURIComponent(newName)}`, null, {
      observe: 'response',
    });
  }

  /**
   * Delete a layout
   */
  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /**
   * Parse layout config from JSON string
   */
  parseLayout(layout: IDashboardLayout): IDashboardLayoutParsed {
    return {
      ...layout,
      config: layout.layoutConfig ? JSON.parse(layout.layoutConfig) : undefined,
    };
  }

  /**
   * Stringify layout config to JSON
   */
  stringifyLayout(layout: IDashboardLayoutParsed): IDashboardLayout {
    return {
      ...layout,
      layoutConfig: layout.config ? JSON.stringify(layout.config) : undefined,
    };
  }
}
