import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { forkJoin } from "rxjs";

import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { SelectModule } from "primeng/select";
import { ToolbarModule } from "primeng/toolbar";
import { DividerModule } from "primeng/divider";

import { IStockValuation, IStockValuationSummary } from "app/shared/model/report/stock-valuation.model";
import { StockValuationReportService } from "../services/stock-valuation-report.service";
import { formatCurrency, formatDecimal } from "app/shared/utils/format-utils";
import { FamilleProduitService } from "../../famille-produit/famille-produit.service";
import { RayonService } from "../../rayon/rayon.service";
import { IFamilleProduit } from "../../../shared/model/famille-produit.model";
import { IRayon } from "../../../shared/model/rayon.model";
import { TauriPrinterService } from "../../../shared/services/tauri-printer.service";
import { handleBlobForTauri } from "../../../shared/util/tauri-util";

@Component({
  selector: "jhi-stock-valuation",
  templateUrl: "./stock-valuation.component.html",
  styleUrl: "./stock-valuation.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, SelectModule, ToolbarModule, DividerModule]
})
export default class StockValuationComponent implements OnInit {
  valuations = signal<IStockValuation[]>([]);
  summary = signal<IStockValuationSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedFamilleProduit = signal<IFamilleProduit | null>(null);
  selectedRayon = signal<IRayon | null>(null);

  familleProduitOptions = signal<IFamilleProduit[]>([]);
  rayonOptions = signal<IRayon[]>([]);
  // Format methods using shared utilities
  formatCurrency = formatCurrency;
  formatDecimal = formatDecimal;
  private readonly stockValuationService = inject(StockValuationReportService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly rayonService = inject(RayonService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadReferentielData();
    this.loadData();
  }

  /** Charge valuations + summary en parallèle avec les filtres courants */
  protected loadData(): void {
    this.isLoading.set(true);
    const params = this.buildRequestParams();

    forkJoin({
      valuations: this.stockValuationService.getAllStockValuation(params),
      summary: this.stockValuationService.getStockValuationSummary(params)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ valuations, summary }) => {
          this.valuations.set(valuations.body ?? []);
          this.summary.set(summary.body ?? null);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
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

  /** Charge familles et rayons en parallèle — une seule fois au démarrage */
  private loadReferentielData(): void {
    forkJoin({
      familles: this.familleProduitService.query({ page: 0, size: 9999 }),
      rayons: this.rayonService.query({ page: 0, size: 9999 })
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ familles, rayons }) => {
          this.familleProduitOptions.set(familles.body ?? []);
          this.rayonOptions.set(rayons.body ?? []);
        }
      });
  }

  private buildRequestParams(): any {
    const params: any = {};
    if (this.selectedFamilleProduit()) {
      params["familleProduitId"] = this.selectedFamilleProduit()!.id;
    }
    if (this.selectedRayon()) {
      params["rayonId"] = this.selectedRayon()!.id;
    }

    return params;
  }
}
