import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { ChipModule } from 'primeng/chip';
import { ProgressBarModule } from 'primeng/progressbar';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IABCPareto, IABCParetoSummary } from 'app/shared/model/report';
import { ClassePareto } from 'app/shared/model/report/classe-pareto.enum';
import { ABCParetoReportService } from '../services/abc-pareto-report.service';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Drawer } from 'primeng/drawer';
import { formatCurrency } from 'app/shared/utils/format-utils';

@Component({
  selector: 'jhi-abc-pareto',
  templateUrl: './abc-pareto.component.html',
  styleUrl: './abc-pareto.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    ChipModule,
    ProgressBarModule,
    WarehouseCommonModule,
    InputText,
    IconField,
    InputIcon,
    Drawer
  ]
})
export default class ABCParetoComponent implements OnInit {
  products = signal<IABCPareto[]>([]);
  summary = signal<IABCParetoSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedCategorie = signal<string | null>(null);
  selectedClassePareto = signal<ClassePareto | null>(null);
  helpDrawerVisible = signal<boolean>(false);

  categorieOptions = signal<Array<{ label: string; value: string }>>([]);
  classeParetoOptions = signal<Array<{ label: string; value: ClassePareto }>>([
    { label: 'Toutes', value: '' as any },
    { label: 'Classe A (80% du CA)', value: ClassePareto.A },
    { label: 'Classe B (15% du CA)', value: ClassePareto.B },
    { label: 'Classe C (5% du CA)', value: ClassePareto.C }
  ]);

  ClassePareto = ClassePareto;

  private readonly abcParetoService = inject(ABCParetoReportService);

  ngOnInit(): void {
    this.loadABCPareto();
    this.loadSummary();
  }

  loadABCPareto(): void {
    this.isLoading.set(true);
    const categorie = this.selectedCategorie();
    const classePareto = this.selectedClassePareto();

    let request;
    if (classePareto) {
      request = this.abcParetoService.getABCParetoByClass(classePareto);
    } else if (categorie) {
      request = this.abcParetoService.getABCParetoByCategory(categorie);
    } else {
      request = this.abcParetoService.getAllABCParetoAnalysis();
    }

    request.subscribe({
      next: (res: HttpResponse<IABCPareto[]>) => {
        this.products.set(res.body ?? []);
        this.extractFilterOptions(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSummary(): void {
    this.abcParetoService.getABCParetoSummary().subscribe({
      next: (res: HttpResponse<IABCParetoSummary>) => {
        this.summary.set(res.body ?? null);
      },
      error: () => {
        console.error('Error loading summary');
      },
    });
  }

  onFilterChange(): void {
    this.loadABCPareto();
  }

  onClearFilters(): void {
    this.selectedCategorie.set(null);
    this.selectedClassePareto.set(null);
    this.loadABCPareto();
  }

  showTopContributors(): void {
    this.isLoading.set(true);
    this.abcParetoService.getTopRevenueContributors(50).subscribe({
      next: (res: HttpResponse<IABCPareto[]>) => {
        this.products.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  exportToPdf(): void {
    this.abcParetoService.exportABCParetoToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `abc-pareto-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  private extractFilterOptions(products: IABCPareto[]): void {
    // Extract unique categories
    const categories = [...new Set(products.map(p => p.categorie).filter(c => c))];
    this.categorieOptions.set([
      { label: 'Toutes les catégories', value: '' },
      ...categories.map(c => ({ label: c!, value: c! }))
    ]);
  }

  getClasseParetoLabel(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A:
        return 'A';
      case ClassePareto.B:
        return 'B';
      case ClassePareto.C:
        return 'C';
      default:
        return '';
    }
  }

  getClasseParetoSeverity(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A:
        return 'success';
      case ClassePareto.B:
        return 'info';
      case ClassePareto.C:
        return 'warn';
      default:
        return 'secondary';
    }
  }

  getClasseParetoDescription(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A:
        return '80% du CA';
      case ClassePareto.B:
        return '15% du CA (80-95%)';
      case ClassePareto.C:
        return '5% du CA (95-100%)';
      default:
        return '';
    }
  }

  getCumulativePercentageColor(caCumulePct: number | undefined): string {
    if (!caCumulePct) return 'secondary';
    if (caCumulePct <= 80) return 'success';
    if (caCumulePct <= 95) return 'info';
    return 'warn';
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }

  // Format methods using shared utilities
  formatCurrency = formatCurrency;
}
