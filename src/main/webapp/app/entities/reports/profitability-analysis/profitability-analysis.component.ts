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
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IProductProfitability, IProfitabilitySummary } from 'app/shared/model/report';
import { BCGCategory } from 'app/shared/model/report/bcg-category.enum';
import { ProfitabilityReportService } from '../services/profitability-report.service';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Drawer } from 'primeng/drawer';

@Component({
  selector: 'jhi-profitability-analysis',
  templateUrl: './profitability-analysis.component.html',
  styleUrl: './profitability-analysis.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    ChipModule,
    WarehouseCommonModule,
    InputText,
    IconField,
    InputIcon,
    Drawer
  ]
})
export default class ProfitabilityAnalysisComponent implements OnInit {
  protected products = signal<IProductProfitability[]>([]);
  protected summary = signal<IProfitabilitySummary | null>(null);
  protected isLoading = signal<boolean>(false);
  protected selectedCategorie = signal<string | null>(null);
  protected selectedBCGCategory = signal<BCGCategory | null>(null);
  protected  showLowMarginOnly = signal<boolean>(false);
  protected helpDrawerVisible = signal<boolean>(false);

  protected categorieOptions = signal<Array<{ label: string; value: string }>>([]);
  protected  bcgCategoryOptions = signal<Array<{ label: string; value: BCGCategory }>>([
    { label: 'Toutes', value: '' as any },
    { label: 'Stars (Marge élevée + Rotation élevée)', value: BCGCategory.STAR },
    { label: 'Cash Cows (Marge élevée + Rotation faible)', value: BCGCategory.CASH_COW },
    { label: 'Question Marks (Marge faible + Rotation élevée)', value: BCGCategory.QUESTION_MARK },
    { label: 'Dogs (Marge faible + Rotation faible)', value: BCGCategory.DOG }
  ]);

  BCGCategory = BCGCategory;

  private readonly profitabilityService = inject(ProfitabilityReportService);

  ngOnInit(): void {
    this.loadProfitability();
    this.loadSummary();
  }

  protected  loadProfitability(): void {
    this.isLoading.set(true);
    const categorie = this.selectedCategorie();
    const bcgCategory = this.selectedBCGCategory();
    const lowMargin = this.showLowMarginOnly();

    let request;
    if (lowMargin) {
      request = this.profitabilityService.getLowMarginProducts();
    } else if (bcgCategory) {
      request = this.profitabilityService.getProductProfitabilityByBCGCategory(bcgCategory);
    } else if (categorie) {
      request = this.profitabilityService.getProductProfitabilityByCategory(categorie);
    } else {
      request = this.profitabilityService.getAllProductProfitability();
    }

    request.subscribe({
      next: (res: HttpResponse<IProductProfitability[]>) => {
        console.error('res.body', res.body);
        this.products.set(res.body ?? []);
        this.extractFilterOptions(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  protected loadSummary(): void {
    this.profitabilityService.getProfitabilitySummary().subscribe({
      next: (res: HttpResponse<IProfitabilitySummary>) => {
        this.summary.set(res.body ?? null);
      },
      error: () => {
        console.error('Error loading summary');
      },
    });
  }

  protected onFilterChange(): void {
    this.loadProfitability();
  }

  protected onClearFilters(): void {
    this.selectedCategorie.set(null);
    this.selectedBCGCategory.set(null);
    this.showLowMarginOnly.set(false);
    this.loadProfitability();
  }

  protected  showLowMarginProducts(): void {
    this.showLowMarginOnly.set(true);
    this.selectedCategorie.set(null);
    this.selectedBCGCategory.set(null);
    this.loadProfitability();
  }

  protected exportToPdf(): void {
    this.profitabilityService.exportProfitabilityToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `rentabilite-produits-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  private extractFilterOptions(products: IProductProfitability[]): void {
    // Extract unique categories
    const categories = [...new Set(products.map(p => p.categorie).filter(c => c))];
    this.categorieOptions.set([
      { label: 'Toutes les catégories', value: '' },
      ...categories.map(c => ({ label: c!, value: c! }))
    ]);
  }

  protected getBCGCategoryLabel(bcgCategory: BCGCategory | undefined): string {
    switch (bcgCategory) {
      case BCGCategory.STAR:
        return 'Star';
      case BCGCategory.CASH_COW:
        return 'Cash Cow';
      case BCGCategory.QUESTION_MARK:
        return 'Question Mark';
      case BCGCategory.DOG:
        return 'Dog';
      default:
        return 'Indéfini';
    }
  }

  protected  getBCGCategorySeverity(bcgCategory: BCGCategory | undefined): string {
    switch (bcgCategory) {
      case BCGCategory.STAR:
        return 'success';
      case BCGCategory.CASH_COW:
        return 'info';
      case BCGCategory.QUESTION_MARK:
        return 'warn';
      case BCGCategory.DOG:
        return 'danger';
      default:
        return 'secondary';
    }
  }

  protected getMarginSeverity(margin: number | undefined): string {
    if (!margin) return 'secondary';
    if (margin >= 30) return 'success';
    if (margin >= 20) return 'info';
    if (margin >= 10) return 'warn';
    return 'danger';
  }

  protected calculateMarginPercentage(product: IProductProfitability): number {
    return product.tauxMargePct || 0;
  }

  protected toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }
}
