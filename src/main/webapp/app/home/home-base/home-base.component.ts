import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import {
  faChartArea,
  faChartBar,
  faChartLine,
  faChartPie,
  faCommentsDollar,
  faCreditCard,
  faShippingFast,
  faShoppingBasket,
} from '@fortawesome/free-solid-svg-icons';
import { TableModule } from 'primeng/table';
import { VenteByTypeRecord, VenteModePaimentRecord, VenteRecord, VenteRecordWrapper } from '../../shared/model/vente-record.model';
import { ProductStatParetoRecord, ProductStatRecord } from '../../shared/model/produit-record.model';
import { AchatRecord } from '../../shared/model/achat-record.model';
import { CaPeriodeFilter } from '../../shared/model/enumerations/ca-periode-filter.model';
import { TOPS } from '../../shared/constants/pagination.constants';
import { DashboardService } from '../dashboard.service';
import { ProduitStatService } from '../../entities/produit/stat/produit-stat.service';
import { TypeCa } from '../../shared/model/enumerations/type-ca.model';
import { OrderBy } from '../../shared/model/enumerations/type-vente.model';
import { forkJoin } from 'rxjs';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { TiersPayantService } from '../../entities/tiers-payant/tierspayant.service';
import { TiersPayantAchat } from '../../entities/tiers-payant/model/tiers-payant-achat.model';
import { ChartModule } from 'primeng/chart';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { backgroundColor, hoverBackgroundColor, surfaceBorder, textColor, textColorSecondary } from '../../shared/chart-color-helper';
import { ToggleStateService } from './toggle-state.service';
import { ToggleButtonChangeEvent } from 'primeng/togglebutton/togglebutton.interface';
import { SelectModule } from 'primeng/select';

interface TopSelection {
  label: string;
  value: number;
}

@Component({
  selector: 'jhi-home-base',
  imports: [CommonModule, FormsModule, DecimalPipe, TableModule, FaIconComponent, ChartModule, ToggleButtonModule, SelectModule],
  templateUrl: './home-base.component.html',
  styleUrl: './home-base.component.scss',
})
export class HomeBaseComponent implements OnInit {
  protected readonly faShoppingBasket = faShoppingBasket;
  protected readonly faShippingFast = faShippingFast;
  protected readonly faCommentsDollar = faCommentsDollar;
  protected readonly faChartArea = faChartArea;
  protected readonly faChartBar = faChartBar;
  protected readonly faChartLine = faChartLine;
  protected readonly faChartPie = faChartPie;
  protected readonly faCreditCard = faCreditCard;
  protected readonly tops: TopSelection[] = TOPS;

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
  protected dashboardPeriode: CaPeriodeFilter | null = null;
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
  protected readonly toggleStateService = inject(ToggleStateService);
  protected showGraphs = false;
  protected quantityChartData: any;
  protected quantityChartOptions: any;
  protected amountChartData: any;
  protected amountChartOptions: any;
  protected twentyEightyChartData: any;
  protected twentyEightyChartOptions: any;
  protected modePaimentChartData: any;
  protected modePaimentChartOptions: any;
  protected tiersPayantChartData: any;
  protected tiersPayantChartOptions: any;

  private readonly dashboardService = inject(DashboardService);
  private readonly produitStatService = inject(ProduitStatService);
  private readonly tiersPayantService = inject(TiersPayantService);

  private documentStyle: CSSStyleDeclaration;
  private textColor: string;
  private textColorSecondary: string;
  private surfaceBorder: string;

  constructor() {
    this.TOP_MAX_QUANTITY = this.tops[1];
    this.TOP_MAX_AMOUNT = this.tops[1];
    this.TOP_MAX_TP = this.tops[1];
  }

  ngOnInit(): void {
    this.initializeChartStyles();
    this.loadDashboardData();
  }

  protected loadDashboardData(): void {
    const sources = {
      ca: this.dashboardService.fetchCa({
        categorieChiffreAffaire: TypeCa.CA,
        dashboardPeriode: this.dashboardPeriode,
      }),
      caAchat: this.dashboardService.fetchCaAchat({
        dashboardPeriode: this.dashboardPeriode,
      }),
      caTypeVente: this.dashboardService.fetchCaByTypeVente({
        dashboardPeriode: this.dashboardPeriode,
      }),
      byModePaiment: this.dashboardService.getCaByModePaiment({
        dashboardPeriode: this.dashboardPeriode,
      }),
      produitCa: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        limit: this.TOP_MAX_QUANTITY?.value,
      }),
      produitAmount: this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        limit: this.TOP_MAX_AMOUNT?.value,
      }),
      twentyEighty: this.produitStatService.fetch20x80({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
      }),
      twentyEightyMontant: this.produitStatService.fetch20x80({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
      }),
      tiersPayantAchat: this.tiersPayantService.fetchAchatTiersPayant({
        dashboardPeriode: this.dashboardPeriode,
        limit: this.TOP_MAX_TP?.value,
      }),
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
        this.buildAllCharts();
      },
      error: err => {
        console.error('Error loading dashboard data', err);
      },
    });
  }

  protected onTopQuantityChange(): void {
    this.produitStatService
      .fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        limit: this.TOP_MAX_QUANTITY?.value,
      })
      .subscribe(res => {
        this.onFetchPoduitCaSuccess(res.body);
        this.buildQuantityChart();
      });
  }

  protected onTopAmountChange(): void {
    this.produitStatService
      .fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        limit: this.TOP_MAX_AMOUNT?.value,
      })
      .subscribe(res => {
        this.onFetchPoduitAmountSuccess(res.body);
        this.buildAmountChart();
      });
  }
  protected onToggleChange(evt: ToggleButtonChangeEvent): void {
    this.toggleStateService.update(evt.checked);
  }
  protected onTopTiersPayantChange(): void {
    this.tiersPayantService
      .fetchAchatTiersPayant({
        dashboardPeriode: this.dashboardPeriode,
        limit: this.TOP_MAX_TP?.value,
      })
      .subscribe(res => {
        this.onFetchTiersPayantSuccess(res.body);
        this.buildTiersPayantChart();
      });
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
    this.vno = venteByTypeRecords.find(e => e.typeVente === 'CashSale')?.venteRecord;
    this.assurance = venteByTypeRecords.find(e => e.typeVente === 'ThirdPartySales')?.venteRecord;
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
    if (this.row20x80Montant?.length) {
      this.totalAmount20x80 = this.row20x80Montant[0].totalGlobal;
    }
    this.totalAmountAvg = this.row20x80Montant?.reduce((sum, p) => sum + p.pourcentage, 0);
  }

  private computeAmountrow20x80(): void {
    if (this.row20x80?.length) {
      this.totalQuantity20x80 = this.row20x80[0].totalGlobal;
    }
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
  }

  private buildQuantityChart(): void {
    this.quantityChartData = {
      labels: this.rowQuantity.map(p => p.libelle.slice(0, 20)), // Truncate labels
      datasets: [
        {
          type: 'bar',
          label: 'Quantité vendue',
          backgroundColor: this.documentStyle.getPropertyValue('--p-primary-200'),
          data: this.rowQuantity.map(p => p.quantitySold),
        },
      ],
    };
    this.quantityChartOptions = this.getCommonChartOptions();
  }

  private buildAmountChart(): void {
    this.amountChartData = {
      labels: this.rowAmount.map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: 'bar',
          label: 'Montant HT',
          backgroundColor: this.documentStyle.getPropertyValue('--p-blue-200'),
          data: this.rowAmount.map(p => p.montantHt),
        },
      ],
    };
    this.amountChartOptions = this.getCommonChartOptions();
  }

  private build2080Chart(): void {
    this.twentyEightyChartData = {
      labels: this.row20x80.map(p => p.libelle.slice(0, 20)),
      datasets: [
        {
          type: 'line',
          label: '% Montant',
          borderColor: this.documentStyle.getPropertyValue('--p-cyan-300'),
          tension: 0.4,
          data: this.row20x80.map(p => p.pourcentage),
        },
        {
          type: 'bar',
          label: 'Montant HT',
          backgroundColor: this.documentStyle.getPropertyValue('--p-orange-300'),
          data: this.row20x80.map(p => p.total),
        },
      ],
    };
    this.twentyEightyChartOptions = this.getCommonChartOptions();
  }

  private buildModePaimentChart(): void {
    this.modePaimentChartData = {
      labels: this.venteModePaiments.map(p => p.libelle),
      datasets: [
        {
          data: this.venteModePaiments.map(p => p.paidAmount),
          backgroundColor: backgroundColor(this.documentStyle),
          hoverBackgroundColor: hoverBackgroundColor(this.documentStyle),
        },
      ],
    };
    this.modePaimentChartOptions = this.getCommonPieChartOptions();
    console.log(this.modePaimentChartData);
  }

  private buildTiersPayantChart(): void {
    const bgs = backgroundColor(this.documentStyle);
    const hovers = hoverBackgroundColor(this.documentStyle);
    this.tiersPayantChartData = {
      labels: this.tiersPayantAchat.map(p => p.tiersPayantName),
      datasets: [
        {
          data: this.tiersPayantAchat.map(p => p.montantTtc),
          backgroundColor: bgs.reverse(),
          hoverBackgroundColor: hovers.reverse(),
        },
      ],
    };
    this.tiersPayantChartOptions = this.getCommonPieChartOptions();
  }

  private getCommonChartOptions(): any {
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: {
          labels: {
            color: this.textColor,
          },
        },
      },
      scales: {
        y: {
          ticks: {
            color: this.textColorSecondary,
          },
          grid: {
            color: this.surfaceBorder,
          },
        },
        x: {
          ticks: {
            color: this.textColorSecondary,
          },
          grid: {
            color: this.surfaceBorder,
          },
        },
      },
    };
  }

  private getCommonPieChartOptions(): any {
    return {
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: this.textColor,
            usePointStyle: true,
          },
        },
      },
    };
  }
}
