import { Component, effect, ElementRef, inject, input, OnDestroy, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppTableLazyLoadEvent, ButtonComponent, DataTableComponent } from 'app/shared/ui';
import { NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { ProduitStatService } from 'app/entities/produit/stat/produit-stat.service';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import {
  HistoriqueProduitAchats,
  HistoriqueProduitAchatsSummary,
  ProduitAuditingParam,
} from 'app/shared/model/produit-record.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

interface PeriodShortcut {
  label: string;
  key: string;
  days?: number;
}

@Component({
  selector: 'app-produit-achats-tab',
  templateUrl: './produit-achats-tab.component.html',
  styleUrls: ['./produit-achats-tab.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, DataTableComponent, ButtonComponent, NgbTooltip, PharmaDatePickerComponent, TranslatePipe],
})
export class ProduitAchatsTabComponent implements OnDestroy {
  readonly produitId = input.required<number>();

  protected data = signal<HistoriqueProduitAchats[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected loadingChart = signal(false);
  protected summary = signal<HistoriqueProduitAchatsSummary | null>(null);
  protected activePeriod = signal<string>('');
  protected showChart = signal(false);

  protected fromDate: NgbDateStruct = this.toStruct(new Date(new Date().getFullYear(), new Date().getMonth() - 2, 1));
  protected toDate: NgbDateStruct = this.toStruct(new Date());
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected readonly PERIOD_SHORTCUTS: PeriodShortcut[] = [
    { label: 'Hier', key: 'yesterday', days: 1 },
    { label: '7 j', key: '7d', days: 7 },
    { label: 'Ce mois', key: 'month' },
    { label: '3 mois', key: '3m', days: 90 },
    { label: '1 an', key: '1y', days: 365 },
  ];

  private fromDateStr = '';
  private toDateStr = '';
  private chart?: Chart;
  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('achatsChart');
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

  protected applyShortcut(shortcut: PeriodShortcut): void {
    const today = new Date();
    this.toDate = this.toStruct(today);
    if (shortcut.days !== undefined) {
      const from = new Date(today);
      from.setDate(from.getDate() - shortcut.days);
      this.fromDate = this.toStruct(from);
    } else {
      this.fromDate = this.toStruct(new Date(today.getFullYear(), today.getMonth(), 1));
    }
    this.fromDateStr = '';
    this.toDateStr = '';
    this.activePeriod.set(shortcut.key);
    this.load();
  }

  protected onFromDateChange(date: NgbDateStruct | null): void {
    this.fromDateStr = NGB_DATE_TO_ISO(date) ?? '';
    this.activePeriod.set('');
  }

  protected onToDateChange(date: NgbDateStruct | null): void {
    this.toDateStr = NGB_DATE_TO_ISO(date) ?? '';
    this.activePeriod.set('');
    this.load();
  }

  protected load(page = 0): void {
    this.loading.set(true);
    const param = this.buildParam(page);
    this.statService.getProduitHistoriqueAchat(param).subscribe({
      next: res => {
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.data.set(res.body ?? []);
        this.loading.set(false);
        if (this.showChart()) this.loadChartData();
      },
      error: () => this.loading.set(false),
    });
    this.statService.getHistoriqueAchatSummary(param).subscribe({
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
    this.statService.exportHistoriqueAchatToPdf(this.buildParam()).subscribe({
      next: blob => this.downloadService.downloadPdf(blob, 'historique-achats'),
    });
  }

  private loadChartData(): void {
    this.loadingChart.set(true);
    this.statService.getProduitHistoriqueAchat({ ...this.buildParam(0), size: 3000 }).subscribe({
      next: res => {
        this.loadingChart.set(false);
        setTimeout(() => this.buildChart(res.body ?? []), 0);
      },
      error: () => this.loadingChart.set(false),
    });
  }

  private buildChart(rows: HistoriqueProduitAchats[]): void {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas || !rows.length) return;
    this.chart?.destroy();

    // Aggregate by date
    const map = new Map<string, { qty: number; amount: number }>();
    for (const row of rows) {
      const key = String(row.mvtDate).substring(0, 10);
      const prev = map.get(key) ?? { qty: 0, amount: 0 };
      map.set(key, { qty: prev.qty + (row.quantite ?? 0), amount: prev.amount + (row.montantAchat ?? 0) });
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
            label: 'Quantité achetée',
            data: quantities,
            backgroundColor: 'rgba(20, 184, 166, 0.7)',
            borderColor: 'rgba(20, 184, 166, 1)',
            borderWidth: 1,
            yAxisID: 'yQty',
            order: 1,
          },
          {
            type: 'line' as any,
            label: 'Montant achat',
            data: amounts,
            borderColor: 'rgba(139, 92, 246, 1)',
            backgroundColor: 'rgba(139, 92, 246, 0.08)',
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
      fromDate: this.fromDateStr || NGB_DATE_TO_ISO(this.fromDate),
      toDate: this.toDateStr || NGB_DATE_TO_ISO(this.toDate),
      page,
      size: this.itemsPerPage,
    };
  }

  private toStruct(date: Date): NgbDateStruct {
    return { year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate() };
  }
}

