import { Component, DestroyRef, effect, inject, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, RouterModule } from "@angular/router";
import {
  NgbNav,
  NgbNavChangeEvent,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import { CommandCommonService } from "../../../../entities/commande/command-common.service";
import { AppRetourFournisseurComponent } from "../retour-fournisseur/retour-fournisseur.component";
import { AppRepartitionStockComponent } from "../repartition-stock/repartition-stock.component";
import { ApproUnifiedDashboardComponent } from "../appro-unified-dashboard/appro-unified-dashboard.component";
import { SuggestionsUnifiedComponent } from "../suggestions-unified/suggestions-unified.component";
import { BedHomeComponent } from "../bon-entree-diverse/bed-home/bed-home.component";
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";
import { AlertBadgeService } from "../../../../shared/services/alert-badge.service";
import { BreadcrumbService } from "../../../../shared/components/breadcrumb/breadcrumb.service";
import { AbilityService } from "app/core/auth/ability.service";

/** Labels fil d'Ariane pour chaque onglet */
const TAB_LABELS: Record<string, string> = {
  DASHBOARD:         'Tableau de bord Appro',
  SUGGESTIONS:       'Commandes & Réceptions',
  REPARTITION_STOCK: 'Répartition & Transferts',
  RETOUR_FOURNISSEUR:'Retours fournisseurs',
  BED:               "Bons d'Entrée Diverses",
};

@Component({
  selector: "app-commande-home",
  templateUrl: "./commande-home.component.html",
  styleUrl: "./commande-home.component.scss",
  imports: [
    CommonModule,
    RouterModule,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    AppRetourFournisseurComponent,
    AppRepartitionStockComponent,
    ApproUnifiedDashboardComponent,
    SuggestionsUnifiedComponent,
    BedHomeComponent,
  ]
})
export class CommandeHomeComponent implements OnInit {
  protected active = "DASHBOARD";

  private readonly route = inject(ActivatedRoute);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  protected readonly alertBadgeService = inject(AlertBadgeService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly ability = inject(AbilityService);

  protected readonly showDashboard        = this.ability.canSignal('display', 'commande.dashboard');
  protected readonly showSuggestions      = this.ability.canSignal('display', 'commande.suggestions');
  protected readonly showRepartitionStock = this.ability.canSignal('display', 'commande.repartition-stock');
  protected readonly showRetourFournisseur = this.ability.canSignal('display', 'commande.retour-fournisseur');
  protected readonly showBed              = this.ability.canSignal('display', 'commande.bed');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.breadcrumbService.clearTabCrumb());

    effect(() => {
      const nav = this.commandCommonService.commandPreviousActiveNav();
      if (nav !== this.active) {
        this.active = nav;
        this.breadcrumbService.setTabCrumb(TAB_LABELS[nav] ?? nav);
      }
    });
  }

  ngOnInit(): void {
    this.translate.use("fr");
    this.translate.stream("primeng").subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.route.queryParams.subscribe(params => {
      if (params["tab"]) {
        this.active = params["tab"];
        this.commandCommonService.updateCommandPreviousActiveNav(this.active);
      } else {
        this.active = this.commandCommonService.commandPreviousActiveNav();
      }
      this.breadcrumbService.setTabCrumb(TAB_LABELS[this.active] ?? this.active);
    });
    this.alertBadgeService.init();
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
    this.breadcrumbService.setTabCrumb(TAB_LABELS[this.active] ?? this.active);
  }
}


