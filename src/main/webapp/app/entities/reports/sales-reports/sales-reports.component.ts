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
    MarketBasketComponent
  ],
  templateUrl: "./sales-reports.component.html",
  styleUrl: "./sales-reports.component.scss"
})
export default class SalesReportsComponent implements OnInit {
  active = signal<string>("dashboard-ca");

  private readonly ability = inject(AbilityService);

  protected readonly showDashboardCa = this.ability.canSignal("display", "rapport-ventes.dashboard-ca");
  protected readonly showSalesSummary = this.ability.canSignal("display", "rapport-ventes.sales-summary");
  protected readonly showTopProducts = this.ability.canSignal("display", "rapport-ventes.top-products");
  protected readonly showProfitability = this.ability.canSignal("display", "rapport-ventes.profitability");
  protected readonly showComparative = this.ability.canSignal("display", "rapport-ventes.comparative");
  protected readonly showSalesForecast = this.ability.canSignal("display", "rapport-ventes.sales-forecast");
  protected readonly showMarketBasket = this.ability.canSignal("display", "rapport-ventes.market-basket");

  ngOnInit(): void {
    if (this.active() === "dashboard-ca" && !this.showDashboardCa()) {
      if (this.showTopProducts()) {
        this.active.set("top-products");
      } else if (this.showSalesSummary()) {
        this.active.set("sales-summary");
      } else if (this.showProfitability()) {
        this.active.set("profitability");
      } else if (this.showComparative()) {
        this.active.set("comparative");
      } else if (this.showSalesForecast()) {
        this.active.set("sales-forecast");
      } else if (this.showMarketBasket()) {
        this.active.set("market-basket");
      } else {

        //TODO: handle no access to any tab ajout d'un tab commun Access denied
      }

    }
  }
}
