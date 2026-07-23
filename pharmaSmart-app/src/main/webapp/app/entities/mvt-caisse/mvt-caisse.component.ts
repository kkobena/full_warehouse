import {ChangeDetectionStrategy, Component, inject} from "@angular/core";
import {AbilityService} from "app/core/auth/ability.service";
import {RouterModule} from "@angular/router";
import {NgxSpinnerModule} from "ngx-spinner";
import {ReactiveFormsModule} from "@angular/forms";
import {VisualisationMvtCaisseComponent} from "./visualisation-mvt-caisse.component";
import {GestionCaisseComponent} from "./gestion-caisse/gestion-caisse.component";
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: "app-mvt-caisse",
  imports: [
    RouterModule,
    NgxSpinnerModule,
    ReactiveFormsModule,
    VisualisationMvtCaisseComponent,
    GestionCaisseComponent,
    NgbNavOutlet,
    NgbNav,
    NgbNavItem,
    NgbNavContent,
    NgbNavLink
  ],
  templateUrl: "./mvt-caisse.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./mvt-caisse.component.scss"]
})
export class MvtCaisseComponent {
  protected active = "mvt-caisse";

  private readonly ability = inject(AbilityService);

  protected readonly showMvtCaisse = this.ability.canSignal("display", "mvt-caisse.mvt-caisse");
  protected readonly showGestionCaisse = this.ability.canSignal("display", "mvt-caisse.gestion-caisse");

}
