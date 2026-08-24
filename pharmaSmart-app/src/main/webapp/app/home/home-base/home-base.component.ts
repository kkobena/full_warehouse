import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {CommonModule} from "@angular/common";
import {
  VenteByTypeRecord,
  VenteModePaimentRecord,
  VenteRecord,
  VenteRecordWrapper
} from "../../shared/model/vente-record.model";
import {ProductStatParetoRecord, ProductStatRecord} from "../../shared/model/produit-record.model";
import {AchatRecord} from "../../shared/model/achat-record.model";
import {CaPeriodeFilter} from "../../shared/model/enumerations/ca-periode-filter.model";
import {TOPS} from "../../shared/constants/pagination.constants";
import {DashboardService} from "../dashboard.service";
import {ProduitStatService} from "../../entities/produit/stat/produit-stat.service";
import {TypeCa} from "../../shared/model/enumerations/type-ca.model";
import {OrderBy} from "../../shared/model/enumerations/type-vente.model";
import {forkJoin, interval} from "rxjs";
import {FormsModule} from "@angular/forms";
import {TiersPayantService} from "../../entities/tiers-payant/tierspayant.service";
import {TiersPayantAchat} from "../../entities/tiers-payant/model/tiers-payant-achat.model";
import {ChartComponent} from 'app/shared/chart/chart.component';
import {DataTableComponent, SelectComponent, SkeletonComponent} from 'app/shared/ui';
import {
  backgroundColor,
  hoverBackgroundColor,
  surfaceBorder,
  textColor,
  textColorSecondary
} from "../../shared/chart-color-helper";
import {ToggleStateService} from "./toggle-state.service";
import {Router, RouterModule} from "@angular/router";
import {AlertBadgeService} from "../../shared/services/alert-badge.service";
// Report services
import {MargeReportService} from "../../entities/reports/services/marge-report.service";
import {DashboardCAService} from "../../entities/reports/services/dashboard-ca.service";
import {
  StockValuationReportService
} from "../../entities/reports/services/stock-valuation-report.service";
import {
  TiersPayantReportService
} from "../../entities/reports/services/tiers-payant-report.service";
import {
  SupplierPerformanceReportService
} from "../../entities/reports/services/supplier-performance-report.service";
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
import {DiffereApiService} from "../../features/differes/data-access/services/differe-api.service";
import {IDiffereSummary} from "../../features/differes/data-access/models";
import {
  FactureApiService
} from "../../features/facturation/data-access/services/facture-api.service";
import {IFacturationKpi} from "../../features/facturation/data-access/models";

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
  selector: "app-home-base",
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    DataTableComponent,
    ChartComponent,
    SelectComponent,
    SkeletonComponent
  ],
  templateUrl: "./home-base.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./home-base.component.scss"
})
export class HomeBaseComponent implements OnInit {

  protected readonly tops: TopSelection[] = TOPS;

  // Alert counters — via AlertBadgeService (signaux)
  protected readonly alertBadgeService = inject(AlertBadgeService);
  // ─── Période (P2) ───────────────────────────────────────────────
  protected readonly periodeOptions: PeriodOption[] = [
    {label: "Auj.", value: CaPeriodeFilter.daily, icon: "pi pi-sun"},
    {label: "Semaine", value: CaPeriodeFilter.weekly, icon: "pi pi-calendar"},
    {label: "Mois", value: CaPeriodeFilter.monthly, icon: "pi pi-calendar-plus"},
    {label: "Semestre", value: CaPeriodeFilter.halfyearly, icon: "pi pi-chart-bar"},
    {label: "Année", value: CaPeriodeFilter.yearly, icon: "pi pi-chart-line"}
  ];
  protected activePeriode = signal<CaPeriodeFilter>(CaPeriodeFilter.daily);
  protected isLoading = signal(false);
  protected lastUpdate = signal<Date | null>(null);
  protected activePareto: "qty" | "amt" = "qty";
  // ─── State ──────────────────────────────────────────────────────
  protected readonly venteRecord = signal<VenteRecord | null>(null);
  protected readonly canceled = signal<VenteRecord | null>(null);
  protected readonly rowQuantity = signal<ProductStatRecord[]>([]);
  protected readonly rowAmount = signal<ProductStatRecord[]>([]);
  protected readonly row20x80 = signal<ProductStatParetoRecord[]>([]);
  protected readonly row20x80Montant = signal<ProductStatParetoRecord[]>([]);
  protected readonly achatRecord = signal<AchatRecord | null>(null);
  protected assurance: VenteRecord | null = null;
  protected readonly vno = signal<VenteRecord | null>(null);
  protected readonly venteModePaiments = signal<VenteModePaimentRecord[]>([]);
  protected dashboardPeriode: CaPeriodeFilter | null = CaPeriodeFilter.daily;
  protected readonly TOP_MAX_QUANTITY = signal<TopSelection | undefined>(undefined);
  protected readonly TOP_MAX_AMOUNT = signal<TopSelection | undefined>(undefined);
  protected readonly TOP_MAX_TP = signal<TopSelection | undefined>(undefined);
  protected readonly totalAmountTopQuantity = signal(0);
  protected readonly totalQuantityToQuantity = signal(0);
  protected readonly totalAmountTopAmount = signal(0);
  protected readonly totalQuantityTopAmount = signal(0);
  protected readonly totalAmount20x80 = signal(0);
  protected readonly totalQuantityAvg = signal(0);
  protected readonly totalAmountAvg = signal(0);
  protected readonly totalQuantity20x80 = signal(0);
  protected readonly tiersPayantAchat = signal<TiersPayantAchat[]>([]);
  // ─── KPI P1 ─────────────────────────────────────────────────────
  protected readonly margeSummary = signal<IMargeSummary | null>(null);
  protected caSummary: IDashboardCASummary | null = null;
  protected readonly stockValuationSummary = signal<IStockValuationSummary | null>(null);
  protected creancesSummary: ITiersPayantCreancesSummary[] = [];
  protected readonly totalCreances = signal(0);
  protected readonly creancesPlusDe90j = signal(0);
  // ─── KPI P2 ─────────────────────────────────────────────────────
  protected readonly differeSummary = signal<IDiffereSummary | null>(null);
  protected readonly facturationKpi = signal<IFacturationKpi | null>(null);
  // ─── Fournisseurs P3 ────────────────────────────────────────────
  protected readonly topFournisseurs = signal<ISupplierPerformance[]>([]);
  protected readonly supplierSummary = signal<ISupplierPerformanceSummary | null>(null);
  protected fournisseurPeriod: "30d" | "12m" = "30d";
  protected readonly TOP_MAX_FOURNISSEUR = signal<TopSelection | undefined>(undefined);
  protected readonly fournisseurChartData = signal<any | undefined>(undefined);
  protected readonly fournisseurChartOptions = signal<any | undefined>(undefined);
  // ─── Charts ──────────────────────────────────────────────────────
  protected readonly toggleStateService = inject(ToggleStateService);
  protected readonly showGraphs = signal(false);
  protected readonly quantityChartData = signal<any | undefined>(undefined);
  protected readonly quantityChartOptions = signal<any | undefined>(undefined);
  protected readonly amountChartData = signal<any | undefined>(undefined);
  protected readonly amountChartOptions = signal<any | undefined>(undefined);
  protected readonly twentyEightyChartData = signal<any | undefined>(undefined);
  protected readonly twentyEightyMontantChartData = signal<any | undefined>(undefined);
  protected readonly twentyEightyChartOptions = signal<any | undefined>(undefined);
  protected readonly modePaimentChartData = signal<any | undefined>(undefined);
  protected readonly modePaimentChartOptions = signal<any | undefined>(undefined);
  protected readonly tiersPayantChartData = signal<any | undefined>(undefined);
  protected readonly tiersPayantChartOptions = signal<any | undefined>(undefined);
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
    this.TOP_MAX_QUANTITY.set(this.tops[1]);
    this.TOP_MAX_AMOUNT.set(this.tops[1]);
    this.TOP_MAX_TP.set(this.tops[1]);
    this.TOP_MAX_FOURNISSEUR.set(this.tops[0]);
  }

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
      case CaPeriodeFilter.daily:
        return this.caSummary?.caTodayEvolutionPct;
      case CaPeriodeFilter.weekly:
        return this.caSummary?.caWeekEvolutionPct;
      case CaPeriodeFilter.monthly:
        return this.caSummary?.caMonthEvolutionPct;
      case CaPeriodeFilter.yearly:
        return this.caSummary?.caYearEvolutionPct;
      default:
        return null; // halfyearly : pas de champ dédié
    }
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
    this.showGraphs.set(this.toggleStateService.toggleState());
    this.loadDashboardData();
  }

  protected loadDashboardData(): void {
    this.isLoading.set(true);
    const sources = {
      ca: this.dashboardService.fetchCa({
        categorieChiffreAffaire: TypeCa.CA,
        dashboardPeriode: this.dashboardPeriode
      }),
      caAchat: this.dashboardService.fetchCaAchat({dashboardPeriode: this.dashboardPeriode}),
      caTypeVente: this.dashboardService.fetchCaByTypeVente({dashboardPeriode: this.dashboardPeriode}),
      byModePaiment: this.dashboardService.getCaByModePaiment({dashboardPeriode: this.dashboardPeriode}),
      produitCa: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        size: this.TOP_MAX_QUANTITY()?.value
      }),
      produitAmount: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        size: this.TOP_MAX_AMOUNT()?.value
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
        limit: this.TOP_MAX_TP()?.value
      }),
      // P1 — KPI services
      margeSummary: this.margeReportService.getMargeSummary(),
      caSummary: this.dashboardCAService.getOverallSummary(),
      stockValuation: this.stockValuationReportService.getStockValuationSummary(),
      creancesSummary: this.tiersPayantReportService.getCreancesSummary(),
      // P3 — Fournisseurs
      topFournisseurs: this.supplierService.getTopSuppliersByVolume(this.TOP_MAX_FOURNISSEUR().value),
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
        this.margeSummary.set(data.margeSummary.body);
        this.caSummary = data.caSummary.body;
        this.stockValuationSummary.set(data.stockValuation.body);
        this.creancesSummary = data.creancesSummary.body ?? [];
        this.totalCreances.set(this.creancesSummary.reduce((s, c) => s + (c.montantTotal ?? 0), 0));
        this.creancesPlusDe90j.set(this.creancesSummary.reduce((s, c) => s + (c.montantPlusDe90Jours ?? 0), 0));
        // P3
        this.topFournisseurs.set(data.topFournisseurs.body ?? []);
        this.supplierSummary.set(data.supplierSummary.body);
        // P2
        this.differeSummary.set(data.differeSummary.body);
        this.facturationKpi.set(data.facturationKpi.body);
        this.buildAllCharts();
        this.isLoading.set(false);
        this.lastUpdate.set(new Date());
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  protected onTopQuantityChange(top: TopSelection): void {
    this.TOP_MAX_QUANTITY.set(top);
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

  protected onTopAmountChange(top: TopSelection): void {
    this.TOP_MAX_AMOUNT.set(top);
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

  protected onTopTiersPayantChange(top: TopSelection): void {
    this.TOP_MAX_TP.set(top);
    this.tiersPayantService
      .fetchAchatTiersPayant({dashboardPeriode: this.dashboardPeriode, limit: top.value})
      .subscribe(res => {
        this.onFetchTiersPayantSuccess(res.body);
        this.buildTiersPayantChart();
      });
  }

  protected onTopFournisseurChange(top: TopSelection): void {
    this.TOP_MAX_FOURNISSEUR.set(top);
    this.supplierService.getTopSuppliersByVolume(top.value)
      .subscribe(res => {
        this.topFournisseurs.set(res.body ?? []);
        this.buildFournisseurChart();
      });
  }

  protected buildFournisseurChart(): void {
    const items = this.topFournisseurs().slice(0, this.TOP_MAX_FOURNISSEUR().value);
    const amounts = this.fournisseurPeriod === "30d"
      ? items.map(f => f.purchaseAmountLast30Days ?? 0)
      : items.map(f => f.purchaseAmountLast12Months ?? 0);
    this.fournisseurChartData.set({
      labels: items.map(f => f.fournisseurName?.slice(0, 18) ?? ""),
      datasets: [{
        data: amounts,
        backgroundColor: ["#008cba", "#5bc0de", "#43ac6a", "#e99002", "#f04124"],
        borderWidth: 2
      }]
    });
    this.fournisseurChartOptions.set({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {legend: {position: "right", labels: {boxWidth: 12, font: {size: 10}}}}
    });
  }

  protected voirPeremptions(): void {
    this.router.navigate(["/gestion-peremption"]);
  }

  protected voirRuptures(): void {
    this.router.navigate(["/produits"], {queryParams: {rupture: true}});
  }

  protected voirUrgents(): void {
    this.router.navigate(["/commande"], {queryParams: {tab: "SUGGESTIONS"}});
  }

  protected voirAjustements(): void {
    this.router.navigate(["/features-ajustement"]);
  }

  protected voirModifPrix(): void {
    this.router.navigate(["/produit"], {queryParams: {prixModif: true}});
  }

  private onCaSuccess(ca: VenteRecordWrapper | null): void {
    if (!ca) {
      return;
    }
    this.venteRecord.set(ca.close);
    this.canceled.set(ca.canceled);
  }

  private onCaAchatSuccess(achatRecordIn: AchatRecord | null): void {
    this.achatRecord.set(achatRecordIn);
  }

  private onCaByTypeVenteSuccess(venteByTypeRecords: VenteByTypeRecord[] | null): void {
    if (!venteByTypeRecords) {
      return;
    }
    this.vno.set(venteByTypeRecords.find(e => e.typeVente === "CashSale")?.venteRecord);
    this.assurance = venteByTypeRecords.find(e => e.typeVente === "ThirdPartySales")?.venteRecord;
  }

  private onGetCaByModePaimentSuccess(venteModePaimentRecords: VenteModePaimentRecord[] | []): void {
    this.venteModePaiments.set(venteModePaimentRecords);
  }

  private onFetchTiersPayantSuccess(tps: TiersPayantAchat[] | []): void {
    this.tiersPayantAchat.set(tps);
  }

  private onFetchPoduitCaSuccess(productStatRecords: ProductStatRecord[] | []): void {
    this.rowQuantity.set(productStatRecords);
    this.computeAmountTopQuantity();
  }

  private onFetchPoduitAmountSuccess(productStatRecords: ProductStatRecord[] | []): void {
    this.rowAmount.set(productStatRecords);
    this.computeAmountTopAmount();
  }

  private onFetch20x80AmountSuccess(productStatRecords: ProductStatParetoRecord[] | []): void {
    this.row20x80Montant.set(productStatRecords);
    this.computeAmountrow20x80Amount();
  }

  private onFetch20x80Success(productStatRecords: ProductStatParetoRecord[] | []): void {
    this.row20x80.set(productStatRecords);
    this.computeAmountrow20x80();
  }

  private computeAmountTopQuantity(): void {
    this.totalQuantityToQuantity.set(this.rowQuantity().reduce((sum, p) => sum + p.quantitySold, 0));
    this.totalAmountTopQuantity.set(this.rowQuantity().reduce((sum, p) => sum + p.montantHt, 0));
  }

  private computeAmountTopAmount(): void {
    this.totalQuantityTopAmount.set(this.rowAmount().reduce((sum, p) => sum + p.quantitySold, 0));
    this.totalAmountTopAmount.set(this.rowAmount().reduce((sum, p) => sum + p.montantHt, 0));
  }

  private computeAmountrow20x80Amount(): void {
    if (this.row20x80Montant()?.length) {
      this.totalAmount20x80.set(this.row20x80Montant()[0].totalGlobal);
    }
    this.totalAmountAvg.set(this.row20x80Montant()?.reduce((sum, p) => sum + p.pourcentage, 0));
  }

  private computeAmountrow20x80(): void {
    if (this.row20x80()?.length) {
      this.totalQuantity20x80.set(this.row20x80()[0].totalGlobal);
    }
    this.totalQuantityAvg.set(this.row20x80()?.reduce((sum, p) => sum + p.pourcentage, 0));
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
    this.quantityChartData.set({
      labels: this.rowQuantity().map(p => p.libelle.slice(0, 20)),
      datasets: [{
        type: "bar",
        label: "Quantité vendue",
        backgroundColor: this.documentStyle.getPropertyValue("--p-primary-200"),
        data: this.rowQuantity().map(p => p.quantitySold)
      }]
    });
    this.quantityChartOptions.set(this.getCommonChartOptions());
  }

  private buildAmountChart(): void {
    this.amountChartData.set({
      labels: this.rowAmount().map(p => p.libelle.slice(0, 20)),
      datasets: [{
        type: "bar",
        label: "Montant HT",
        backgroundColor: this.documentStyle.getPropertyValue("--p-blue-200"),
        data: this.rowAmount().map(p => p.montantHt)
      }]
    });
    this.amountChartOptions.set(this.getCommonChartOptions());
  }

  // ─── Actions de navigation ────────────────────────────────────────────────

  private build2080Chart(): void {
    // Pareto quantité
    this.twentyEightyChartData.set({
      labels: this.row20x80().map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: "line",
          label: "% Quantité cumulé",
          borderColor: this.documentStyle.getPropertyValue("--p-cyan-300"),
          tension: 0.4,
          data: this.row20x80().map(p => p.pourcentage)
        },
        {
          type: "bar",
          label: "Quantité",
          backgroundColor: this.documentStyle.getPropertyValue("--p-orange-300"),
          data: this.row20x80().map(p => p.total)
        }
      ]
    });

    this.twentyEightyMontantChartData.set({
      labels: this.row20x80Montant().map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: "line",
          label: "% Montant cumulé",
          borderColor: "rgba(234,88,12,1)",
          backgroundColor: "rgba(234,88,12,0.08)",
          tension: 0.4,
          data: this.row20x80Montant().map(p => p.pourcentage)
        },
        {
          type: "bar",
          label: "Montant",
          backgroundColor: "rgba(234,88,12,0.7)",
          data: this.row20x80Montant().map(p => p.total)
        }
      ]
    });
    this.twentyEightyChartOptions.set(this.getCommonChartOptions());
  }

  private buildModePaimentChart(): void {
    this.modePaimentChartData.set({
      labels: this.venteModePaiments().map(p => p.libelle),
      datasets: [{
        data: this.venteModePaiments().map(p => p.paidAmount),
        backgroundColor: backgroundColor(this.documentStyle),
        hoverBackgroundColor: hoverBackgroundColor(this.documentStyle)
      }]
    });
    this.modePaimentChartOptions.set(this.getCommonPieChartOptions());
  }

  private buildTiersPayantChart(): void {
    const bgs = backgroundColor(this.documentStyle);
    const hovers = hoverBackgroundColor(this.documentStyle);
    this.tiersPayantChartData.set({
      labels: this.tiersPayantAchat().map(p => p.tiersPayantName),
      datasets: [{
        data: this.tiersPayantAchat().map(p => p.montantTtc),
        backgroundColor: bgs.reverse(),
        hoverBackgroundColor: hovers.reverse()
      }]
    });
    this.tiersPayantChartOptions.set(this.getCommonPieChartOptions());
  }

  private getCommonChartOptions(): any {
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {legend: {labels: {color: this.textColor}}},
      scales: {
        y: {ticks: {color: this.textColorSecondary}, grid: {color: this.surfaceBorder}},
        x: {ticks: {color: this.textColorSecondary}, grid: {color: this.surfaceBorder}}
      }
    };
  }

  private getCommonPieChartOptions(): any {
    return {
      plugins: {
        legend: {
          position: "bottom",
          labels: {color: this.textColor, usePointStyle: true}
        }
      }
    };
  }
}
