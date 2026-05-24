import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../app.constants';
import { FactureId, FneResponse } from './facture.model';

@Injectable({
  providedIn: 'root',
})
export class CertificationService {
  private readonly resourceUrl = SERVER_API_URL + 'api/certification-factures';
  private readonly http = inject(HttpClient);

  certify(factureId: FactureId): Observable<HttpResponse<FneResponse>> {
    return this.http.get<FneResponse>(`${this.resourceUrl}/certifier/${factureId.id}/${factureId.invoiceDate}`, {
      observe: 'response',
    });
  }

  certifyGroupInvoice(factureId: FactureId): Observable<HttpResponse<void>> {
    return this.http.get<void>(`${this.resourceUrl}/certifier-groupe/${factureId.id}/${factureId.invoiceDate}`, {
      observe: 'response',
    });
  }
}
