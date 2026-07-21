import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from '../../../shared/ui';
import { ChartComponent } from 'app/shared/chart/chart.component';
import { forkJoin } from 'rxjs';

import { DashboardCAService } from '../services/dashboard-ca.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IRemisesAnalysisKpi, ITopRemiseProduit } from '../../../shared/model/report';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ChartBuilderService, ChartConfig } from '../../../shared/util/chart-builder.service';
import { formatCurrency, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-remises-analysis',
  imports: [CommonModule, DataTableComponent, ChartComponent, DateRangeFilterComponent],
  templateUrl: './remises-analysis.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./remises-analysis.component.scss'],
})
export default class RemisesAnalysisComponent implements OnInit {
  protected readonly kpi          = signal<IRemisesAnalysisKpi | null>(null);
  protected readonly topProducts  = signal<ITopRemiseProduit[]>([]);
  protected readonly isLoading    = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate   = signal<Date | null>(new Date());

  protected chartConfig = signal<ChartConfig | null>(null);

  protected readonly tauxParticipation = computed(() => {
    const k = this.kpi();
    if (!k?.nbVentesTotal) return 0;
    return Math.round(((k.nbVentesAvecRemise ?? 0) / k.nbVentesTotal) * 100);
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatDecimal  = formatDecimal;
  protected readonly formatNumber   = formatNumber;

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
    forkJoin({
      kpi:     this.svc.getRemisesKpi(startDate, endDate),
      topProds: this.svc.getRemisesTopProducts(startDate, endDate, 10),
    }).subscribe({
      next: ({ kpi, topProds }) => {
        this.kpi.set(kpi.body);
        const prods = topProds.body ?? [];
        this.topProducts.set(prods);
        this.buildChart(prods);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private buildChart(data: ITopRemiseProduit[]): void {
    if (!data.length) { this.chartConfig.set(null); return; }
    const top5 = data.slice(0, 5);
    this.chartConfig.set(
      this.chartBuilder.barConfig(
        top5.map(p => p.libelle ?? ''),
        top5.map(p => p.montantRemise ?? 0),
        'Montant remisé',
      )
    );
  }

  protected sharePercent(montant: number | undefined): number {
    const total = this.kpi()?.totalRemise;
    if (!total || !montant) return 0;
    return Math.round((montant / total) * 100);
  }
}
