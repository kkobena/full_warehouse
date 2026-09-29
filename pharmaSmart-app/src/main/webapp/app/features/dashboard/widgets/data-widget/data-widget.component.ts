import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import { ChartComponent } from 'app/shared/chart/chart.component';
import { ButtonComponent, DataTableComponent, SkeletonComponent } from 'app/shared/ui';
import { DashboardItem, PeriodPreset, VizType, WidgetComponent } from '../../models/dashboard.model';
import { evolutionPct, formatCell, formatEvolution, formatKpiValue, isEmpty, kpiAlert, toSeries, toTable } from '../../models/widget-data';
import { resolvePeriod } from '../../models/period';
import { buildChart } from '../../models/chart-config';
import { findWidgetDefinition } from '../widget-registry';
import { DashboardContext } from '../../data-access/dashboard-context';
import { DashboardWidgetApiService } from '../../data-access/services/dashboard-widget-api.service';
import { errorMessage } from '../../data-access/error-message';

/**
 * Widget de données générique : charge `/api/dashboard-widgets/{key}` pour la période du
 * dashboard et l'affiche dans la visualisation choisie (indicateur, courbe, barres, camembert,
 * tableau). Recharge quand la période change ou que le dashboard se rafraîchit.
 */
@Component({
  selector: 'app-data-widget',
  imports: [ChartComponent, SkeletonComponent, ButtonComponent, DataTableComponent],
  templateUrl: './data-widget.component.html',
  styleUrl: './data-widget.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataWidgetComponent implements WidgetComponent {
  readonly item = input.required<DashboardItem>();
  readonly editMode = input(false);
  readonly paramsChange = output<Record<string, string>>();

  private readonly context = inject(DashboardContext);
  private readonly api = inject(DashboardWidgetApiService);

  private readonly spec = computed(() => findWidgetDefinition(this.item().widgetKey)?.data);

  protected readonly viz = computed<VizType>(() => {
    const spec = this.spec();
    const chosen = this.item().params?.['viz'] as VizType | undefined;
    return chosen && spec?.visualizations.includes(chosen) ? chosen : (spec?.defaultViz ?? 'TABLE');
  });

  /** Période propre à la tuile si elle en a une, sinon celle du tableau de bord. */
  private readonly range = computed(() => {
    const own = this.item().params?.['periode'] as PeriodPreset | undefined;
    return own ? resolvePeriod(own) : this.context.range();
  });

  protected readonly resource = rxResource({
    params: () => ({
      key: this.item().widgetKey,
      range: this.spec()?.usesPeriod ? this.range() : null,
      params: this.item().params ?? {},
      tick: this.context.tick(),
    }),
    stream: ({ params }) => this.api.load(params.key, params.range, params.params),
  });

  protected readonly data = computed(() => (this.resource.hasValue() ? this.resource.value() : null));
  protected readonly empty = computed(() => {
    const data = this.data();
    return data != null && isEmpty(data);
  });
  protected readonly emptyLabel = computed(() => (this.spec()?.usesPeriod ? 'Aucune donnée sur la période' : 'Rien à signaler'));
  protected readonly error = computed(() => {
    const err = this.resource.error();
    return err ? errorMessage(err as HttpErrorResponse, 'Impossible de charger ce widget.') : null;
  });

  protected readonly kpi = computed(() => {
    const data = this.data();
    if (data?.kind !== 'KPI') {
      return null;
    }
    const pct = evolutionPct(data);
    return {
      data,
      value: formatKpiValue(data.value),
      pct,
      pctLabel: pct == null ? null : formatEvolution(pct),
      alert: kpiAlert(data.value, this.item().params),
    };
  });

  protected readonly table = computed(() => {
    const data = this.data();
    return data && this.viz() === 'TABLE' ? toTable(data) : null;
  });

  protected readonly chart = computed(() => {
    const data = this.data();
    const viz = this.viz();
    if (!data || viz === 'TABLE' || viz === 'KPI') {
      return null;
    }
    const series = toSeries(data, this.spec()?.chartColumns);
    return series ? buildChart(viz, series) : null;
  });

  protected readonly footer = computed(() => {
    const data = this.data();
    return data && data.kind !== 'KPI' ? data.footer : null;
  });

  protected readonly formatCell = formatCell;

  protected isNumeric(type: string): boolean {
    return type === 'number' || type === 'amount' || type === 'percent';
  }
}
