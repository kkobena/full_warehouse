import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {forkJoin} from 'rxjs';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {SelectModule} from 'primeng/select';
import {ToolbarModule} from 'primeng/toolbar';
import {DividerModule} from 'primeng/divider';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';

import {IStockValuation, IStockValuationSummary} from 'app/shared/model/report/stock-valuation.model';
import {StockValuationReportService} from '../services/stock-valuation-report.service';
import {formatCurrency, formatDecimal} from 'app/shared/utils/format-utils';
import {FamilleProduitService} from "../../famille-produit/famille-produit.service";
import {RayonService} from "../../rayon/rayon.service";
import {IFamilleProduit} from "../../../shared/model/famille-produit.model";
import {IRayon} from "../../../shared/model/rayon.model";
import {TauriPrinterService} from "../../../shared/services/tauri-printer.service";
import {handleBlobForTauri} from "../../../shared/util/tauri-util";

@Component({
  selector: 'jhi-stock-valuation',
  templateUrl: './stock-valuation.component.html',
  styleUrl: './stock-valuation.component.scss',
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, SelectModule, ToolbarModule, DividerModule, WarehouseCommonModule],
})
export default class StockValuationComponent implements OnInit {
  valuations = signal<IStockValuation[]>([]);
  summary = signal<IStockValuationSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedFamilleProduit = signal<IFamilleProduit | null>(null);
  selectedRayon = signal<IRayon | null>(null);

  familleProduitOptions = signal<IFamilleProduit[]>([]);
  rayonOptions = signal<IRayon[]>([]);

  private readonly stockValuationService = inject(StockValuationReportService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly rayonService = inject(RayonService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadReferentielData();
    this.loadData();
  }

  /** Charge familles et rayons en parallèle — une seule fois au démarrage */
  private loadReferentielData(): void {
    forkJoin({
      familles: this.familleProduitService.query({page: 0, size: 9999}),
      rayons: this.rayonService.query({page: 0, size: 9999}),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({familles, rayons}) => {
          this.familleProduitOptions.set(familles.body ?? []);
          this.rayonOptions.set(rayons.body ?? []);
        },
      });
  }

  /** Charge valuations + summary en parallèle avec les filtres courants */
  protected loadData(): void {
    this.isLoading.set(true);
    const params = this.buildRequestParams();

    forkJoin({
      valuations: this.stockValuationService.getAllStockValuation(params),
      summary: this.stockValuationService.getStockValuationSummary(params),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({valuations, summary}) => {
          this.valuations.set(valuations.body ?? []);
          this.summary.set(summary.body ?? null);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false),
      });
  }

  protected onFilterChange(): void {
    this.loadData();
  }

  protected onClearFilters(): void {
    this.selectedFamilleProduit.set(null);
    this.selectedRayon.set(null);
    this.loadData();
  }

  protected exportToPdf(): void {
    this.stockValuationService.exportStockValuationToPdf(this.buildRequestParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(resp => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(resp.body, `stock-valuation`);
        } else {
          window.open(URL.createObjectURL(resp.body));
        }
      });
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

  private buildRequestParams(): any {
    const params: any = {};
    if (this.selectedFamilleProduit()) {
      params['familleProduitId'] = this.selectedFamilleProduit()!.id;
    }
    if (this.selectedRayon()) {
      params['rayonId'] = this.selectedRayon()!.id;
    }

    return params;
  }
}
