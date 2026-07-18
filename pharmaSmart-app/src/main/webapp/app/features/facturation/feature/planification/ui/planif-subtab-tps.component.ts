import { Component, computed, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Tooltip } from 'primeng/tooltip';

import { ITiersPayant } from '../../../../../shared/model';
import { PlanificationStateService } from '../planification-state.service';

@Component({
  selector: 'app-planif-subtab-tps',
  imports: [FormsModule, TableModule, ButtonModule, Select, ToggleSwitch, Tooltip],
  templateUrl: './planif-subtab-tps.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planif-subtab-tps.component.scss',
})
export class PlanifSubtabTpsComponent {
  readonly mode = input.required<'def' | 'prov'>();
  protected readonly state = inject(PlanificationStateService);

  protected readonly tps = computed(() => (this.mode() === 'def' ? this.state.tpsDef() : this.state.tpsProv()));

  protected readonly loading = computed(() =>
    this.mode() === 'def' ? this.state.loadingTpsDef() : this.state.loadingTpsProv(),
  );

  protected get filtrePeriodicite(): string | null {
    return this.mode() === 'def' ? this.state.filtrePeriodiciteTPsDef : this.state.filtrePeriodiciteTPsProv;
  }
  protected set filtrePeriodicite(val: string | null) {
    if (this.mode() === 'def') this.state.filtrePeriodiciteTPsDef = val;
    else this.state.filtrePeriodiciteTPsProv = val;
  }

  protected get filtreCategorie(): string | null {
    return this.mode() === 'def' ? this.state.filtreCategorieTpsDef : this.state.filtreCategorieTpsProv;
  }
  protected set filtreCategorie(val: string | null) {
    if (this.mode() === 'def') this.state.filtreCategorieTpsDef = val;
    else this.state.filtreCategorieTpsProv = val;
  }

  protected get massPeriodicite(): string | null {
    return this.mode() === 'def' ? this.state.massPeriodiciteTPsDef : this.state.massPeriodiciteTPsProv;
  }
  protected set massPeriodicite(val: string | null) {
    if (this.mode() === 'def') this.state.massPeriodiciteTPsDef = val;
    else this.state.massPeriodiciteTPsProv = val;
  }

  protected get selectedTps(): ITiersPayant[] {
    return this.mode() === 'def' ? this.state.selectedTpsDef : this.state.selectedTpsProv;
  }
  protected set selectedTps(val: ITiersPayant[]) {
    if (this.mode() === 'def') this.state.selectedTpsDef = val;
    else this.state.selectedTpsProv = val;
  }

  protected loadTps(): void {
    if (this.mode() === 'def') this.state.loadTpsDef();
    else this.state.loadTpsProv();
  }

  protected onMassInclude(): void {
    if (this.mode() === 'def') this.state.onMassIncludeTpsDef();
    else this.state.onMassIncludeTpsProv();
  }

  protected onMassExclude(): void {
    if (this.mode() === 'def') this.state.onMassExcludeTpsDef();
    else this.state.onMassExcludeTpsProv();
  }

  protected onMassSetPeriodicite(): void {
    if (this.mode() === 'def') this.state.onMassSetPeriodiciteTpsDef();
    else this.state.onMassSetPeriodiciteTpsProv();
  }

  protected onToggleInclure(tp: ITiersPayant): void {
    if (this.mode() === 'def') this.state.onToggleInclureTpDef(tp);
    else this.state.onToggleInclureTpProv(tp);
  }
}

