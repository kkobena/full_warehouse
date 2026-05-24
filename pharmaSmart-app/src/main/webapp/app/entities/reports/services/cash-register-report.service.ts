import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IDailyCashRegisterReport, ICashMovement } from 'app/shared/model/report/cash-register-report.model';

type DailyReportArrayResponseType = HttpResponse<IDailyCashRegisterReport[]>;
type CashMovementArrayResponseType = HttpResponse<ICashMovement[]>;

@Injectable({ providedIn: 'root' })
export class CashRegisterReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/cash-register';
  private readonly http = inject(HttpClient);

  /**
   * Get daily cash register report for a specific date
   */
  getDailyReport(date?: string): Observable<DailyReportArrayResponseType> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<IDailyCashRegisterReport[]>(`${this.resourceUrl}/daily-report`, { params, observe: 'response' });
  }

  /**
   * Get cash movements history
   */
  getCashMovements(
    startDate: string,
    endDate: string,
    userId?: number,
    cashRegisterId?: number,
  ): Observable<CashMovementArrayResponseType> {
    let params = new HttpParams().set('startDate', startDate).set('endDate', endDate);

    if (userId) {
      params = params.set('userId', userId.toString());
    }
    if (cashRegisterId) {
      params = params.set('cashRegisterId', cashRegisterId.toString());
    }

    return this.http.get<ICashMovement[]>(`${this.resourceUrl}/movements`, { params, observe: 'response' });
  }

  /**
   * Get cash register summary for a period
   */
  getCashRegisterSummary(startDate?: string, endDate?: string): Observable<DailyReportArrayResponseType> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<IDailyCashRegisterReport[]>(`${this.resourceUrl}/summary`, { params, observe: 'response' });
  }

  /**
   * Export daily cash register report as PDF
   */
  exportDailyReportToPdf(date?: string): Observable<HttpResponse<Blob>> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get(`${this.resourceUrl}/daily-report/export`, {
      params,
      responseType: 'blob',
      observe: 'response',
    });
  }
}
