import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';
import { MultiSelectModule } from 'primeng/multiselect';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IStockAlert, StockAlertType } from 'app/shared/model/report/stock-alert.model';
import { StockAlertReportService } from '../services/stock-alert-report.service';

@Component({
  selector: 'jhi-stock-alerts',
  templateUrl: './stock-alerts.component.html',
  styleUrl: './stock-alerts.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    Card,
    Tag,
    MultiSelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule,
  ],
})
export default class StockAlertsComponent implements OnInit {
  alerts = signal<IStockAlert[]>([]);
  alertCounts = signal<Record<string, number>>({});
  selectedAlertTypes = signal<StockAlertType[]>([]);
  isLoading = signal<boolean>(false);

  // Expose StockAlertType enum to template
  readonly StockAlertType = StockAlertType;

  alertTypeOptions = [
    { label: 'Rupture de stock', value: StockAlertType.RUPTURE },
    { label: 'Alerte stock bas', value: StockAlertType.ALERTE },
    { label: 'Proche péremption', value: StockAlertType.PEREMPTION },
  ];

  private readonly stockAlertService = inject(StockAlertReportService);

  ngOnInit(): void {
    this.loadAlerts();
    this.loadAlertCounts();
  }

  loadAlerts(): void {
    this.isLoading.set(true);
    const types = this.selectedAlertTypes().length > 0 ? this.selectedAlertTypes() : undefined;

    this.stockAlertService.getStockAlerts(types).subscribe({
      next: (res: HttpResponse<IStockAlert[]>) => {
        this.alerts.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadAlertCounts(): void {
    this.stockAlertService.getStockAlertsCount().subscribe({
      next: (res: HttpResponse<Record<StockAlertType, number>>) => {
        this.alertCounts.set(res.body ?? {});
      },
    });
  }

  onFilterChange(): void {
    this.loadAlerts();
  }

  exportToPdf(): void {
    const types = this.selectedAlertTypes().length > 0 ? this.selectedAlertTypes() : undefined;

    this.stockAlertService.exportStockAlertsToPdf(types).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `stock-alerts-${new Date().toISOString().split('T')[0]}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
    });
  }

  getAlertSeverity(alertType?: StockAlertType): 'danger' | 'warn' | 'info' {
    switch (alertType) {
      case StockAlertType.RUPTURE:
        return 'danger';
      case StockAlertType.ALERTE:
        return 'warn';
      case StockAlertType.PEREMPTION:
        return 'info';
      default:
        return 'info';
    }
  }

  getAlertLabel(alertType?: StockAlertType): string {
    switch (alertType) {
      case StockAlertType.RUPTURE:
        return 'Rupture';
      case StockAlertType.ALERTE:
        return 'Stock bas';
      case StockAlertType.PEREMPTION:
        return 'Péremption';
      default:
        return '';
    }
  }
}
