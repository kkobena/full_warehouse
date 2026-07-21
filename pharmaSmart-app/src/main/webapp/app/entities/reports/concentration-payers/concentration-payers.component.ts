import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { IConcentrationEvolution, IConcentrationOrganisme, IConcentrationSummary } from 'app/shared/model/report';
import { ConcentrationPayersService } from '../services/concentration-payers.service';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';

import { Chart, ChartConfiguration, registerables } from 'chart.js';
import {
  DataTableComponent,
  SelectComponent
} from '../../../shared/ui';

Chart.register(...registerables);

type Periode = 'quarter' | 'year';

const TOP_N_OPTIONS = [5, 10, 15, 20, 30].map(n => ({ label: `Top ${n}`, value: n }));

const CHART_COLORS = [
  'rgba(54, 162, 235, 0.85)',
  'rgba(255, 99, 132, 0.85)',
  'rgba(75, 192, 192, 0.85)',
  'rgba(255, 205, 86, 0.85)',
  'rgba(153, 102, 255, 0.85)',
  'rgba(150, 150, 150, 0.75)',
];
const CHART_BORDERS = [
  'rgb(54, 162, 235)',
  'rgb(255, 99, 132)',
  'rgb(75, 192, 192)',
  'rgb(255, 205, 86)',
  'rgb(153, 102, 255)',
  'rgb(120, 120, 120)',
];

@Component({
  selector: 'app-concentration-payers',
  templateUrl: './concentration-payers.component.html',
  styleUrl: './concentration-payers.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    SelectComponent
  ],
})
export default class ConcentrationPayersComponent implements OnInit, OnDestroy {
  @ViewChild('evolutionChartCanvas') evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly topNOptions = TOP_N_OPTIONS;

  protected readonly periode = signal<Periode>('quarter');
  protected readonly summaryTopN = signal<number>(10);
  protected selectedSummaryTopN = 10;
  protected readonly stressTopN = signal<number>(10);
  protected selectedStressTopN = 10;
  protected readonly evolutionTopN = signal<number>(10);
  protected selectedEvolutionTopN = 10;
  protected readonly summary = signal<IConcentrationSummary | null>(null);
  protected readonly stressOrganismes = signal<IConcentrationOrganisme[] | null>(null);
  protected readonly evolution = signal<IConcentrationEvolution | null>(null);
  protected readonly isLoading = signal<boolean>(false);
  protected readonly isStressLoading = signal<boolean>(false);
  protected readonly evolutionLoaded = signal<boolean>(false);

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;

  private evolutionChart?: Chart;
  private readonly svc = inject(ConcentrationPayersService);

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.evolutionChart?.destroy();
  }

  setPeriode(p: Periode): void {
    this.periode.set(p);
    this.loadSummary();
    this.loadStress();
  }

  protected onSummaryTopNChange(n: number): void {
    this.summaryTopN.set(n);
    this.loadSummary();
  }

  protected onStressTopNChange(n: number): void {
    this.stressTopN.set(n);
    this.loadStress();
  }

  protected onEvolutionTopNChange(n: number): void {
    this.evolutionTopN.set(n);
    this.loadEvolution();
  }

  protected hhiBadgeClass(riskLevel?: string): string {
    if (riskLevel === 'ELEVE') return 'badge bg-danger fs-6';
    if (riskLevel === 'MODERE') return 'badge bg-warning text-dark fs-6';
    return 'badge bg-success fs-6';
  }

  protected hhiLabel(riskLevel?: string): string {
    if (riskLevel === 'ELEVE') return 'Concentration élevée';
    if (riskLevel === 'MODERE') return 'Concentration modérée';
    return 'Concentration faible';
  }

  protected barWidthPct(partPct?: number): string {
    return Math.min(partPct ?? 0, 100).toFixed(1) + '%';
  }

  private load(): void {
    this.isLoading.set(true);
    this.isStressLoading.set(true);
    forkJoin({
      summary: this.svc.getSummary(this.periode(), this.summaryTopN()),
      stress: this.svc.getOrganismes(this.periode(), this.stressTopN()),
      evolution: this.svc.getEvolution(this.evolutionTopN()),
    }).subscribe({
      next: ({ summary, stress, evolution }) => {
        this.summary.set(summary.body);
        this.stressOrganismes.set(stress.body);
        const evo = evolution.body;
        this.evolution.set(evo);
        this.evolutionLoaded.set(true);
        this.isLoading.set(false);
        this.isStressLoading.set(false);
        if (evo) setTimeout(() => this.createEvolutionChart(evo), 50);
      },
      error: () => {
        this.isLoading.set(false);
        this.isStressLoading.set(false);
      },
    });
  }

  private loadSummary(): void {
    this.isLoading.set(true);
    this.svc.getSummary(this.periode(), this.summaryTopN()).subscribe({
      next: res => {
        this.summary.set(res.body);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private loadStress(): void {
    this.isStressLoading.set(true);
    this.svc.getOrganismes(this.periode(), this.stressTopN()).subscribe({
      next: res => {
        this.stressOrganismes.set(res.body);
        this.isStressLoading.set(false);
      },
      error: () => this.isStressLoading.set(false),
    });
  }

  private loadEvolution(): void {
    this.svc.getEvolution(this.evolutionTopN()).subscribe({
      next: res => {
        const evo = res.body;
        this.evolution.set(evo);
        if (evo) setTimeout(() => this.createEvolutionChart(evo), 50);
      },
      error: () => {},
    });
  }

  private createEvolutionChart(evo: IConcentrationEvolution): void {
    this.evolutionChart?.destroy();
    this.evolutionChart = undefined;

    const canvas = this.evolutionChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: evo.labels ?? [],
        datasets: (evo.series ?? []).map((serie, i) => ({
          label: serie.organisme ?? '',
          data: serie.caValues ?? [],
          backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
          borderColor: CHART_BORDERS[i % CHART_BORDERS.length],
          borderWidth: 1,
          stack: 'total',
        })),
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 14 } },
          tooltip: {
            callbacks: {
              label: ctx => {
                const total = (evo.series ?? []).reduce((s, serie) =>
                  s + ((serie.caValues ?? [])[ctx.dataIndex] ?? 0), 0);
                const pct = total > 0 ? Math.round((ctx.parsed.y / total) * 100) : 0;
                return `${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)} FCFA (${pct}%)`;
              },
            },
          },
        },
        scales: {
          x: { stacked: true, ticks: { maxRotation: 45 } },
          y: { stacked: true, title: { display: true, text: 'CA TP facturé (FCFA)' } },
        },
      },
    };

    this.evolutionChart = new Chart(ctx, config);
  }
}
