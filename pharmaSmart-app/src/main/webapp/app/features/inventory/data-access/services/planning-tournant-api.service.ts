import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IPlanningInventaireTournant, ITournantDashboard } from '../../models/planning-tournant.model';

@Injectable({ providedIn: 'root' })
export class PlanningTournantApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/plannings-tournants';

  findAll(): Observable<IPlanningInventaireTournant[]> {
    return this.http.get<IPlanningInventaireTournant[]>(this.resourceUrl);
  }

  findById(id: number): Observable<IPlanningInventaireTournant> {
    return this.http.get<IPlanningInventaireTournant>(`${this.resourceUrl}/${id}`);
  }

  create(record: IPlanningInventaireTournant): Observable<IPlanningInventaireTournant> {
    return this.http.post<IPlanningInventaireTournant>(this.resourceUrl, record);
  }

  update(id: number, record: IPlanningInventaireTournant): Observable<IPlanningInventaireTournant> {
    return this.http.put<IPlanningInventaireTournant>(`${this.resourceUrl}/${id}`, record);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }

  toggleActif(id: number): Observable<IPlanningInventaireTournant> {
    return this.http.put<IPlanningInventaireTournant>(`${this.resourceUrl}/${id}/toggle`, {});
  }

  executerManuellement(id: number): Observable<{ inventoryId: number }> {
    return this.http.post<{ inventoryId: number }>(`${this.resourceUrl}/${id}/executer`, {});
  }

  getDashboard(storageId?: number): Observable<ITournantDashboard> {
    const params = storageId ? { storageId: storageId.toString() } : {};
    return this.http.get<ITournantDashboard>(`${this.resourceUrl}/dashboard`, { params });
  }
}
