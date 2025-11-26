import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { ITiersPayantInvoice, ITiersPayantCreancesSummary, AgeCategory } from 'app/shared/model/report/tiers-payant-report.model';

type InvoiceArrayResponseType = HttpResponse<ITiersPayantInvoice[]>;
type SummaryArrayResponseType = HttpResponse<ITiersPayantCreancesSummary[]>;

@Injectable({ providedIn: 'root' })
export class TiersPayantReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/tiers-payant';
  private readonly http = inject(HttpClient);

  /**
   * Get unpaid invoices (créances)
   */
  getUnpaidInvoices(groupeTiersPayantId?: number, ageCategory?: AgeCategory): Observable<InvoiceArrayResponseType> {
    let params = new HttpParams();
    if (groupeTiersPayantId) {
      params = params.set('groupeTiersPayantId', groupeTiersPayantId.toString());
    }
    if (ageCategory) {
      params = params.set('ageCategory', ageCategory);
    }
    return this.http.get<ITiersPayantInvoice[]>(`${this.resourceUrl}/creances`, { params, observe: 'response' });
  }

  /**
   * Get créances summary by groupe tiers payant
   */
  getCreancesSummary(): Observable<SummaryArrayResponseType> {
    return this.http.get<ITiersPayantCreancesSummary[]>(`${this.resourceUrl}/creances/summary`, { observe: 'response' });
  }

  /**
   * Get payment history
   */
  getPaymentHistory(groupeTiersPayantId?: number, startDate?: string, endDate?: string): Observable<InvoiceArrayResponseType> {
    let params = new HttpParams();
    if (groupeTiersPayantId) {
      params = params.set('groupeTiersPayantId', groupeTiersPayantId.toString());
    }
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<ITiersPayantInvoice[]>(`${this.resourceUrl}/payment-history`, { params, observe: 'response' });
  }

  /**
   * Export créances report as PDF
   */
  exportCreancesToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/creances/export`, {
      responseType: 'blob',
      observe: 'response',
    });
  }
}
