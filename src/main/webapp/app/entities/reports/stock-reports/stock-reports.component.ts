import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import StockAlertsComponent from '../stock-alerts/stock-alerts.component';
import StockValuationComponent from '../stock-valuation/stock-valuation.component';
import StockRotationComponent from '../stock-rotation/stock-rotation.component';
import ABCParetoComponent from '../abc-pareto/abc-pareto.component';

@Component({
  selector: 'jhi-stock-reports',
  imports: [CommonModule, NgbNavModule, StockAlertsComponent, StockValuationComponent, StockRotationComponent, ABCParetoComponent],
  templateUrl: './stock-reports.component.html',
  styleUrl: './stock-reports.component.scss',
})
export default class StockReportsComponent {
  active = signal<string>('stock-alerts');
}
