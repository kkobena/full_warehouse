import { inject, Injectable } from '@angular/core';
import { AccountService } from '../../../core/auth/account.service';

@Injectable({
  providedIn: 'root'
})
export class HasAuthorityService {
  private accountService = inject(AccountService);

  hasAuthorities(authorities: string | string[]): boolean {
    return this.accountService.hasAnyAuthority(authorities);
  }
}
