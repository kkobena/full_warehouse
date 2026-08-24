import { ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
import { AbilityService } from "app/core/auth/ability.service";
import { RouterModule } from "@angular/router";
import { NgxSpinnerModule } from "ngx-spinner";
import { ReactiveFormsModule } from "@angular/forms";
import { VisualisationMvtCaisseComponent } from "./visualisation-mvt-caisse.component";
import { GestionCaisseComponent } from "./gestion-caisse/gestion-caisse.component";
import { NavSidebarComponent } from "app/shared/ui/nav-sidebar/nav-sidebar.component";
import { NavSectionLinkComponent } from "app/shared/ui/nav-sidebar/nav-section-link.component";
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink } from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: "app-mvt-caisse",
  imports: [NavSidebarComponent, NavSectionLinkComponent,
    RouterModule,
    NgxSpinnerModule,
    ReactiveFormsModule,
    VisualisationMvtCaisseComponent,
    GestionCaisseComponent,
    NgbNav,
    NgbNavItem,
    NgbNavContent,
    NgbNavLink
  ],
  templateUrl: "./mvt-caisse.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./mvt-caisse.component.scss"]
})
export class MvtCaisseComponent {
  protected active = "mvt-caisse";


  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
  private readonly ability = inject(AbilityService);

  protected readonly showMvtCaisse = this.ability.canSignal("display", "mvt-caisse.mvt-caisse");
  protected readonly showGestionCaisse = this.ability.canSignal("display", "mvt-caisse.gestion-caisse");

}
