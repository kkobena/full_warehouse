import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ChartComponent} from 'app/shared/chart/chart.component';

import {DashboardCAService} from '../services/dashboard-ca.service';
import {
  DateRangeFilterComponent
} from '../../../shared/components/date-range-filter/date-range-filter.component';
import {IPerformanceVendeur} from '../../../shared/model/report';
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {ChartBuilderService, ChartConfig} from '../../../shared/util/chart-builder.service';
import {formatCurrency, formatDecimal, formatNumber} from 'app/shared/utils/format-utils';
import {DataTableComponent, SortableHeaderDirective} from '../../../shared/ui';

@Component({
  selector: 'app-sales-by-staff',
  imports: [
    CommonModule,
    ChartComponent,
    DateRangeFilterComponent,
    DataTableComponent,
    SortableHeaderDirective
  ],
  templateUrl: './sales-by-staff.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./sales-by-staff.component.scss'],
})
export default class SalesByStaffComponent implements OnInit {
  protected readonly staff = signal<IPerformanceVendeur[]>([]);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate = signal<Date | null>(new Date());

  protected chartConfig = signal<ChartConfig | null>(null);

  protected readonly totalVentes = computed(() => this.staff().reduce((s, v) => s + (v.nombreVentes ?? 0), 0));
  protected readonly totalMontant = computed(() => this.staff().reduce((s, v) => s + (v.montantTotal ?? 0), 0));

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatDecimal = formatDecimal;
  protected readonly formatNumber = formatNumber;

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
    this.svc.getSalesByStaff(startDate, endDate).subscribe({
      next: res => {
        const data = res.body ?? [];
        this.staff.set(data);
        this.buildChart(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected sharePercent(montant: number | undefined): number {
    const total = this.totalMontant();
    if (!total || !montant) {
      return 0;
    }
    return Math.round((montant / total) * 100);
  }

  private buildChart(data: IPerformanceVendeur[]): void {
    if (!data.length) {
      this.chartConfig.set(null);
      return;
    }
    const labels = data.map(v => v.vendeurNom ?? '');
    const amounts = data.map(v => v.montantTotal ?? 0);
    this.chartConfig.set(this.chartBuilder.barConfig(labels, amounts, 'CA Vendeur'));
  }
}
