import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IScheduledReport } from 'app/shared/model/scheduler/scheduled-report.model';

type EntityResponseType = HttpResponse<IScheduledReport>;
type EntityArrayResponseType = HttpResponse<IScheduledReport[]>;

/**
 * Service for managing scheduled reports
 */
@Injectable({ providedIn: 'root' })
export class ScheduledReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/scheduled-reports';
  private readonly http = inject(HttpClient);

  /**
   * Create a new scheduled report
   */
  create(scheduledReport: IScheduledReport): Observable<EntityResponseType> {
    return this.http.post<IScheduledReport>(this.resourceUrl, scheduledReport, { observe: 'response' });
  }

  /**
   * Update a scheduled report
   */
  update(scheduledReport: IScheduledReport): Observable<EntityResponseType> {
    return this.http.put<IScheduledReport>(`${this.resourceUrl}/${scheduledReport.id}`, scheduledReport, {
      observe: 'response',
    });
  }

  /**
   * Get all scheduled reports
   */
  query(): Observable<EntityArrayResponseType> {
    return this.http.get<IScheduledReport[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get all active scheduled reports
   */
  queryActive(): Observable<EntityArrayResponseType> {
    return this.http.get<IScheduledReport[]>(`${this.resourceUrl}/active`, { observe: 'response' });
  }

  /**
   * Delete a scheduled report
   */
  delete(id: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /**
   * Execute a scheduled report immediately
   */
  execute(id: number): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/${id}/execute`, null, { observe: 'response' });
  }
}
