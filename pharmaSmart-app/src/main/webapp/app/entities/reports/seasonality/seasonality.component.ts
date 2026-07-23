import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, DataTableComponent, ToolbarComponent} from '../../../shared/ui';

import {DashboardCAService} from '../services/dashboard-ca.service';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {IDashboardCAEvolution} from '../../../shared/model/report';
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {ChartBuilderService, ChartConfig} from '../../../shared/util/chart-builder.service';
import {formatCurrency, formatNumber} from 'app/shared/utils/format-utils';
import {ChartComponent} from "../../../shared/chart/chart.component";

@Component({
  selector: 'app-seasonality',
  imports: [CommonModule, FormsModule, DataTableComponent, ChartComponent, PharmaDatePickerComponent, ButtonComponent, ToolbarComponent],
  templateUrl: './seasonality.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./seasonality.component.scss'],
})
export default class SeasonalityComponent implements OnInit {
  protected readonly currentEvolution = signal<IDashboardCAEvolution | null>(null);
  protected readonly previousEvolution = signal<IDashboardCAEvolution | null>(null);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate = signal<Date | null>(new Date());

  // `computed`, pas un appel direct à `dateToStruct()` dans le template : voir
  // date-range-filter (supprimé) pour l'explication de la boucle silencieuse évitée.
  protected readonly fromStruct = computed(() => this.dateToStruct(this.fromDate()));
  protected readonly toStruct = computed(() => this.dateToStruct(this.toDate()));

  protected chartConfig = signal<ChartConfig | null>(null);

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;

  protected readonly tableRows = computed(() => {
    const curr = this.currentEvolution();
    if (!curr?.labels) {
      return [];
    }
    return curr.labels.map((label, i) => ({
      label,
      ca: curr.caValues?.[i] ?? 0,
      caPrev: curr.caPreviousValues?.[i] ?? 0,
      transactions: curr.transactionCounts?.[i] ?? 0,
      evolution: this.calcEvolution(curr.caValues?.[i] ?? 0, curr.caPreviousValues?.[i] ?? 0),
    }));
  });

  protected readonly totalCA = computed(() => this.tableRows().reduce((s, r) => s + r.ca, 0));
  protected readonly totalPrev = computed(() => this.tableRows().reduce((s, r) => s + r.caPrev, 0));
  protected readonly totalEvo = computed(() => this.calcEvolution(this.totalCA(), this.totalPrev()));

  private readonly svc = inject(DashboardCAService);
  private readonly chartBuilder = inject(ChartBuilderService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const startDate = DATE_FORMAT_ISO_DATE(this.fromDate()) ?? undefined;
    const endDate = DATE_FORMAT_ISO_DATE(this.toDate()) ?? undefined;
    if (!startDate || !endDate) {
      return;
    }

    this.isLoading.set(true);
    this.svc.getEvolutionData('monthly', startDate, endDate).subscribe({
      next: res => {
        this.currentEvolution.set(res.body);
        this.buildChart(res.body);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected calcEvolution(current: number, previous: number): number {
    if (!previous) {
      return current > 0 ? 100 : 0;
    }
    return Math.round(((current - previous) / previous) * 100);
  }

  protected evoClass(pct: number): string {
    if (pct > 0) {
      return 'text-success';
    }
    if (pct < 0) {
      return 'text-danger';
    }
    return 'text-muted';
  }

  protected evoIcon(pct: number): string {
    if (pct > 0) {
      return 'pi pi-arrow-up';
    }
    if (pct < 0) {
      return 'pi pi-arrow-down';
    }
    return 'pi pi-minus';
  }

  private buildChart(evo: IDashboardCAEvolution | null): void {
    if (!evo?.labels?.length) {
      this.chartConfig.set(null);
      return;
    }
    const config = this.chartBuilder.multiBarConfig(evo.labels, [
      {label: 'CA Période', data: evo.caValues ?? []},
      {label: 'CA Période préc.', data: evo.caPreviousValues ?? []},
    ]);
    this.chartConfig.set(config);
  }

  protected dateToStruct(d: Date | null): NgbDateStruct | null {
    if (!d) return null;
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  protected structToDate(s: NgbDateStruct | null): Date | null {
    if (!s) return null;
    return new Date(s.year, s.month - 1, s.day);
  }
}
