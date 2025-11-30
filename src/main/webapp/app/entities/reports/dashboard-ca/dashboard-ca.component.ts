import { Component, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { Tag } from 'primeng/tag';
import { DatePicker } from 'primeng/datepicker';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import {
  IDailyCA,
  IDashboardCASummary,
  IDashboardCAEvolution,
  IPaymentMethodCA,
  IProductFamilyCA,
  IPaymentMethodSummary,
  IProductFamilySummary,
} from 'app/shared/model/report';
import { ITopProduct } from 'app/shared/model/report/top-product.model';
import { DashboardCAService } from '../services/dashboard-ca.service';

import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
import { FloatLabel } from 'primeng/floatlabel';
import { PrimeNG } from 'primeng/config';
import { TranslateService } from '@ngx-translate/core';
Chart.register(...registerables);

interface PeriodOption {
  label: string;
  value: string;
}

@Component({
  selector: 'jhi-dashboard-ca',
  templateUrl: './dashboard-ca.component.html',
  styleUrl: './dashboard-ca.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    Card,
    SelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule,
    Tag,
    DatePicker,
    FloatLabel
  ]
})
export default class DashboardCAComponent implements OnInit, OnDestroy {

  @ViewChild('evolutionChartCanvas') evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('paymentChartCanvas') paymentChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('familyChartCanvas') familyChartCanvas?: ElementRef<HTMLCanvasElement>;

  selectedPeriod = signal<string>('week');
  startDate = signal<Date>(this.getDefaultStartDate());
  endDate = signal<Date>(new Date());
  summary = signal<IDashboardCASummary | null>(null);
  topProducts = signal<ITopProduct[]>([]);
  paymentMethodData = signal<IPaymentMethodSummary[]>([]);
  productFamilyData = signal<IProductFamilySummary[]>([]);
  isLoading = signal<boolean>(false);

  // Charts
  private evolutionChart?: Chart;
  private paymentChart?: Chart;
  private familyChart?: Chart;

  // Filters
  private dashboardCAService = inject(DashboardCAService);

  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  periodOptions: PeriodOption[] = [
    { label: 'Aujourd\'hui', value: 'today' },
    { label: '7 derniers jours', value: 'week' },
    { label: '30 derniers jours', value: 'month' },
    { label: 'Cette année', value: 'year' },
    { label: 'Période personnalisée', value: 'custom' },
  ];

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadDashboard(): void {
    this.isLoading.set(true);

    // Load summary KPIs
    this.dashboardCAService.getOverallSummary().subscribe({
      next: (res: HttpResponse<IDashboardCASummary>) => {
        this.summary.set(res.body);
      },
      error: () => {
        console.error('Error loading summary');
      },
    });

    // Calculate date range based on selected period
    const { start, end } = this.getDateRange();

    // Load evolution data for chart
    this.dashboardCAService.getEvolutionData('daily', start, end).subscribe({
      next: (res: HttpResponse<IDashboardCAEvolution>) => {
        if (res.body) {
          this.createEvolutionChart(res.body);
        }
      },
      error: () => {
        console.error('Error loading evolution data');
      },
    });

    // Load payment method distribution
    this.dashboardCAService.getPaymentMethodDistribution(start, end).subscribe({
      next: (res: HttpResponse<IPaymentMethodCA[]>) => {
        const data = this.aggregatePaymentMethodData(res.body ?? []);
        this.paymentMethodData.set(data);
        this.createPaymentChart(data);
      },
      error: () => {
        console.error('Error loading payment method data');
      },
    });

    // Load product family distribution
    this.dashboardCAService.getProductFamilyDistribution(start, end).subscribe({
      next: (res: HttpResponse<IProductFamilyCA[]>) => {
        const data = this.aggregateProductFamilyData(res.body ?? []);
        this.productFamilyData.set(data);
        this.createFamilyChart(data);
      },
      error: () => {
        console.error('Error loading product family data');
      },
    });

    // Load top products
    this.dashboardCAService.getTopProducts(start, end, 10).subscribe({
      next: (res: HttpResponse<ITopProduct[]>) => {
        this.topProducts.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        console.error('Error loading top products');
      },
    });
  }

  onPeriodChange(): void {
    const period = this.selectedPeriod();
    if (period !== 'custom') {
      const { start, end } = this.getDateRange();
      this.startDate.set(new Date(start));
      this.endDate.set(new Date(end));
    }
    this.loadDashboard();
  }

  onRefresh(): void {
    this.dashboardCAService.refreshViews().subscribe({
      next: () => {
        this.loadDashboard();
      },
      error: () => {
        console.error('Error refreshing views');
      },
    });
  }

  exportToPdf(): void {
    const { start, end } = this.getDateRange();
    this.dashboardCAService.exportDashboardToPdf(start, end).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `dashboard-ca-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  // Chart creation methods

  private createEvolutionChart(data: IDashboardCAEvolution): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }

    if (!this.evolutionChartCanvas) {
      setTimeout(() => this.createEvolutionChart(data), 100);
      return;
    }

    const ctx = this.evolutionChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'line'> = {
      labels: data.labels ?? [],
      datasets: [
        {
          label: 'CA (FCFA)',
          data: data.caValues ?? [],
          borderColor: '#2196F3',
          backgroundColor: 'rgba(33, 150, 243, 0.1)',
          tension: 0.4,
          fill: true,
        },
        {
          label: 'Nb Transactions',
          data: data.transactionCounts ?? [],
          borderColor: '#4CAF50',
          backgroundColor: 'rgba(76, 175, 80, 0.1)',
          tension: 0.4,
          yAxisID: 'y1',
        },
      ],
    };

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: chartData,
      options: {
        responsive: true,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Évolution du Chiffre d\'Affaires',
          },
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            grid: {
              drawOnChartArea: false,
            },
          },
        },
      },
    };

    this.evolutionChart = new Chart(ctx, config);
  }

  private createPaymentChart(data: IPaymentMethodSummary[]): void {
    if (this.paymentChart) {
      this.paymentChart.destroy();
    }

    if (!this.paymentChartCanvas) {
      setTimeout(() => this.createPaymentChart(data), 100);
      return;
    }

    const ctx = this.paymentChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'pie'> = {
      labels: data.map(d => d.paymentMethod),
      datasets: [
        {
          data: data.map(d => d.montantTotal),
          backgroundColor: ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0', '#00BCD4'],
        },
      ],
    };

    const config: ChartConfiguration<'pie'> = {
      type: 'pie',
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'right',
          },
          title: {
            display: true,
            text: 'Répartition CA par Mode de Paiement',
          },
        },
      },
    };

    this.paymentChart = new Chart(ctx, config);
  }

  private createFamilyChart(data: IProductFamilySummary[]): void {
    if (this.familyChart) {
      this.familyChart.destroy();
    }

    if (!this.familyChartCanvas) {
      setTimeout(() => this.createFamilyChart(data), 100);
      return;
    }

    const ctx = this.familyChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'bar'> = {
      labels: data.map(d => d.famille),
      datasets: [
        {
          label: 'CA (FCFA)',
          data: data.map(d => d.caTotal),
          backgroundColor: '#2196F3',
        },
        {
          label: 'Marge Brute (FCFA)',
          data: data.map(d => d.margeBrute),
          backgroundColor: '#4CAF50',
        },
      ],
    };

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'CA par Famille de Produits',
          },
        },
      },
    };

    this.familyChart = new Chart(ctx, config);
  }

  private destroyCharts(): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }
    if (this.paymentChart) {
      this.paymentChart.destroy();
    }
    if (this.familyChart) {
      this.familyChart.destroy();
    }
  }

  // Helper methods

  private getDefaultStartDate(): Date {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date;
  }

  private getDateRange(): { start: string; end: string } {
    const period = this.selectedPeriod();
    const end = new Date();
    let start = new Date();

    switch (period) {
      case 'today':
        start = new Date();
        break;
      case 'week':
        start.setDate(end.getDate() - 7);
        break;
      case 'month':
        start.setDate(end.getDate() - 30);
        break;
      case 'year':
        start.setFullYear(end.getFullYear(), 0, 1);
        break;
      case 'custom':
        start = this.startDate();
        end.setTime(this.endDate().getTime());
        break;
    }

    return {
      start: this.formatDate(start),
      end: this.formatDate(end),
    };
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  private aggregatePaymentMethodData(data: IPaymentMethodCA[]): IPaymentMethodSummary[] {
    const map = new Map<string, { total: number; count: number; code: string }>();

    data.forEach(item => {
      const key = item.paymentMethod ?? 'Unknown';
      const existing = map.get(key) || { total: 0, count: 0, code: item.paymentCode ?? '' };
      existing.total += item.montantTotal ?? 0;
      existing.count += item.nbPayments ?? 0;
      map.set(key, existing);
    });

    const totalCA = Array.from(map.values()).reduce((sum, item) => sum + item.total, 0);

    return Array.from(map.entries())
      .map(([method, data]) => ({
        paymentMethod: method,
        paymentCode: data.code,
        montantTotal: data.total,
        nbPayments: data.count,
        percentage: totalCA > 0 ? (data.total * 100) / totalCA : 0,
      }))
      .sort((a, b) => b.montantTotal - a.montantTotal);
  }

  private aggregateProductFamilyData(data: IProductFamilyCA[]): IProductFamilySummary[] {
    const map = new Map<string, { ca: number; marge: number; tauxMarge: number; count: number }>();

    data.forEach(item => {
      const key = item.famille ?? 'Non classé';
      const existing = map.get(key) || { ca: 0, marge: 0, tauxMarge: 0, count: 0 };
      existing.ca += item.caTotal ?? 0;
      existing.marge += item.margeBrute ?? 0;
      existing.count++;
      map.set(key, existing);
    });

    const totalCA = Array.from(map.values()).reduce((sum, item) => sum + item.ca, 0);

    return Array.from(map.entries())
      .map(([famille, data]) => ({
        famille,
        caTotal: data.ca,
        margeBrute: data.marge,
        tauxMargePct: data.ca > 0 ? (data.marge * 100) / data.ca : 0,
        percentage: totalCA > 0 ? (data.ca * 100) / totalCA : 0,
      }))
      .sort((a, b) => b.caTotal - a.caTotal)
      .slice(0, 10); // Top 10
  }

  getEvolutionClass(value: number | undefined): string {
    if (!value) return '';
    return value >= 0 ? 'evolution-positive' : 'evolution-negative';
  }

  formatCurrency(value: number | undefined): string {
    if (value === undefined || value === null) return '0';
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  formatPercent(value: number | undefined): string {
    if (value === undefined || value === null) return '0';
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  // Export methods
  exportDailySummaryToExcel(): void {
    const startDate = this.formatDateForAPI(this.startDate());
    const endDate = this.formatDateForAPI(this.endDate());

    this.dashboardCAService.exportDailySummaryToExcel(startDate, endDate).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          this.downloadFile(res.body, `dashboard_ca_${startDate}_${endDate}.xlsx`, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        }
      },
      error: () => {
        console.error('Error exporting to Excel');
      },
    });
  }

  exportDailySummaryToCsv(): void {
    const startDate = this.formatDateForAPI(this.startDate());
    const endDate = this.formatDateForAPI(this.endDate());

    this.dashboardCAService.exportDailySummaryToCsv(startDate, endDate).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          this.downloadFile(res.body, `dashboard_ca_${startDate}_${endDate}.csv`, 'text/csv');
        }
      },
      error: () => {
        console.error('Error exporting to CSV');
      },
    });
  }

  exportTopProductsToExcel(): void {
    const startDate = this.formatDateForAPI(this.startDate());
    const endDate = this.formatDateForAPI(this.endDate());

    this.dashboardCAService.exportTopProductsToExcel(startDate, endDate).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          this.downloadFile(res.body, `top_products_${startDate}_${endDate}.xlsx`, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        }
      },
      error: () => {
        console.error('Error exporting top products to Excel');
      },
    });
  }

  exportTopProductsToCsv(): void {
    const startDate = this.formatDateForAPI(this.startDate());
    const endDate = this.formatDateForAPI(this.endDate());

    this.dashboardCAService.exportTopProductsToCsv(startDate, endDate).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          this.downloadFile(res.body, `top_products_${startDate}_${endDate}.csv`, 'text/csv');
        }
      },
      error: () => {
        console.error('Error exporting top products to CSV');
      },
    });
  }

  private downloadFile(blob: Blob, filename: string, mimeType: string): void {
    const url = window.URL.createObjectURL(new Blob([blob], { type: mimeType }));
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private formatDateForAPI(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
