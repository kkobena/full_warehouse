import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { Drawer } from 'primeng/drawer';

import { Chart, ChartConfiguration, registerables } from 'chart.js';

import { SupplierPerformanceReportService } from '../services/supplier-performance-report.service';
import { ISupplierEvolution, ISupplierPerformance, ISupplierPerformanceSummary } from 'app/shared/model/report';
import { formatCurrency, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';

Chart.register(...registerables);

interface FilterOption {
  label: string;
  value: string;
}

@Component({
  selector: 'jhi-supplier-performance',
  imports: [FormsModule, ButtonModule, TableModule, SelectModule, InputTextModule, TooltipModule, ToolbarModule, Drawer],
  templateUrl: './supplier-performance.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './supplier-performance.component.scss',
})
export default class SupplierPerformanceComponent implements OnInit, OnDestroy {
  @ViewChild('montantsChartCanvas') montantsChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('delaisChartCanvas') delaisChartCanvas?: ElementRef<HTMLCanvasElement>;

  suppliers = signal<ISupplierPerformance[]>([]);
  summary = signal<ISupplierPerformanceSummary | null>(null);
  evolution = signal<ISupplierEvolution | null>(null);
  isLoading = signal<boolean>(false);
  isEvolutionLoading = signal<boolean>(false);
  selectedFilter = signal<string>('all');
  searchText = signal<string>('');
  helpDrawerVisible = signal<boolean>(false);
  activeView = signal<'table' | 'evolution'>('table');

  filterOptions: FilterOption[] = [
    { label: 'Tous les fournisseurs', value: 'all' },
    { label: 'Top 10 par volume', value: 'top' },
    { label: 'Performance excellente (≥ 70)', value: 'good' },
    { label: 'Performance moyenne (50-70)', value: 'average' },
    { label: 'Performance faible (< 50)', value: 'poor' },
    { label: 'Problèmes de livraison', value: 'delivery-issues' },
  ];

  formatCurrency = formatCurrency;
  formatNumber = formatNumber;
  formatDecimal = formatDecimal;

  private supplierPerformanceService = inject(SupplierPerformanceReportService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private montantsChart?: Chart;
  private delaisChart?: Chart;

  ngOnInit(): void {
    this.loadData();
    this.loadSummary();
  }

  ngOnDestroy(): void {
    this.montantsChart?.destroy();
    this.delaisChart?.destroy();
  }

  setView(view: 'table' | 'evolution'): void {
    this.activeView.set(view);
    if (view === 'evolution' && !this.evolution()) {
      this.loadEvolution();
    }
  }

  loadEvolution(): void {
    this.isEvolutionLoading.set(true);
    this.supplierPerformanceService.getEvolution().subscribe({
      next: (res: HttpResponse<ISupplierEvolution>) => {
        const evo = res.body;
        this.evolution.set(evo);
        this.isEvolutionLoading.set(false);
        if (evo) setTimeout(() => this.createCharts(evo), 50);
      },
      error: () => this.isEvolutionLoading.set(false),
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    const filter = this.selectedFilter();

    switch (filter) {
      case 'top':
        this.loadTopSuppliers();
        break;
      case 'good':
        this.loadSuppliersByScore(70);
        break;
      case 'average':
        this.loadSuppliersByScore(50);
        break;
      case 'poor':
        this.loadSuppliersByScore(0);
        break;
      case 'delivery-issues':
        this.loadSuppliersWithDeliveryIssues();
        break;
      default:
        this.loadAllSuppliers();
    }
  }

  loadAllSuppliers(): void {
    this.supplierPerformanceService.getAllSupplierPerformance().subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadTopSuppliers(): void {
    this.supplierPerformanceService.getTopSuppliersByVolume(10).subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSuppliersByScore(minScore: number): void {
    this.supplierPerformanceService.getSuppliersByPerformanceScore(minScore).subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        let suppliers = res.body ?? [];

        if (minScore === 50) {
          suppliers = suppliers.filter(s => (s.performanceScore ?? 0) >= 50 && (s.performanceScore ?? 0) < 70);
        } else if (minScore === 0) {
          suppliers = suppliers.filter(s => (s.performanceScore ?? 0) < 50);
        }

        this.suppliers.set(suppliers);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSuppliersWithDeliveryIssues(): void {
    this.supplierPerformanceService.getSuppliersWithDeliveryIssues().subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSummary(): void {
    this.supplierPerformanceService.getSupplierPerformanceSummary().subscribe({
      next: (res: HttpResponse<ISupplierPerformanceSummary>) => {
        this.summary.set(res.body ?? null);
      },
    });
  }

  onFilterChange(): void {
    this.loadData();
  }

  exportToPdf(): void {
    this.isLoading.set(true);
    this.supplierPerformanceService.exportSupplierPerformanceToPdf().subscribe(resp => {
      if (this.tauriPrinter.isRunningInTauri()) {
        handleBlobForTauri(resp.body, `supplier-performance_${new Date().getTime()}`);
      } else {
        window.open(URL.createObjectURL(resp.body));
      }
    });
  }

  getPerformanceScoreLabel(score: number | undefined): string {
    if (!score) return 'N/A';
    if (score >= 70) return 'Excellent';
    if (score >= 50) return 'Moyen';
    return 'Faible';
  }

  getFilteredSuppliers(): ISupplierPerformance[] {
    const search = this.searchText().toLowerCase();
    if (!search) return this.suppliers();
    return this.suppliers().filter(
      s =>
        s.fournisseurName?.toLowerCase().includes(search) ||
        s.fournisseurCode?.toLowerCase().includes(search) ||
        s.phone?.includes(search) ||
        s.mobile?.includes(search),
    );
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }

  private createCharts(evo: ISupplierEvolution): void {
    this.createMontantsChart(evo);
    this.createDelaisChart(evo);
  }

  private createMontantsChart(evo: ISupplierEvolution): void {
    this.montantsChart?.destroy();
    const canvas = this.montantsChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: evo.labels ?? [],
        datasets: [
          {
            label: 'N (période actuelle)',
            data: evo.montantsN ?? [],
            backgroundColor: 'rgba(54, 162, 235, 0.7)',
            borderColor: 'rgb(54, 162, 235)',
            borderWidth: 1,
          },
          {
            label: 'N-1 (période précédente)',
            data: evo.montantsN1 ?? [],
            backgroundColor: 'rgba(201, 203, 207, 0.7)',
            borderColor: 'rgb(150, 150, 150)',
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16 } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)} FCFA`,
            },
          },
        },
        scales: {
          y: { title: { display: true, text: 'Montant (FCFA)' } },
          x: { ticks: { maxRotation: 45 } },
        },
      },
    };

    this.montantsChart = new Chart(ctx, config);
  }

  private createDelaisChart(evo: ISupplierEvolution): void {
    this.delaisChart?.destroy();
    const canvas = this.delaisChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: evo.labels ?? [],
        datasets: [
          {
            label: 'N (période actuelle)',
            data: evo.delaisN ?? [],
            borderColor: 'rgb(255, 99, 132)',
            backgroundColor: 'rgba(255, 99, 132, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 4,
          },
          {
            label: 'N-1 (période précédente)',
            data: evo.delaisN1 ?? [],
            borderColor: 'rgb(201, 203, 207)',
            backgroundColor: 'rgba(201, 203, 207, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 4,
            borderDash: [5, 5],
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16 } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${ctx.parsed.y} jours`,
            },
          },
        },
        scales: {
          y: { title: { display: true, text: 'Délai moyen (jours)' }, beginAtZero: true },
          x: { ticks: { maxRotation: 45 } },
        },
      },
    };

    this.delaisChart = new Chart(ctx, config);
  }
}
