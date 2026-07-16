import { Component, inject, signal } from "@angular/core";
import { AbilityService } from "app/core/auth/ability.service";
import { RouterModule } from "@angular/router";
import { NgxSpinnerModule } from "ngx-spinner";
import { PanelModule } from "primeng/panel";
import { ButtonModule } from "primeng/button";
import { CardModule } from "primeng/card";
import { InputTextModule } from "primeng/inputtext";
import { ReactiveFormsModule } from "@angular/forms";
import { RippleModule } from "primeng/ripple";
import { ToolbarModule } from "primeng/toolbar";
import { VisualisationMvtCaisseComponent } from "./visualisation-mvt-caisse.component";

import { TableauPharmacienComponent } from "./tableau-pharmacien/tableau-pharmacien.component";
import { BalanceMvtCaisseComponent } from "./balance-mvt-caisse/balance-mvt-caisse.component";
import { TaxeReportComponent } from "./taxe-report/taxe-report.component";
import { GestionCaisseComponent } from "./gestion-caisse/gestion-caisse.component";
import { ActivitySummaryComponent } from "../raport-gestion/activity-summary/activity-summary.component";
import { RecapitualtifCaisseComponent } from "../ticketZ/recapitualtif-caisse/recapitualtif-caisse.component";
import { DeclarationTvaComponent } from "../../features/finances/feature/declaration-tva/declaration-tva.component";
import { ExportComptableComponent } from "../../features/finances/feature/export-comptable/export-comptable.component";
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: "app-mvt-caisse",
  imports: [
    RouterModule,
    NgxSpinnerModule,
    PanelModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    ReactiveFormsModule,
    RippleModule,
    ToolbarModule,
    VisualisationMvtCaisseComponent,
    BalanceMvtCaisseComponent,
    TableauPharmacienComponent,
    TaxeReportComponent,
    GestionCaisseComponent,
    ActivitySummaryComponent,
    RecapitualtifCaisseComponent,
    DeclarationTvaComponent,
    ExportComptableComponent,
    NgbNavOutlet,
    NgbNav,
    NgbNavItem,
    NgbNavContent,
    NgbNavLink
  ],
  templateUrl: "./mvt-caisse.component.html",
  styleUrls: ["./mvt-caisse.component.scss"]
})
export class MvtCaisseComponent {
  protected active = "mvt-caisse";

  private readonly ability = inject(AbilityService);
  protected readonly hideDeclarationTva = signal(true);//La fonctionnalité n'est Ok pour le moment

  protected readonly showMvtCaisse = this.ability.canSignal("display", "mvt-caisse.mvt-caisse");
  protected readonly showBalance = this.ability.canSignal("display", "mvt-caisse.balance");
  protected readonly showTaxeReport = this.ability.canSignal("display", "mvt-caisse.taxe-report");
  protected readonly showTableauPharmacien = this.ability.canSignal("display", "mvt-caisse.tableau-pharmacien");
  protected readonly showRecapCaisse = this.ability.canSignal("display", "mvt-caisse.recapitulatif-caisse");
  protected readonly showGestionCaisse = this.ability.canSignal("display", "mvt-caisse.gestion-caisse");
  protected readonly showRaportActivite = this.ability.canSignal("display", "mvt-caisse.raport-activite");
  protected readonly showDeclarationTva = this.ability.canSignal("display", "mvt-caisse.declaration-tva");
  protected readonly showExportComptable = this.ability.canSignal("display", "mvt-caisse.export-comptable");
}
