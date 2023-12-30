import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { VERSION } from 'app/app.constants';
import { LANGUAGES } from 'app/config/language.constants';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import { faShippingFast, faShoppingBag, faShoppingBasket, faStore, faUserTimes, faWarehouse } from '@fortawesome/free-solid-svg-icons';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { HasAnyAuthorityDirective } from '../../shared/auth/has-any-authority.directive';
import ActiveMenuDirective from './active-menu.directive';
import NavbarItem from './navbar-item.model';
import { StateStorageService } from '../../core/auth/state-storage.service';

@Component({
  standalone: true,

  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [RouterModule, WarehouseCommonModule, HasAnyAuthorityDirective, ActiveMenuDirective],
})
export class NavbarComponent implements OnInit {
  entitiesNavbarItems: NavbarItem[] = [];
  protected inProduction?: boolean;
  protected isNavbarCollapsed = true;
  protected languages = LANGUAGES;
  protected openAPIEnabled?: boolean = false;
  protected version = '';
  protected account: Account | null = null;
  // protected entitiesNavbarItems: any[] = [];
  protected faUserTimes = faUserTimes;
  protected hideLanguage?: boolean = true;
  protected faWarehouse = faWarehouse;
  protected faShoppingBag = faShoppingBag;
  protected faShippingFast = faShippingFast;
  protected faShoppingBasket = faShoppingBasket;
  protected faStore = faStore;
  protected menuStock: string[];

  constructor(
    private loginService: LoginService,
    private translateService: TranslateService,
    private stateStorageService: StateStorageService,
    private accountService: AccountService,
    private profileService: ProfileService,
    private router: Router,
  ) {
    this.menuStock = ['gestion-entree', 'commande', 'gestion-stock', 'produit'];
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction = profileInfo.inProduction;
    });

    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
    });
  }

  /*
    changeLanguage(languageKey: string): void {
      this.sessionStorageService.store('locale', languageKey);
      this.translateService.use(languageKey);
    }
  */
  collapseNavbar(): void {
    this.isNavbarCollapsed = true;
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.accountService.isAuthenticated();
  }

  /* logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
  }*/

  toggleNavbar(): void {
    this.isNavbarCollapsed = !this.isNavbarCollapsed;
  }

  getImageUrl(): string {
    return '';
  }

  changeLanguage(languageKey: string): void {
    this.stateStorageService.storeLocale(languageKey);
    this.translateService.use(languageKey);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }
}
