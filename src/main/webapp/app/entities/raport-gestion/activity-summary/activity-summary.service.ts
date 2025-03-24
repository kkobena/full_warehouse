import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../../shared/util/request-util';
import { Recette } from './model/recette.model';
import { SERVER_API_URL } from '../../../app.constants';
import { MouvementCaisse } from './model/mouvement-caisse.model';
import { GroupeFournisseurAchat } from './model/groupe-fournisseur-achat.model';
import { ReglementTiersPayant } from './model/reglement-tiers-payant.model';
import { AchatTiersPayant } from './model/achat-tiers-payant.model';
import { ChiffreAffaire } from './model/chiffre-affaire.model';

@Injectable({
  providedIn: 'root',
})
export class ActivitySummaryService {
  private readonly http = inject(HttpClient);
  private resourceUrl = SERVER_API_URL + 'api/activity-summary';

  queryCa(req?: any): Observable<HttpResponse<ChiffreAffaire>> {
    const options = createRequestOptions(req);
    return this.http.get<ChiffreAffaire>(this.resourceUrl + '/ca', { params: options, observe: 'response' });
  }
  queryRecettes(req?: any): Observable<HttpResponse<Recette[]>> {
    const options = createRequestOptions(req);
    return this.http.get<Recette[]>(this.resourceUrl + '/recettes', { params: options, observe: 'response' });
  }

  getMouvementsCaisse(req?: any): Observable<HttpResponse<MouvementCaisse[]>> {
    const options = createRequestOptions(req);
    return this.http.get<MouvementCaisse[]>(this.resourceUrl + '/mouvements-caisse', { params: options, observe: 'response' });
  }

  getGroupeFournisseurAchat(req?: any): Observable<HttpResponse<GroupeFournisseurAchat[]>> {
    const options = createRequestOptions(req);
    return this.http.get<GroupeFournisseurAchat[]>(this.resourceUrl + '/achats', { params: options, observe: 'response' });
  }
  getReglementTiersPayants(req?: any): Observable<HttpResponse<ReglementTiersPayant[]>> {
    const options = createRequestOptions(req);
    return this.http.get<ReglementTiersPayant[]>(this.resourceUrl + '/reglements-tiers-payants', { params: options, observe: 'response' });
  }
  getAchatTiersPayant(req?: any): Observable<HttpResponse<AchatTiersPayant[]>> {
    const options = createRequestOptions(req);
    return this.http.get<AchatTiersPayant[]>(this.resourceUrl + '/achats-tiers-payants', { params: options, observe: 'response' });
  }

  onPrintPdf(req?: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/ca/pdf`, { params: options, responseType: 'blob' });
  }
}
