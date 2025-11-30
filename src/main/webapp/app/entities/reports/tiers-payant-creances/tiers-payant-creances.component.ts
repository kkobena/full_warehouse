import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import {
  AgeCategory,
  ITiersPayantCreancesSummary,
  ITiersPayantInvoice
} from 'app/shared/model/report/tiers-payant-report.model';
import { TiersPayantReportService } from '../services/tiers-payant-report.service';

@Component({
  selector: 'jhi-tiers-payant-creances',
  templateUrl: './tiers-payant-creances.component.html',
  styleUrl: './tiers-payant-creances.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    Card,
    Tag,
    Select,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule
  ]
})
export default class TiersPayantCreancesComponent implements OnInit {
  summary = signal<ITiersPayantCreancesSummary[]>([]);
  invoices = signal<ITiersPayantInvoice[]>([]);
  selectedAgeCategory = signal<AgeCategory | undefined>(undefined);
  isLoading = signal<boolean>(false);

  ageCategoryOptions = [
    { label: 'Tous', value: undefined },
    { label: 'Moins de 30 jours', value: AgeCategory.LESS_THAN_30 },
    { label: '30-60 jours', value: AgeCategory.BETWEEN_30_60 },
    { label: '60-90 jours', value: AgeCategory.BETWEEN_60_90 },
    { label: 'Plus de 90 jours', value: AgeCategory.MORE_THAN_90 }
  ];

  private readonly tiersPayantService = inject(TiersPayantReportService);

  ngOnInit(): void {
    this.loadSummary();
    this.loadInvoices();
  }

  loadSummary(): void {
    this.tiersPayantService.getCreancesSummary().subscribe({
      next: (res: HttpResponse<ITiersPayantCreancesSummary[]>) => {
        this.summary.set(res.body ?? []);
      }
    });
  }

  loadInvoices(): void {
    this.isLoading.set(true);
    const ageCategory = this.selectedAgeCategory();

    this.tiersPayantService.getUnpaidInvoices(undefined, ageCategory).subscribe({
      next: (res: HttpResponse<ITiersPayantInvoice[]>) => {
        this.invoices.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.loadInvoices();
  }

  exportToPdf(): void {
    this.tiersPayantService.exportCreancesToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `creances-tiers-payant-${new Date().toISOString().split('T')[0]}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      }
    });
  }

  getTotalCreances(): number {
    return this.summary().reduce((sum, item) => sum + (item.montantTotal ?? 0), 0);
  }

  getAgeCategorySeverity(ageCategory?: AgeCategory): 'success' | 'warn' | 'danger' {
    switch (ageCategory) {
      case AgeCategory.LESS_THAN_30:
        return 'success';
      case AgeCategory.BETWEEN_30_60:
        return 'warn';
      case AgeCategory.BETWEEN_60_90:
      case AgeCategory.MORE_THAN_90:
        return 'danger';
      default:
        return 'success';
    }
  }

  getAgeCategoryLabel(ageCategory?: AgeCategory): string {
    switch (ageCategory) {
      case AgeCategory.LESS_THAN_30:
        return '< 30j';
      case AgeCategory.BETWEEN_30_60:
        return '30-60j';
      case AgeCategory.BETWEEN_60_90:
        return '60-90j';
      case AgeCategory.MORE_THAN_90:
        return '> 90j';
      default:
        return '';
    }
  }
}
