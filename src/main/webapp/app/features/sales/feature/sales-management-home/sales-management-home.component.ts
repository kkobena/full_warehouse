import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import {
  NgbNav,
  NgbNavChangeEvent,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import { SalesManagementTab, SaleToolbarService } from "../../data-access/services/sale-toolbar.service";
import { SalesJournalComponent } from "../sales-journal/sales-journal.component";
import { SalesEnCoursComponent } from "../sales-en-cours/sales-en-cours.component";
import { PresaleListComponent } from "../presale-list/presale-list.component";
import { DevisListComponent } from "../devis-list/devis-list.component";
import { SalesAnnulationsComponent } from "../sales-annulations/sales-annulations.component";
import { VenteDepotListComponent } from "../vente-depot-list/vente-depot-list.component";
import { AvoirsClientListComponent } from "../avoirs-client-list/avoirs-client-list.component";
import { SalesKpiDashboardComponent } from "../sales-kpi-dashboard/sales-kpi-dashboard.component";
import { BreadcrumbService } from "../../../../shared/components/breadcrumb/breadcrumb.service";
import { AbilityService } from "app/core/auth/ability.service";
import { RetourClientComponent } from "../retour-client/retour-client.component";

const TAB_LABELS: Record<SalesManagementTab, string> = {
  "journal": "Journal des ventes",
  "en-cours": "Ventes en cours",
  "presales": "Pré-ventes",
  "devis": "Proformas",
  "annulations": "Annulations",
  "vente-depot": "Ventes dépôt",
  "avoirs": "Avoirs clients",
  "kpi": "Tableau de bord",
  "retour-client": "Retours client",
};

@Component({
  selector: "app-sales-management-home",
  templateUrl: "./sales-management-home.component.html",
  styleUrl: "./sales-management-home.component.scss",
  imports: [
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    SalesJournalComponent,
    SalesEnCoursComponent,
    PresaleListComponent,
    DevisListComponent,
    SalesAnnulationsComponent,
    VenteDepotListComponent,
    AvoirsClientListComponent,
    SalesKpiDashboardComponent,
    RetourClientComponent
  ]
})
export class SalesManagementHomeComponent implements OnInit {
  private readonly toolbarService = inject(SaleToolbarService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly ability = inject(AbilityService);

  protected active = signal<SalesManagementTab>("journal");

  protected readonly showJournal = this.ability.canSignal("display", "ventes.journal");
  protected readonly showEnCours = this.ability.canSignal("display", "ventes.en-cours");
  protected readonly showPresales = this.ability.canSignal("display", "ventes.presales");
  protected readonly showDevis = this.ability.canSignal("display", "ventes.devis");
  protected readonly showAnnulations = this.ability.canSignal("display", "ventes.annulations");
  protected readonly showVenteDepot = this.ability.canSignal("display", "ventes.vente-depot");
  protected readonly showAvoirs = this.ability.canSignal("display", "ventes.avoirs");
 // protected readonly showKpi = this.ability.canSignal("display", "ventes.kpi");
  protected readonly showRetourClient = this.ability.canSignal("display", "ventes.retours-client");

  constructor() {
    inject(DestroyRef).onDestroy(() => this.breadcrumbService.clearTabCrumb());
  }

  ngOnInit(): void {
    const tab = this.toolbarService.params().activeTab;

    if (this.active() === "journal" && !this.showJournal()) {
      if (this.showPresales()) {
        this.active.set("presales");
      } else if (this.showDevis()) {
        this.active.set("devis");
      } else if (this.showEnCours()) {
        this.active.set("en-cours");
      }else{

        //TODO: handle no access to any tab ajout d'un tab commun Access denied
      }

    } else {
      this.active.set(tab);
    }

    this.breadcrumbService.setTabCrumb(TAB_LABELS[tab]);
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    const tab = evt.nextId as SalesManagementTab;
    this.active.set(tab);
    this.toolbarService.update({ activeTab: tab });
    this.breadcrumbService.setTabCrumb(TAB_LABELS[tab] ?? tab);
  }
}
