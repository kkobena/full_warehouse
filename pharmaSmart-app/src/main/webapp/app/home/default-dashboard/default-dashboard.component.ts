import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-default-dashboard',
  templateUrl: './default-dashboard.component.html',
  styleUrl: './default-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, RouterModule],
})
export class DefaultDashboardComponent {
  private readonly accountService = inject(AccountService);

  protected readonly account = toSignal<Account | null>(
    this.accountService.getAuthenticationState(),
    { initialValue: null }
  );
}
