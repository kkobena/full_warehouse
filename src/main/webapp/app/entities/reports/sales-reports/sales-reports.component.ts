import { Component, inject, OnInit, signal } from "@angular/core";
import { AbilityService } from "app/core/auth/ability.service";
import { CommonModule } from "@angular/common";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";

import DashboardCAComponent from "../dashboard-ca/dashboard-ca.component";
import SalesSummaryComponent from "../sales-summary/sales-summary.component";
import TopProductsComponent from "../top-products/top-products.component";
import ProfitabilityAnalysisComponent from "../profitability-analysis/profitability-analysis.component";
import ComparativeAnalysisComponent from "../comparative-analysis/comparative-analysis.component";
import SalesForecastComponent from "../sales-forecast/sales-forecast.component";
import MarketBasketComponent from "../market-basket/market-basket.component";
// Phase 5 — Nouveaux rapports Ventes
import SalesByStaffComponent from "../sales-by-staff/sales-by-staff.component";
import SeasonalityComponent from "../seasonality/seasonality.component";
import GenericsSubstitutionComponent from "../generics-substitution/generics-substitution.component";
// Phase 6 — Rapports secondaires
import RemisesAnalysisComponent from "../remises-analysis/remises-analysis.component";
import ClientRetentionComponent from "../client-retention/client-retention.component";

@Component({
  selector: "jhi-sales-reports",
  imports: [
    CommonModule,
    NgbNavModule,
    DashboardCAComponent,
    SalesSummaryComponent,
    TopProductsComponent,
    ProfitabilityAnalysisComponent,
    ComparativeAnalysisComponent,
    SalesForecastComponent,
    MarketBasketComponent,
    SalesByStaffComponent,
    SeasonalityComponent,
    GenericsSubstitutionComponent,
    RemisesAnalysisComponent,
    ClientRetentionComponent,
  ],
  templateUrl: "./sales-reports.component.html",
  styleUrl: "./sales-reports.component.scss"
})
export default class SalesReportsComponent implements OnInit {
  active = signal<string>("dashboard-ca");

  private readonly ability = inject(AbilityService);

  protected readonly showDashboardCa           = this.ability.canSignal("display", "rapport-ventes.dashboard-ca");
  protected readonly showSalesSummary           = this.ability.canSignal("display", "rapport-ventes.sales-summary");
  protected readonly showTopProducts            = this.ability.canSignal("display", "rapport-ventes.top-products");
  protected readonly showProfitability          = this.ability.canSignal("display", "rapport-ventes.profitability");
  protected readonly showComparative            = this.ability.canSignal("display", "rapport-ventes.comparative");
  protected readonly showSalesForecast          = this.ability.canSignal("display", "rapport-ventes.sales-forecast");
  protected readonly showMarketBasket           = this.ability.canSignal("display", "rapport-ventes.market-basket");
  // Phase 5
  protected readonly showSalesByStaff           = this.ability.canSignal("display", "rapport-ventes.sales-by-staff");
  protected readonly showSeasonality            = this.ability.canSignal("display", "rapport-ventes.seasonality");
  protected readonly showGenericsSubstitution   = this.ability.canSignal("display", "rapport-ventes.generics-substitution");
  // Phase 6
  protected readonly showRemisesAnalysis        = this.ability.canSignal("display", "rapport-ventes.remises-analysis");
  protected readonly showClientRetention        = this.ability.canSignal("display", "rapport-ventes.client-retention");

  ngOnInit(): void {
    if (this.active() === "dashboard-ca" && !this.showDashboardCa()) {
      if      (this.showTopProducts())          this.active.set("top-products");
      else if (this.showSalesSummary())         this.active.set("sales-summary");
      else if (this.showComparative())          this.active.set("comparative");
      else if (this.showSalesForecast())        this.active.set("sales-forecast");
      else if (this.showMarketBasket())         this.active.set("market-basket");
      else if (this.showSalesByStaff())         this.active.set("sales-by-staff");
      else if (this.showSeasonality())          this.active.set("seasonality");
      else if (this.showGenericsSubstitution()) this.active.set("generics-substitution");
      else if (this.showRemisesAnalysis())      this.active.set("remises-analysis");
      else if (this.showClientRetention())      this.active.set("client-retention");
    }
  }
}
