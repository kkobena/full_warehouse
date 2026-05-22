import { Component, inject, signal, ViewChild } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import VieillissementCreancesComponent from '../vieillissement-creances/vieillissement-creances.component';
import ConcentrationPayersComponent from '../concentration-payers/concentration-payers.component';

type TranchePill = 'all' | '0-30' | '31-60' | '61-90' | '90+';
type Periode = 'quarter' | 'year';

@Component({
  selector: 'app-finance-creances',
  imports: [NgbNavModule, VieillissementCreancesComponent, ConcentrationPayersComponent],
  templateUrl: './finance-creances.component.html',
  styleUrls: ['./finance-creances.component.scss'],
})
export default class FinanceCreancesComponent {
  @ViewChild(VieillissementCreancesComponent) private vieillissementComp?: VieillissementCreancesComponent;
  @ViewChild(ConcentrationPayersComponent) private concentrationComp?: ConcentrationPayersComponent;

  active = signal<string>('vieillissement-creances');
  protected readonly trancheFilter = signal<TranchePill>('all');
  protected readonly periode = signal<Periode>('quarter');

  private readonly ability = inject(AbilityService);

  protected readonly showVieillissement = this.ability.canSignal('display', 'rapport-finance.vieillissement-creances');
  protected readonly showConcentration = this.ability.canSignal('display', 'rapport-finance.concentration-payers');

  protected onActiveChange(tabId: string): void {
    this.active.set(tabId);
    this.trancheFilter.set('all');
    this.periode.set('quarter');
  }

  protected setTranche(t: TranchePill): void {
    this.trancheFilter.set(t);
    this.vieillissementComp?.setTranche(t);
  }

  protected setPeriode(p: Periode): void {
    this.periode.set(p);
    this.concentrationComp?.setPeriode(p);
  }
}
