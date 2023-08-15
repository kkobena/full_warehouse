import { Component, OnInit } from '@angular/core';
import { VenteByTypeRecord, VenteModePaimentRecord, VenteRecord, VenteRecordWrapper } from '../../../shared/model/vente-record.model';
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
import { TOP_MAX_RESULT } from '../../../shared/constants/pagination.constants';
import { Observable } from 'rxjs';
import { DashboardService } from '../../dashboard.service';
import { TypeCa } from '../../../shared/model/enumerations/type-ca.model';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { HttpResponse } from '@angular/common/http';
import { formatNumberToString } from '../../../shared/util/warehouse-util';
import { IStatistiqueProduit } from '../../../shared/model/statistique-produit.model';
import { AchatRecord } from '../../../shared/model/achat-record.model';

@Component({
  selector: 'jhi-daily-data',
  templateUrl: './daily-data.component.html',
  styleUrls: ['./daily-data.component.scss'],
})
export class DailyDataComponent implements OnInit {
  protected faShoppingBasket = faShoppingBasket;
  protected faShippingFast = faShippingFast;
  protected faShoppingCart = faShoppingCart;
  protected faCommentsDollar = faCommentsDollar;
  protected faChartArea = faChartArea;
  protected faChartBar = faChartBar;
  protected faChartLine = faChartLine;
  protected faChartPie = faChartPie;
  protected venteRecord: VenteRecord | null = null;
  protected canceled: VenteRecord | null = null;
  protected columnDefs: any[];
  protected rowQuantityMonthly: any = [];
  protected rowAmountMonthly: any = [];
  protected columnDefsMonthAmount: any[];
  protected rowQuantityYear: any = [];
  protected rowAmountYear: any = [];
  protected columnDefsYearQunatity: any[];
  protected columnDefsYearAmount: any[];
  protected TOP_MAX_RESULT = TOP_MAX_RESULT;
  protected achatRecord: AchatRecord | null = null;
  protected assurance: VenteRecord | null = null;
  protected vno: VenteRecord | null = null;
  protected venteModePaiments: VenteModePaimentRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.daily;

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

  protected subscribeToMonthlyQuantityResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe((res: HttpResponse<IStatistiqueProduit>) => this.onMonthlyQuantitySuccess(res.body));
  }

  protected subscribeToMonthlyAmountResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe((res: HttpResponse<IStatistiqueProduit>) => this.onMonthlyAmountSuccess(res.body));
  }

  protected onMonthlyQuantitySuccess(stat: IStatistiqueProduit | null): void {
    this.rowQuantityMonthly = stat;
    this.subscribeToMonthlyAmountResponse(this.dashboardService.monthlyAmount({ maxResult: TOP_MAX_RESULT }));
  }

  protected onMonthlyAmountSuccess(stat: IStatistiqueProduit | null): void {
    this.rowAmountMonthly = stat;
    this.subscribeToYearQuantityResponse(this.dashboardService.yearlyQuantity({ maxResult: TOP_MAX_RESULT }));
  }

  protected subscribeToYearAmountResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe((res: HttpResponse<IStatistiqueProduit>) => this.onYearAmountSuccess(res.body));
  }

  protected subscribeToYearQuantityResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe((res: HttpResponse<IStatistiqueProduit>) => this.onYearQuantitySuccess(res.body));
  }

  protected onYearQuantitySuccess(stat: IStatistiqueProduit | null): void {
    this.rowQuantityYear = stat;
    this.subscribeToYearAmountResponse(this.dashboardService.yearlyAmount({ maxResult: TOP_MAX_RESULT }));
  }

  protected onYearAmountSuccess(stat: IStatistiqueProduit | null): void {
    this.rowAmountYear = stat;
  }
}
