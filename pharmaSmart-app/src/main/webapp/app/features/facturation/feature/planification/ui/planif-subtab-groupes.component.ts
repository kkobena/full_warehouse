import { Component, computed, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Tooltip } from 'primeng/tooltip';

import { IGroupeTiersPayant } from '../../../../../shared/model/groupe-tierspayant.model';
import { PlanificationStateService } from '../planification-state.service';

@Component({
  selector: 'app-planif-subtab-groupes',
  imports: [FormsModule, TableModule, ButtonModule, Select, ToggleSwitch, Tooltip],
  templateUrl: './planif-subtab-groupes.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planif-subtab-groupes.component.scss',
})
export class PlanifSubtabGroupesComponent {
  readonly mode = input.required<'def' | 'prov'>();
  protected readonly state = inject(PlanificationStateService);

  protected readonly groupes = computed(() =>
    this.mode() === 'def' ? this.state.groupesDef() : this.state.groupesProv(),
  );

  protected readonly loading = computed(() =>
    this.mode() === 'def' ? this.state.loadingGroupesDef() : this.state.loadingGroupesProv(),
  );

  protected get filtrePeriodicite(): string | null {
    return this.mode() === 'def' ? this.state.filtrePeriodiciteGroupesDef : this.state.filtrePeriodiciteGroupesProv;
  }
  protected set filtrePeriodicite(val: string | null) {
    if (this.mode() === 'def') this.state.filtrePeriodiciteGroupesDef = val;
    else this.state.filtrePeriodiciteGroupesProv = val;
  }

  protected get massPeriodicite(): string | null {
    return this.mode() === 'def' ? this.state.massPeriodiciteGroupesDef : this.state.massPeriodiciteGroupesProv;
  }
  protected set massPeriodicite(val: string | null) {
    if (this.mode() === 'def') this.state.massPeriodiciteGroupesDef = val;
    else this.state.massPeriodiciteGroupesProv = val;
  }

  protected get selectedGroupes(): IGroupeTiersPayant[] {
    return this.mode() === 'def' ? this.state.selectedGroupesDef : this.state.selectedGroupesProv;
  }
  protected set selectedGroupes(val: IGroupeTiersPayant[]) {
    if (this.mode() === 'def') this.state.selectedGroupesDef = val;
    else this.state.selectedGroupesProv = val;
  }

  protected loadGroupes(): void {
    if (this.mode() === 'def') this.state.loadGroupesDef();
    else this.state.loadGroupesProv();
  }

  protected onMassInclude(): void {
    if (this.mode() === 'def') this.state.onMassIncludeGroupesDef();
    else this.state.onMassIncludeGroupesProv();
  }

  protected onMassExclude(): void {
    if (this.mode() === 'def') this.state.onMassExcludeGroupesDef();
    else this.state.onMassExcludeGroupesProv();
  }

  protected onMassSetPeriodicite(): void {
    if (this.mode() === 'def') this.state.onMassSetPeriodiciteGroupesDef();
    else this.state.onMassSetPeriodiciteGroupesProv();
  }

  protected onToggleInclure(groupe: IGroupeTiersPayant): void {
    if (this.mode() === 'def') this.state.onToggleInclureGroupeDef(groupe);
    else this.state.onToggleInclureGroupeProv(groupe);
  }
}

