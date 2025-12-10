import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { Drawer } from 'primeng/drawer';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { ISalesForecast, IForecastSummary, ForecastMethod, FORECAST_METHOD_LABELS } from 'app/shared/model/report/sales-forecast.model';
import { SalesForecastService } from '../services/sales-forecast.service';
import { formatCurrency, formatPercent, formatMonth } from 'app/shared/utils/format-utils';

import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
Chart.register(...registerables);

interface MethodOption {
  label: string;
  value: string;
}

interface PeriodOption {
  label: string;
  value: number;
}

@Component({
  selector: 'jhi-sales-forecast',
  templateUrl: './sales-forecast.component.html',
  styleUrl: './sales-forecast.component.scss',
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule, ToolbarModule, WarehouseCommonModule, Tag, Drawer],
})
export default class SalesForecastComponent implements OnInit {

  @ViewChild('forecastChartCanvas') forecastChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('confidenceChartCanvas') confidenceChartCanvas?: ElementRef<HTMLCanvasElement>;

  // Data signals
  protected summary = signal<IForecastSummary | null>(null);
  protected forecasts = signal<ISalesForecast[]>([]);
  protected seasonalityDetected = signal<boolean>(false);
  protected isLoading = signal<boolean>(false);
  protected helpDrawerVisible = signal<boolean>(false);

  // Charts

  // Filters
  protected selectedMethod = signal<string>('LINEAR_REGRESSION');
  protected selectedPeriod = signal<number>(6);

  protected methodOptions: MethodOption[] = [
    { label: 'Régression Linéaire', value: 'LINEAR_REGRESSION' },
    { label: 'Moyenne Mobile', value: 'MOVING_AVERAGE' },
    { label: 'Saisonnier', value: 'SEASONAL' },
  ];

 protected periodOptions: PeriodOption[] = [
    { label: '3 mois', value: 3 },
    { label: '6 mois', value: 6 },
    { label: '12 mois', value: 12 },
  ];
  private salesForecastService = inject(SalesForecastService);

  private forecastChart?: Chart;
  private confidenceChart?: Chart;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    // Load summary
    this.salesForecastService.getSummary().subscribe({
      next: (res: HttpResponse<IForecastSummary>) => {
        this.summary.set(res.body);
      },
      error: () => {
        console.error('Error loading forecast summary');
      },
    });

    // Load forecast
    const method = this.selectedMethod();
    const period = this.selectedPeriod();

    this.salesForecastService.getForecast(period, method).subscribe({
      next: (res: HttpResponse<ISalesForecast[]>) => {
        this.forecasts.set(res.body ?? []);
        this.createForecastChart(res.body ?? []);
        this.createConfidenceChart(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        console.error('Error loading forecast');
      },
    });

    // Check seasonality
    this.salesForecastService.detectSeasonality().subscribe({
      next: (res: HttpResponse<boolean>) => {
        this.seasonalityDetected.set(res.body ?? false);
      },
    });
  }

protected  onMethodChange(): void {
    this.loadData();
  }

  protected onPeriodChange(): void {
    this.loadData();
  }

  // Chart creation

  private createForecastChart(data: ISalesForecast[]): void {
    if (this.forecastChart) {
      this.forecastChart.destroy();
    }

    if (!this.forecastChartCanvas) {
      setTimeout(() => this.createForecastChart(data), 100);
      return;
    }

    const ctx = this.forecastChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'line'> = {
      labels: data.map(d => this.formatMonth(d.forecastPeriod ?? '')),
      datasets: [
        {
          label: 'CA Prévu',
          data: data.map(d => d.forecastedCA ?? 0),
          borderColor: '#2196F3',
          backgroundColor: 'rgba(33, 150, 243, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
        },
      ],
    };

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Prévisions de Chiffre d\'Affaires',
          },
          tooltip: {
            callbacks: {
              label: context => {
                return `CA: ${this.formatCurrency(context.parsed.y)} FCFA`;
              },
            },
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: value => this.formatCurrency(Number(value)),
            },
          },
        },
      },
    };

    this.forecastChart = new Chart(ctx, config);
  }

  private createConfidenceChart(data: ISalesForecast[]): void {
    if (this.confidenceChart) {
      this.confidenceChart.destroy();
    }

    if (!this.confidenceChartCanvas) {
      setTimeout(() => this.createConfidenceChart(data), 100);
      return;
    }

    const ctx = this.confidenceChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'line'> = {
      labels: data.map(d => this.formatMonth(d.forecastPeriod ?? '')),
      datasets: [
        {
          label: 'Limite Haute',
          data: data.map(d => d.upperBound ?? 0),
          borderColor: '#FF9800',
          backgroundColor: 'rgba(255, 152, 0, 0.1)',
          borderWidth: 1,
          borderDash: [5, 5],
          fill: false,
        },
        {
          label: 'Prévision',
          data: data.map(d => d.forecastedCA ?? 0),
          borderColor: '#4CAF50',
          backgroundColor: 'rgba(76, 175, 80, 0.2)',
          borderWidth: 2,
          fill: '+1',
        },
        {
          label: 'Limite Basse',
          data: data.map(d => d.lowerBound ?? 0),
          borderColor: '#FF5722',
          backgroundColor: 'rgba(255, 87, 34, 0.1)',
          borderWidth: 1,
          borderDash: [5, 5],
          fill: false,
        },
      ],
    };

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'Intervalles de Confiance (95%)',
          },
        },
        scales: {
          y: {
            beginAtZero: true,
          },
        },
      },
    };

    this.confidenceChart = new Chart(ctx, config);
  }

  // Helpers - using shared utilities

  protected formatCurrency = formatCurrency;
  protected formatPercent = formatPercent;
  protected formatMonth = formatMonth;

  protected formatMonthOld(period: string): string {
    // Convert "2024-01" to "Janvier 2024" - OLD VERSION KEPT FOR REFERENCE
    const [year, month] = period.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1, 1);
    return date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  protected getMethodLabel(method: string): string {
    return FORECAST_METHOD_LABELS[method as ForecastMethod] || method;
  }

  protected getAccuracySeverity(accuracy: number | undefined): 'success' | 'warn' | 'danger' {
    if (!accuracy) return 'danger';
    if (accuracy >= 80) return 'success';
    if (accuracy >= 60) return 'warn';
    return 'danger';
  }

  protected getTotalForecastedCA(): number {
    return this.forecasts().reduce((sum, f) => sum + (f.forecastedCA || 0), 0);
  }

  protected toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }
}
