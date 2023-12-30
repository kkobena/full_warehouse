import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { WarehouseCommonModule } from '../shared/warehouse-common/warehouse-common.module';
import { CardModule } from 'primeng/card';
import { HomeGrapheComponent } from './home-graphe/home-graphe.component';
import { YearlyDataComponent } from './yearly/yearly-data/yearly-data.component';
import { MonthlyDataComponent } from './monthly/monthly-data/monthly-data.component';
import { HalfyearlyDataComponent } from './halfyearly/halfyearly-data/halfyearly-data.component';
import { WeeklyDataComponent } from './weekly/weekly-data/weekly-data.component';
import { DailyDataComponent } from './daily/daily-data/daily-data.component';

@Component({
  standalone: true,
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [
    WarehouseCommonModule,
    RouterModule,
    CardModule,
    HomeGrapheComponent,
    HalfyearlyDataComponent,
    YearlyDataComponent,
    MonthlyDataComponent,
    WeeklyDataComponent,
    DailyDataComponent,
  ],
})
export class HomeComponent implements OnInit, OnDestroy {
  account: Account | null = null;
  active = 'daily';
  private readonly destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private router: Router,
  ) {}

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
