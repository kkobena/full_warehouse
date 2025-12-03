import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { DatePicker } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IDailySalesSummary } from 'app/shared/model/report/daily-sales-summary.model';
import { SalesSummaryReportService } from '../services/sales-summary-report.service';
import { Tag } from 'primeng/tag';
import { PrimeNG } from 'primeng/config';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-sales-summary',
  templateUrl: './sales-summary.component.html',
  styleUrl: './sales-summary.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DatePicker,
    SelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule,
    Tag
  ]
})
export default class SalesSummaryComponent implements OnInit {
  summaries = signal<IDailySalesSummary[]>([]);
  isLoading = signal<boolean>(false);
  startDate = signal<Date | null>(null);
  endDate = signal<Date | null>(null);
  selectedTypeVente = signal<string | null>(null);

  typeVenteOptions = [
    { label: 'Tous', value: null },
    { label: 'Vente ordonnancées (VO)', value: 'ThirdPartySales' },
    { label: 'Vente au comptant (VNO)', value: 'CashSale' },
    { label: 'Vente aux dépôts', value: 'VenteDepot' }
  ];
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly salesSummaryService = inject(SalesSummaryReportService);

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.startDate.set(firstDay);
    this.endDate.set(lastDay);

    this.loadSummaries();
  }

  loadSummaries(): void {
    if (!this.startDate() || !this.endDate()) {
      return;
    }

    this.isLoading.set(true);
    const startDateStr = this.formatDate(this.startDate()!);
    const endDateStr = this.formatDate(this.endDate()!);
    const typeVente = this.selectedTypeVente();

    const request = typeVente
      ? this.salesSummaryService.getDailySalesSummaryByType(startDateStr, endDateStr, typeVente)
      : this.salesSummaryService.getDailySalesSummary(startDateStr, endDateStr);

    request.subscribe({
      next: (res: HttpResponse<IDailySalesSummary[]>) => {
        this.summaries.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.loadSummaries();
  }

  getTotalCA(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caTotal || 0), 0);
  }

  getTotalCANet(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caNet || 0), 0);
  }

  getTotalVentes(): number {
    return this.summaries().reduce((sum, item) => sum + (item.nbVentes || 0), 0);
  }

  getAveragePanier(): number {
    const total = this.getTotalCA();
    const count = this.getTotalVentes();
    return count > 0 ? total / count : 0;
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  getSeverityForType(type: string | undefined): string {
    if (!type) return 'secondary';
    switch (type) {
      case 'VO':
        return 'info';
      case 'VNO':
        return 'success';
      case 'VENTES_DEPOTS':
        return 'warn';
      default:
        return 'secondary';
    }
  }
}
