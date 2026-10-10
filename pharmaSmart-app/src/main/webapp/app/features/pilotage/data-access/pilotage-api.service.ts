import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import {
  AchatsPilotage,
  AchatsVentesPilotage,
  AnalysePilotage,
  Axe,
  AlertePilotage,
  AxeAnalyse,
  ClientsPilotage,
  ComparaisonAnnees,
  DemarqueRentabilite,
  DifferesTresorerie,
  EncaissementsTresorerie,
  EquipePilotage,
  GrilleObjectifs,
  SaisieObjectifs,
  SuiviObjectifs,
  EcartsPilotage,
  ExplicationEcart,
  Indicateur,
  MargeRentabilite,
  ReglageAnalyse,
  ReglageAnnees,
  RemisesRentabilite,
  RequetePilotage,
  RupturesPilotage,
  SeriesPilotage,
  StockPilotage,
  TiersPayantTresorerie,
  VuePilotage,
} from '../models/pilotage.model';

@Injectable({ providedIn: 'root' })
export class PilotageApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/pilotage';
  private readonly http = inject(HttpClient);

  lireSeries(requete: RequetePilotage, indicateurs: readonly string[]): Observable<SeriesPilotage> {
    return this.http.get<SeriesPilotage>(`${this.resourceUrl}/series`, { params: versParametres(requete).set('indicateurs', indicateurs.join(',')) });
  }

  lireEcarts(requete: RequetePilotage, contributions: number): Observable<EcartsPilotage> {
    return this.http.get<EcartsPilotage>(`${this.resourceUrl}/ecarts`, { params: versParametres(requete).set('contributions', contributions) });
  }

  listerIndicateurs(): Observable<Indicateur[]> {
    return this.http.get<Indicateur[]>(`${this.resourceUrl}/indicateurs`);
  }

  listerAxes(): Observable<Axe[]> {
    return this.http.get<Axe[]>(`${this.resourceUrl}/axes`);
  }

  analyser(requete: RequetePilotage, reglage: ReglageAnalyse): Observable<AnalysePilotage> {
    return this.http.get<AnalysePilotage>(`${this.resourceUrl}/analyse`, { params: versParametresAnalyse(requete, reglage) });
  }

  /** Le chemin du réglage mène à l'élément dont on explique l'écart, cet élément compris. */
  expliquerEcart(requete: RequetePilotage, reglage: ReglageAnalyse): Observable<ExplicationEcart> {
    return this.http.get<ExplicationEcart>(`${this.resourceUrl}/analyse/explication`, { params: versParametresAnalyse(requete, reglage) });
  }

  /** Années civiles : seul « à date » vient de la barre de période. */
  comparerAnnees(aDate: boolean, reglage: ReglageAnnees): Observable<ComparaisonAnnees> {
    return this.http.get<ComparaisonAnnees>(`${this.resourceUrl}/annees`, { params: versParametresComparaisonAnnees(aDate, reglage) });
  }

  exporterSeries(requete: RequetePilotage, indicateurs: readonly string[]): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/series/export`, {
      params: versParametres(requete).set('indicateurs', indicateurs.join(',')),
      observe: 'response',
      responseType: 'blob',
    });
  }

  exporterAnalyse(requete: RequetePilotage, reglage: ReglageAnalyse): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/analyse/export`, { params: versParametresAnalyse(requete, reglage), observe: 'response', responseType: 'blob' });
  }

  exporterAnnees(aDate: boolean, reglage: ReglageAnnees): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/annees/export`, {
      params: versParametresComparaisonAnnees(aDate, reglage),
      observe: 'response',
      responseType: 'blob',
    });
  }

  lireMarge(requete: RequetePilotage, axe: AxeAnalyse): Observable<MargeRentabilite> {
    return this.http.get<MargeRentabilite>(`${this.resourceUrl}/rentabilite/marge`, { params: versParametres(requete).set('axe', axe) });
  }

  lireRemises(requete: RequetePilotage): Observable<RemisesRentabilite> {
    return this.http.get<RemisesRentabilite>(`${this.resourceUrl}/rentabilite/remises`, { params: versParametres(requete) });
  }

  lireDemarque(requete: RequetePilotage): Observable<DemarqueRentabilite> {
    return this.http.get<DemarqueRentabilite>(`${this.resourceUrl}/rentabilite/demarque`, { params: versParametres(requete) });
  }

  lireAchats(requete: RequetePilotage): Observable<AchatsPilotage> {
    return this.http.get<AchatsPilotage>(`${this.resourceUrl}/achats-stock/achats`, { params: versParametres(requete) });
  }

  lireAchatsVentes(requete: RequetePilotage): Observable<AchatsVentesPilotage> {
    return this.http.get<AchatsVentesPilotage>(`${this.resourceUrl}/achats-stock/achats-ventes`, { params: versParametres(requete) });
  }

  lireStock(requete: RequetePilotage): Observable<StockPilotage> {
    return this.http.get<StockPilotage>(`${this.resourceUrl}/achats-stock/stock`, { params: versParametres(requete) });
  }

  lireRuptures(requete: RequetePilotage): Observable<RupturesPilotage> {
    return this.http.get<RupturesPilotage>(`${this.resourceUrl}/achats-stock/ruptures`, { params: versParametres(requete) });
  }

  lireEncaissements(requete: RequetePilotage): Observable<EncaissementsTresorerie> {
    return this.http.get<EncaissementsTresorerie>(`${this.resourceUrl}/tresorerie/encaissements`, { params: versParametres(requete) });
  }

  lireTiersPayant(requete: RequetePilotage): Observable<TiersPayantTresorerie> {
    return this.http.get<TiersPayantTresorerie>(`${this.resourceUrl}/tresorerie/tiers-payant`, { params: versParametres(requete) });
  }

  lireDifferes(requete: RequetePilotage): Observable<DifferesTresorerie> {
    return this.http.get<DifferesTresorerie>(`${this.resourceUrl}/tresorerie/differes`, { params: versParametres(requete) });
  }

  lireClients(requete: RequetePilotage): Observable<ClientsPilotage> {
    return this.http.get<ClientsPilotage>(`${this.resourceUrl}/clients-equipe/clients`, { params: versParametres(requete) });
  }

  lireEquipe(requete: RequetePilotage): Observable<EquipePilotage> {
    return this.http.get<EquipePilotage>(`${this.resourceUrl}/clients-equipe/equipe`, { params: versParametres(requete) });
  }

  listerAlertes(): Observable<AlertePilotage[]> {
    return this.http.get<AlertePilotage[]>(`${this.resourceUrl}/alertes`);
  }

  lireObjectifs(annee: number): Observable<GrilleObjectifs> {
    return this.http.get<GrilleObjectifs>(`${this.resourceUrl}/objectifs`, { params: { annee } });
  }

  enregistrerObjectifs(saisie: SaisieObjectifs): Observable<GrilleObjectifs> {
    return this.http.put<GrilleObjectifs>(`${this.resourceUrl}/objectifs`, saisie);
  }

  proposerObjectifs(annee: number, indicateur: string, hausse: number): Observable<(number | null)[]> {
    return this.http.get<(number | null)[]>(`${this.resourceUrl}/objectifs/proposition`, { params: { annee, indicateur, hausse } });
  }

  suivreObjectifs(annee: number): Observable<SuiviObjectifs> {
    return this.http.get<SuiviObjectifs>(`${this.resourceUrl}/objectifs/suivi`, { params: { annee } });
  }

  listerVues(): Observable<VuePilotage[]> {
    return this.http.get<VuePilotage[]>(`${this.resourceUrl}/vues`);
  }

  enregistrerVue(vue: VuePilotage): Observable<VuePilotage> {
    return this.http.post<VuePilotage>(`${this.resourceUrl}/vues`, vue);
  }

  supprimerVue(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/vues/${id}`);
  }
}

function versParametresAnalyse(requete: RequetePilotage, reglage: ReglageAnalyse): HttpParams {
  let params = versParametres(requete)
    .set('indicateurs', reglage.indicateurs.join(','))
    .set('axe', reglage.axe)
    .set('top', reglage.top)
    .set('tri', reglage.tri);
  if (reglage.axe2) {
    params = params.set('axe2', reglage.axe2);
  }
  return reglage.chemin.reduce((parametres, etape) => parametres.append('filtre', `${etape.axe}:${etape.cle}`), params);
}

function versParametres(requete: RequetePilotage): HttpParams {
  return new HttpParams()
    .set('du', requete.du)
    .set('au', requete.au)
    .set('comparaison', requete.comparaison)
    .set('anneesEnArriere', requete.anneesEnArriere)
    .set('aDate', requete.aDate)
    .set('granularite', requete.granularite);
}

function versParametresComparaisonAnnees(aDate: boolean, reglage: ReglageAnnees): HttpParams {
  const params = new HttpParams()
    .set('indicateur', reglage.indicateur)
    .set('annees', reglage.annees)
    .set('mode', reglage.mode)
    .set('parJourOuvre', reglage.parJourOuvre)
    .set('aDate', aDate);
  return reglage.filtre ? params.set('filtre', `${reglage.filtre.axe}:${reglage.filtre.cle}`) : params;
}
