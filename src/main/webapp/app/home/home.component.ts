import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
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
import { VenteRecord } from '../shared/model/vente-record.model';

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
  dailyCa: VenteRecord | null = null;
  weeklyCa: VenteRecord | null = null;
  monthlyCa: VenteRecord | null = null;
  yearlyCa: VenteRecord | null = null;
  columnDefs: any[];
  rowQuantityMonthly: any = [];
  rowAmountMonthly: any = [];
  columnDefsMonthAmount: any[];
  rowQuantityYear: any = [];
  rowAmountYear: any = [];
  columnDefsYearQunatity: any[];
  columnDefsYearAmount: any[];
  TOP_MAX_RESULT = TOP_MAX_RESULT;
  active = 'daily';
  private readonly destroy$ = new Subject<void>();

  constructor(private accountService: AccountService, private router: Router) {}

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account = account;
      });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
