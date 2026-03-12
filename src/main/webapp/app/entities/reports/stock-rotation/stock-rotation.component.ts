import {Component, inject, OnInit, signal} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {SelectModule} from 'primeng/select';
import {ToolbarModule} from 'primeng/toolbar';
import {DividerModule} from 'primeng/divider';
import {Tag} from 'primeng/tag';
import {Drawer} from 'primeng/drawer';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';

import {CategorieABC, IStockRotation} from 'app/shared/model/report/stock-rotation.model';
import {StockRotationReportService} from '../services/stock-rotation-report.service';
import {formatCurrency} from 'app/shared/utils/format-utils';
import {handleBlobForTauri} from "../../../shared/util/tauri-util";
import {TauriPrinterService} from "../../../shared/services/tauri-printer.service";

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
    Tag,
    Drawer,
  ],
})
export default class StockRotationComponent implements OnInit {
  rotations = signal<IStockRotation[]>([]);
  abcCounts = signal<Record<CategorieABC, number>>({A: 0, B: 0, C: 0});
  isLoading = signal<boolean>(false);
  selectedCategorie = signal<string | null>(null);
  selectedABC = signal<CategorieABC | null>(null);
  showSlowMovingOnly = signal<boolean>(false);
  helpDrawerVisible = signal<boolean>(false);

  categorieOptions = signal<{ label: string; value: string }[]>([]);
  abcOptions = [
    {label: 'Toutes les classifications', value: null},
    {label: 'A - Forte rotation (z ≥ 1.96)', value: CategorieABC.A},
    {label: 'B - Rotation moyenne (z ≥ 1.65)', value: CategorieABC.B},
    {label: 'C - Faible rotation (z < 1.65)', value: CategorieABC.C},
  ];
  // Format methods using shared utilities
  formatCurrency = formatCurrency;
  private readonly stockRotationService = inject(StockRotationReportService);
  private readonly tauriPrinter = inject(TauriPrinterService);

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
      next: (res: HttpResponse<Record<CategorieABC, number>>) => {
        this.abcCounts.set(res.body ?? {A: 0, B: 0, C: 0});
      },
      error() {
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
    this.stockRotationService.exportStockRotationToPdf().subscribe(resp => {
      if (this.tauriPrinter.isRunningInTauri()) {
        handleBlobForTauri(resp.body, `rotation-stock-${new Date().getTime()}`);
      } else {
        window.open(URL.createObjectURL(resp.body));
      }
    });


  }

  getTotalStockValue(): number {
    return this.rotations().reduce((sum, item) => sum + (item.stockValue || 0), 0);
  }


  getABCSeverity(abc: CategorieABC | undefined): string {
    if (!abc) {
      return 'secondary';
    }
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
    if (!abc) {
      return 'N/A';
    }
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
    if (!rate) {
      return 'secondary';
    }
    if (rate >= 6) {
      return 'success';
    }
    if (rate >= 3) {
      return 'info';
    }
    if (rate >= 1) {
      return 'warn';
    }
    return 'danger';
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }

  private extractCategorieOptions(rotations: IStockRotation[]): void {
    const categories = [...new Set(rotations.map(r => r.categorie).filter(c => c))];
    this.categorieOptions.set([{
      label: 'Toutes les catégories',
      value: ''
    }, ...categories.map(c => ({label: c, value: c}))]);
  }
}
