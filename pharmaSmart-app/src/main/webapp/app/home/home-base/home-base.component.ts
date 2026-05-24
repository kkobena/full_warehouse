import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { TableModule } from "primeng/table";
import {
  VenteByTypeRecord,
  VenteModePaimentRecord,
  VenteRecord,
  VenteRecordWrapper
} from "../../shared/model/vente-record.model";
import { ProductStatParetoRecord, ProductStatRecord } from "../../shared/model/produit-record.model";
import { AchatRecord } from "../../shared/model/achat-record.model";
import { CaPeriodeFilter } from "../../shared/model/enumerations/ca-periode-filter.model";
import { TOPS } from "../../shared/constants/pagination.constants";
import { DashboardService } from "../dashboard.service";
import { ProduitStatService } from "../../entities/produit/stat/produit-stat.service";
import { TypeCa } from "../../shared/model/enumerations/type-ca.model";
import { OrderBy } from "../../shared/model/enumerations/type-vente.model";
import { forkJoin, interval } from "rxjs";
import { FormsModule } from "@angular/forms";
import { TiersPayantService } from "../../entities/tiers-payant/tierspayant.service";
import { TiersPayantAchat } from "../../entities/tiers-payant/model/tiers-payant-achat.model";
import { ChartModule } from "primeng/chart";
import { ToggleButtonChangeEvent, ToggleButtonModule } from "primeng/togglebutton";
import { SelectChangeEvent } from 'primeng/select';
import {
  backgroundColor,
  hoverBackgroundColor,
  surfaceBorder,
  textColor,
  textColorSecondary
} from "../../shared/chart-color-helper";
import { ToggleStateService } from "./toggle-state.service";
import { SelectModule } from "primeng/select";
import { Router, RouterModule } from "@angular/router";
import { AlertBadgeService } from "../../shared/services/alert-badge.service";
import { SkeletonModule } from "primeng/skeleton";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
// Report services
import { MargeReportService } from "../../entities/reports/services/marge-report.service";
import { DashboardCAService } from "../../entities/reports/services/dashboard-ca.service";
import { StockValuationReportService } from "../../entities/reports/services/stock-valuation-report.service";
import { TiersPayantReportService } from "../../entities/reports/services/tiers-payant-report.service";
import { SupplierPerformanceReportService } from "../../entities/reports/services/supplier-performance-report.service";
// Report models
import {
  IDashboardCASummary,
  IMargeSummary,
  IStockValuationSummary,
  ISupplierPerformance,
  ISupplierPerformanceSummary,
  ITiersPayantCreancesSummary
} from "../../shared/model/report";
// Différés & Facturation
import { DiffereApiService } from "../../features/differes/data-access/services/differe-api.service";
import { IDiffereSummary } from "../../features/differes/data-access/models";
import { FactureApiService } from "../../features/facturation/data-access/services/facture-api.service";
import { IFacturationKpi } from "../../features/facturation/data-access/models";

interface TopSelection {
  label: string;
  value: number;
}

interface PeriodOption {
  label: string;
  value: CaPeriodeFilter;
  icon: string;
}

@Component({
  selector: "jhi-home-base",
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    TableModule,
    ChartModule,
    ToggleButtonModule,
    SelectModule,
    SkeletonModule,
    ButtonModule,
    TooltipModule
  ],
  templateUrl: "./home-base.component.html",
  styleUrl: "./home-base.component.scss"
})
export class HomeBaseComponent implements OnInit {

  protected readonly tops: TopSelection[] = TOPS;

  // Alert counters — via AlertBadgeService (signaux)
  protected readonly alertBadgeService = inject(AlertBadgeService);

  get peremptionCount(): number {
    return this.alertBadgeService.peremptionCount();
  }

  get ruptureCount(): number {
    return this.alertBadgeService.ruptureCount();
  }

  get ajustementCount(): number {
    return this.alertBadgeService.ajustementCount();
  }

  get prixModifCount(): number {
    return this.alertBadgeService.prixModifCount();
  }

  get urgentCount(): number {
    return this.alertBadgeService.urgentCount();
  }

  // ─── Évolution CA selon la période active ───────────────────────
  get caEvolutionPct(): number | null | undefined {
    switch (this.activePeriode()) {
      case CaPeriodeFilter.daily:     return this.caSummary?.caTodayEvolutionPct;
      case CaPeriodeFilter.weekly:    return this.caSummary?.caWeekEvolutionPct;
      case CaPeriodeFilter.monthly:   return this.caSummary?.caMonthEvolutionPct;
      case CaPeriodeFilter.yearly:    return this.caSummary?.caYearEvolutionPct;
      default:                        return null; // halfyearly : pas de champ dédié
    }
  }



  // ─── Période (P2) ───────────────────────────────────────────────
  protected readonly periodeOptions: PeriodOption[] = [
    { label: "Auj.", value: CaPeriodeFilter.daily, icon: "pi pi-sun" },
    { label: "Semaine", value: CaPeriodeFilter.weekly, icon: "pi pi-calendar" },
    { label: "Mois", value: CaPeriodeFilter.monthly, icon: "pi pi-calendar-plus" },
    { label: "Semestre", value: CaPeriodeFilter.halfyearly, icon: "pi pi-chart-bar" },
    { label: "Année", value: CaPeriodeFilter.yearly, icon: "pi pi-chart-line" }
  ];

  protected activePeriode = signal<CaPeriodeFilter>(CaPeriodeFilter.daily);
  protected isLoading = signal(false);
  protected lastUpdate = signal<Date | null>(null);
  protected activePareto: "qty" | "amt" = "qty";

  // ─── State ──────────────────────────────────────────────────────
  protected venteRecord: VenteRecord | null = null;
  protected canceled: VenteRecord | null = null;
  protected rowQuantity: ProductStatRecord[] = [];
  protected rowAmount: ProductStatRecord[] = [];
  protected row20x80: ProductStatParetoRecord[] = [];
  protected row20x80Montant: ProductStatParetoRecord[] = [];
  protected achatRecord: AchatRecord | null = null;
  protected assurance: VenteRecord | null = null;
  protected vno: VenteRecord | null = null;
  protected venteModePaiments: VenteModePaimentRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter | null = CaPeriodeFilter.daily;
  protected TOP_MAX_QUANTITY: TopSelection;
  protected TOP_MAX_AMOUNT: TopSelection;
  protected TOP_MAX_TP: TopSelection;
  protected totalAmountTopQuantity = 0;
  protected totalQuantityToQuantity = 0;
  protected totalAmountTopAmount = 0;
  protected totalQuantityTopAmount = 0;
  protected totalAmount20x80 = 0;
  protected totalQuantityAvg = 0;
  protected totalAmountAvg = 0;
  protected totalQuantity20x80 = 0;
  protected tiersPayantAchat: TiersPayantAchat[] = [];

  // ─── KPI P1 ─────────────────────────────────────────────────────
  protected margeSummary: IMargeSummary | null = null;
  protected caSummary: IDashboardCASummary | null = null;
  protected stockValuationSummary: IStockValuationSummary | null = null;
  protected creancesSummary: ITiersPayantCreancesSummary[] = [];
  protected totalCreances = 0;
  protected creancesPlusDe90j = 0;
  // ─── KPI P2 ─────────────────────────────────────────────────────
  protected differeSummary: IDiffereSummary | null = null;
  protected facturationKpi: IFacturationKpi | null = null;

  // ─── Fournisseurs P3 ────────────────────────────────────────────
  protected topFournisseurs: ISupplierPerformance[] = [];
  protected supplierSummary: ISupplierPerformanceSummary | null = null;
  protected fournisseurPeriod: "30d" | "12m" = "30d";
  protected TOP_MAX_FOURNISSEUR: TopSelection;
  protected fournisseurChartData: any;
  protected fournisseurChartOptions: any;

  // ─── Charts ──────────────────────────────────────────────────────
  protected readonly toggleStateService = inject(ToggleStateService);
  protected showGraphs = false;
  protected quantityChartData: any;
  protected quantityChartOptions: any;
  protected amountChartData: any;
  protected amountChartOptions: any;
  protected twentyEightyChartData: any;
  protected twentyEightyMontantChartData: any;
  protected twentyEightyChartOptions: any;
  protected modePaimentChartData: any;
  protected modePaimentChartOptions: any;
  protected tiersPayantChartData: any;
  protected tiersPayantChartOptions: any;

  private readonly destroyRef = inject(DestroyRef);
  private readonly dashboardService = inject(DashboardService);
  private readonly produitStatService = inject(ProduitStatService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly router = inject(Router);
  private readonly margeReportService = inject(MargeReportService);
  private readonly dashboardCAService = inject(DashboardCAService);
  private readonly stockValuationReportService = inject(StockValuationReportService);
  private readonly tiersPayantReportService = inject(TiersPayantReportService);
  private readonly supplierService = inject(SupplierPerformanceReportService);
  private readonly differeApiService = inject(DiffereApiService);
  private readonly factureApiService = inject(FactureApiService);

  private documentStyle: CSSStyleDeclaration;
  private textColor: string;
  private textColorSecondary: string;
  private surfaceBorder: string;

  constructor() {
    this.TOP_MAX_QUANTITY = this.tops[1];
    this.TOP_MAX_AMOUNT = this.tops[1];
    this.TOP_MAX_TP = this.tops[1];
    this.TOP_MAX_FOURNISSEUR = this.tops[0];
  }

  ngOnInit(): void {
    this.initializeChartStyles();
    this.loadDashboardData();
    this.alertBadgeService.init();
    interval(120000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadDashboardData();
        this.alertBadgeService.refresh();
      });
  }

  protected onPeriodeChange(p: CaPeriodeFilter): void {
    this.activePeriode.set(p);
    this.dashboardPeriode = p;
    this.showGraphs = this.toggleStateService.toggleState();
    this.loadDashboardData();
  }

  protected loadDashboardData(): void {
    this.isLoading.set(true);
    const sources = {
      ca: this.dashboardService.fetchCa({
        categorieChiffreAffaire: TypeCa.CA,
        dashboardPeriode: this.dashboardPeriode
      }),
      caAchat: this.dashboardService.fetchCaAchat({ dashboardPeriode: this.dashboardPeriode }),
      caTypeVente: this.dashboardService.fetchCaByTypeVente({ dashboardPeriode: this.dashboardPeriode }),
      byModePaiment: this.dashboardService.getCaByModePaiment({ dashboardPeriode: this.dashboardPeriode }),
      produitCa: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        size: this.TOP_MAX_QUANTITY?.value
      }),
      produitAmount: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        size: this.TOP_MAX_AMOUNT?.value
      }),
      twentyEighty: this.produitStatService.fetch20x80({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD
      }),
      twentyEightyMontant: this.produitStatService.fetch20x80({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT
      }),
      tiersPayantAchat: this.tiersPayantService.fetchAchatTiersPayant({
        dashboardPeriode: this.dashboardPeriode,
        limit: this.TOP_MAX_TP?.value
      }),
      // P1 — KPI services
      margeSummary: this.margeReportService.getMargeSummary(),
      caSummary: this.dashboardCAService.getOverallSummary(),
      stockValuation: this.stockValuationReportService.getStockValuationSummary(),
      creancesSummary: this.tiersPayantReportService.getCreancesSummary(),
      // P3 — Fournisseurs
      topFournisseurs: this.supplierService.getTopSuppliersByVolume(this.TOP_MAX_FOURNISSEUR.value),
      supplierSummary: this.supplierService.getSupplierPerformanceSummary(),
      // P2 — Différés & Facturation
      differeSummary: this.differeApiService.getDiffereSummary({}),
      facturationKpi: this.factureApiService.getKpi({})
    };

    forkJoin(sources).subscribe({
      next: data => {
        this.onCaSuccess(data.ca.body);
        this.onCaAchatSuccess(data.caAchat.body);
        this.onCaByTypeVenteSuccess(data.caTypeVente.body);
        this.onGetCaByModePaimentSuccess(data.byModePaiment.body);
        this.onFetchPoduitCaSuccess(data.produitCa.body);
        this.onFetchPoduitAmountSuccess(data.produitAmount.body);
        this.onFetch20x80Success(data.twentyEighty.body);
        this.onFetch20x80AmountSuccess(data.twentyEightyMontant.body);
        this.onFetchTiersPayantSuccess(data.tiersPayantAchat.body);
        // P1
        this.margeSummary = data.margeSummary.body;
        this.caSummary = data.caSummary.body;
        this.stockValuationSummary = data.stockValuation.body;
        this.creancesSummary = data.creancesSummary.body ?? [];
        this.totalCreances = this.creancesSummary.reduce((s, c) => s + (c.montantTotal ?? 0), 0);
        this.creancesPlusDe90j = this.creancesSummary.reduce((s, c) => s + (c.montantPlusDe90Jours ?? 0), 0);
        // P3
        this.topFournisseurs = data.topFournisseurs.body ?? [];
        this.supplierSummary = data.supplierSummary.body;
        // P2
        this.differeSummary = data.differeSummary.body;
        this.facturationKpi = data.facturationKpi.body;
        this.buildAllCharts();
        this.isLoading.set(false);
        this.lastUpdate.set(new Date());
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  protected onTopQuantityChange(event: SelectChangeEvent): void {
    const top: TopSelection = event.value;
    this.TOP_MAX_QUANTITY = top;
    this.produitStatService
      .fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        size: top.value
      })
      .subscribe(res => {
        this.onFetchPoduitCaSuccess(res.body);
        this.buildQuantityChart();
      });
  }

  protected onTopAmountChange(event: SelectChangeEvent): void {
    const top: TopSelection = event.value;
    this.TOP_MAX_AMOUNT = top;
    this.produitStatService
      .fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        size: top.value
      })
      .subscribe(res => {
        this.onFetchPoduitAmountSuccess(res.body);
        this.buildAmountChart();
      });
  }

  protected onToggleChange(evt: ToggleButtonChangeEvent): void {
    this.toggleStateService.update(evt.checked);
  }

  protected onTopTiersPayantChange(event: SelectChangeEvent): void {
    const top: TopSelection = event.value;
    this.TOP_MAX_TP = top;
    this.tiersPayantService
      .fetchAchatTiersPayant({ dashboardPeriode: this.dashboardPeriode, limit: top.value })
      .subscribe(res => {
        this.onFetchTiersPayantSuccess(res.body);
        this.buildTiersPayantChart();
      });
  }

  protected onTopFournisseurChange(event: SelectChangeEvent): void {
    const top: TopSelection = event.value;
    this.TOP_MAX_FOURNISSEUR = top;
    this.supplierService.getTopSuppliersByVolume(top.value)
      .subscribe(res => {
        this.topFournisseurs = res.body ?? [];
        this.buildFournisseurChart();
      });
  }

  protected buildFournisseurChart(): void {
    const items = this.topFournisseurs.slice(0, this.TOP_MAX_FOURNISSEUR.value);
    const amounts = this.fournisseurPeriod === "30d"
      ? items.map(f => f.purchaseAmountLast30Days ?? 0)
      : items.map(f => f.purchaseAmountLast12Months ?? 0);
    this.fournisseurChartData = {
      labels: items.map(f => f.fournisseurName?.slice(0, 18) ?? ""),
      datasets: [{
        data: amounts,
        backgroundColor: ["#008cba", "#5bc0de", "#43ac6a", "#e99002", "#f04124"],
        borderWidth: 2
      }]
    };
    this.fournisseurChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "right", labels: { boxWidth: 12, font: { size: 10 } } } }
    };
  }

  private onCaSuccess(ca: VenteRecordWrapper | null): void {
    if (!ca) return;
    this.venteRecord = ca.close;
    this.canceled = ca.canceled;
  }

  private onCaAchatSuccess(achatRecordIn: AchatRecord | null): void {
    this.achatRecord = achatRecordIn;
  }

  private onCaByTypeVenteSuccess(venteByTypeRecords: VenteByTypeRecord[] | null): void {
    if (!venteByTypeRecords) return;
    this.vno = venteByTypeRecords.find(e => e.typeVente === "CashSale")?.venteRecord;
    this.assurance = venteByTypeRecords.find(e => e.typeVente === "ThirdPartySales")?.venteRecord;
  }

  private onGetCaByModePaimentSuccess(venteModePaimentRecords: VenteModePaimentRecord[] | []): void {
    this.venteModePaiments = venteModePaimentRecords;
  }

  private onFetchTiersPayantSuccess(tps: TiersPayantAchat[] | []): void {
    this.tiersPayantAchat = tps;
  }

  private onFetchPoduitCaSuccess(productStatRecords: ProductStatRecord[] | []): void {
    this.rowQuantity = productStatRecords;
    this.computeAmountTopQuantity();
  }

  private onFetchPoduitAmountSuccess(productStatRecords: ProductStatRecord[] | []): void {
    this.rowAmount = productStatRecords;
    this.computeAmountTopAmount();
  }

  private onFetch20x80AmountSuccess(productStatRecords: ProductStatParetoRecord[] | []): void {
    this.row20x80Montant = productStatRecords;
    this.computeAmountrow20x80Amount();
  }

  private onFetch20x80Success(productStatRecords: ProductStatParetoRecord[] | []): void {
    this.row20x80 = productStatRecords;
    this.computeAmountrow20x80();
  }

  private computeAmountTopQuantity(): void {
    this.totalQuantityToQuantity = this.rowQuantity.reduce((sum, p) => sum + p.quantitySold, 0);
    this.totalAmountTopQuantity = this.rowQuantity.reduce((sum, p) => sum + p.montantHt, 0);
  }

  private computeAmountTopAmount(): void {
    this.totalQuantityTopAmount = this.rowAmount.reduce((sum, p) => sum + p.quantitySold, 0);
    this.totalAmountTopAmount = this.rowAmount.reduce((sum, p) => sum + p.montantHt, 0);
  }

  private computeAmountrow20x80Amount(): void {
    if (this.row20x80Montant?.length) this.totalAmount20x80 = this.row20x80Montant[0].totalGlobal;
    this.totalAmountAvg = this.row20x80Montant?.reduce((sum, p) => sum + p.pourcentage, 0);
  }

  private computeAmountrow20x80(): void {
    if (this.row20x80?.length) this.totalQuantity20x80 = this.row20x80[0].totalGlobal;
    this.totalQuantityAvg = this.row20x80?.reduce((sum, p) => sum + p.pourcentage, 0);
  }

  private initializeChartStyles(): void {
    this.documentStyle = getComputedStyle(document.documentElement);
    this.textColor = textColor(this.documentStyle);
    this.textColorSecondary = textColorSecondary(this.documentStyle);
    this.surfaceBorder = surfaceBorder(this.documentStyle);
  }

  private buildAllCharts(): void {
    this.buildQuantityChart();
    this.buildAmountChart();
    this.build2080Chart();
    this.buildModePaimentChart();
    this.buildTiersPayantChart();
    this.buildFournisseurChart();
  }

  private buildQuantityChart(): void {
    this.quantityChartData = {
      labels: this.rowQuantity.map(p => p.libelle.slice(0, 20)),
      datasets: [{
        type: "bar",
        label: "Quantité vendue",
        backgroundColor: this.documentStyle.getPropertyValue("--p-primary-200"),
        data: this.rowQuantity.map(p => p.quantitySold)
      }]
    };
    this.quantityChartOptions = this.getCommonChartOptions();
  }

  private buildAmountChart(): void {
    this.amountChartData = {
      labels: this.rowAmount.map(p => p.libelle.slice(0, 20)),
      datasets: [{
        type: "bar",
        label: "Montant HT",
        backgroundColor: this.documentStyle.getPropertyValue("--p-blue-200"),
        data: this.rowAmount.map(p => p.montantHt)
      }]
    };
    this.amountChartOptions = this.getCommonChartOptions();
  }

  private build2080Chart(): void {
    // Pareto quantité
    this.twentyEightyChartData = {
      labels: this.row20x80.map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: "line",
          label: "% Quantité cumulé",
          borderColor: this.documentStyle.getPropertyValue("--p-cyan-300"),
          tension: 0.4,
          data: this.row20x80.map(p => p.pourcentage)
        },
        {
          type: "bar",
          label: "Quantité",
          backgroundColor: this.documentStyle.getPropertyValue("--p-orange-300"),
          data: this.row20x80.map(p => p.total)
        }
      ]
    };

    this.twentyEightyMontantChartData = {
      labels: this.row20x80Montant.map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: "line",
          label: "% Montant cumulé",
          borderColor: "rgba(234,88,12,1)",
          backgroundColor: "rgba(234,88,12,0.08)",
          tension: 0.4,
          data: this.row20x80Montant.map(p => p.pourcentage)
        },
        {
          type: "bar",
          label: "Montant",
          backgroundColor: "rgba(234,88,12,0.7)",
          data: this.row20x80Montant.map(p => p.total)
        }
      ]
    };
    this.twentyEightyChartOptions = this.getCommonChartOptions();
  }

  private buildModePaimentChart(): void {
    this.modePaimentChartData = {
      labels: this.venteModePaiments.map(p => p.libelle),
      datasets: [{
        data: this.venteModePaiments.map(p => p.paidAmount),
        backgroundColor: backgroundColor(this.documentStyle),
        hoverBackgroundColor: hoverBackgroundColor(this.documentStyle)
      }]
    };
    this.modePaimentChartOptions = this.getCommonPieChartOptions();
  }

  private buildTiersPayantChart(): void {
    const bgs = backgroundColor(this.documentStyle);
    const hovers = hoverBackgroundColor(this.documentStyle);
    this.tiersPayantChartData = {
      labels: this.tiersPayantAchat.map(p => p.tiersPayantName),
      datasets: [{
        data: this.tiersPayantAchat.map(p => p.montantTtc),
        backgroundColor: bgs.reverse(),
        hoverBackgroundColor: hovers.reverse()
      }]
    };
    this.tiersPayantChartOptions = this.getCommonPieChartOptions();
  }

  private getCommonChartOptions(): any {
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: { legend: { labels: { color: this.textColor } } },
      scales: {
        y: { ticks: { color: this.textColorSecondary }, grid: { color: this.surfaceBorder } },
        x: { ticks: { color: this.textColorSecondary }, grid: { color: this.surfaceBorder } }
      }
    };
  }

  private getCommonPieChartOptions(): any {
    return { plugins: { legend: { position: "bottom", labels: { color: this.textColor, usePointStyle: true } } } };
  }

  // ─── Actions de navigation ────────────────────────────────────────────────

  protected voirPeremptions(): void {
    this.router.navigate(["/gestion-peremption"]);
  }

  protected voirRuptures(): void {
    this.router.navigate(["/produits"], { queryParams: { rupture: true } });
  }

  protected voirUrgents(): void {
    this.router.navigate(["/commande"], { queryParams: { tab: "SUGGESTIONS" } });
  }

  protected voirAjustements(): void {
    this.router.navigate(["/ajustement"]);
  }

  protected voirModifPrix(): void {
    this.router.navigate(["/produit"], { queryParams: { prixModif: true } });
  }
}
