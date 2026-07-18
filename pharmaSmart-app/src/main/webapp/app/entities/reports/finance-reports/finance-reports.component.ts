import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';

import PnlAnalytiqueComponent from '../pnl-analytique/pnl-analytique.component';
import VieillissementCreancesComponent from '../vieillissement-creances/vieillissement-creances.component';
import ConcentrationPayersComponent from '../concentration-payers/concentration-payers.component';
import CashFlowBfrComponent from '../cash-flow-bfr/cash-flow-bfr.component';
import ProfitabilityAnalysisComponent from '../profitability-analysis/profitability-analysis.component';
import FinanceCreancesComponent from '../finance-creances/finance-creances.component';
// Phase 4 — Nouveaux rapports Créances & Finance
import SituationCreancesComponent from '../situation-creances/situation-creances.component';
import VieillissementDifferesComponent from '../vieillissement-differes/vieillissement-differes.component';
import AvoirsAnalyticsComponent from '../avoirs-analytics/avoirs-analytics.component';
import TauxRecouvrementTpComponent from '../taux-recouvrement-tp/taux-recouvrement-tp.component';

@Component({
  selector: 'app-finance-reports',
  imports: [CommonModule, NgbNavModule,
    PnlAnalytiqueComponent,
    VieillissementCreancesComponent,  // conservé — utilisé dans finance-creances
    ConcentrationPayersComponent,     // conservé — utilisé dans finance-creances
    CashFlowBfrComponent,
    ProfitabilityAnalysisComponent,
    FinanceCreancesComponent,
    SituationCreancesComponent,
    VieillissementDifferesComponent,
    AvoirsAnalyticsComponent,
    TauxRecouvrementTpComponent,
  ],
  templateUrl: './finance-reports.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './finance-reports.component.scss',
})
export default class FinanceReportsComponent implements OnInit {
  active = signal<string>('pnl-analytique');

  private readonly ability = inject(AbilityService);

  protected readonly showPnlAnalytique        = this.ability.canSignal('display', 'rapport-finance.pnl-analytique');
  protected readonly showProfitability        = this.ability.canSignal('display', 'rapport-finance.profitability');
  protected readonly showCreances             = this.ability.canSignal('display', 'rapport-finance.creances');
  protected readonly showCashFlowBfr          = this.ability.canSignal('display', 'rapport-finance.cash-flow-bfr');
  // Phase 4
  protected readonly showSituationCreances    = this.ability.canSignal('display', 'rapport-finance.situation-creances');
  protected readonly showVieillissementDiff   = this.ability.canSignal('display', 'rapport-finance.vieillissement-differes');
  protected readonly showAvoirsAnalytics      = this.ability.canSignal('display', 'rapport-finance.avoirs-analytics');
  protected readonly showTauxRecouvrement     = this.ability.canSignal('display', 'rapport-finance.taux-recouvrement-tp');
  // Désactivés en HTML (migré vers finance-creances) — À SUPPRIMER après TNR
  protected readonly showVieillissement       = this.ability.canSignal('display', 'rapport-finance.vieillissement-creances');
  protected readonly showConcentration        = this.ability.canSignal('display', 'rapport-finance.concentration-payers');

  ngOnInit(): void {
    if (this.active() === 'pnl-analytique' && !this.showPnlAnalytique()) {
      if (this.showProfitability())          this.active.set('profitability');
      else if (this.showCreances())          this.active.set('creances');
      else if (this.showCashFlowBfr())       this.active.set('cash-flow-bfr');
      else if (this.showSituationCreances()) this.active.set('situation-creances');
      else if (this.showVieillissementDiff())this.active.set('vieillissement-differes');
      else if (this.showAvoirsAnalytics())   this.active.set('avoirs-analytics');
      else if (this.showTauxRecouvrement())  this.active.set('taux-recouvrement-tp');
    }
  }
}
