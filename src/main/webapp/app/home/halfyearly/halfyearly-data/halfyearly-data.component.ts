import { Component, inject, OnInit } from '@angular/core';
import { VenteByTypeRecord, VenteModePaimentRecord, VenteRecord, VenteRecordWrapper } from '../../../shared/model/vente-record.model';
import { DashboardService } from '../../dashboard.service';
import { TypeCa } from '../../../shared/model/enumerations/type-ca.model';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { TOP_MAX_RESULT, TOPS } from '../../../shared/constants/pagination.constants';
import {
  faChartArea,
  faChartBar,
  faChartLine,
  faChartPie,
  faCommentsDollar,
  faShippingFast,
  faShoppingBasket,
  faShoppingCart,
} from '@fortawesome/free-solid-svg-icons';
import { AchatRecord } from '../../../shared/model/achat-record.model';
import { ProduitStatService } from '../../../entities/produit/stat/produit-stat.service';
import { ProductStatRecord } from '../../../shared/model/produit-record.model';
import { OrderBy } from '../../../shared/model/enumerations/type-vente.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-halfyearly-data',
  templateUrl: './halfyearly-data.component.html',
  styleUrls: ['./halfyearly-data.component.scss'],
  imports: [WarehouseCommonModule, DropdownModule, TableModule, FormsModule],
})
export class HalfyearlyDataComponent implements OnInit {
  faShoppingBasket = faShoppingBasket;
  faShippingFast = faShippingFast;
  faShoppingCart = faShoppingCart;
  faCommentsDollar = faCommentsDollar;
  faChartArea = faChartArea;
  faChartBar = faChartBar;
  faChartLine = faChartLine;
  faChartPie = faChartPie;
  venteRecord: VenteRecord | null = null;
  canceled: VenteRecord | null = null;
  TOP_MAX_RESULT = TOP_MAX_RESULT;
  protected rowQuantity: ProductStatRecord[] = [];
  protected rowAmount: ProductStatRecord[] = [];
  protected row20x80: ProductStatRecord[] = [];
  protected achatRecord: AchatRecord | null = null;
  protected assurance: VenteRecord | null = null;
  protected vno: VenteRecord | null = null;
  protected venteModePaiments: VenteModePaimentRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.halfyearly;
  protected tops = TOPS;
  protected TOP_MAX_QUANTITY: any;
  protected TOP_MAX_AMOUNT: any;
  protected TOP_MAX_TP: any;
  protected totalAmountTopQuantity: number;
  protected totalQuantityToQuantity: number;
  protected totalAmountTopAmount: number;
  protected totalQuantityTopAmount: number;
  protected totalAmount20x80: number;
  protected totalQuantityAvg: number;
  protected totalAmountAvg: number;
  protected totalQuantity20x80: number;
  private dashboardService = inject(DashboardService);
  private produitStatService = inject(ProduitStatService);

  constructor() {
    this.TOP_MAX_QUANTITY = this.tops[1];
    this.TOP_MAX_AMOUNT = this.tops[1];
    this.TOP_MAX_TP = this.tops[1];
  }

  ngOnInit(): void {
    this.subscribeToCaResponse(
      this.dashboardService.fetchCa({
        categorieChiffreAffaire: TypeCa.CA,
        dashboardPeriode: this.dashboardPeriode,
      }),
    );
    this.subscribeToCaAchatResponse(
      this.dashboardService.fetchCaAchat({
        dashboardPeriode: this.dashboardPeriode,
      }),
    );
    this.subscribeToCaTypeVenteResponse(
      this.dashboardService.fetchCaByTypeVente({
        dashboardPeriode: this.dashboardPeriode,
      }),
    );
    this.subscribeToByModePaimentResponse(
      this.dashboardService.getCaByModePaiment({
        dashboardPeriode: this.dashboardPeriode,
      }),
    );
    this.subscribeToFetchPoduitCaResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        limit: this.TOP_MAX_QUANTITY?.value,
      }),
    );
    this.subscribeToFetchPoduitAmountResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        limit: this.TOP_MAX_AMOUNT?.value,
      }),
    );
    this.subscribeToFetch20x80Response(
      this.produitStatService.fetch20x80({
        dashboardPeriode: this.dashboardPeriode,
      }),
    );
  }

  onTopQuantityChange(): void {
    this.subscribeToFetchPoduitCaResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.QUANTITY_SOLD,
        limit: this.TOP_MAX_QUANTITY?.value,
      }),
    );
  }

  onTopAmountChange(): void {
    this.subscribeToFetchPoduitAmountResponse(
      this.produitStatService.fetchPoduitCa({
        dashboardPeriode: this.dashboardPeriode,
        order: OrderBy.AMOUNT,
        limit: this.TOP_MAX_AMOUNT?.value,
      }),
    );
  }

  onTopTiersPayantChange(): void {}

  protected subscribeToCaResponse(result: Observable<HttpResponse<VenteRecordWrapper>>): void {
    result.subscribe((res: HttpResponse<VenteRecordWrapper>) => this.onCaSuccess(res.body));
  }

  protected onCaSuccess(ca: VenteRecordWrapper | null): void {
    this.venteRecord = ca.close;
    this.canceled = ca.canceled;
  }

  protected subscribeToCaAchatResponse(result: Observable<HttpResponse<AchatRecord>>): void {
    result.subscribe((res: HttpResponse<AchatRecord>) => this.onCaAchatSuccess(res.body));
  }

  protected onCaAchatSuccess(achatRecordIn: AchatRecord | null): void {
    this.achatRecord = achatRecordIn;
  }

  protected subscribeToCaTypeVenteResponse(result: Observable<HttpResponse<VenteByTypeRecord[]>>): void {
    result.subscribe((res: HttpResponse<VenteByTypeRecord[]>) => this.onCaByTypeVenteSuccess(res.body));
  }

  protected onCaByTypeVenteSuccess(venteByTypeRecords: VenteByTypeRecord[] | null): void {
    this.vno = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VNO')?.venteRecord;
    this.assurance = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VO')?.venteRecord;
  }

  protected subscribeToByModePaimentResponse(result: Observable<HttpResponse<VenteModePaimentRecord[]>>): void {
    result.subscribe((res: HttpResponse<VenteModePaimentRecord[]>) => this.getCaByModePaimentSuccess(res.body));
  }

  protected getCaByModePaimentSuccess(venteModePaimentRecords: VenteModePaimentRecord[] | []): void {
    this.venteModePaiments = venteModePaimentRecords;
  }

  protected fetchPoduitCa(productStatRecords: ProductStatRecord[] | []): void {
    this.rowQuantity = productStatRecords;
    this.computeAmountTopQuantity();
  }

  protected fetchPoduitCaAmount(productStatRecords: ProductStatRecord[] | []): void {
    this.rowAmount = productStatRecords;
    this.computeAmountTopAmount();
  }

  protected onFetch20x80(productStatRecords: ProductStatRecord[] | []): void {
    this.row20x80 = productStatRecords;
    this.computeAmountrow20x80();
  }

  protected subscribeToFetchPoduitCaResponse(result: Observable<HttpResponse<ProductStatRecord[]>>): void {
    result.subscribe((res: HttpResponse<ProductStatRecord[]>) => this.fetchPoduitCa(res.body));
  }

  protected subscribeToFetchPoduitAmountResponse(result: Observable<HttpResponse<ProductStatRecord[]>>): void {
    result.subscribe((res: HttpResponse<ProductStatRecord[]>) => this.fetchPoduitCaAmount(res.body));
  }

  protected subscribeToFetch20x80Response(result: Observable<HttpResponse<ProductStatRecord[]>>): void {
    result.subscribe((res: HttpResponse<ProductStatRecord[]>) => this.onFetch20x80(res.body));
  }

  private computeAmountTopQuantity(): void {
    let quantity = 0;
    let amount = 0;
    for (const produit of this.rowQuantity) {
      quantity += produit.quantitySold;
      amount += produit.htAmount;
    }
    this.totalQuantityToQuantity = quantity;
    this.totalAmountTopQuantity = amount;
  }

  private computeAmountTopAmount(): void {
    let quantity = 0;
    let amount = 0;
    for (const produit of this.rowAmount) {
      quantity += produit.quantitySold;
      amount += produit.htAmount;
    }
    this.totalAmountTopAmount = amount;
    this.totalQuantityTopAmount = quantity;
  }

  private computeAmountrow20x80(): void {
    let quantity = 0;
    let amount = 0;
    let quantityAvg = 0;
    let amountAvg = 0;
    for (const produit of this.row20x80) {
      quantity += produit.quantitySold;
      amount += produit.htAmount;
      quantityAvg += produit.quantityAvg;
      amountAvg += produit.amountAvg;
    }
    this.totalAmount20x80 = amount;
    this.totalQuantity20x80 = quantity;
    this.totalQuantityAvg = quantityAvg;
    this.totalAmountAvg = amountAvg;
  }
}
