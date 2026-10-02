import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOption } from '../../shared/util/request-util';
import { Dci } from '../../shared/model/produit.model';
import { IProduitReferentiel } from '../../shared/model/produit-referentiel.model';

@Injectable({
  providedIn: 'root',
})
export class DciService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/dci';

  query(req?: any): Observable<HttpResponse<Dci[]>> {
    const options = createRequestOption(req);
    return this.http.get<Dci[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  /** Rapprochement du produit avec le référentiel médicament : sert au badge « DCI posée automatiquement ». */
  chargerReferentielProduit(produitId: number): Observable<IProduitReferentiel> {
    return this.http.get<IProduitReferentiel>(SERVER_API_URL + 'api/referentiel-medicament/produits/' + produitId);
  }

  queryUnpaged(req?: any): Observable<HttpResponse<Dci[]>> {
    const options = createRequestOption(req);
    return this.http.get<Dci[]>(this.resourceUrl + '/unpaged', { params: options, observe: 'response' });
  }
}
