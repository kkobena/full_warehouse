import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink } from "@ng-bootstrap/ng-bootstrap";
import { NavManagerComponent } from "../nav-manager/nav-manager.component";
import { RoleComponent } from "../role/role.component";

import { NavSidebarComponent } from "app/shared/ui/nav-sidebar/nav-sidebar.component";
import { NavSectionLinkComponent } from "app/shared/ui/nav-sidebar/nav-section-link.component";

@Component({
  selector: "app-access-managment-home",
  templateUrl: "./access-managment-home.component.html",
  styleUrl: "./access-managment-home.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NavSidebarComponent, NavSectionLinkComponent,
    NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NavManagerComponent,
    RoleComponent
  ]
})
export class AccessManagmentHomeComponent {
  protected readonly active = signal("roles");


  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
  /** Rôle dont on veut gérer les autorisations (passé à nav-manager). */
  protected readonly pendingRole = signal<string | null>(null);

  openPermissions(roleName: string): void {
    this.pendingRole.set(roleName);
    this.active.set("autorisations");
  }
}
