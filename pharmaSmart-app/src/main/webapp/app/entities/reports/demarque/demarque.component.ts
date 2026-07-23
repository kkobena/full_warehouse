import { Component, computed, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin } from 'rxjs';
import { Chart, registerables } from 'chart.js';

import { DemarqueReportService } from '../services/demarque-report.service';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';
import { IDemarqueByMotif, IDemarqueKpi } from '../../../shared/model/report';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ChartColorsUtilsService } from '../../../shared/util/chart-colors-utils.service';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';
import {
  ButtonComponent,
  DataTableComponent,
  ToolbarComponent
} from '../../../shared/ui';

Chart.register(...registerables);

@Component({
  selector: 'app-demarque',
  imports: [
    CommonModule,
    FormsModule,
    PharmaDatePickerComponent,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent
  ],
  templateUrl: './demarque.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./demarque.component.scss'],
})
export default class DemarqueComponent implements OnInit, OnDestroy {
  @ViewChild('doughnutCanvas') doughnutCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly kpi       = signal<IDemarqueKpi | null>(null);
  protected readonly byMotif   = signal<IDemarqueByMotif[]>([]);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate   = signal<Date | null>(new Date());

  // `computed`, pas un appel direct à `dateToStruct()` dans le template : voir
  // date-range-filter (supprimé) pour l'explication de la boucle silencieuse évitée.
  protected readonly fromStruct = computed(() => this.dateToStruct(this.fromDate()));
  protected readonly toStruct   = computed(() => this.dateToStruct(this.toDate()));

  protected readonly totalValeur = computed(() =>
    this.byMotif().reduce((s, m) => s + (m.valeur ?? 0), 0)
  );

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber   = formatNumber;

  private doughnutChart?: Chart;
  private readonly svc    = inject(DemarqueReportService);
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
    forkJoin({
      kpi:     this.svc.getKpi(startDate, endDate),
      byMotif: this.svc.getByMotif(startDate, endDate),
    }).subscribe({
      next: ({ kpi, byMotif }) => {
        this.kpi.set(kpi.body);
        const data = byMotif.body ?? [];
        this.byMotif.set(data);
        this.isLoading.set(false);
        if (data.length) setTimeout(() => this.buildDoughnut(data), 50);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected sharePercent(valeur: number | undefined): number {
    const total = this.totalValeur();
    if (!total || !valeur) return 0;
    return Math.round((valeur / total) * 100);
  }

  private buildDoughnut(data: IDemarqueByMotif[]): void {
    this.doughnutChart?.destroy();
    this.doughnutChart = undefined;

    const canvas = this.doughnutCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const palette      = this.colors.colors();
    const hoverPalette = this.colors.hoverColors();

    this.doughnutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(m => m.motif ?? ''),
        datasets: [{
          data: data.map(m => m.valeur ?? 0),
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
            labels: {
              color: this.colors.textColor(),
              usePointStyle: true,
            },
          },
        },
      },
    });
  }

  protected dateToStruct(d: Date | null): NgbDateStruct | null {
    if (!d) return null;
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  protected structToDate(s: NgbDateStruct | null): Date | null {
    if (!s) return null;
    return new Date(s.year, s.month - 1, s.day);
  }
}
