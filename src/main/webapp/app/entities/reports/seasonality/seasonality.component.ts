import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';

import { DashboardCAService } from '../services/dashboard-ca.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IDashboardCAEvolution } from '../../../shared/model/report';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ChartBuilderService, ChartConfig } from '../../../shared/util/chart-builder.service';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-seasonality',
  imports: [CommonModule, ChartModule, TableModule, DateRangeFilterComponent],
  templateUrl: './seasonality.component.html',
  styleUrls: ['./seasonality.component.scss'],
})
export default class SeasonalityComponent implements OnInit {
  protected readonly currentEvolution  = signal<IDashboardCAEvolution | null>(null);
  protected readonly previousEvolution = signal<IDashboardCAEvolution | null>(null);
  protected readonly isLoading         = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate   = signal<Date | null>(new Date());

  protected chartConfig = signal<ChartConfig | null>(null);

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber   = formatNumber;

  protected readonly tableRows = computed(() => {
    const curr = this.currentEvolution();
    if (!curr?.labels) return [];
    return curr.labels.map((label, i) => ({
      label,
      ca:           curr.caValues?.[i]             ?? 0,
      caPrev:       curr.caPreviousValues?.[i]      ?? 0,
      transactions: curr.transactionCounts?.[i]     ?? 0,
      evolution:    this.calcEvolution(curr.caValues?.[i] ?? 0, curr.caPreviousValues?.[i] ?? 0),
    }));
  });

  protected readonly totalCA   = computed(() => this.tableRows().reduce((s, r) => s + r.ca, 0));
  protected readonly totalPrev = computed(() => this.tableRows().reduce((s, r) => s + r.caPrev, 0));
  protected readonly totalEvo  = computed(() => this.calcEvolution(this.totalCA(), this.totalPrev()));

  private readonly svc          = inject(DashboardCAService);
  private readonly chartBuilder = inject(ChartBuilderService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const startDate = DATE_FORMAT_ISO_DATE(this.fromDate()) ?? undefined;
    const endDate   = DATE_FORMAT_ISO_DATE(this.toDate())   ?? undefined;
    if (!startDate || !endDate) return;

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

  private buildChart(evo: IDashboardCAEvolution | null): void {
    if (!evo?.labels?.length) { this.chartConfig.set(null); return; }
    const config = this.chartBuilder.multiBarConfig(evo.labels, [
      { label: 'CA Période',      data: evo.caValues         ?? [] },
      { label: 'CA Période préc.', data: evo.caPreviousValues ?? [] },
    ]);
    this.chartConfig.set(config);
  }

  protected calcEvolution(current: number, previous: number): number {
    if (!previous) return current > 0 ? 100 : 0;
    return Math.round(((current - previous) / previous) * 100);
  }

  protected evoClass(pct: number): string {
    if (pct > 0)  return 'text-success';
    if (pct < 0)  return 'text-danger';
    return 'text-muted';
  }

  protected evoIcon(pct: number): string {
    if (pct > 0)  return 'pi pi-arrow-up';
    if (pct < 0)  return 'pi pi-arrow-down';
    return 'pi pi-minus';
  }
}
