import { inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ApplicationConfigService } from "app/core/config/application-config.service";
import { DashboardResponseType, ICaissierDashboard } from "./caissier-dashboard.model";

@Injectable({ providedIn: "root" })
export class CaissierDashboardService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);

  private resourceUrl = this.applicationConfigService.getEndpointFor("api/caissier/dashboard");

  /** Charge toutes les données du dashboard en un seul appel */
  getDashboardData(): Observable<DashboardResponseType> {
    return this.http.get<ICaissierDashboard>(this.resourceUrl, { observe: "response" });
  }

}


