import { Component, inject, OnDestroy, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AccountService } from 'app/core/auth/account.service';
import { DashboardResolverService } from 'app/core/auth/dashboard-resolver.service';
import { Account } from 'app/core/auth/account.model';
import { SkeletonComponent } from 'app/shared/ui';
import { CaissierDashboardComponent } from './caissier-dashboard/caissier-dashboard.component';
import { CommandeHomeComponent } from '../features/commande/feature/commande-home/commande-home.component';
import { HomeBaseComponent } from './home-base/home-base.component';
import { DefaultDashboardComponent } from './default-dashboard/default-dashboard.component';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    RouterModule,
    SkeletonComponent,
    HomeBaseComponent,
    CaissierDashboardComponent,
    CommandeHomeComponent,
    DefaultDashboardComponent,
  ],
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  /** true pendant le chargement du layout résolu */
  loading = signal(true);

  private readonly destroy$ = new Subject<void>();
  private readonly accountService = inject(AccountService);
  private readonly dashboardResolver = inject(DashboardResolverService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account.set(account);
        if (!account) {
          this.router.navigate(['/login']);
          return;
        }
        // Remet loading à true pour afficher le skeleton pendant la résolution
        this.loading.set(true);
        // Charge (ou retourne depuis le cache signal) le layout résolu
        this.dashboardResolver.init()
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            complete: () => {
              this.loading.set(false);
              // Redirection si isRoute=true
              if (this.dashboardResolver.isRoute()) {
                const path = this.dashboardResolver.routePath();
                console.error('path', path);
                if (path) {
                  this.router.navigate([path]);
                }
              }
            },
          });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Exposé au template via le service resolver */
  protected get resolvedLayout() {
    return this.dashboardResolver.resolvedLayout;
  }

  protected get isRoute() {
    return this.dashboardResolver.isRoute;
  }
}
