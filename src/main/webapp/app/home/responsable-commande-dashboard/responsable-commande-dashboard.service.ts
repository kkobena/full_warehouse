import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IResponsableCommandeDashboard,
  ISuggestionReappro,
  ICommandeAReceptionner,
  IAnalyseABC,
  IPerformanceFournisseur,
  IAlertNotification,
} from './responsable-commande-dashboard.model';

type DashboardResponseType = HttpResponse<IResponsableCommandeDashboard>;
type SuggestionsResponseType = HttpResponse<ISuggestionReappro[]>;
type CommandesResponseType = HttpResponse<ICommandeAReceptionner[]>;
type ABCResponseType = HttpResponse<IAnalyseABC>;
type FournisseursResponseType = HttpResponse<IPerformanceFournisseur[]>;
type NotificationsResponseType = HttpResponse<IAlertNotification[]>;

@Injectable({ providedIn: 'root' })
export class ResponsableCommandeDashboardService {
  private http = inject(HttpClient);
  private resourceUrl = 'api/responsable-commande/dashboard';

  /**
   * Récupère les données complètes du dashboard
   */
  getDashboardData(): Observable<DashboardResponseType> {
    return this.http.get<IResponsableCommandeDashboard>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Récupère les suggestions de réapprovisionnement automatiques
   */
  getSuggestionsReappro(): Observable<SuggestionsResponseType> {
    return this.http.get<ISuggestionReappro[]>(`${this.resourceUrl}/suggestions`, { observe: 'response' });
  }

  /**
   * Génère une commande à partir des suggestions sélectionnées
   */
  genererCommandeFromSuggestions(suggestions: ISuggestionReappro[]): Observable<HttpResponse<any>> {
    return this.http.post(`api/commande/auto-generate`, suggestions, { observe: 'response' });
  }

  /**
   * Récupère les commandes à réceptionner
   */
  getCommandesAReceptionner(): Observable<CommandesResponseType> {
    return this.http.get<ICommandeAReceptionner[]>(`${this.resourceUrl}/commandes-a-receptionner`, { observe: 'response' });
  }

  /**
   * Récupère l'analyse ABC du stock
   */
  getAnalyseABC(): Observable<ABCResponseType> {
    return this.http.get<IAnalyseABC>(`${this.resourceUrl}/analyse-abc`, { observe: 'response' });
  }

  /**
   * Récupère les performances des fournisseurs
   */
  getPerformanceFournisseurs(top = 5): Observable<FournisseursResponseType> {
    return this.http.get<IPerformanceFournisseur[]>(`${this.resourceUrl}/performance-fournisseurs`, {
      params: { top: top.toString() },
      observe: 'response',
    });
  }

  /**
   * Récupère les notifications et alertes
   */
  getNotifications(): Observable<NotificationsResponseType> {
    return this.http.get<IAlertNotification[]>(`${this.resourceUrl}/notifications`, { observe: 'response' });
  }

  /**
   * Rafraîchit les données du dashboard
   */
  refreshDashboard(): Observable<DashboardResponseType> {
    return this.http.post<IResponsableCommandeDashboard>(`${this.resourceUrl}/refresh`, {}, { observe: 'response' });
  }
}
