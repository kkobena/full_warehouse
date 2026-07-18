import { Component, computed, ElementRef, inject, OnInit, signal, ViewChild, ChangeDetectionStrategy } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";

import { ButtonModule } from "primeng/button";
import { SelectModule } from "primeng/select";
import { ToolbarModule } from "primeng/toolbar";

import {
  IComparativeByFamily,
  IComparativeByFournisseur,
  IComparativeByType,
  IComparativeCA,
  IComparativeSummary
} from "app/shared/model/report/comparative-report.model";
import { ComparativeReportService } from "../services/comparative-report.service";
import { formatCurrency } from "app/shared/utils/format-utils";

import { Chart, ChartConfiguration, ChartData, registerables } from "chart.js";
import { BlobDownloadService } from "../../../shared/services/blob-download.service";

Chart.register(...registerables);

type ActiveView = "global" | "famille" | "fournisseur";
type FamilySortColumn = "currentYearCA" | "previousYearCA" | "evolutionPct";

interface ComparisonTypeOption {
  label: string;
  value: string;
}

@Component({
  selector: "app-comparative-analysis",
  templateUrl: "./comparative-analysis.component.html",
  styleUrl: "./comparative-analysis.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule, ToolbarModule]
})
export default class ComparativeAnalysisComponent implements OnInit {
  @ViewChild("evolutionChartCanvas") evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("typeChartCanvas") typeChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("familyChartCanvas") familyChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("fournisseurChartCanvas") fournisseurChartCanvas?: ElementRef<HTMLCanvasElement>;

  // Data signals
  protected summary = signal<IComparativeSummary | null>(null);
  protected comparisons = signal<IComparativeCA[]>([]);
  protected byType = signal<IComparativeByType[]>([]);
  protected byFamily = signal<IComparativeByFamily[]>([]);
  protected byFournisseur = signal<IComparativeByFournisseur[]>([]);
  protected isLoading = signal<boolean>(false);
  protected isFamilyLoading = signal<boolean>(false);
  protected isFournisseurLoading = signal<boolean>(false);

  // View
  protected activeView = signal<ActiveView>("global");

  // Family sort
  protected familySortColumn = signal<FamilySortColumn>("evolutionPct");
  protected familySortAsc = signal<boolean>(false);

  protected sortedFamily = computed(() => {
    const col = this.familySortColumn();
    const asc = this.familySortAsc();
    return [...this.byFamily()].sort((a, b) => {
      const va = (a[col] as number) ?? 0;
      const vb = (b[col] as number) ?? 0;
      return asc ? va - vb : vb - va;
    });
  });

  // Fournisseur sort
  protected fournisseurSortColumn = signal<FamilySortColumn>("currentYearCA");
  protected fournisseurSortAsc = signal<boolean>(false);
  protected sortedFournisseur = computed(() => {
    const col = this.fournisseurSortColumn();
    const asc = this.fournisseurSortAsc();
    return [...this.byFournisseur()].sort((a, b) => {
      const va = (a[col] as number) ?? 0;
      const vb = (b[col] as number) ?? 0;
      return asc ? va - vb : vb - va;
    });
  });

  // Filters
  protected selectedComparisonType = signal<string>("MONTHLY");
  protected selectedYear = signal<number>(new Date().getFullYear());
  protected comparisonTypeOptions: ComparisonTypeOption[] = [
    { label: "Mensuel", value: "MONTHLY" },
    { label: "Trimestriel", value: "QUARTERLY" },
    { label: "Annuel", value: "YEARLY" }
  ];
  protected yearOptions: number[] = [];

  protected formatCurrency = formatCurrency;

  private evolutionChart?: Chart;
  private typeChart?: Chart;
  private familyChart?: Chart;
  private fournisseurChart?: Chart;
  private comparativeReportService = inject(ComparativeReportService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    const currentYear = new Date().getFullYear();
    for (let i = 0; i < 6; i++) {
      this.yearOptions.push(currentYear - i);
    }
    this.loadData();
  }

  protected onViewChange(view: ActiveView): void {
    this.activeView.set(view);
    if (view === "famille" && this.byFamily().length === 0) {
      this.loadFamilyData();
    }
    if (view === "fournisseur" && this.byFournisseur().length === 0) {
      this.loadFournisseurData();
    }
  }

  protected onFamilySort(col: FamilySortColumn): void {
    if (this.familySortColumn() === col) {
      this.familySortAsc.update(v => !v);
    } else {
      this.familySortColumn.set(col);
      this.familySortAsc.set(false);
    }
  }

  protected familySortIcon(col: FamilySortColumn): string {
    if (this.familySortColumn() !== col) return "pi pi-sort-alt";
    return this.familySortAsc() ? "pi pi-sort-amount-up" : "pi pi-sort-amount-down";
  }

  protected onFournisseurSort(col: FamilySortColumn): void {
    if (this.fournisseurSortColumn() === col) {
      this.fournisseurSortAsc.update(v => !v);
    } else {
      this.fournisseurSortColumn.set(col);
      this.fournisseurSortAsc.set(false);
    }
  }

  protected fournisseurSortIcon(col: FamilySortColumn): string {
    if (this.fournisseurSortColumn() !== col) return "pi pi-sort-alt";
    return this.fournisseurSortAsc() ? "pi pi-sort-amount-up" : "pi pi-sort-amount-down";
  }

  protected loadData(): void {
    this.isLoading.set(true);

    this.comparativeReportService.getComparativeSummary().subscribe({
      next: (res: HttpResponse<IComparativeSummary>) => {
        this.summary.set(res.body);
      },
      error() {
        console.error("Error loading summary");
      }
    });

    const year = this.selectedYear();
    const comparisonType = this.selectedComparisonType();

    if (comparisonType === "MONTHLY") {
      this.comparativeReportService.getMonthlyComparison(year).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        }
      });
    } else if (comparisonType === "QUARTERLY") {
      this.comparativeReportService.getQuarterlyComparison(year).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        }
      });
    } else {
      const startDate = `${year - 5}-01-01`;
      const endDate = `${year}-12-31`;
      this.comparativeReportService.getYearlyComparison(startDate, endDate).subscribe({
        next: (res: HttpResponse<IComparativeCA[]>) => {
          this.comparisons.set(res.body ?? []);
          this.createEvolutionChart(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        }
      });
    }

    this.comparativeReportService.getComparisonBySalesType(year, year - 1).subscribe({
      next: (res: HttpResponse<IComparativeByType[]>) => {
        this.byType.set(res.body ?? []);
        this.createTypeChart(res.body ?? []);
      },
      error() {
        console.error("Error loading sales type comparison");
      }
    });
  }

  protected onComparisonTypeChange(): void {
    this.loadData();
  }

  protected onYearChange(): void {
    this.byFamily.set([]);
    this.byFournisseur.set([]);
    this.loadData();
    if (this.activeView() === "famille") {
      this.loadFamilyData();
    }
    if (this.activeView() === "fournisseur") {
      this.loadFournisseurData();
    }
  }

  protected exportToPdf(): void {
    // handleBlobForTauri(resp.body, `tableaux-comparatifs_${comparisonType.toLowerCase()}_${year}`);
    const comparisonType = this.selectedComparisonType();
    const year = this.selectedYear();
    this.comparativeReportService.exportToPdf(comparisonType, year).subscribe(resp => {
      this.blobDownloadService.downloadPdf(resp.body, "tableaux-comparatifs");

    });
  }

  protected getEvolutionClass(value: number | undefined): string {
    if (!value) return "";
    return value >= 0 ? "evolution-positive" : "evolution-negative";
  }

  protected formatPercent(value: number | undefined): string {
    if (value === undefined || value === null) return "0";
    const sign = value >= 0 ? "+" : "";
    return (
      sign +
      new Intl.NumberFormat("fr-FR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(value)
    );
  }

  private loadFournisseurData(): void {
    const year = this.selectedYear();
    this.isFournisseurLoading.set(true);
    this.comparativeReportService.getComparisonByFournisseur(year, year - 1).subscribe({
      next: res => {
        this.byFournisseur.set(res.body ?? []);
        this.createFournisseurChart(res.body ?? []);
        this.isFournisseurLoading.set(false);
      },
      error: () => {
        this.isFournisseurLoading.set(false);
      }
    });
  }

  private loadFamilyData(): void {
    const year = this.selectedYear();
    this.isFamilyLoading.set(true);
    this.comparativeReportService.getComparisonByFamily(year, year - 1).subscribe({
      next: res => {
        this.byFamily.set(res.body ?? []);
        this.createFamilyChart(res.body ?? []);
        this.isFamilyLoading.set(false);
      },
      error: () => {
        this.isFamilyLoading.set(false);
      }
    });
  }

  private createEvolutionChart(data: IComparativeCA[]): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }
    if (!this.evolutionChartCanvas) {
      setTimeout(() => this.createEvolutionChart(data), 100);
      return;
    }
    const ctx = this.evolutionChartCanvas.nativeElement.getContext("2d");
    if (!ctx) return;

    const chartData: ChartData<"bar"> = {
      labels: data.map(d => d.periodLabel ?? ""),
      datasets: [
        { label: "CA Actuel", data: data.map(d => d.currentCA ?? 0), backgroundColor: "#2196F3" },
        { label: "CA Précédent", data: data.map(d => d.previousCA ?? 0), backgroundColor: "#FF9800" }
      ]
    };
    const config: ChartConfiguration<"bar"> = {
      type: "bar",
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: { position: "top" },
          title: { display: true, text: "Évolution Comparative du CA" }
        },
        scales: { y: { beginAtZero: true } }
      }
    };
    this.evolutionChart = new Chart(ctx, config);
  }

  private createTypeChart(data: IComparativeByType[]): void {
    if (this.typeChart) {
      this.typeChart.destroy();
    }
    if (!this.typeChartCanvas) {
      setTimeout(() => this.createTypeChart(data), 100);
      return;
    }
    const ctx = this.typeChartCanvas.nativeElement.getContext("2d");
    if (!ctx) return;

    const year = this.selectedYear();
    const chartData: ChartData<"bar"> = {
      labels: data.map(d => d.saleTypeLabel ?? ""),
      datasets: [
        { label: `CA ${year}`, data: data.map(d => d.currentYearCA ?? 0), backgroundColor: "#4CAF50" },
        { label: `CA ${year - 1}`, data: data.map(d => d.previousYearCA ?? 0), backgroundColor: "#9C27B0" }
      ]
    };
    const config: ChartConfiguration<"bar"> = {
      type: "bar",
      data: chartData,
      options: {
        responsive: true,
        plugins: { legend: { position: "top" }, title: { display: true, text: "CA par Type de Vente" } }
      }
    };
    this.typeChart = new Chart(ctx, config);
  }

  private createFournisseurChart(data: IComparativeByFournisseur[]): void {
    if (this.fournisseurChart) {
      this.fournisseurChart.destroy();
    }
    if (!this.fournisseurChartCanvas) {
      setTimeout(() => this.createFournisseurChart(data), 100);
      return;
    }
    const ctx = this.fournisseurChartCanvas.nativeElement.getContext("2d");
    if (!ctx) return;

    const year = this.selectedYear();
    const top15 = [...data].sort((a, b) => (b.currentYearCA ?? 0) - (a.currentYearCA ?? 0)).slice(0, 15);

    const config: ChartConfiguration<"bar"> = {
      type: "bar",
      data: {
        labels: top15.map(d => d.fournisseurLibelle ?? ""),
        datasets: [
          { label: `CA ${year}`, data: top15.map(d => d.currentYearCA ?? 0), backgroundColor: "#4CAF50" },
          { label: `CA ${year - 1}`, data: top15.map(d => d.previousYearCA ?? 0), backgroundColor: "#FF9800" }
        ]
      },
      options: {
        indexAxis: "y",
        responsive: true,
        plugins: {
          legend: { position: "top" },
          title: { display: true, text: `Top 15 fournisseurs — ${year} vs ${year - 1}` }
        },
        scales: { x: { beginAtZero: true } }
      }
    };
    this.fournisseurChart = new Chart(ctx, config);
  }

  private createFamilyChart(data: IComparativeByFamily[]): void {
    if (this.familyChart) {
      this.familyChart.destroy();
    }
    if (!this.familyChartCanvas) {
      setTimeout(() => this.createFamilyChart(data), 100);
      return;
    }
    const ctx = this.familyChartCanvas.nativeElement.getContext("2d");
    if (!ctx) return;

    const year = this.selectedYear();
    const top15 = [...data].sort((a, b) => (b.currentYearCA ?? 0) - (a.currentYearCA ?? 0)).slice(0, 15);

    const config: ChartConfiguration<"bar"> = {
      type: "bar",
      data: {
        labels: top15.map(d => d.familleLibelle ?? ""),
        datasets: [
          { label: `CA ${year}`, data: top15.map(d => d.currentYearCA ?? 0), backgroundColor: "#2196F3" },
          { label: `CA ${year - 1}`, data: top15.map(d => d.previousYearCA ?? 0), backgroundColor: "#FF9800" }
        ]
      },
      options: {
        indexAxis: "y",
        responsive: true,
        plugins: {
          legend: { position: "top" },
          title: { display: true, text: `Top 15 familles — ${year} vs ${year - 1}` }
        },
        scales: { x: { beginAtZero: true } }
      }
    };
    this.familyChart = new Chart(ctx, config);
  }
}
