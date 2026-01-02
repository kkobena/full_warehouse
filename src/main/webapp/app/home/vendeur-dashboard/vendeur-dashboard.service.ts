import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  IVendeurDashboard,
  IMesPerformances,
  IMesClients,
  IVentesParType,
  ICommission,
  ITopProduitVendeur,
  IVenteRecenteVendeur,
  IOpportuniteVente,
  IObjectifMensuel,
  IClientFidele,
  VendeurDashboardResponseType,
  PerformancesResponseType,
  ClientsResponseType,
  VentesParTypeResponseType,
  CommissionResponseType,
  TopProduitsVendeurResponseType,
  VentesRecentesVendeurResponseType,
  OpportunitesResponseType,
  ObjectifsMensuelsResponseType,
  ClientsFidelesResponseType,
} from './vendeur-dashboard.model';

@Injectable({ providedIn: 'root' })
export class VendeurDashboardService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/vendeur/dashboard');

  /**
   * Get all dashboard data in one call
   */
  getDashboardData(): Observable<VendeurDashboardResponseType> {
    return this.http.get<IVendeurDashboard>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get seller's performance metrics
   */
  getMesPerformances(): Observable<PerformancesResponseType> {
    return this.http.get<IMesPerformances>(`${this.resourceUrl}/mes-performances`, { observe: 'response' });
  }

  /**
   * Get seller's clients statistics
   */
  getMesClients(): Observable<ClientsResponseType> {
    return this.http.get<IMesClients>(`${this.resourceUrl}/mes-clients`, { observe: 'response' });
  }

  /**
   * Get sales by type (ordonnance, conseil, parapharmacie)
   */
  getVentesParType(): Observable<VentesParTypeResponseType> {
    return this.http.get<IVentesParType>(`${this.resourceUrl}/ventes-par-type`, { observe: 'response' });
  }

  /**
   * Get commission information
   */
  getCommission(): Observable<CommissionResponseType> {
    return this.http.get<ICommission>(`${this.resourceUrl}/commission`, { observe: 'response' });
  }

  /**
   * Get top products sold by this seller
   */
  getTopProduits(limit = 10): Observable<TopProduitsVendeurResponseType> {
    return this.http.get<ITopProduitVendeur[]>(`${this.resourceUrl}/top-produits`, {
      params: { limit: limit.toString() },
      observe: 'response',
    });
  }

  /**
   * Get recent sales by this seller
   */
  getVentesRecentes(limit = 10): Observable<VentesRecentesVendeurResponseType> {
    return this.http.get<IVenteRecenteVendeur[]>(`${this.resourceUrl}/ventes-recentes`, {
      params: { limit: limit.toString() },
      observe: 'response',
    });
  }

  /**
   * Get sales opportunities
   */
  getOpportunites(): Observable<OpportunitesResponseType> {
    return this.http.get<IOpportuniteVente[]>(`${this.resourceUrl}/opportunites`, { observe: 'response' });
  }

  /**
   * Get monthly objectives
   */
  getObjectifsMensuels(): Observable<ObjectifsMensuelsResponseType> {
    return this.http.get<IObjectifMensuel[]>(`${this.resourceUrl}/objectifs-mensuels`, { observe: 'response' });
  }

  /**
   * Get loyal clients
   */
  getClientsFideles(limit = 10): Observable<ClientsFidelesResponseType> {
    return this.http.get<IClientFidele[]>(`${this.resourceUrl}/clients-fideles`, {
      params: { limit: limit.toString() },
      observe: 'response',
    });
  }

  /**
   * Refresh dashboard data
   */
  refreshDashboard(): Observable<VendeurDashboardResponseType> {
    return this.http.post<IVendeurDashboard>(`${this.resourceUrl}/refresh`, {}, { observe: 'response' });
  }

  /**
   * Export performance report
   */
  exportPerformanceReport(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export-performance`, {
      responseType: 'blob',
      observe: 'response',
    });
  }
}
