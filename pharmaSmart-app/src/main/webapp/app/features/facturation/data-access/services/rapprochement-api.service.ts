import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../../app.constants';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IEtatRapprochement } from '../models';
import { IPaymentId, IReglementParams, IResponseReglement } from '../models';

/**
 * Service de consultation de l'état de rapprochement.
 *
 * Mutations (créer / annuler un règlement) → déléguer vers ReglementApiService :
 *   - Créer  : POST /api/reglement-factures-tp  (ReglementApiService.doReglement)
 *   - Annuler: DELETE /api/reglements/{id}/{transactionDate}  (ReglementApiService.delete)
 *
 * Ce service ne contient QUE la lecture agrégée, pour éviter tout doublon avec
 * le service de règlement existant (ReglementFactureTpResource / AbstractReglementService)
 * qui gère correctement la mise à jour des ThirdPartySaleLine et InvoicePaymentItem.
 */
@Injectable({ providedIn: 'root' })
export class RapprochementApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/rapprochement';
  private readonly http = inject(HttpClient);

  /** Lecture seule — état agrégé par organisme */
  query(params: any, pageable?: any): Observable<HttpResponse<IEtatRapprochement[]>> {
    return this.http.get<IEtatRapprochement[]>(this.resourceUrl, {
      params: createRequestOptions({ ...params, ...pageable }),
      observe: 'response',
    });
  }

  exportPdf(params: any): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }

  exportExcel(params: any): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/excel`, {
      params: createRequestOptions(params),
      responseType: 'blob',
    });
  }
}
