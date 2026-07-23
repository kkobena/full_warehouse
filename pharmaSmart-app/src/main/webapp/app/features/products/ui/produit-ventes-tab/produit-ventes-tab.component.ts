import { Component, effect, ElementRef, inject, input, OnDestroy, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppTableLazyLoadEvent, ButtonComponent, DataTableComponent, PillSelectorComponent } from 'app/shared/ui';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { ProduitStatService } from 'app/entities/produit/stat/produit-stat.service';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import {
  HistoriqueProduitVente,
  HistoriqueProduitVenteSummary,
  ProduitAuditingParam,
} from 'app/shared/model/produit-record.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { createPeriodDateFilter } from '../period-date-filter';

Chart.register(...registerables);

@Component({
  selector: 'app-produit-ventes-tab',
  templateUrl: './produit-ventes-tab.component.html',
  styleUrls: ['./produit-ventes-tab.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    ButtonComponent,
    NgbTooltip,
    PharmaDatePickerComponent,
    PillSelectorComponent,
    TranslatePipe,
  ],
})
export class ProduitVentesTabComponent implements OnDestroy {
  readonly produitId = input.required<number>();

  protected data = signal<HistoriqueProduitVente[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected loadingChart = signal(false);
  protected summary = signal<HistoriqueProduitVenteSummary | null>(null);
  protected showChart = signal(false);

  protected readonly periodFilter = createPeriodDateFilter({ defaultKey: 'today', onChange: () => this.load() });
  protected itemsPerPage = ITEMS_PER_PAGE;

  private chart?: Chart;
  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('ventesChart');
  private readonly statService = inject(ProduitStatService);
  private readonly downloadService = inject(BlobDownloadService);

  constructor() {
    effect(() => {
      if (this.produitId()) {
        this.load();
      }
    });
    effect(() => {
      const visible = this.showChart();
      if (!visible) {
        this.chart?.destroy();
        this.chart = undefined;
      } else {
        this.loadChartData();
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  protected load(page = 0): void {
    this.loading.set(true);
    const param = this.buildParam(page);
    this.statService.getProduitHistoriqueVente(param).subscribe({
      next: res => {
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.data.set(res.body ?? []);
        this.loading.set(false);
        if (this.showChart()) this.loadChartData();
      },
      error: () => this.loading.set(false),
    });
    this.statService.getHistoriqueVenteSummary(param).subscribe({
      next: res => this.summary.set(res.body),
    });
  }

  protected onLazyLoad(event: AppTableLazyLoadEvent): void {
    if (event.rows) {
      this.load(event.first / event.rows);
    }
  }

  protected toggleChart(): void {
    this.showChart.update(v => !v);
  }

  protected exportPdf(): void {
    this.statService.exportHistoriqueVenteToPdf(this.buildParam()).subscribe({
      next: blob => this.downloadService.downloadPdf(blob, 'historique-ventes'),
    });
  }

  private loadChartData(): void {
    this.loadingChart.set(true);
    this.statService.getProduitHistoriqueVente({ ...this.buildParam(0), size: 3000 }).subscribe({
      next: res => {
        this.loadingChart.set(false);
        setTimeout(() => this.buildChart(res.body ?? []), 0);
      },
      error: () => this.loadingChart.set(false),
    });
  }

  private buildChart(rows: HistoriqueProduitVente[]): void {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas || !rows.length) return;
    this.chart?.destroy();

    // Aggregate by date
    const map = new Map<string, { qty: number; amount: number }>();
    for (const row of rows) {
      const key = String(row.mvtDate).substring(0, 10);
      const prev = map.get(key) ?? { qty: 0, amount: 0 };
      map.set(key, { qty: prev.qty + (row.quantite ?? 0), amount: prev.amount + (row.montantNet ?? 0) });
    }
    const keys = [...map.keys()].sort();
    const labels = keys.map(k => new Date(k).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }));
    const quantities = keys.map(k => map.get(k)!.qty);
    const amounts = keys.map(k => map.get(k)!.amount);

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Quantité vendue',
            data: quantities,
            backgroundColor: 'rgba(99, 102, 241, 0.7)',
            borderColor: 'rgba(99, 102, 241, 1)',
            borderWidth: 1,
            yAxisID: 'yQty',
            order: 1,
          },
          {
            type: 'line' as any,
            label: 'Montant net',
            data: amounts,
            borderColor: 'rgba(249, 115, 22, 1)',
            backgroundColor: 'rgba(249, 115, 22, 0.08)',
            borderWidth: 2,
            fill: false,
            tension: 0.3,
            yAxisID: 'yAmount',
            pointRadius: keys.length > 60 ? 0 : 3,
            order: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } },
        },
        scales: {
          x: { ticks: { font: { size: 10 }, maxRotation: 45, autoSkip: true, maxTicksLimit: 20 } },
          yQty: {
            type: 'linear',
            position: 'left',
            title: { display: true, text: 'Quantité', font: { size: 10 } },
            ticks: { precision: 0, font: { size: 10 } },
            beginAtZero: true,
          },
          yAmount: {
            type: 'linear',
            position: 'right',
            title: { display: true, text: 'Montant (XOF)', font: { size: 10 } },
            ticks: { font: { size: 10 } },
            beginAtZero: true,
            grid: { drawOnChartArea: false },
          },
        },
      },
    };
    this.chart = new Chart(canvas, config);
  }

  private buildParam(page = 0): ProduitAuditingParam {
    return {
      produitId: this.produitId(),
      ...this.periodFilter.dateParams(),
      page,
      size: this.itemsPerPage,
    };
  }
}
