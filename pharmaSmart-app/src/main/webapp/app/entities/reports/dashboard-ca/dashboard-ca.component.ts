import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";

import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";

import { ButtonComponent, SelectComponent, ToolbarComponent } from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from '../../../shared/util/warehouse-util';

import {
  IBasketEvolution,
  IDashboardCAEvolution,
  IDashboardCASummary,
  IPaymentMethodCA,
  IPaymentMethodSummary,
  IProductFamilyCA,
  IProductFamilySummary
} from "app/shared/model/report";
import { ITopProduct } from "app/shared/model/report/top-product.model";
import { DashboardCAService } from "../services/dashboard-ca.service";
import {
  FinancesDashboardApiService
} from "../../../features/finances/data-access/services/finances-dashboard-api.service";
import { IFinancesSummary } from "../../../features/finances/data-access/models";
import { formatCurrency, formatDate, formatPercent } from "app/shared/utils/format-utils";

import { Chart, ChartConfiguration, ChartData, registerables } from "chart.js";
import { BlobDownloadService } from "../../../shared/services/blob-download.service";

Chart.register(...registerables);

interface PeriodOption {
  label: string;
  value: string;
}

@Component({
  selector: "app-dashboard-ca",
  templateUrl: "./dashboard-ca.component.html",
  styleUrl: "./dashboard-ca.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SelectComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    RouterModule
  ]
})
export default class DashboardCAComponent implements OnInit, OnDestroy {
  @ViewChild("evolutionChartCanvas") evolutionChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("paymentChartCanvas") paymentChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("familyChartCanvas") familyChartCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild("basketChartCanvas") basketChartCanvas?: ElementRef<HTMLCanvasElement>;

  selectedPeriod = signal<string>("week");
  startDate = signal<NgbDateStruct>(this.getDefaultStartDate());
  endDate = signal<NgbDateStruct>(this.dateToNgbStruct(new Date()));
  summary = signal<IDashboardCASummary | null>(null);
  topProducts = signal<ITopProduct[]>([]);
  paymentMethodData = signal<IPaymentMethodSummary[]>([]);
  productFamilyData = signal<IProductFamilySummary[]>([]);
  basketEvolution = signal<IBasketEvolution | null>(null);
  isLoading = signal<boolean>(false);
  summaryFinances = signal<IFinancesSummary | null>(null);
  periodOptions: PeriodOption[] = [
    { label: "Aujourd'hui", value: "today" },
    { label: "7 derniers jours", value: "week" },
    { label: "30 derniers jours", value: "month" },
    { label: "Cette année", value: "year" },
    { label: "Période personnalisée", value: "custom" }
  ];
  // Format methods using shared utilities
  formatCurrency = formatCurrency;
  formatPercent = formatPercent;
  // Charts
  private evolutionChart?: Chart;
  private paymentChart?: Chart;
  private familyChart?: Chart;
  private basketChart?: Chart;
  private dashboardCAService = inject(DashboardCAService);
  private readonly financesDashboardApi = inject(FinancesDashboardApiService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {

    this.loadDashboard();
    this.financesDashboardApi.getSummaryFinances().subscribe({
      next: res => this.summaryFinances.set(res.body)
    });
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadDashboard(): void {
    this.isLoading.set(true);

    // Load summary KPIs
    this.dashboardCAService.getOverallSummary().subscribe({
      next: (res: HttpResponse<IDashboardCASummary>) => {
        this.summary.set(res.body);
      },
      error() {
        console.error("Error loading summary");
      }
    });

    // Calculate date range based on selected period
    const { start, end } = this.getDateRange();


    this.dashboardCAService.getEvolutionData("daily", start, end).subscribe({
      next: (res: HttpResponse<IDashboardCAEvolution>) => {
        if (res.body) {
          this.createEvolutionChart(res.body);
        }
      },
      error() {
        console.error("Error loading evolution data");
      }
    });


    this.dashboardCAService.getPaymentMethodDistribution(start, end).subscribe({
      next: (res: HttpResponse<IPaymentMethodCA[]>) => {
        const data = this.aggregatePaymentMethodData(res.body ?? []);
        this.paymentMethodData.set(data);
        this.createPaymentChart(data);
      },
      error() {
        console.error("Error loading payment method data");
      }
    });


    this.dashboardCAService.getProductFamilyDistribution(start, end).subscribe({
      next: (res: HttpResponse<IProductFamilyCA[]>) => {
        const data = this.aggregateProductFamilyData(res.body ?? []);
        this.productFamilyData.set(data);
        this.createFamilyChart(data);
      },
      error() {
        console.error("Error loading product family data");
      }
    });

    // Load top products
    this.dashboardCAService.getTopProducts(start, end, 10).subscribe({
      next: (res: HttpResponse<ITopProduct[]>) => {
        this.topProducts.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        console.error("Error loading top products");
      }
    });

    // Load basket evolution (GAP-C2) — always 12 months, independent of period filter
    this.dashboardCAService.getBasketEvolution().subscribe({
      next: res => {
        this.basketEvolution.set(res.body);
        if (res.body) this.createBasketChart(res.body);
      }
    });
  }

  onPeriodChange(): void {
    const period = this.selectedPeriod();
    if (period !== "custom") {
      const { start, end } = this.getDateRange();
      this.startDate.set(this.dateToNgbStruct(new Date(start)));
      this.endDate.set(this.dateToNgbStruct(new Date(end)));
    }
    this.loadDashboard();
  }

  // Chart creation methods

  onRefresh(): void {
    this.dashboardCAService.refreshViews().subscribe({
      next: () => {
        this.loadDashboard();
      },
      error() {
        console.error("Error refreshing views");
      }
    });
  }

  exportToPdf(): void {
    const { start, end } = this.getDateRange();
    this.dashboardCAService.exportDashboardToPdf(start, end)

      .subscribe(resp => {
        this.blobDownloadService.downloadPdf(resp.body, "dashboard-ca");

      });

  }


  exportDailySummaryToExcel(): void {
    const startDate = NGB_DATE_TO_ISO(this.startDate())!;
    const endDate = NGB_DATE_TO_ISO(this.endDate())!;

    this.dashboardCAService.exportDailySummaryToExcel(startDate, endDate)

      .subscribe(resp => {
        this.blobDownloadService.downloadExcel(resp.body, "dashboard-ca");

      });

  }


  exportDailySummaryToCsv(): void {
    const startDate = NGB_DATE_TO_ISO(this.startDate())!;
    const endDate = NGB_DATE_TO_ISO(this.endDate())!;

    this.dashboardCAService.exportDailySummaryToCsv(startDate, endDate)

      .subscribe(resp => {
        this.blobDownloadService.downloadCsv(resp.body, "dashboard-ca");
      });

  }


  private createEvolutionChart(data: IDashboardCAEvolution): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }

    if (!this.evolutionChartCanvas) {
      setTimeout(() => this.createEvolutionChart(data), 100);
      return;
    }

    const ctx = this.evolutionChartCanvas.nativeElement.getContext("2d");
    if (!ctx) {
      return;
    }

    const chartData: ChartData<"line"> = {
      labels: data.labels ?? [],
      datasets: [
        {
          label: "CA (FCFA)",
          data: data.caValues ?? [],
          borderColor: "#2196F3",
          backgroundColor: "rgba(33, 150, 243, 0.1)",
          tension: 0.4,
          fill: true
        },
        {
          label: "Nb Transactions",
          data: data.transactionCounts ?? [],
          borderColor: "#4CAF50",
          backgroundColor: "rgba(76, 175, 80, 0.1)",
          tension: 0.4,
          yAxisID: "y1"
        }
      ]
    };

    const config: ChartConfiguration<"line"> = {
      type: "line",
      data: chartData,
      options: {
        responsive: true,
        interaction: {
          mode: "index",
          intersect: false
        },
        plugins: {
          legend: {
            position: "top"
          },
          title: {
            display: true,
            text: "Évolution du Chiffre d'Affaires"
          }
        },
        scales: {
          y: {
            type: "linear",
            display: true,
            position: "left"
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            grid: {
              drawOnChartArea: false
            }
          }
        }
      }
    };

    this.evolutionChart = new Chart(ctx, config);
  }

  private createPaymentChart(data: IPaymentMethodSummary[]): void {
    if (this.paymentChart) {
      this.paymentChart.destroy();
    }

    if (!this.paymentChartCanvas) {
      setTimeout(() => this.createPaymentChart(data), 100);
      return;
    }

    const ctx = this.paymentChartCanvas.nativeElement.getContext("2d");
    if (!ctx) {
      return;
    }

    const chartData: ChartData<"pie"> = {
      labels: data.map(d => d.paymentMethod),
      datasets: [
        {
          data: data.map(d => d.montantTotal),
          backgroundColor: ["#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0", "#00BCD4"]
        }
      ]
    };

    const config: ChartConfiguration<"pie"> = {
      type: "pie",
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: "right"
          },
          title: {
            display: true,
            text: "Répartition CA par Mode de Paiement"
          }
        }
      }
    };

    this.paymentChart = new Chart(ctx, config);
  }

  private createFamilyChart(data: IProductFamilySummary[]): void {
    if (this.familyChart) {
      this.familyChart.destroy();
    }

    if (!this.familyChartCanvas) {
      setTimeout(() => this.createFamilyChart(data), 100);
      return;
    }

    const ctx = this.familyChartCanvas.nativeElement.getContext("2d");
    if (!ctx) {
      return;
    }

    const chartData: ChartData<"bar"> = {
      labels: data.map(d => d.famille),
      datasets: [
        {
          label: "CA (FCFA)",
          data: data.map(d => d.caTotal),
          backgroundColor: "#2196F3"
        },
        {
          label: "Marge Brute (FCFA)",
          data: data.map(d => d.margeBrute),
          backgroundColor: "#4CAF50"
        }
      ]
    };

    const config: ChartConfiguration<"bar"> = {
      type: "bar",
      data: chartData,
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: "top"
          },
          title: {
            display: true,
            text: "CA par Famille de Produits"
          }
        }
      }
    };

    this.familyChart = new Chart(ctx, config);
  }

  private createBasketChart(data: IBasketEvolution): void {
    if (this.basketChart) this.basketChart.destroy();

    if (!this.basketChartCanvas) {
      setTimeout(() => this.createBasketChart(data), 100);
      return;
    }

    const ctx = this.basketChartCanvas.nativeElement.getContext("2d");
    if (!ctx) return;

    const config: ChartConfiguration<"line"> = {
      type: "line",
      data: {
        labels: data.labels ?? [],
        datasets: [{
          label: "Panier moyen (FCFA)",
          data: data.values ?? [],
          borderColor: "#2196F3",
          backgroundColor: "rgba(33, 150, 243, 0.08)",
          tension: 0.4,
          fill: true,
          pointRadius: 3,
          pointHoverRadius: 5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: false, ticks: { font: { size: 10 } } },
          x: { ticks: { font: { size: 10 } } }
        }
      }
    };

    this.basketChart = new Chart(ctx, config);
  }

  private destroyCharts(): void {
    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }
    if (this.paymentChart) {
      this.paymentChart.destroy();
    }
    if (this.familyChart) {
      this.familyChart.destroy();
    }
    if (this.basketChart) {
      this.basketChart.destroy();
    }
  }

  private getDefaultStartDate(): NgbDateStruct {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return this.dateToNgbStruct(date);
  }

  private dateToNgbStruct(date: Date): NgbDateStruct {
    return { year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate() };
  }

  private ngbStructToDate(struct: NgbDateStruct): Date {
    return new Date(struct.year, struct.month - 1, struct.day);
  }

  private getDateRange(): { start: string; end: string } {
    const period = this.selectedPeriod();
    const end = new Date();
    let start = new Date();

    switch (period) {
      case "today":
        start = new Date();
        break;
      case "week":
        start.setDate(end.getDate() - 7);
        break;
      case "month":
        start.setDate(end.getDate() - 30);
        break;
      case "year":
        start.setFullYear(end.getFullYear(), 0, 1);
        break;
      case "custom": {
        const customEnd = this.ngbStructToDate(this.endDate());
        start = this.ngbStructToDate(this.startDate());
        end.setTime(customEnd.getTime());
        break;
      }
    }

    return {
      start: formatDate(start),
      end: formatDate(end)
    };
  }

  private aggregatePaymentMethodData(data: IPaymentMethodCA[]): IPaymentMethodSummary[] {
    const map = new Map<string, { total: number; count: number; code: string }>();

    data.forEach(item => {
      const key = item.paymentMethod ?? "Unknown";
      const existing = map.get(key) || { total: 0, count: 0, code: item.paymentCode ?? "" };
      existing.total += item.montantTotal ?? 0;
      existing.count += item.nbPayments ?? 0;
      map.set(key, existing);
    });

    const totalCA = Array.from(map.values()).reduce((sum, item) => sum + item.total, 0);

    return Array.from(map.entries())
      .map(([method, data]) => ({
        paymentMethod: method,
        paymentCode: data.code,
        montantTotal: data.total,
        nbPayments: data.count,
        percentage: totalCA > 0 ? (data.total * 100) / totalCA : 0
      }))
      .sort((a, b) => b.montantTotal - a.montantTotal);
  }

  private aggregateProductFamilyData(data: IProductFamilyCA[]): IProductFamilySummary[] {
    const map = new Map<string, { ca: number; marge: number; tauxMarge: number; count: number }>();

    data.forEach(item => {
      const key = item.famille ?? "Non classé";
      const existing = map.get(key) || { ca: 0, marge: 0, tauxMarge: 0, count: 0 };
      existing.ca += item.caTotal ?? 0;
      existing.marge += item.margeBrute ?? 0;
      existing.count++;
      map.set(key, existing);
    });

    const totalCA = Array.from(map.values()).reduce((sum, item) => sum + item.ca, 0);

    return Array.from(map.entries())
      .map(([famille, data]) => ({
        famille,
        caTotal: data.ca,
        margeBrute: data.marge,
        tauxMargePct: data.ca > 0 ? (data.marge * 100) / data.ca : 0,
        percentage: totalCA > 0 ? (data.ca * 100) / totalCA : 0
      }))
      .sort((a, b) => b.caTotal - a.caTotal)
      .slice(0, 10); // Top 10
  }
}
