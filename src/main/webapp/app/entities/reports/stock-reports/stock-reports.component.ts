import { Component, inject, signal } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import StockAlertsComponent from '../stock-alerts/stock-alerts.component';
import StockValuationComponent from '../stock-valuation/stock-valuation.component';
import StockRotationComponent from '../stock-rotation/stock-rotation.component';
import ABCParetoComponent from '../abc-pareto/abc-pareto.component';
import RecapProduitVenduComponent from "../recap-produit-vendu/recap-produit-vendu.component";

@Component({
  selector: 'jhi-stock-reports',
  imports: [CommonModule, NgbNavModule, StockAlertsComponent, StockValuationComponent, StockRotationComponent, ABCParetoComponent, RecapProduitVenduComponent],
  templateUrl: './stock-reports.component.html',
  styleUrl: './stock-reports.component.scss',
})
export default class StockReportsComponent {
  active = signal<string>('stock-alerts');

  private readonly ability = inject(AbilityService);

  protected readonly showStockAlerts      = this.ability.canSignal('display', 'rapport-stock.stock-alerts');
  protected readonly showStockValuation   = this.ability.canSignal('display', 'rapport-stock.stock-valuation');
  protected readonly showRecapProduit     = this.ability.canSignal('display', 'rapport-stock.recap-produit-vendu');
  protected readonly showStockRotation    = this.ability.canSignal('display', 'rapport-stock.stock-rotation');
  protected readonly showAbcPareto        = this.ability.canSignal('display', 'rapport-stock.abc-pareto');
}
