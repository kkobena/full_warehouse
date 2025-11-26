import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IComparativeCA, IComparativeByType, IComparativeSummary } from 'app/shared/model/report/comparative-report.model';
import { ComparativeReportService } from '../services/comparative-report.service';

import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
Chart.register(...registerables);

interface ComparisonTypeOption {
  label: string;
  value: string;
}

@Component({
  selector: 'jhi-comparative-analysis',
  templateUrl: './comparative-analysis.component.html',
  styleUrl: './comparative-analysis.component.scss',
  imports: [CommonModule, FormsModule, ButtonModule, Card, SelectModule, ToolbarModule, WarehouseCommonModule, Tag],
})
export default class ComparativeAnalysisComponent implements OnInit {
  private comparativeReportService = inject(ComparativeReportService);

  @ViewChild('evolutionChartCanvas') evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('typeChartCanvas') typeChartCanvas?: ElementRef<HTMLCanvasElement>;

  // Data signals
  summary = signal<IComparativeSummary | null>(null);
  comparisons = signal<IComparativeCA[]>([]);
  byType = signal<IComparativeByType[]>([]);
  isLoading = signal<boolean>(false);

  // Charts
  private evolutionChart?: Chart;
  private typeChart?: Chart;

  // Filters
  selectedComparisonType = signal<string>('MONTHLY');
  selectedYear = signal<number>(new Date().getFullYear());

  comparisonTypeOptions: ComparisonTypeOption[] = [
    { label: 'Mensuel', value: 'MONTHLY' },
    { label: 'Trimestriel', value: 'QUARTERLY' },
    { label: 'Annuel', value: 'YEARLY' },
  ];

  yearOptions: number[] = [];

  ngOnInit(): void {
    // Generate year options (current year + 5 years back)
    const currentYear = new Date().getFullYear();
    for (let i = 0; i < 6; i++) {
      this.yearOptions.push(currentYear - i);
    }

    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    // Load summary
    this.comparativeReportService.getComparativeSummary().subscribe({
      next: (res: HttpResponse<IComparativeSummary>) => {
        this.summary.set(res.body);
      },
      error: () => {
        console.error('Error loading summary');
      },
    });

    // Load comparison data based on type
    const year = this.selectedYear();
    const comparisonType = this.selectedComparisonType();

    if (comparisonType === 'MONTHLY') {
      this.comparativeReportService.getMonthlyComparison(year).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          console.error('Error loading monthly comparison');
        },
      });
    } else if (comparisonType === 'QUARTERLY') {
      this.comparativeReportService.getQuarterlyComparison(year).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          console.error('Error loading quarterly comparison');
        },
      });
    } else {
      const startDate = `${year - 5}-01-01`;
      const endDate = `${year}-12-31`;
      this.comparativeReportService.getYearlyComparison(startDate, endDate).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          console.error('Error loading yearly comparison');
        },
      });
    }

    // Load by sales type
    this.comparativeReportService.getComparisonBySalesType(year, year - 1).subscribe({
      next: (res: HttpResponse<IComparativeByType[]>) => {
        this.byType.set(res.body ?? []);
        this.createTypeChart(res.body ?? []);
      },
      error: () => {
        console.error('Error loading sales type comparison');
      },
    });
  }

  onComparisonTypeChange(): void {
    this.loadData();
  }

  onYearChange(): void {
    this.loadData();
  }

  exportToPdf(): void {
    const comparisonType = this.selectedComparisonType();
    const year = this.selectedYear();

    this.comparativeReportService.exportToPdf(comparisonType, year).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `tableaux-comparatifs-${comparisonType.toLowerCase()}-${year}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  // Chart creation

  private createEvolutionChart(data: IComparativeCA[]): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }

    if (!this.evolutionChartCanvas) {
      setTimeout(() => this.createEvolutionChart(data), 100);
      return;
    }

    const ctx = this.evolutionChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const chartData: ChartData<'bar'> = {
      labels: data.map(d => d.periodLabel ?? ''),
      datasets: [
        {
          label: 'CA Actuel',
          data: data.map(d => d.currentCA ?? 0),
          backgroundColor: '#2196F3',
        },
        {
          label: 'CA Précédent',
          data: data.map(d => d.previousCA ?? 0),
          backgroundColor: '#FF9800',
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
            text: 'Évolution Comparative du CA',
          },
        },
        scales: {
          y: {
            beginAtZero: true,
          },
        },
      },
    };

    this.evolutionChart = new Chart(ctx, config);
  }

  private createTypeChart(data: IComparativeByType[]): void {
    if (this.typeChart) {
      this.typeChart.destroy();
    }

    if (!this.typeChartCanvas) {
      setTimeout(() => this.createTypeChart(data), 100);
      return;
    }

    const ctx = this.typeChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const year = this.selectedYear();

    const chartData: ChartData<'bar'> = {
      labels: data.map(d => d.saleTypeLabel ?? ''),
      datasets: [
        {
          label: `CA ${year}`,
          data: data.map(d => d.currentYearCA ?? 0),
          backgroundColor: '#4CAF50',
        },
        {
          label: `CA ${year - 1}`,
          data: data.map(d => d.previousYearCA ?? 0),
          backgroundColor: '#9C27B0',
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
            text: 'CA par Type de Vente',
          },
        },
      },
    };

    this.typeChart = new Chart(ctx, config);
  }

  // Helpers

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
    const sign = value >= 0 ? '+' : '';
    return (
      sign +
      new Intl.NumberFormat('fr-FR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(value)
    );
  }
}
