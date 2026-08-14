import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map, mergeMap, switchMap } from 'rxjs/operators';

import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-session.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { DashboardResolverService } from 'app/core/auth/dashboard-resolver.service';
import { LicenseService } from 'app/core/license/license.service';
import { Login } from './login.model';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly dashboardResolver = inject(DashboardResolverService);
  private readonly router = inject(Router);
  private readonly licenseService = inject(LicenseService);

  /**
   * Exigence **B2** : le statut de licence est chargé dans la foulée de l'authentification, et
   * l'alerte est levée à ce moment-là.
   *
   * La connexion est le seul instant où l'on est certain que l'utilisateur regarde son écran —
   * c'est donc là qu'une échéance proche a une chance d'être vue et relayée au pharmacien.
   *
   * `load()` absorbe ses propres erreurs : un statut indisponible ne doit jamais empêcher
   * quiconque de se connecter.
   */
  login(credentials: Login): Observable<Account | null> {
    return this.authServerProvider.login(credentials).pipe(
      mergeMap(() => this.accountService.identity(true)),
      switchMap(account =>
        this.licenseService.load().pipe(
          map(() => {
            this.licenseService.notifyIfExpiring();
            return account;
          }),
        ),
      ),
    );
  }

  logoutUrl(): string {
    return this.applicationConfigService.getEndpointFor('api/logout');
  }

  logoutInClient(): void {
    this.dashboardResolver.reset();
    this.licenseService.clear();
    this.accountService.authenticate(null);
  }

  logout(): void {
    this.authServerProvider.logout().subscribe({
      complete: () => {
        this.dashboardResolver.reset();
        this.licenseService.clear();
        this.accountService.authenticate(null);
        this.router.navigate(['/login']);
      },
    });
  }
}
