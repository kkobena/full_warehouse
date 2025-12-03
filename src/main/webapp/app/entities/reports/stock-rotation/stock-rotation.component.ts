import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { Tag } from 'primeng/tag';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IStockRotation, CategorieABC } from 'app/shared/model/report/stock-rotation.model';
import { StockRotationReportService } from '../services/stock-rotation-report.service';
import { formatCurrency } from 'app/shared/utils/format-utils';

@Component({
  selector: 'jhi-stock-rotation',
  templateUrl: './stock-rotation.component.html',
  styleUrl: './stock-rotation.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule,
    Tag
  ]
})
export default class StockRotationComponent implements OnInit {
  rotations = signal<IStockRotation[]>([]);
  abcCounts = signal<{ [key in CategorieABC]: number }>({ A: 0, B: 0, C: 0 });
  isLoading = signal<boolean>(false);
  selectedCategorie = signal<string | null>(null);
  selectedABC = signal<CategorieABC | null>(null);
  showSlowMovingOnly = signal<boolean>(false);

  categorieOptions = signal<Array<{ label: string; value: string }>>([]);
  abcOptions = [
    { label: 'Toutes les classifications', value: null },
    { label: 'A - Forte rotation (z ≥ 1.96)', value: CategorieABC.A },
    { label: 'B - Rotation moyenne (z ≥ 1.65)', value: CategorieABC.B },
    { label: 'C - Faible rotation (z < 1.65)', value: CategorieABC.C },
  ];

  private readonly stockRotationService = inject(StockRotationReportService);

  ngOnInit(): void {
    this.loadRotations();
    this.loadABCCounts();
  }

  loadRotations(): void {
    this.isLoading.set(true);
    const categorie = this.selectedCategorie();
    const abc = this.selectedABC();
    const slowOnly = this.showSlowMovingOnly();

    let request;
    if (slowOnly) {
      request = this.stockRotationService.getSlowMovingProducts();
    } else if (abc) {
      request = this.stockRotationService.getStockRotationByABCClassification(abc);
    } else if (categorie) {
      request = this.stockRotationService.getStockRotationByCategory(categorie);
    } else {
      request = this.stockRotationService.getAllStockRotation();
    }

    request.subscribe({
      next: (res: HttpResponse<IStockRotation[]>) => {
        this.rotations.set(res.body ?? []);
        this.extractCategorieOptions(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadABCCounts(): void {
    this.stockRotationService.getStockRotationCountByABCClassification().subscribe({
      next: (res: HttpResponse<{ [key in CategorieABC]: number }>) => {
        this.abcCounts.set(res.body ?? { A: 0, B: 0, C: 0 });
      },
      error: () => {
        console.error('Error loading ABC counts');
      },
    });
  }

  onFilterChange(): void {
    if (this.showSlowMovingOnly()) {
      this.selectedABC.set(null);
      this.selectedCategorie.set(null);
    }
    this.loadRotations();
  }

  onSlowMovingToggle(): void {
    this.showSlowMovingOnly.update(value => !value);
    this.onFilterChange();
  }

  onClearFilters(): void {
    this.selectedCategorie.set(null);
    this.selectedABC.set(null);
    this.showSlowMovingOnly.set(false);
    this.loadRotations();
  }

  exportToPdf(): void {
    this.stockRotationService.exportStockRotationToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `rotation-stock-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  private extractCategorieOptions(rotations: IStockRotation[]): void {
    const categories = [...new Set(rotations.map(r => r.categorie).filter(c => c))];
    this.categorieOptions.set([
      { label: 'Toutes les catégories', value: '' },
      ...categories.map(c => ({ label: c!, value: c! }))
    ]);
  }

  getTotalStockValue(): number {
    return this.rotations().reduce((sum, item) => sum + (item.stockValue || 0), 0);
  }

  getTotalCA12Months(): number {
    return this.rotations().reduce((sum, item) => sum + (item.caLast12Months || 0), 0);
  }

  getAverageRotationRate(): number {
    const rotations = this.rotations().filter(r => r.rotationRateAnnual);
    if (rotations.length === 0) return 0;
    const sum = rotations.reduce((acc, r) => acc + (r.rotationRateAnnual || 0), 0);
    return sum / rotations.length;
  }

  getABCSeverity(abc: CategorieABC | undefined): string {
    if (!abc) return 'secondary';
    switch (abc) {
      case CategorieABC.A:
        return 'success';
      case CategorieABC.B:
        return 'info';
      case CategorieABC.C:
        return 'warn';
      default:
        return 'secondary';
    }
  }

  getABCLabel(abc: CategorieABC | undefined): string {
    if (!abc) return 'N/A';
    switch (abc) {
      case CategorieABC.A:
        return 'A - Forte rotation';
      case CategorieABC.B:
        return 'B - Rotation moyenne';
      case CategorieABC.C:
        return 'C - Faible rotation';
      default:
        return abc;
    }
  }

  getRotationRateSeverity(rate: number | undefined): string {
    if (!rate) return 'secondary';
    if (rate >= 6) return 'success';
    if (rate >= 3) return 'info';
    if (rate >= 1) return 'warn';
    return 'danger';
  }

  // Format methods using shared utilities
  formatCurrency = formatCurrency;
}
