import { Component, OnInit } from '@angular/core';
import { VenteByTypeRecord, VenteModePaimentRecord, VenteRecord, VenteRecordWrapper } from '../../../shared/model/vente-record.model';
import { DashboardService } from '../../dashboard.service';
import { formatNumberToString } from '../../../shared/util/warehouse-util';
import { TypeCa } from '../../../shared/model/enumerations/type-ca.model';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { TOP_MAX_RESULT } from '../../../shared/constants/pagination.constants';
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

@Component({
  selector: 'jhi-halfyearly-data',
  templateUrl: './halfyearly-data.component.html',
  styleUrls: ['./halfyearly-data.component.scss'],
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
  columnDefs: any[];
  rowQuantityMonthly: any = [];
  rowAmountMonthly: any = [];
  columnDefsMonthAmount: any[];
  rowQuantityYear: any = [];
  rowAmountYear: any = [];
  columnDefsYearQunatity: any[];
  columnDefsYearAmount: any[];
  TOP_MAX_RESULT = TOP_MAX_RESULT;
  protected achatRecord: AchatRecord | null = null;
  protected assurance: VenteRecord | null = null;
  protected vno: VenteRecord | null = null;
  protected venteModePaiments: VenteModePaimentRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.halfyearly;

  constructor(private dashboardService: DashboardService) {
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'libelleProduit',
        flex: 1.3,
      },
      {
        headerName: 'Quantité',
        width: 120,
        field: 'quantity',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
      },
    ];
    this.columnDefsYearQunatity = [
      {
        headerName: 'Libellé',
        field: 'libelleProduit',
        flex: 1.3,
      },
      {
        headerName: 'Quantité',
        width: 120,
        field: 'quantity',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
      },
    ];
    this.columnDefsMonthAmount = [
      {
        headerName: 'Libellé',
        field: 'libelleProduit',
        flex: 1.3,
      },
      {
        headerName: 'Montant',
        width: 120,
        field: 'amount',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
      },
    ];

    this.columnDefsYearAmount = [
      {
        headerName: 'Libellé',
        field: 'libelleProduit',
        flex: 1.3,
      },
      {
        headerName: 'Montant',
        width: 120,
        field: 'amount',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
      },
    ];
  }

  ngOnInit(): void {
    this.subscribeToCaResponse(
      this.dashboardService.fetchCa({
        categorieChiffreAffaire: TypeCa.CA,
        dashboardPeriode: this.dashboardPeriode,
      })
    );
    this.subscribeToCaAchatResponse(
      this.dashboardService.fetchCaAchat({
        dashboardPeriode: this.dashboardPeriode,
      })
    );
    this.subscribeToCaTypeVenteResponse(
      this.dashboardService.fetchCaByTypeVente({
        dashboardPeriode: this.dashboardPeriode,
      })
    );
    this.subscribeToByModePaimentResponse(
      this.dashboardService.getCaByModePaiment({
        dashboardPeriode: this.dashboardPeriode,
      })
    );
  }

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
    this.vno = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VNO').venteRecord;
    this.assurance = venteByTypeRecords?.find((e: VenteByTypeRecord) => e.typeVente === 'VO').venteRecord;
  }

  protected subscribeToByModePaimentResponse(result: Observable<HttpResponse<VenteModePaimentRecord[]>>): void {
    result.subscribe((res: HttpResponse<VenteModePaimentRecord[]>) => this.getCaByModePaimentSuccess(res.body));
  }

  protected getCaByModePaimentSuccess(venteModePaimentRecords: VenteModePaimentRecord[] | []): void {
    this.venteModePaiments = venteModePaimentRecords;
  }
}
