import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IStockValuation, IStockValuationSummary } from 'app/shared/model/report/stock-valuation.model';
import { StockValuationReportService } from '../services/stock-valuation-report.service';
import { formatCurrency, formatDecimal } from 'app/shared/utils/format-utils';

@Component({
  selector: 'jhi-stock-valuation',
  templateUrl: './stock-valuation.component.html',
  styleUrl: './stock-valuation.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule
  ]
})
export default class StockValuationComponent implements OnInit {
  valuations = signal<IStockValuation[]>([]);
  summary = signal<IStockValuationSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedCategorie = signal<string | null>(null);
  selectedStorage = signal<string | null>(null);

  categorieOptions = signal<Array<{ label: string; value: string }>>([]);
  storageOptions = signal<Array<{ label: string; value: string }>>([]);

  private readonly stockValuationService = inject(StockValuationReportService);

  ngOnInit(): void {
    this.loadValuations();
    this.loadSummary();
  }

  loadValuations(): void {
    this.isLoading.set(true);
    const categorie = this.selectedCategorie();
    const storage = this.selectedStorage();

    let request;
    if (categorie) {
      request = this.stockValuationService.getStockValuationByCategory(categorie);
    } else if (storage) {
      request = this.stockValuationService.getStockValuationByStorage(storage);
    } else {
      request = this.stockValuationService.getAllStockValuation();
    }

    request.subscribe({
      next: (res: HttpResponse<IStockValuation[]>) => {
        this.valuations.set(res.body ?? []);
        this.extractFilterOptions(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSummary(): void {
    this.stockValuationService.getStockValuationSummary().subscribe({
      next: (res: HttpResponse<IStockValuationSummary>) => {
        this.summary.set(res.body ?? null);
      },
      error: () => {
        console.error('Error loading summary');
      },
    });
  }

  onFilterChange(): void {
    this.loadValuations();
  }

  onClearFilters(): void {
    this.selectedCategorie.set(null);
    this.selectedStorage.set(null);
    this.loadValuations();
  }

  exportToPdf(): void {
    this.stockValuationService.exportStockValuationToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `valorisation-stock-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  private extractFilterOptions(valuations: IStockValuation[]): void {
    // Extract unique categories
    const categories = [...new Set(valuations.map(v => v.categorie).filter(c => c))];
    this.categorieOptions.set([
      { label: 'Toutes les catégories', value: '' },
      ...categories.map(c => ({ label: c!, value: c! }))
    ]);

    // Extract unique storage locations
    const storages = [...new Set(valuations.map(v => v.storageLocation).filter(s => s))];
    this.storageOptions.set([
      { label: 'Tous les emplacements', value: '' },
      ...storages.map(s => ({ label: s!, value: s! }))
    ]);
  }

  getTotalStockValue(): number {
    return this.valuations().reduce((sum, item) => sum + (item.totalPurchaseValue || 0), 0);
  }

  getTotalSalesValue(): number {
    return this.valuations().reduce((sum, item) => sum + (item.totalSalesValue || 0), 0);
  }

  getTotalPotentialMargin(): number {
    return this.valuations().reduce((sum, item) => sum + (item.potentialMargin || 0), 0);
  }

  getAverageMarginPercentage(): number {
    const valuations = this.valuations().filter(v => v.marginPercentage && v.marginPercentage > 0);
    if (valuations.length === 0) return 0;
    const sum = valuations.reduce((acc, v) => acc + (v.marginPercentage || 0), 0);
    return sum / valuations.length;
  }

  getMarginSeverity(margin: number | undefined): string {
    if (!margin) return 'secondary';
    if (margin >= 30) return 'success';
    if (margin >= 20) return 'info';
    if (margin >= 10) return 'warn';
    return 'danger';
  }

  // Format methods using shared utilities
  formatCurrency = formatCurrency;
  formatDecimal = formatDecimal;
}
