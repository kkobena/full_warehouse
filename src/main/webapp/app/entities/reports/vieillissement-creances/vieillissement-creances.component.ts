import { Component, computed, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

import { TableModule } from 'primeng/table';
import { IDsoOrganisme, IEncoursMensuel, IVieillissementGlobal } from 'app/shared/model/report';
import { VieillissementCreancesService } from '../services/vieillissement-creances.service';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';

import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

type TranchePill = 'all' | '0-30' | '31-60' | '61-90' | '90+';

@Component({
  selector: 'app-vieillissement-creances',
  templateUrl: './vieillissement-creances.component.html',
  styleUrl: './vieillissement-creances.component.scss',
  imports: [CommonModule, TableModule],
})
export default class VieillissementCreancesComponent implements OnInit, OnDestroy {
  @ViewChild('encoursMensuelChartCanvas') encoursMensuelChartCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly global = signal<IVieillissementGlobal | null>(null);
  protected readonly organismes = signal<IDsoOrganisme[]>([]);
  protected readonly encoursMensuel = signal<IEncoursMensuel | null>(null);
  protected readonly isLoading = signal<boolean>(false);
  protected readonly trancheFilter = signal<TranchePill>('all');

  protected readonly filteredOrganismes = computed(() => {
    const filter = this.trancheFilter();
    const orgs = this.organismes();
    if (filter === 'all') return orgs;
    return orgs.filter(o => {
      if (filter === '0-30') return (o.tranche0_30 ?? 0) > 0;
      if (filter === '31-60') return (o.tranche31_60 ?? 0) > 0;
      if (filter === '61-90') return (o.tranche61_90 ?? 0) > 0;
      if (filter === '90+') return (o.tranche90Plus ?? 0) > 0;
      return true;
    });
  });

  protected readonly pctTranche = computed(() => {
    const g = this.global();
    const total = g?.totalEncours ?? 0;
    if (total === 0) return { t030: 0, t3160: 0, t6190: 0, t90p: 0 };
    return {
      t030: Math.round(((g?.tranche0_30 ?? 0) / total) * 100),
      t3160: Math.round(((g?.tranche31_60 ?? 0) / total) * 100),
      t6190: Math.round(((g?.tranche61_90 ?? 0) / total) * 100),
      t90p: Math.round(((g?.tranche90Plus ?? 0) / total) * 100),
    };
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;

  private encoursMensuelChart?: Chart;
  private readonly svc = inject(VieillissementCreancesService);

  ngOnInit(): void {
    this.isLoading.set(true);
    forkJoin({
      global: this.svc.getAgingGlobal(),
      organismes: this.svc.getDsoByOrganisme(),
      encoursMensuel: this.svc.getEncoursMensuelEvolution(),
    }).subscribe({
      next: ({ global, organismes, encoursMensuel }) => {
        this.global.set(global.body);
        this.organismes.set(organismes.body ?? []);
        const data = encoursMensuel.body;
        this.encoursMensuel.set(data);
        this.isLoading.set(false);
        if (data) setTimeout(() => this.createEncoursMensuelChart(data), 50);
      },
      error: () => this.isLoading.set(false),
    });
  }

  ngOnDestroy(): void {
    this.encoursMensuelChart?.destroy();
  }

  protected setTranche(t: TranchePill): void {
    this.trancheFilter.set(t);
  }

  protected fiabiliteBadgeClass(fiabilite?: string): string {
    if (fiabilite === 'RISQUE') return 'badge bg-danger';
    if (fiabilite === 'SURVEILLER') return 'badge bg-warning text-dark';
    return 'badge bg-success';
  }

  protected fiabiliteLabel(fiabilite?: string): string {
    if (fiabilite === 'RISQUE') return 'Payeur difficile';
    if (fiabilite === 'SURVEILLER') return 'À surveiller';
    return 'Payeur régulier';
  }

  private createEncoursMensuelChart(data: IEncoursMensuel): void {
    this.encoursMensuelChart?.destroy();
    this.encoursMensuelChart = undefined;

    const canvas = this.encoursMensuelChartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const facture = data.montantFacture ?? [];
    const encours = data.encoursRestant ?? [];
    const regle = facture.map((f, i) => Math.max(f - (encours[i] ?? 0), 0));

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: data.labels ?? [],
        datasets: [
          {
            label: 'Réglé',
            data: regle,
            backgroundColor: 'rgba(67, 172, 106, 0.85)',
            borderColor: 'rgb(67, 172, 106)',
            borderWidth: 1,
            stack: 'total',
          },
          {
            label: 'Encours restant',
            data: encours,
            backgroundColor: 'rgba(233, 144, 2, 0.85)',
            borderColor: 'rgb(233, 144, 2)',
            borderWidth: 1,
            stack: 'total',
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
              label: ctx => `${ctx.dataset.label} : ${formatCurrency(ctx.parsed.y)} FCFA`,
              footer: items => {
                const i = items[0].dataIndex;
                return `Total facturé : ${formatCurrency(facture[i] ?? 0)} FCFA`;
              },
            },
          },
        },
        scales: {
          x: { stacked: true, ticks: { maxRotation: 45 } },
          y: {
            stacked: true,
            title: { display: true, text: 'Montant (FCFA)' },
          },
        },
      },
    };

    this.encoursMensuelChart = new Chart(ctx, config);
  }
}
