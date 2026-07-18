import { Component, computed, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpResponse } from '@angular/common/http';

import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { IPnlEvolution, IPnlFamille, IPnlSegment } from 'app/shared/model/report';
import { PnlAnalytiqueService } from '../services/pnl-analytique.service';
import { formatCurrency, formatPercent, formatNumber } from 'app/shared/utils/format-utils';

import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

type PnlView = 'snapshot' | 'evolution';
type SnapshotTab = 'segment' | 'famille';
type FamilleSortCol = 'ca' | 'margeBrute' | 'tauxMarge';

const CHART_COLORS = [
  'rgb(54, 162, 235)',
  'rgb(255, 99, 132)',
  'rgb(75, 192, 192)',
  'rgb(255, 205, 86)',
  'rgb(153, 102, 255)',
];

@Component({
  selector: 'app-pnl-analytique',
  templateUrl: './pnl-analytique.component.html',
  styleUrl: './pnl-analytique.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, TableModule, SelectModule, FormsModule],
})
export default class PnlAnalytiqueComponent implements OnInit, OnDestroy {
  @ViewChild('familleEvolutionChartCanvas') familleEvolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('segmentEvolutionChartCanvas') segmentEvolutionChartCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly view = signal<PnlView>('snapshot');
  protected readonly snapshotTab = signal<SnapshotTab>('segment');
  protected readonly selectedYear = signal<number>(new Date().getFullYear());

  protected readonly segmentData = signal<IPnlSegment[]>([]);
  protected readonly familleData = signal<IPnlFamille[]>([]);
  protected readonly familleEvolutionData = signal<IPnlEvolution | null>(null);
  protected readonly segmentEvolutionData = signal<IPnlEvolution | null>(null);
  protected readonly isLoading = signal<boolean>(false);
  protected readonly familleEvolutionLoaded = signal<boolean>(false);
  protected readonly segmentEvolutionLoaded = signal<boolean>(false);
  protected readonly familleLoaded = signal<boolean>(false);

  protected readonly familleSortCol = signal<FamilleSortCol>('ca');
  protected readonly familleSortAsc = signal<boolean>(false);

  protected readonly sortedFamille = computed(() => {
    const data = [...this.familleData()];
    const col = this.familleSortCol();
    const asc = this.familleSortAsc();
    return data.sort((a, b) => {
      const va = (a[col] ?? 0) as number;
      const vb = (b[col] ?? 0) as number;
      return asc ? va - vb : vb - va;
    });
  });

  protected readonly totalSegment = computed(() => {
    const segs = this.segmentData();
    const totalCA = segs.reduce((s, r) => s + (r.ca ?? 0), 0);
    const totalMarge = segs.reduce((s, r) => s + (r.margeBrute ?? 0), 0);
    return {
      ca: totalCA,
      coutAchat: segs.reduce((s, r) => s + (r.coutAchat ?? 0), 0),
      margeBrute: totalMarge,
      tauxMarge: totalCA > 0 ? (totalMarge / totalCA) * 100 : 0,
      nbTransactions: segs.reduce((s, r) => s + (r.nbTransactions ?? 0), 0),
    };
  });

  protected readonly totalFamilleCA = computed(() =>
    this.familleData().reduce((s, f) => s + (f.ca ?? 0), 0)
  );

  protected readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    const result: number[] = [];
    for (let y = current; y >= 2025; y--) result.push(y);
    return result;
  })();

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatPercent = formatPercent;
  protected readonly formatNumber = formatNumber;

  protected partCaPct(ca?: number, total?: number): number {
    const t = total ?? this.totalSegment().ca;
    return t > 0 ? ((ca ?? 0) / t) * 100 : 0;
  }

  private familleEvolutionChart?: Chart;
  private segmentEvolutionChart?: Chart;
  private readonly pnlService = inject(PnlAnalytiqueService);

  ngOnInit(): void {
    this.loadSegment();
  }

  ngOnDestroy(): void {
    this.familleEvolutionChart?.destroy();
    this.segmentEvolutionChart?.destroy();
  }

  protected setView(v: PnlView): void {
    this.view.set(v);
    if (v === 'evolution') {
      if (!this.familleEvolutionLoaded()) {
        this.loadEvolutionByFamille();
      }
      if (!this.segmentEvolutionLoaded()) {
        this.loadEvolutionBySegment();
      }
    }
  }

  protected setSnapshotTab(tab: SnapshotTab): void {
    this.snapshotTab.set(tab);
    if (tab === 'famille' && !this.familleLoaded()) {
      this.loadFamille();
    }
    if (this.view() === 'evolution') {
      if (tab === 'famille') {
        const data = this.familleEvolutionData();
        if (data) setTimeout(() => this.createFamilleEvolutionChart(data), 50);
      } else {
        const data = this.segmentEvolutionData();
        if (data) setTimeout(() => this.createSegmentEvolutionChart(data), 50);
      }
    }
  }

  protected onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.familleLoaded.set(false);
    if (this.snapshotTab() === 'segment') {
      this.loadSegment();
    } else {
      this.loadFamille();
    }
  }

  protected sortFamille(col: FamilleSortCol): void {
    if (this.familleSortCol() === col) {
      this.familleSortAsc.update(v => !v);
    } else {
      this.familleSortCol.set(col);
      this.familleSortAsc.set(false);
    }
  }

  protected sortIcon(col: FamilleSortCol): string {
    if (this.familleSortCol() !== col) return 'pi pi-sort-alt';
    return this.familleSortAsc() ? 'pi pi-sort-amount-up' : 'pi pi-sort-amount-down';
  }

  protected segmentLabel(segment?: string): string {
    switch (segment) {
      case 'COMPTANT':  return 'Comptant';
      case 'ASSURANCE': return 'Remboursable (Assurance)';
      case 'CARNET':    return 'Carnet';
      default:          return segment ?? 'Autre';
    }
  }

  protected segmentBadgeClass(segment?: string): string {
    if (segment === 'COMPTANT') return 'pharma-badge pharma-badge-success';
    if (segment === 'ASSURANCE') return 'pharma-badge pharma-badge-info';
    if (segment === 'CARNET') return 'pharma-badge pharma-badge-primary';
    return 'badge bg-secondary';
  }

  protected margeBadgeClass(taux?: number): string {
    const t = taux ?? 0;
    if (t >= 30) return 'badge bg-success';
    if (t >= 20) return 'badge bg-info text-dark';
    if (t >= 10) return 'badge bg-warning text-dark';
    return 'badge bg-danger';
  }

  private loadSegment(): void {
    this.isLoading.set(true);
    this.pnlService.getSnapshotBySegment(this.selectedYear()).subscribe({
      next: (res: HttpResponse<IPnlSegment[]>) => {
        this.segmentData.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private loadFamille(): void {
    this.isLoading.set(true);
    this.pnlService.getSnapshotByFamille(this.selectedYear()).subscribe({
      next: (res: HttpResponse<IPnlFamille[]>) => {
        this.familleData.set(res.body ?? []);
        this.familleLoaded.set(true);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private loadEvolutionByFamille(): void {
    this.pnlService.getEvolutionByFamille().subscribe({
      next: (res: HttpResponse<IPnlEvolution>) => {
        const data = res.body;
        this.familleEvolutionData.set(data);
        this.familleEvolutionLoaded.set(true);
        if (data && this.snapshotTab() === 'famille') {
          setTimeout(() => this.createFamilleEvolutionChart(data), 50);
        }
      },
      error: () => {},
    });
  }

  private loadEvolutionBySegment(): void {
    this.pnlService.getEvolutionBySegment().subscribe({
      next: (res: HttpResponse<IPnlEvolution>) => {
        const data = res.body;
        this.segmentEvolutionData.set(data);
        this.segmentEvolutionLoaded.set(true);
        if (data && this.snapshotTab() === 'segment') {
          setTimeout(() => this.createSegmentEvolutionChart(data), 50);
        }
      },
      error: () => {},
    });
  }

  private createFamilleEvolutionChart(evolution: IPnlEvolution): void {
    this.familleEvolutionChart?.destroy();
    this.familleEvolutionChart = undefined;

    const canvas = this.familleEvolutionChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    this.familleEvolutionChart = new Chart(ctx, this.buildLineConfig(
      evolution,
      serie => serie.famille ?? '',
    ));
  }

  private createSegmentEvolutionChart(evolution: IPnlEvolution): void {
    this.segmentEvolutionChart?.destroy();
    this.segmentEvolutionChart = undefined;

    const canvas = this.segmentEvolutionChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    this.segmentEvolutionChart = new Chart(ctx, this.buildLineConfig(
      evolution,
      serie => serie.segment ?? serie.famille ?? '',
    ));
  }

  private buildLineConfig(evolution: IPnlEvolution, getLabel: (s: any) => string): ChartConfiguration<'line'> {
    return {
      type: 'line',
      data: {
        labels: evolution.labels ?? [],
        datasets: (evolution.series ?? []).map((serie, i) => ({
          label: getLabel(serie),
          data: (serie.tauxMargeValues ?? []).map(v => Number(v)),
          borderColor: CHART_COLORS[i % CHART_COLORS.length],
          backgroundColor: CHART_COLORS[i % CHART_COLORS.length].replace('rgb', 'rgba').replace(')', ', 0.08)'),
          fill: false,
          tension: 0.3,
          pointRadius: 4,
          pointHoverRadius: 6,
        })),
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16 } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${ctx.parsed.y.toFixed(2)} %`,
            },
          },
        },
        scales: {
          y: {
            title: { display: true, text: 'Taux de marge (%)' },
            ticks: { callback: v => v + ' %' },
          },
          x: { ticks: { maxRotation: 45 } },
        },
      },
    };
  }
}
