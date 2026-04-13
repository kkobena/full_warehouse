import { Component, inject, OnDestroy, OnInit, signal } from "@angular/core";
import { Router, RouterModule } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { AccountService } from "app/core/auth/account.service";
import { Account } from "app/core/auth/account.model";
import { Authority } from "../shared/constants/authority.constants";
import { CaissierDashboardComponent } from "./caissier-dashboard/caissier-dashboard.component";
import { CommandeHomeComponent } from "../features/commande/feature/commande-home/commande-home.component";
import { HomeBaseComponent } from "./home-base/home-base.component";

@Component({
  selector: "jhi-home",
  templateUrl: "./home.component.html",
  styleUrl: "./home.component.scss",
  imports: [
    RouterModule,
    HomeBaseComponent,
    CaissierDashboardComponent,
    CommandeHomeComponent
  ]
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  private readonly destroy$ = new Subject<void>();
  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account.set(account);
        if (account && this.isVendeur() && !this.isAdmin() && !this.isResponsableCommande() && !this.isCaissier()) {
          this.router.navigate(['/sales-home/prevente']);
        }
      });
  }

  protected login(): void {
    this.router.navigate(["/login"]);
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
