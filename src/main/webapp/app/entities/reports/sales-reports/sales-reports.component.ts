import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import DashboardCAComponent from '../dashboard-ca/dashboard-ca.component';
import SalesSummaryComponent from '../sales-summary/sales-summary.component';
import TopProductsComponent from '../top-products/top-products.component';
import ProfitabilityAnalysisComponent from '../profitability-analysis/profitability-analysis.component';
import ComparativeAnalysisComponent from '../comparative-analysis/comparative-analysis.component';
import SalesForecastComponent from '../sales-forecast/sales-forecast.component';
import MarketBasketComponent from '../market-basket/market-basket.component';

@Component({
  selector: 'jhi-sales-reports',
  standalone: true,
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
  ],
  templateUrl: './sales-reports.component.html',
  styleUrl: './sales-reports.component.scss',
})
export default class SalesReportsComponent {
  active = signal<string>('dashboard-ca');
}
