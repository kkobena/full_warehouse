import { Component, computed, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';

import { DashboardCAService } from '../services/dashboard-ca.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IGenericsSubstitution } from '../../../shared/model/report';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ChartColorsUtilsService } from '../../../shared/util/chart-colors-utils.service';
import { formatCurrency, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';

Chart.register(...registerables);

@Component({
  selector: 'app-generics-substitution',
  imports: [CommonModule, DateRangeFilterComponent],
  templateUrl: './generics-substitution.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./generics-substitution.component.scss'],
})
export default class GenericsSubstitutionComponent implements OnInit, OnDestroy {
  @ViewChild('doughnutCanvas') doughnutCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly stats     = signal<IGenericsSubstitution | null>(null);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate   = signal<Date | null>(new Date());

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatDecimal  = formatDecimal;
  protected readonly formatNumber   = formatNumber;

  protected readonly tauxGeneriques = computed(() => {
    const s = this.stats();
    if (!s?.totalProduits) return 0;
    return Math.round(((s.produitsGeneriques ?? 0) / s.totalProduits) * 100);
  });

  protected readonly tauxSubstituables = computed(() => {
    const s = this.stats();
    if (!s?.totalProduits) return 0;
    return Math.round(((s.princepsAvecGenerique ?? 0) / s.totalProduits) * 100);
  });

  protected readonly tauxCaGeneriques = computed(() => {
    const s = this.stats();
    if (!s?.caTotal) return 0;
    return Math.round(((s.caGeneriques ?? 0) / s.caTotal) * 100);
  });

  private doughnutChart?: Chart;
  private readonly svc    = inject(DashboardCAService);
  private readonly colors = inject(ChartColorsUtilsService);

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.doughnutChart?.destroy();
  }

  protected load(): void {
    const startDate = DATE_FORMAT_ISO_DATE(this.fromDate()) ?? undefined;
    const endDate   = DATE_FORMAT_ISO_DATE(this.toDate())   ?? undefined;
    if (!startDate || !endDate) return;

    this.isLoading.set(true);
    this.svc.getGenericsSubstitution(startDate, endDate).subscribe({
      next: res => {
        this.stats.set(res.body);
        this.isLoading.set(false);
        if (res.body?.caTotal) setTimeout(() => this.buildDoughnut(res.body!), 50);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private buildDoughnut(data: IGenericsSubstitution): void {
    this.doughnutChart?.destroy();
    this.doughnutChart = undefined;

    const canvas = this.doughnutCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const caGeneriques = data.caGeneriques ?? 0;
    const caPrinceps   = data.caPrincepsAvecGenerique ?? 0;
    const caOther      = Math.max(0, (data.caTotal ?? 0) - caGeneriques - caPrinceps);

    const palette      = this.colors.colors();
    const hoverPalette = this.colors.hoverColors();

    this.doughnutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Génériques', 'Princeps substituables', 'Autres'],
        datasets: [{
          data: [caGeneriques, caPrinceps, caOther],
          backgroundColor:      palette,
          hoverBackgroundColor: hoverPalette,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: this.colors.textColor(), usePointStyle: true },
          },
        },
      },
    });
  }
}
