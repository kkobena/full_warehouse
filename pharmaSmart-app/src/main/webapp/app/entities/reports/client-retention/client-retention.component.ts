import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {forkJoin} from 'rxjs';
import {Chart, registerables} from 'chart.js';

import {ClientRetentionReportService} from '../services/client-retention-report.service';
import {IClientRetentionKpi, IClientRetentionRow} from '../../../shared/model/report';
import {ChartColorsUtilsService} from '../../../shared/util/chart-colors-utils.service';
import {formatCurrency, formatNumber} from 'app/shared/utils/format-utils';
import {DataTableComponent, SortableHeaderDirective} from '../../../shared/ui';

Chart.register(...registerables);

@Component({
  selector: 'app-client-retention',
  imports: [
    CommonModule,
    DataTableComponent,
    SortableHeaderDirective
  ],
  templateUrl: './client-retention.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./client-retention.component.scss'],
})
export default class ClientRetentionComponent implements OnInit, OnDestroy {
  @ViewChild('doughnutCanvas') doughnutCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly kpi = signal<IClientRetentionKpi | null>(null);
  protected readonly clients = signal<IClientRetentionRow[]>([]);
  protected readonly isLoading = signal(false);

  protected readonly tauxActifs = computed(() => this.pct(this.kpi()?.clientsActifs, this.kpi()?.totalClients));
  protected readonly tauxRisque = computed(() => this.pct(this.kpi()?.clientsARisque, this.kpi()?.totalClients));
  protected readonly tauxPerdus = computed(() => this.pct(this.kpi()?.clientsPerdus, this.kpi()?.totalClients));

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;

  private doughnutChart?: Chart;
  private readonly svc = inject(ClientRetentionReportService);
  private readonly colors = inject(ChartColorsUtilsService);

  ngOnInit(): void {
    this.isLoading.set(true);
    forkJoin({
      kpi: this.svc.getKpi(),
      list: this.svc.getClientList(200),
    }).subscribe({
      next: ({kpi, list}) => {
        this.kpi.set(kpi.body);
        this.clients.set(list.body ?? []);
        this.isLoading.set(false);
        if (kpi.body?.totalClients) {
          setTimeout(() => this.buildDoughnut(kpi.body!), 50);
        }
      },
      error: () => this.isLoading.set(false),
    });
  }

  ngOnDestroy(): void {
    this.doughnutChart?.destroy();
  }

  protected segmentBadge(row: IClientRetentionRow): string {
    if ((row.joursAbsence ?? 0) <= 30) {
      return 'bg-success-subtle text-success';
    }
    if ((row.joursAbsence ?? 0) <= 90) {
      return 'bg-warning-subtle text-warning';
    }
    return 'bg-danger-subtle text-danger';
  }

  protected segmentLabel(row: IClientRetentionRow): string {
    if ((row.joursAbsence ?? 0) <= 30) {
      return 'Actif';
    }
    if ((row.joursAbsence ?? 0) <= 90) {
      return 'À risque';
    }
    return 'Perdu';
  }

  private buildDoughnut(kpi: IClientRetentionKpi): void {
    this.doughnutChart?.destroy();
    this.doughnutChart = undefined;

    const canvas = this.doughnutCanvas?.nativeElement;
    if (!canvas) {
      return;
    }
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    const palette = this.colors.colors();
    const hoverPalette = this.colors.hoverColors();

    this.doughnutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Actifs (≤ 30j)', 'À risque (31–90j)', 'Perdus (> 90j)'],
        datasets: [{
          data: [kpi.clientsActifs ?? 0, kpi.clientsARisque ?? 0, kpi.clientsPerdus ?? 0],
          backgroundColor: palette,
          hoverBackgroundColor: hoverPalette,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {color: this.colors.textColor(), usePointStyle: true},
          },
        },
      },
    });
  }

  private pct(part: number | undefined, total: number | undefined): number {
    if (!total || !part) {
      return 0;
    }
    return Math.round((part / total) * 100);
  }
}
