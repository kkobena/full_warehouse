import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SERVER_API_URL} from '../../../../app.constants';
import {IAppariement, IOrdonnance, IOrdonnanceCreation, IPrescripteur, StatutOrdonnance} from '../../../../shared/model/ordonnance.model';

/** Service HTTP du suivi d'ordonnance (/api/ordonnances/*) et des prescripteurs (/api/prescripteurs/*). */
@Injectable({providedIn: 'root'})
export class OrdonnanceApiService {
  private readonly http = inject(HttpClient);
  private readonly ordonnancesUrl = SERVER_API_URL + 'api/ordonnances';
  private readonly prescripteursUrl = SERVER_API_URL + 'api/prescripteurs';

  duClient(customerId: number, statut?: StatutOrdonnance): Observable<IOrdonnance[]> {
    let params = new HttpParams().set('customerId', customerId);
    if (statut) {
      params = params.set('statut', statut);
    }
    return this.http.get<IOrdonnance[]>(this.ordonnancesUrl, {params});
  }

  creer(ordonnance: IOrdonnanceCreation): Observable<IOrdonnance> {
    return this.http.post<IOrdonnance>(this.ordonnancesUrl, ordonnance);
  }

  cloturer(id: number, cloturee: boolean): Observable<IOrdonnance> {
    return this.http.put<IOrdonnance>(`${this.ordonnancesUrl}/${id}/cloture`, null, {params: {cloturee}});
  }

  rattacherVente(id: number, salesId: number, salesDate: string): Observable<IOrdonnance> {
    return this.http.post<IOrdonnance>(`${this.ordonnancesUrl}/${id}/ventes`, {salesId, salesDate});
  }

  /** Rattache la vente et lie ses lignes aux lignes prescrites : même produit, ou même groupe générique. */
  apparierVente(id: number, salesId: number, salesDate: string): Observable<IAppariement> {
    return this.http.post<IAppariement>(`${this.ordonnancesUrl}/${id}/ventes/apparier`, {salesId, salesDate});
  }

  /** Prescripteur d'une vente sans ordonnance : lève l'exigence des produits sur ordonnance à la clôture. */
  definirPrescripteurDeVente(salesId: number, salesDate: string, prescripteurId: number): Observable<void> {
    return this.http.post<void>(`${this.ordonnancesUrl}/ventes/prescripteur`, {salesId, salesDate, prescripteurId});
  }

  lierLigneDeVente(id: number, ligneId: number, salesLineId: number, salesLineDate: string): Observable<IOrdonnance> {
    return this.http.post<IOrdonnance>(`${this.ordonnancesUrl}/${id}/lignes/${ligneId}/delivrances`, {salesLineId, salesLineDate});
  }

  rechercherPrescripteurs(q: string, limite = 10): Observable<IPrescripteur[]> {
    return this.http.get<IPrescripteur[]>(this.prescripteursUrl, {params: {q, limite}});
  }

  modifierPrescripteur(id: number, prescripteur: IPrescripteur): Observable<IPrescripteur> {
    return this.http.put<IPrescripteur>(`${this.prescripteursUrl}/${id}`, prescripteur);
  }

  definirPrescripteurActif(id: number, actif: boolean): Observable<void> {
    return this.http.put<void>(`${this.prescripteursUrl}/${id}/actif`, null, {params: {actif}});
  }

  /** Les ordonnances de `id` passent sur `cibleId` ; la fiche `id` est désactivée. */
  fusionnerPrescripteur(id: number, cibleId: number): Observable<IPrescripteur> {
    return this.http.post<IPrescripteur>(`${this.prescripteursUrl}/${id}/fusion`, null, {params: {cibleId}});
  }

  creerPrescripteur(prescripteur: IPrescripteur): Observable<IPrescripteur> {
    return this.http.post<IPrescripteur>(this.prescripteursUrl, prescripteur);
  }
}
