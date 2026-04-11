import { inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ApplicationConfigService } from "app/core/config/application-config.service";
import { INavNode, NavAssignPayload, NavItemAssignment, NavReorderPayload } from "app/shared/model/nav-item.model";

export interface INavRole {
  name: string;
  libelle?: string;
}

@Injectable({ providedIn: "root" })
export class NavApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);

  getMyNavItems(): Observable<INavNode[]> {
    return this.http.get<INavNode[]>(this.config.getEndpointFor("api/nav/my-items"));
  }

  saveUserReorder(reorderList: NavReorderPayload[]): Observable<void> {
    return this.http.put<void>(this.config.getEndpointFor("api/nav/reorder"), reorderList);
  }

  getAllNavItems(): Observable<INavNode[]> {
    return this.http.get<INavNode[]>(this.config.getEndpointFor("api/admin/nav/items"));
  }

  getAllNavItemsForRole(roleName: string): Observable<INavNode[]> {
    return this.http.get<INavNode[]>(this.config.getEndpointFor("api/admin/nav/items"), { params: { roleName } });
  }

  saveAdminReorder(reorderList: NavReorderPayload[]): Observable<void> {
    return this.http.put<void>(this.config.getEndpointFor("api/admin/nav/reorder"), reorderList);
  }

  assignItemsToRole(dto: NavAssignPayload): Observable<void> {
    return this.http.post<void>(this.config.getEndpointFor("api/admin/nav/assign"), dto);
  }

  updateSinglePermission(roleName: string, assignment: NavItemAssignment): Observable<void> {
    return this.http.post<void>(this.config.getEndpointFor("api/admin/nav/assign"), {
      roleName,
      assignments: [assignment],
    });
  }

  updateNavItemLibelle(id: number, libelle: string): Observable<void> {
    return this.http.patch<void>(this.config.getEndpointFor(`api/admin/nav/items/${id}/libelle`), { libelle });
  }


  getAllRoles(): Observable<INavRole[]> {
    return this.http.get<INavRole[]>(this.config.getEndpointFor("api/authorities/all/v2"));
  }

  createRole(name: string, libelle: string): Observable<void> {
    return this.http.post<void>(this.config.getEndpointFor("api/authorities/save"), { name, libelle });
  }

  updateRoleLibelle(name: string, libelle: string): Observable<void> {
    return this.http.post<void>(this.config.getEndpointFor("api/authorities/save"), { name, libelle });
  }

  deleteRole(name: string): Observable<void> {
    return this.http.delete<void>(this.config.getEndpointFor(`api/authorities/delete/${name}`));
  }
}
