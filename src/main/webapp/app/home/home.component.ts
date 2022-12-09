import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { IDailyca } from '../shared/model/dailyca.model';
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
import { TOP_MAX_RESULT } from '../shared/constants/pagination.constants';
import { DashboardService } from './dashboard.service';
import { HttpResponse } from '@angular/common/http';
import { IStatistiqueProduit } from '../shared/model/statistique-produit.model';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  account: Account | null = null;
  faShoppingBasket = faShoppingBasket;
  faShippingFast = faShippingFast;
  faShoppingCart = faShoppingCart;
  faCommentsDollar = faCommentsDollar;
  faChartArea = faChartArea;
  faChartBar = faChartBar;
  faChartLine = faChartLine;
  faChartPie = faChartPie;
  dailyCa: IDailyca | null = null;
  weeklyCa: IDailyca | null = null;
  monthlyCa: IDailyca | null = null;
  yearlyCa: IDailyca | null = null;
  columnDefs: any[];
  rowQuantityMonthly: any = [];
  rowAmountMonthly: any = [];
  columnDefsMonthAmount: any[];
  rowQuantityYear: any = [];
  rowAmountYear: any = [];
  columnDefsYearQunatity: any[];
  columnDefsYearAmount: any[];
  TOP_MAX_RESULT = TOP_MAX_RESULT;
  private readonly destroy$ = new Subject<void>();

  constructor(private accountService: AccountService, private dashboardService: DashboardService, private router: Router) {
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
        valueFormatter: this.formatNumber,
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
        valueFormatter: this.formatNumber,
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
        valueFormatter: this.formatNumber,
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
        valueFormatter: this.formatNumber,
      },
    ];
  }

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => (this.account = account));
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  protected subscribeToCaResponse(result: Observable<HttpResponse<IDailyca>>): void {
    result.subscribe(
      (res: HttpResponse<IDailyca>) => this.onDailyCaSuccess(res.body),
      () => {}
    );
  }

  protected onDailyCaSuccess(ca: IDailyca | null): void {
    this.dailyCa = ca;
    this.subscribeToWeeklyCaResponse(this.dashboardService.weekly());
  }

  protected onWeeklyCaSuccess(ca: IDailyca | null): void {
    this.weeklyCa = ca;
    this.subscribeToMonthylyCaResponse(this.dashboardService.monthly());
  }

  protected onMonthyCaCaSuccess(ca: IDailyca | null): void {
    this.monthlyCa = ca;
    this.subscribeToYearlyCaResponse(this.dashboardService.yearly());
  }

  protected onYearlyCaSuccess(ca: IDailyca | null): void {
    this.yearlyCa = ca;
    this.subscribeToMonthlyQuantityResponse(this.dashboardService.monthlyQuantity({ maxResult: TOP_MAX_RESULT }));
  }

  protected subscribeToWeeklyCaResponse(result: Observable<HttpResponse<IDailyca>>): void {
    result.subscribe(
      (res: HttpResponse<IDailyca>) => this.onWeeklyCaSuccess(res.body),
      () => {}
    );
  }

  protected subscribeToYearlyCaResponse(result: Observable<HttpResponse<IDailyca>>): void {
    result.subscribe(
      (res: HttpResponse<IDailyca>) => this.onYearlyCaSuccess(res.body),
      () => {}
    );
  }

  protected subscribeToMonthylyCaResponse(result: Observable<HttpResponse<IDailyca>>): void {
    result.subscribe(
      (res: HttpResponse<IDailyca>) => this.onMonthyCaCaSuccess(res.body),
      () => {}
    );
  }

  protected subscribeToMonthlyQuantityResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe(
      (res: HttpResponse<IStatistiqueProduit>) => this.onMonthlyQuantitySuccess(res.body),
      () => {}
    );
  }

  protected subscribeToMonthlyAmountResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe(
      (res: HttpResponse<IStatistiqueProduit>) => this.onMonthlyAmountSuccess(res.body),
      () => {}
    );
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
    result.subscribe(
      (res: HttpResponse<IStatistiqueProduit>) => this.onYearAmountSuccess(res.body),
      () => {}
    );
  }

  protected subscribeToYearQuantityResponse(result: Observable<HttpResponse<IStatistiqueProduit>>): void {
    result.subscribe(
      (res: HttpResponse<IStatistiqueProduit>) => this.onYearQuantitySuccess(res.body),
      () => {}
    );
  }

  protected onYearQuantitySuccess(stat: IStatistiqueProduit | null): void {
    this.rowQuantityYear = stat;
    this.subscribeToYearAmountResponse(this.dashboardService.yearlyAmount({ maxResult: TOP_MAX_RESULT }));
  }

  protected onYearAmountSuccess(stat: IStatistiqueProduit | null): void {
    this.rowAmountYear = stat;
  }
}
