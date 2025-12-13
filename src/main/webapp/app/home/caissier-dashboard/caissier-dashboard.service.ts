import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  ICaissierDashboard,
  IVentesJour,
  ICaisseStatus,
  IStatistiquesRapides,
  IVenteRecente,
  ITopProduit,
  IPerformanceVendeur,
  IAlerteCaisse,
  DashboardResponseType,
  VentesResponseType,
  CaisseResponseType,
  StatistiquesResponseType,
  VentesRecentesResponseType,
  TopProduitsResponseType,
  PerformanceResponseType,
  AlertesResponseType,
} from './caissier-dashboard.model';

@Injectable({ providedIn: 'root' })
export class CaissierDashboardService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/caissier/dashboard');

  getDashboardData(): Observable<DashboardResponseType> {
    return this.http.get<ICaissierDashboard>(this.resourceUrl, { observe: 'response' });
  }

  getVentesJour(): Observable<VentesResponseType> {
    return this.http.get<IVentesJour>(`${this.resourceUrl}/ventes-jour`, { observe: 'response' });
  }

  getCaisseStatus(): Observable<CaisseResponseType> {
    return this.http.get<ICaisseStatus>(`${this.resourceUrl}/caisse-status`, { observe: 'response' });
  }

  getStatistiquesRapides(): Observable<StatistiquesResponseType> {
    return this.http.get<IStatistiquesRapides>(`${this.resourceUrl}/statistiques-rapides`, { observe: 'response' });
  }

  getVentesRecentes(limit: number = 10): Observable<VentesRecentesResponseType> {
    return this.http.get<IVenteRecente[]>(`${this.resourceUrl}/ventes-recentes`, {
      params: { limit: limit.toString() },
      observe: 'response',
    });
  }

  getTopProduits(limit: number = 10): Observable<TopProduitsResponseType> {
    return this.http.get<ITopProduit[]>(`${this.resourceUrl}/top-produits`, {
      params: { limit: limit.toString() },
      observe: 'response',
    });
  }

  getPerformanceVendeurs(): Observable<PerformanceResponseType> {
    return this.http.get<IPerformanceVendeur[]>(`${this.resourceUrl}/performance-vendeurs`, { observe: 'response' });
  }

  getAlertes(): Observable<AlertesResponseType> {
    return this.http.get<IAlerteCaisse[]>(`${this.resourceUrl}/alertes`, { observe: 'response' });
  }

  refreshDashboard(): Observable<DashboardResponseType> {
    return this.http.post<ICaissierDashboard>(`${this.resourceUrl}/refresh`, {}, { observe: 'response' });
  }

  ouvrirCaisse(montantOuverture: number): Observable<HttpResponse<any>> {
    return this.http.post<any>(
      `${this.resourceUrl}/ouvrir-caisse`,
      { montantOuverture },
      { observe: 'response' }
    );
  }

  fermerCaisse(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/fermer-caisse`, {}, { observe: 'response' });
  }

  imprimerRapportCaisse(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/imprimer-rapport`, {
      responseType: 'blob',
      observe: 'response',
    });
  }
}
