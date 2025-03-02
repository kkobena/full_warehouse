import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-session.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Login } from './login.model';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly router = inject(Router);
  login(credentials: Login): Observable<Account | null> {
    return this.authServerProvider.login(credentials).pipe(mergeMap(() => this.accountService.identity(true)));
  }

  logoutUrl(): string {
    return this.applicationConfigService.getEndpointFor('api/logout');
  }

  logoutInClient(): void {
    this.accountService.authenticate(null);
  }

  logout(): void {
    this.authServerProvider.logout().subscribe({
      complete: () => {
        this.accountService.authenticate(null);
        this.router.navigate(['/login']);
      },
    });
  }
}
