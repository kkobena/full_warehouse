import { Component, inject, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { NavTabsComponent } from 'app/shared/ui';
import { PillSelectorComponent } from 'app/shared/ui/pill-selector/pill-selector.component';
import { AbilityService } from 'app/core/auth/ability.service';
import VieillissementCreancesComponent from '../vieillissement-creances/vieillissement-creances.component';
import ConcentrationPayersComponent from '../concentration-payers/concentration-payers.component';

type TranchePill = 'all' | '0-30' | '31-60' | '61-90' | '90+';
type Periode = 'quarter' | 'year';

@Component({
  selector: 'app-finance-creances',
  imports: [FormsModule, PillSelectorComponent, NgbNavModule, NavTabsComponent, VieillissementCreancesComponent, ConcentrationPayersComponent],
  templateUrl: './finance-creances.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./finance-creances.component.scss'],
})
export default class FinanceCreancesComponent {
  @ViewChild(VieillissementCreancesComponent) private vieillissementComp?: VieillissementCreancesComponent;
  @ViewChild(ConcentrationPayersComponent) private concentrationComp?: ConcentrationPayersComponent;

  active = signal<string>('vieillissement-creances');
  protected readonly trancheOptions = [
    { label: 'Tous', value: 'all' },
    { label: '0–30j', value: '0-30' },
    { label: '31–60j', value: '31-60' },
    { label: '61–90j', value: '61-90' },
    { label: '>90j', value: '90+', icon: 'pi pi-exclamation-triangle' },
  ];
  protected readonly periodeOptions = [
    { label: 'Trimestre', value: 'quarter', icon: 'pi pi-calendar' },
    { label: 'Année en cours', value: 'year', icon: 'pi pi-calendar-plus' },
  ];
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
