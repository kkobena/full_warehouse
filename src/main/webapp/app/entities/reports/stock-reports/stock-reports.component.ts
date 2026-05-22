import { Component, inject, OnInit, signal } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import StockAlertsComponent from '../stock-alerts/stock-alerts.component';
import StockValuationComponent from '../stock-valuation/stock-valuation.component';
import StockRotationComponent from '../stock-rotation/stock-rotation.component';
import ABCParetoComponent from '../abc-pareto/abc-pareto.component';
import RecapProduitVenduComponent from "../recap-produit-vendu/recap-produit-vendu.component";
import StockABCComponent from '../stock-abc/stock-abc.component';
// Phase 6
import DemarqueComponent from '../demarque/demarque.component';

@Component({
  selector: 'jhi-stock-reports',
  imports: [
    CommonModule,
    NgbNavModule,
    StockAlertsComponent,
    StockValuationComponent,
    StockRotationComponent,
    ABCParetoComponent,
    RecapProduitVenduComponent,
    StockABCComponent,
    DemarqueComponent,
  ],
  templateUrl: './stock-reports.component.html',
  styleUrl: './stock-reports.component.scss',
})
export default class StockReportsComponent implements OnInit {
  active = signal<string>('stock-alerts');

  private readonly ability = inject(AbilityService);

  protected readonly showStockAlerts      = this.ability.canSignal('display', 'rapport-stock.stock-alerts');
  protected readonly showStockValuation   = this.ability.canSignal('display', 'rapport-stock.stock-valuation');
  protected readonly showRecapProduit     = this.ability.canSignal('display', 'rapport-stock.recap-produit-vendu');
  protected readonly showStockAbc         = this.ability.canSignal('display', 'rapport-stock.stock-abc');
  // Phase 6
  protected readonly showDemarque         = this.ability.canSignal('display', 'rapport-stock.demarque');
  // Désactivés en DB (actif=FALSE) — À SUPPRIMER après validation TNR (migrés vers stock-abc)
  protected readonly showStockRotation    = this.ability.canSignal('display', 'rapport-stock.stock-rotation');
  protected readonly showAbcPareto        = this.ability.canSignal('display', 'rapport-stock.abc-pareto');

  ngOnInit(): void {
    if (this.active() === 'stock-alerts' && !this.showStockAlerts()) {
      if      (this.showStockValuation()) this.active.set('stock-valuation');
      else if (this.showRecapProduit())   this.active.set('recap-produit-vendu');
      else if (this.showStockAbc())       this.active.set('stock-abc');
      else if (this.showDemarque())       this.active.set('demarque');
    }
  }
}
