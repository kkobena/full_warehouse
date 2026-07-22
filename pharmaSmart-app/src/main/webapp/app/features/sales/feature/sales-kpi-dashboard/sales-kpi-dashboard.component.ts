import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { ChartComponent } from 'app/shared/chart/chart.component';

import { BadgeComponent, ButtonComponent, DataTableComponent, SkeletonComponent } from '../../../../shared/ui';
import { IDailySalesSummary } from '../../../../shared/model/report/daily-sales-summary.model';
import { IStockAlert, StockAlertType } from '../../../../shared/model/report/stock-alert.model';
import { SalesSummaryReportService } from '../../../../entities/reports/services/sales-summary-report.service';
import { StockAlertReportService } from '../../../../entities/reports/services/stock-alert-report.service';
import {
  backgroundColor,
  hoverBackgroundColor,
  surfaceBorder,
  textColor,
  textColorSecondary,
} from '../../../../shared/chart-color-helper';

interface KpiCard {
  label: string;
  value: number;
  subtitle: string;
  icon: string;
  colorClass: string;
}

@Component({
  selector: 'app-sales-kpi-dashboard',
  templateUrl: './sales-kpi-dashboard.component.html',
  styleUrl: './sales-kpi-dashboard.component.scss',
  providers: [DatePipe],
  imports: [CommonModule, ChartComponent, ButtonComponent, SkeletonComponent, BadgeComponent, DataTableComponent],
})
export class SalesKpiDashboardComponent implements OnInit {
  private readonly summaryService = inject(SalesSummaryReportService);
  private readonly stockAlertService = inject(StockAlertReportService);
  private readonly datePipe = inject(DatePipe);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly StockAlertType = StockAlertType;

  protected loading = signal(true);
  protected alertCounts = signal<Record<string, number>>({});
  protected ruptureAlerts = signal<IStockAlert[]>([]);
  protected ruptureLoading = signal(false);

  protected kpiToday: KpiCard = { label: 'CA du jour', value: 0, subtitle: '0 ventes', icon: 'pi pi-sun', colorClass: 'kpi-primary' };
  protected kpiWeek: KpiCard = { label: 'CA semaine', value: 0, subtitle: '0 ventes', icon: 'pi pi-calendar-week', colorClass: 'kpi-success' };
  protected kpiMonth: KpiCard = { label: 'CA du mois', value: 0, subtitle: '0 ventes', icon: 'pi pi-calendar', colorClass: 'kpi-info' };
  protected kpiBasket: KpiCard = { label: 'Panier moyen', value: 0, subtitle: "aujourd'hui", icon: 'pi pi-shopping-cart', colorClass: 'kpi-warning' };

  protected chartData: any = null;
  protected chartOptions: any = null;

  ngOnInit(): void {
    this.loadKpis();
    this.loadAlertCounts();
    this.loadRuptureAlerts();
  }

  private loadAlertCounts(): void {
    this.stockAlertService.getStockAlertsCount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: res => this.alertCounts.set(res.body ?? {}) });
  }

  protected loadRuptureAlerts(): void {
    this.ruptureLoading.set(true);
    this.stockAlertService.getStockAlerts({ page: 0, size: 20, types: [StockAlertType.RUPTURE] })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.ruptureAlerts.set(res.body ?? []);
          this.ruptureLoading.set(false);
        },
        error: () => this.ruptureLoading.set(false),
      });
  }

  protected loadKpis(): void {
    this.loading.set(true);
    const today = this.datePipe.transform(new Date(), 'yyyy-MM-dd')!;
    const weekStart = this.datePipe.transform(this.getWeekStart(), 'yyyy-MM-dd')!;
    const monthStart = this.datePipe.transform(this.getMonthStart(), 'yyyy-MM-dd')!;

    forkJoin({
      today: this.summaryService.getDailySalesSummaryByDate(today),
      week: this.summaryService.getDailySalesSummary(weekStart, today),
      month: this.summaryService.getDailySalesSummary(monthStart, today),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ today, week, month }) => {
          this.buildKpis(today.body ?? [], week.body ?? [], month.body ?? []);
          this.buildChart(month.body ?? []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private buildKpis(todayData: IDailySalesSummary[], weekData: IDailySalesSummary[], monthData: IDailySalesSummary[]): void {
    const sumCA = (rows: IDailySalesSummary[]) => rows.reduce((s, r) => s + (r.caTotal ?? 0), 0);
    const sumVentes = (rows: IDailySalesSummary[]) => rows.reduce((s, r) => s + (r.nbVentes ?? 0), 0);

    const caToday = sumCA(todayData);
    const nbToday = sumVentes(todayData);
    const panierMoyen = nbToday > 0 ? Math.round(caToday / nbToday) : 0;

    this.kpiToday = { ...this.kpiToday, value: caToday, subtitle: `${nbToday} vente(s)` };
    this.kpiWeek = { ...this.kpiWeek, value: sumCA(weekData), subtitle: `${sumVentes(weekData)} vente(s)` };
    this.kpiMonth = { ...this.kpiMonth, value: sumCA(monthData), subtitle: `${sumVentes(monthData)} vente(s)` };
    this.kpiBasket = { ...this.kpiBasket, value: panierMoyen };
  }

  private buildChart(monthData: IDailySalesSummary[]): void {
    const ds = getComputedStyle(document.documentElement);
    const byType = monthData.reduce((acc, row) => {
      const key = row.typeVente ?? 'Autre';
      acc[key] = (acc[key] ?? 0) + (row.caTotal ?? 0);
      return acc;
    }, {} as Record<string, number>);

    const labels = Object.keys(byType);
    const values = labels.map(k => byType[k]);
    const bgs = backgroundColor(ds);
    const hovers = hoverBackgroundColor(ds);

    this.chartData = {
      labels,
      datasets: [{
        label: 'CA par type (mois en cours)',
        data: values,
        backgroundColor: bgs.slice(0, labels.length),
        hoverBackgroundColor: hovers.slice(0, labels.length),
        borderWidth: 1,
      }],
    };

    this.chartOptions = {
      responsive: true,
      plugins: {
        legend: { labels: { color: textColor(ds) } },
      },
      scales: {
        x: { ticks: { color: textColorSecondary(ds) }, grid: { color: surfaceBorder(ds) } },
        y: { ticks: { color: textColorSecondary(ds) }, grid: { color: surfaceBorder(ds) } },
      },
    };
  }

  private getWeekStart(): Date {
    const d = new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.getFullYear(), d.getMonth(), diff);
  }

  private getMonthStart(): Date {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }
}
