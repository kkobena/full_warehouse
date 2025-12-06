import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import StockAlertsComponent from '../stock-alerts/stock-alerts.component';
import StockValuationComponent from '../stock-valuation/stock-valuation.component';
import StockRotationComponent from '../stock-rotation/stock-rotation.component';
import ABCParetoComponent from '../abc-pareto/abc-pareto.component';
import SalesSummaryComponent from '../sales-summary/sales-summary.component';
import TopProductsComponent from '../top-products/top-products.component';
import ProfitabilityAnalysisComponent from '../profitability-analysis/profitability-analysis.component';
import CashRegisterReportComponent from '../cash-register-report/cash-register-report.component';
import TiersPayantCreancesComponent from '../tiers-payant-creances/tiers-payant-creances.component';
import CustomerSegmentationComponent from '../customer-segmentation/customer-segmentation.component';
import SupplierPerformanceComponent from '../supplier-performance/supplier-performance.component';

@Component({
  selector: 'jhi-reports-home',
  imports: [
    CommonModule,
    NgbNavModule,
    StockAlertsComponent,
    StockValuationComponent,
    StockRotationComponent,
    ABCParetoComponent,
    SalesSummaryComponent,
    TopProductsComponent,
    ProfitabilityAnalysisComponent,
    CashRegisterReportComponent,
    TiersPayantCreancesComponent,
    CustomerSegmentationComponent,
    SupplierPerformanceComponent,
  ],
  templateUrl: './reports-home.component.html',
  styleUrl: './reports-home.component.scss',
})
export default class ReportsHomeComponent {
  active = signal<string>('stock-alerts');
}
