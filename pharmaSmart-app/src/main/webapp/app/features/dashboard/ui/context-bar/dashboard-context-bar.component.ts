import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonComponent, SelectComponent } from 'app/shared/ui';
import { PeriodPreset } from '../../models/dashboard.model';
import { PERIOD_OPTIONS, REFRESH_OPTIONS } from '../../models/period';
import { DashboardContext } from '../../data-access/dashboard-context';

/** Période et rafraîchissement du dashboard, appliqués à toutes ses tuiles. */
@Component({
  selector: 'app-dashboard-context-bar',
  imports: [FormsModule, SelectComponent, ButtonComponent],
  template: `
    <div class="d-flex flex-wrap align-items-center gap-2">
      <app-select
        class="context-select"
        ariaLabel="Période"
        [items]="periodOptions"
        bindLabel="label"
        bindValue="value"
        [searchable]="false"
        [small]="true"
        [ngModel]="context.period()"
        (ngModelChange)="setPeriod($event)"
      />
      <app-select
        class="context-select"
        ariaLabel="Rafraîchissement automatique"
        [items]="refreshOptions"
        bindLabel="label"
        bindValue="value"
        [searchable]="false"
        [small]="true"
        [ngModel]="context.refreshSeconds()"
        (ngModelChange)="setRefresh($event)"
      />
      <app-button
        icon="pi pi-refresh"
        [iconOnly]="true"
        size="small"
        severity="secondary"
        [outlined]="true"
        ariaLabel="Actualiser"
        (clicked)="context.refresh()"
      />
    </div>
  `,
  styles: `
    .context-select {
      min-width: 11rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardContextBarComponent {
  protected readonly context = inject(DashboardContext);
  /** La période ou le rafraîchissement a changé (l'éditeur le compte comme une modification). */
  readonly changed = output<void>();

  protected readonly periodOptions = PERIOD_OPTIONS;
  protected readonly refreshOptions = REFRESH_OPTIONS;

  protected setPeriod(period: PeriodPreset | null): void {
    if (period && period !== this.context.period()) {
      this.context.period.set(period);
      this.changed.emit();
    }
  }

  protected setRefresh(seconds: number | null): void {
    if (seconds != null && seconds !== this.context.refreshSeconds()) {
      this.context.refreshSeconds.set(seconds);
      this.changed.emit();
    }
  }
}
