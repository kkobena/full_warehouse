import { Component, OnInit, inject, signal } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { Drawer } from 'primeng/drawer';

import { SupplierPerformanceReportService } from '../services/supplier-performance-report.service';
import { ISupplierPerformance, ISupplierPerformanceSummary } from 'app/shared/model/report';
import { formatCurrency, formatNumber, formatDecimal } from 'app/shared/utils/format-utils';

interface FilterOption {
  label: string;
  value: string;
}

@Component({
  selector: 'jhi-supplier-performance',
  imports: [FormsModule, ButtonModule, TableModule, SelectModule, InputTextModule, TooltipModule, ToolbarModule, Drawer],
  templateUrl: './supplier-performance.component.html',
  styleUrl: './supplier-performance.component.scss',
})
export default class SupplierPerformanceComponent implements OnInit {
  private supplierPerformanceService = inject(SupplierPerformanceReportService);

  // Signals for reactive state management
  suppliers = signal<ISupplierPerformance[]>([]);
  summary = signal<ISupplierPerformanceSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedFilter = signal<string>('all');
  searchText = signal<string>('');
  helpDrawerVisible = signal<boolean>(false);

  // Filter options
  filterOptions: FilterOption[] = [
    { label: 'Tous les fournisseurs', value: 'all' },
    { label: 'Top 10 par volume', value: 'top' },
    { label: 'Performance excellente (e 70)', value: 'good' },
    { label: 'Performance moyenne (50-70)', value: 'average' },
    { label: 'Performance faible (< 50)', value: 'poor' },
    { label: 'Problèmes de livraison', value: 'delivery-issues' },
  ];

  ngOnInit(): void {
    this.loadData();
    this.loadSummary();
  }

  loadData(): void {
    this.isLoading.set(true);
    const filter = this.selectedFilter();

    switch (filter) {
      case 'top':
        this.loadTopSuppliers();
        break;
      case 'good':
        this.loadSuppliersByScore(70);
        break;
      case 'average':
        this.loadSuppliersByScore(50);
        break;
      case 'poor':
        this.loadSuppliersByScore(0);
        break;
      case 'delivery-issues':
        this.loadSuppliersWithDeliveryIssues();
        break;
      default:
        this.loadAllSuppliers();
    }
  }

  loadAllSuppliers(): void {
    this.supplierPerformanceService.getAllSupplierPerformance().subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadTopSuppliers(): void {
    this.supplierPerformanceService.getTopSuppliersByVolume(10).subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSuppliersByScore(minScore: number): void {
    this.supplierPerformanceService.getSuppliersByPerformanceScore(minScore).subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        let suppliers = res.body ?? [];

        // Filter based on score range
        if (minScore === 50) {
          // Average: 50-70
          suppliers = suppliers.filter(s => (s.performanceScore ?? 0) >= 50 && (s.performanceScore ?? 0) < 70);
        } else if (minScore === 0) {
          // Poor: < 50
          suppliers = suppliers.filter(s => (s.performanceScore ?? 0) < 50);
        }

        this.suppliers.set(suppliers);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSuppliersWithDeliveryIssues(): void {
    this.supplierPerformanceService.getSuppliersWithDeliveryIssues().subscribe({
      next: (res: HttpResponse<ISupplierPerformance[]>) => {
        this.suppliers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSummary(): void {
    this.supplierPerformanceService.getSupplierPerformanceSummary().subscribe({
      next: (res: HttpResponse<ISupplierPerformanceSummary>) => {
        this.summary.set(res.body ?? null);
      },
    });
  }

  onFilterChange(): void {
    this.loadData();
  }

  exportToPdf(): void {
    this.isLoading.set(true);
    this.supplierPerformanceService.exportSupplierPerformanceToPdf().subscribe({
      next: (response: HttpResponse<Blob>) => {
        if (response.body) {
          const blob = new Blob([response.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `supplier-performance-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  getPerformanceScoreLabel(score: number | undefined): string {
    if (!score) return 'N/A';
    if (score >= 70) return 'Excellent';
    if (score >= 50) return 'Moyen';
    return 'Faible';
  }

  // Format methods using shared utilities
  formatCurrency = formatCurrency;
  formatNumber = formatNumber;
  formatDecimal = formatDecimal;

  getFilteredSuppliers(): ISupplierPerformance[] {
    const search = this.searchText().toLowerCase();
    if (!search) {
      return this.suppliers();
    }

    return this.suppliers().filter(
      s =>
        s.fournisseurName?.toLowerCase().includes(search) ||
        s.fournisseurCode?.toLowerCase().includes(search) ||
        s.phone?.includes(search) ||
        s.mobile?.includes(search),
    );
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }
}
