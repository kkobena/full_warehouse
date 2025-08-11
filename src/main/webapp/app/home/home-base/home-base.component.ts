import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
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
import { ProductStatRecord } from '../../shared/model/produit-record.model';
import { AchatRecord } from '../../shared/model/achat-record.model';
import { CaPeriodeFilter } from '../../shared/model/enumerations/ca-periode-filter.model';
import { TOPS } from '../../shared/constants/pagination.constants';
import { DashboardService } from '../dashboard.service';
import { ProduitStatService } from '../../entities/produit/stat/produit-stat.service';
import { TypeCa } from '../../shared/model/enumerations/type-ca.model';
import { OrderBy } from '../../shared/model/enumerations/type-vente.model';
import { forkJoin, Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { TiersPayantService } from '../../entities/tiers-payant/tierspayant.service';
import { TiersPayantAchat } from '../../entities/tiers-payant/model/tiers-payant-achat.model';

interface TopSelection {
  label: string;
  value: number;
}

@Component({
  selector: 'jhi-home-base',
  imports: [CommonModule, FormsModule, DecimalPipe, DropdownModule, TableModule, FaIconComponent],
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
  protected row20x80: ProductStatRecord[] = [];
  protected achatRecord: AchatRecord | null = null;
  protected assurance: VenteRecord | null = null;
  protected vno: VenteRecord | null = null;
  protected venteModePaiments: VenteModePaimentRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter | null = null;
  protected TOP_MAX_QUANTITY: TopSelection;
  protected TOP_MAX_AMOUNT: TopSelection;
  protected TOP_MAX_TP: TopSelection;
  protected totalAmountTopQuantity: number;
  protected totalQuantityToQuantity: number;
  protected totalAmountTopAmount: number;
  protected totalQuantityTopAmount: number;
  protected totalAmount20x80: number;
  protected totalQuantityAvg: number;
  protected totalAmountAvg: number;
  protected totalQuantity20x80: number;
  protected tiersPayantAchat: TiersPayantAchat[] = [];
  private readonly dashboardService = inject(DashboardService);
  private readonly produitStatService = inject(ProduitStatService);
  private readonly tiersPayantService = inject(TiersPayantService);

  constructor() {
    this.TOP_MAX_QUANTITY = this.tops[1];
    this.TOP_MAX_AMOUNT = this.tops[1];
    this.TOP_MAX_TP = this.tops[1];
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
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
      }),
      tiersPayantAchat: this.tiersPayantService.fetchAchatTiersPayant({
        dashboardPeriode: this.dashboardPeriode,
        limit: this.TOP_MAX_QUANTITY?.value,
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
        this.onFetchTiersPayantSuccess(data.tiersPayantAchat.body);
      },
      error: err => {
        console.error('Error loading dashboard data', err);
      },
    });
  }

  protected onTopQuantityChange(): void {
    this.subscribeToFetchPoduitCaResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        limit: this.TOP_MAX_QUANTITY?.value,
      }),
    );
  }

  protected onTopAmountChange(): void {
    this.subscribeToFetchPoduitAmountResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        limit: this.TOP_MAX_AMOUNT?.value,
      }),
    );
  }

  protected onTopTiersPayantChange(): void {
    this.subscribeToFetchTiersPayantAchatResponse(
      this.tiersPayantService.fetchAchatTiersPayant({
        dashboardPeriode: this.dashboardPeriode,
        limit: this.TOP_MAX_QUANTITY?.value,
      }),
    );
  }

  private onCaSuccess(ca: VenteRecordWrapper | null): void {
    this.venteRecord = ca.close;
    this.canceled = ca.canceled;
  }

  private onCaAchatSuccess(achatRecordIn: AchatRecord | null): void {
    this.achatRecord = achatRecordIn;
  }

  private onCaByTypeVenteSuccess(venteByTypeRecords: VenteByTypeRecord[] | null): void {
    this.vno = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VNO')?.venteRecord;
    this.assurance = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VO')?.venteRecord;
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

  private onFetch20x80Success(productStatRecords: ProductStatRecord[] | []): void {
    this.row20x80 = productStatRecords;
    this.computeAmountrow20x80();
  }

  private subscribeToFetchPoduitCaResponse(result: Observable<HttpResponse<ProductStatRecord[]>>): void {
    result.subscribe((res: HttpResponse<ProductStatRecord[]>) => this.onFetchPoduitCaSuccess(res.body));
  }
  private subscribeToFetchTiersPayantAchatResponse(result: Observable<HttpResponse<TiersPayantAchat[]>>): void {
    result.subscribe((res: HttpResponse<TiersPayantAchat[]>) => this.onFetchTiersPayantSuccess(res.body));
  }

  private subscribeToFetchPoduitAmountResponse(result: Observable<HttpResponse<ProductStatRecord[]>>): void {
    result.subscribe((res: HttpResponse<ProductStatRecord[]>) => this.onFetchPoduitAmountSuccess(res.body));
  }

  private computeAmountTopQuantity(): void {
    this.totalQuantityToQuantity = this.rowQuantity.reduce((sum, p) => sum + p.quantitySold, 0);
    this.totalAmountTopQuantity = this.rowQuantity.reduce((sum, p) => sum + p.htAmount, 0);
  }

  private computeAmountTopAmount(): void {
    this.totalQuantityTopAmount = this.rowAmount.reduce((sum, p) => sum + p.quantitySold, 0);
    this.totalAmountTopAmount = this.rowAmount.reduce((sum, p) => sum + p.htAmount, 0);
  }

  private computeAmountrow20x80(): void {
    this.totalAmount20x80 = this.row20x80.reduce((sum, p) => sum + p.htAmount, 0);
    this.totalQuantity20x80 = this.row20x80.reduce((sum, p) => sum + p.quantitySold, 0);
    this.totalQuantityAvg = this.row20x80.reduce((sum, p) => sum + p.quantityAvg, 0);
    this.totalAmountAvg = this.row20x80.reduce((sum, p) => sum + p.amountAvg, 0);
  }
}
