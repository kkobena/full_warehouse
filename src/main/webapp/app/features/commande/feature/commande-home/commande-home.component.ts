import { Component, effect, inject, OnInit } from "@angular/core";
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
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";

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
    SuggestionsUnifiedComponent

  ]
})
export class CommandeHomeComponent implements OnInit {
  protected active = "DASHBOARD";

  private readonly route = inject(ActivatedRoute);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);

  constructor() {
    // Réagit aux changements de nav déclenchés depuis les composants enfants
    // (ex: semois-dashboard → SEMOIS_SUGGESTIONS)
    effect(() => {
      const nav = this.commandCommonService.commandPreviousActiveNav();
      if (nav !== this.active) {
        this.active = nav;
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
    });
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }
}
