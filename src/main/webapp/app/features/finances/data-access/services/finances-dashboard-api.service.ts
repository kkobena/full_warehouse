import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IFinancesSummary } from '../models';

@Injectable({ providedIn: 'root' })
export class FinancesDashboardApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/dashboard-ca';
  private readonly http = inject(HttpClient);

  getSummaryFinances(): Observable<HttpResponse<IFinancesSummary>> {
    return this.http.get<IFinancesSummary>(`${this.resourceUrl}/summary-finances`, { observe: 'response' });
  }
}
