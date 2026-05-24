import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

import { TableModule } from 'primeng/table';
import { IBfrEvolution, IBfrSnapshot } from 'app/shared/model/report';
import { CashFlowBfrService } from '../services/cash-flow-bfr.service';
import { formatCurrency } from 'app/shared/utils/format-utils';

import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-cash-flow-bfr',
  templateUrl: './cash-flow-bfr.component.html',
  styleUrl: './cash-flow-bfr.component.scss',
  imports: [CommonModule, TableModule],
})
export default class CashFlowBfrComponent implements OnInit, OnDestroy {
  @ViewChild('evolutionChartCanvas') evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly snapshot = signal<IBfrSnapshot | null>(null);
  protected readonly evolution = signal<IBfrEvolution | null>(null);
  protected readonly isLoading = signal<boolean>(false);

  protected readonly formatCurrency = formatCurrency;

  private evolutionChart?: Chart;
  private readonly svc = inject(CashFlowBfrService);

  ngOnInit(): void {
    this.isLoading.set(true);
    forkJoin({
      snapshot: this.svc.getSnapshot(),
      evolution: this.svc.getEvolution(),
    }).subscribe({
      next: ({ snapshot, evolution }) => {
        this.snapshot.set(snapshot.body);
        const evo = evolution.body;
        this.evolution.set(evo);
        this.isLoading.set(false);
        if (evo) setTimeout(() => this.createChart(evo), 50);
      },
      error: () => this.isLoading.set(false),
    });
  }

  ngOnDestroy(): void {
    this.evolutionChart?.destroy();
  }

  protected cccBadgeClass(ccc?: number): string {
    const v = ccc ?? 0;
    if (v <= 0) return 'badge bg-success fs-6';
    if (v <= 40) return 'badge bg-info text-dark fs-6';
    if (v <= 60) return 'badge bg-warning text-dark fs-6';
    return 'badge bg-danger fs-6';
  }


  private createChart(evo: IBfrEvolution): void {
    this.evolutionChart?.destroy();
    this.evolutionChart = undefined;

    const canvas = this.evolutionChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: evo.labels ?? [],
        datasets: [
          {
            label: 'Créances TP facturées',
            data: evo.creancesEmises ?? [],
            borderColor: 'rgb(54, 162, 235)',
            backgroundColor: 'rgba(54, 162, 235, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
          },
          {
            label: 'Achats fournisseurs reçus',
            data: evo.achatsRecus ?? [],
            borderColor: 'rgb(255, 99, 132)',
            backgroundColor: 'rgba(255, 99, 132, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
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

    this.evolutionChart = new Chart(ctx, config);
  }
}
