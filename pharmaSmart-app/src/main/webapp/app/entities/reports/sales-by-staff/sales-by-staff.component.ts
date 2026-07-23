import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {DashboardCAService} from '../services/dashboard-ca.service';
import {IPerformanceVendeur} from '../../../shared/model/report';
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {ChartBuilderService, ChartConfig} from '../../../shared/util/chart-builder.service';
import {formatCurrency, formatDecimal, formatNumber} from 'app/shared/utils/format-utils';
import {ButtonComponent, DataTableComponent, SortableHeaderDirective, ToolbarComponent} from '../../../shared/ui';
import {ChartComponent} from "../../../shared/chart/chart.component";
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'app-sales-by-staff',
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    SortableHeaderDirective,
    ChartComponent,
    PharmaDatePickerComponent,
    ButtonComponent,
    ToolbarComponent
  ],
  templateUrl: './sales-by-staff.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./sales-by-staff.component.scss'],
})
export default class SalesByStaffComponent implements OnInit {
  protected readonly staff = signal<IPerformanceVendeur[]>([]);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate = signal<Date | null>(new Date());

  // `computed`, pas un appel direct à `dateToStruct()` dans le template : voir
  // date-range-filter (supprimé) pour l'explication de la boucle silencieuse évitée.
  protected readonly fromStruct = computed(() => this.dateToStruct(this.fromDate()));
  protected readonly toStruct = computed(() => this.dateToStruct(this.toDate()));

  protected chartConfig = signal<ChartConfig | null>(null);

  protected readonly totalVentes = computed(() => this.staff().reduce((s, v) => s + (v.nombreVentes ?? 0), 0));
  protected readonly totalMontant = computed(() => this.staff().reduce((s, v) => s + (v.montantTotal ?? 0), 0));

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatDecimal = formatDecimal;
  protected readonly formatNumber = formatNumber;

  private readonly svc = inject(DashboardCAService);
  private readonly chartBuilder = inject(ChartBuilderService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const startDate = DATE_FORMAT_ISO_DATE(this.fromDate()) ?? undefined;
    const endDate = DATE_FORMAT_ISO_DATE(this.toDate()) ?? undefined;
    if (!startDate || !endDate) {
      return;
    }

    this.isLoading.set(true);
    this.svc.getSalesByStaff(startDate, endDate).subscribe({
      next: res => {
        const data = res.body ?? [];
        this.staff.set(data);
        this.buildChart(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected sharePercent(montant: number | undefined): number {
    const total = this.totalMontant();
    if (!total || !montant) {
      return 0;
    }
    return Math.round((montant / total) * 100);
  }

  private buildChart(data: IPerformanceVendeur[]): void {
    if (!data.length) {
      this.chartConfig.set(null);
      return;
    }
    const labels = data.map(v => v.vendeurNom ?? '');
    const amounts = data.map(v => v.montantTotal ?? 0);
    this.chartConfig.set(this.chartBuilder.barConfig(labels, amounts, 'CA Vendeur'));
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
