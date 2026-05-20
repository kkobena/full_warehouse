import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';

import PnlAnalytiqueComponent from '../pnl-analytique/pnl-analytique.component';
import VieillissementCreancesComponent from '../vieillissement-creances/vieillissement-creances.component';
import ConcentrationPayersComponent from '../concentration-payers/concentration-payers.component';
import CashFlowBfrComponent from '../cash-flow-bfr/cash-flow-bfr.component';

@Component({
  selector: 'app-finance-reports',
  imports: [CommonModule, NgbNavModule, PnlAnalytiqueComponent, VieillissementCreancesComponent, ConcentrationPayersComponent, CashFlowBfrComponent],
  templateUrl: './finance-reports.component.html',
  styleUrl: './finance-reports.component.scss',
})
export default class FinanceReportsComponent implements OnInit {
  active = signal<string>('pnl-analytique');

  private readonly ability = inject(AbilityService);

  protected readonly showPnlAnalytique = this.ability.canSignal('display', 'rapport-finance.pnl-analytique');
  protected readonly showVieillissement = this.ability.canSignal('display', 'rapport-finance.vieillissement-creances');
  protected readonly showConcentration = this.ability.canSignal('display', 'rapport-finance.concentration-payers');
  protected readonly showCashFlowBfr = this.ability.canSignal('display', 'rapport-finance.cash-flow-bfr');

  ngOnInit(): void {
    if (this.active() === 'pnl-analytique' && !this.showPnlAnalytique()) {
      if (this.showVieillissement()) this.active.set('vieillissement-creances');
      else if (this.showConcentration()) this.active.set('concentration-payers');
      else if (this.showCashFlowBfr()) this.active.set('cash-flow-bfr');
    }
  }
}
