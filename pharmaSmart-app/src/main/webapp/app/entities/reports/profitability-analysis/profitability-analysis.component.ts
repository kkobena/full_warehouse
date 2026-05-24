import {Component, inject, OnInit, signal} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {SelectModule} from 'primeng/select';
import {ToolbarModule} from 'primeng/toolbar';
import {DividerModule} from 'primeng/divider';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';
import {InputText} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Drawer} from 'primeng/drawer';
import {Tag} from 'primeng/tag';

import {IMargeDTO, IMargeSummary} from 'app/shared/model/report';
import {MargeReportService} from '../services/marge-report.service';
import {FamilleProduitService} from '../../famille-produit/famille-produit.service';

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
    WarehouseCommonModule,
    InputText,
    IconField,
    InputIcon,
    Drawer,
    Tag,
  ],
})
export default class ProfitabilityAnalysisComponent implements OnInit {
  protected products = signal<IMargeDTO[]>([]);
  protected summary = signal<IMargeSummary | null>(null);
  protected isLoading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected helpDrawerVisible = signal<boolean>(false);

  // Filtres
  protected selectedFamilleId = signal<number | null>(null);
  protected searchQuery = signal<string>('');
  protected showFaibleMarge = signal<boolean>(false);
  protected seuilFaibleMarge = 10;

  // Pagination
  protected pageSize = 20;
  protected first = 0;


  protected familleOptions = signal<{ label: string; value: number | null }[]>([
    {label: 'Toutes les familles', value: null},
  ]);

  private readonly margeService = inject(MargeReportService);
  private readonly familleProduitService = inject(FamilleProduitService);

  ngOnInit(): void {
    this.loadFamilles();
    this.loadProducts();
    this.loadSummary();
  }

  protected loadProducts(page = 0): void {
    this.isLoading.set(true);

    const req = {
      page,
      size: this.pageSize,
      familleProduitId: this.selectedFamilleId() ?? undefined,
      search: this.searchQuery() || undefined,
    };

    const obs = this.showFaibleMarge()
      ? this.margeService.getFaibleMarge(this.seuilFaibleMarge, {page, size: this.pageSize})
      : this.margeService.getMarges(req);

    obs.subscribe({
      next: (res: HttpResponse<IMargeDTO[]>) => {
        this.products.set(res.body ?? []);
        this.totalRecords.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected loadSummary(): void {
    this.margeService
      .getMargeSummary({
        familleProduitId: this.selectedFamilleId() ?? undefined,
        seuilBas: this.seuilFaibleMarge,
        seuilHaut: 20,
      })
      .subscribe({
        next: (res: HttpResponse<IMargeSummary>) => this.summary.set(res.body ?? null),
      });
  }

  protected onFilterChange(): void {
    this.showFaibleMarge.set(false);
    this.first = 0;
    this.loadProducts(0);
    this.loadSummary();
  }

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.showFaibleMarge.set(false);
    this.first = 0;
    this.loadProducts(0);
  }

  protected onClearFilters(): void {
    this.selectedFamilleId.set(null);
    this.searchQuery.set('');
    this.showFaibleMarge.set(false);
    this.first = 0;
    this.loadProducts(0);
    this.loadSummary();
  }

  protected showLowMarginProducts(): void {
    this.showFaibleMarge.set(true);
    this.selectedFamilleId.set(null);
    this.searchQuery.set('');
    this.first = 0;
    this.loadProducts(0);
  }

  protected onPageChange(event: any): void {
    this.first = event.first;
    this.pageSize = event.rows;
    this.loadProducts(event.first / event.rows);
  }

  protected onSort(event: any): void {
    this.first = 0;
    this.loadProducts(0);
  }

  protected getMarginSeverity(margin: number | undefined): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    if (!margin && margin !== 0) {
      return 'secondary';
    }
    if (margin >= 30) {
      return 'success';
    }
    if (margin >= 20) {
      return 'info';
    }
    if (margin >= 10) {
      return 'warn';
    }
    return 'danger';
  }

  protected toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(v => !v);
  }

  private loadFamilles(): void {
    this.familleProduitService.query({page: 0, size: 200}).subscribe({
      next: res => {
        const opts = (res.body ?? []).map((f: any) => ({label: f.libelle, value: f.id}));
        this.familleOptions.set([{label: 'Toutes les familles', value: null}, ...opts]);
      },
    });
  }
}
