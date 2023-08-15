import { Component, OnInit } from '@angular/core';
import { DashboardService } from '../../dashboard.service';
import { formatNumberToString } from '../../../shared/util/warehouse-util';
import { TypeCa } from '../../../shared/model/enumerations/type-ca.model';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { VenteRecord, VenteRecordWrapper } from '../../../shared/model/vente-record.model';
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
import { AchatRecord } from '../../../shared/model/achat-record.model';

@Component({
  selector: 'jhi-yearly-data',
  templateUrl: './yearly-data.component.html',
  styleUrls: ['./yearly-data.component.scss'],
})
export class YearlyDataComponent implements OnInit {
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
        dashboardPeriode: CaPeriodeFilter.yearly,
      })
    );
    this.subscribeToCaAchatResponse(
      this.dashboardService.fetchCaAchat({
        dashboardPeriode: CaPeriodeFilter.yearly,
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
}
