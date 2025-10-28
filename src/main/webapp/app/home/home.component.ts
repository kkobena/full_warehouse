import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { WarehouseCommonModule } from '../shared/warehouse-common/warehouse-common.module';
import { CardModule } from 'primeng/card';
import { HalfyearlyDataComponent } from './halfyearly/halfyearly-data/halfyearly-data.component';
import { YearlyDataComponent } from './yearly/yearly-data/yearly-data.component';
import { MonthlyDataComponent } from './monthly/monthly-data/monthly-data.component';
import { WeeklyDataComponent } from './weekly/weekly-data/weekly-data.component';
import { DailyDataComponent } from './daily/daily-data/daily-data.component';
import { Authority } from '../shared/constants/authority.constants';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  imports: [
    WarehouseCommonModule,
    RouterModule,
    CardModule,
    HalfyearlyDataComponent,
    YearlyDataComponent,
    MonthlyDataComponent,
    WeeklyDataComponent,
    DailyDataComponent,

  ],
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  active = 'daily';
  private readonly destroy$ = new Subject<void>();

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => this.account.set(account));
    if (!this.isAdmin()) {
      if (this.isCaissier() || this.isVendeur) {
        this.router.navigate(['/sales']);
      } else if (this.isResponsableCommande()) {
        this.router.navigate(['/commande']);
      } else {
        this.router.navigate(['/account/settings']);
      }
    }
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected isAdmin(): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    return userIdentity.authorities.includes(Authority.ADMIN) || userIdentity.authorities.includes(Authority.HOME_DASHBOARD);
  }

  protected hasAnyAuthority(authoritie: string): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }

    return userIdentity.authorities.includes(authoritie) && !this.isAdmin();
  }

  protected isCaissier(): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    return userIdentity.authorities.includes(Authority.ROLE_CAISSIER);
  }

  protected isResponsableCommande(): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    return userIdentity.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE);
  }

  protected isVendeur(): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    return userIdentity.authorities.includes(Authority.ROLE_VENDEUR);
  }
}
