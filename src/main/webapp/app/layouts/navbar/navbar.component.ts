import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { StateStorageService } from 'app/core/auth/state-storage.service';
import HasAnyAuthorityDirective from 'app/shared/auth/has-any-authority.directive';
import { VERSION } from 'app/app.constants';
import { LANGUAGES } from 'app/config/language.constants';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import ActiveMenuDirective from './active-menu.directive';
import NavbarItem from './navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faCoins,
  faSackDollar,
  faShippingFast,
  faShoppingBag,
  faShoppingBasket,
  faStore,
  faUserTimes,
  faWarehouse,
} from '@fortawesome/free-solid-svg-icons';

@Component({
  standalone: true,
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  imports: [RouterModule, WarehouseCommonModule, HasAnyAuthorityDirective, ActiveMenuDirective],
})
export default class NavbarComponent implements OnInit {
  inProduction?: boolean;
  isNavbarCollapsed = signal(true);
  languages = LANGUAGES;
  openAPIEnabled?: boolean;
  version = '';
  account = inject(AccountService).trackCurrentAccount();
  entitiesNavbarItems: NavbarItem[] = [];

  // protected entitiesNavbarItems: any[] = [];
  protected readonly faUserTimes = faUserTimes;
  protected readonly hideLanguage?: boolean = true;
  protected readonly faWarehouse = faWarehouse;
  protected readonly faShoppingBag = faShoppingBag;
  protected readonly faShippingFast = faShippingFast;
  protected readonly faShoppingBasket = faShoppingBasket;
  protected readonly faStore = faStore;
  protected menuStock: string[];
  protected readonly faSackDollar = faSackDollar;
  protected faCoins = faCoins;
  private loginService = inject(LoginService);
  private translateService = inject(TranslateService);
  private stateStorageService = inject(StateStorageService);
  private profileService = inject(ProfileService);
  private router = inject(Router);

  constructor() {
    this.menuStock = ['gestion-entree', 'commande', 'gestion-stock', 'produit'];
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    /*  this.profileService.getProfileInfo().subscribe(profileInfo => {
        this.inProduction = profileInfo.inProduction;

      });*/
  }

  changeLanguage(languageKey: string): void {
    this.stateStorageService.storeLocale(languageKey);
    this.translateService.use(languageKey);
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  isAuthenticated(): boolean {
    return this.account() !== null;
  }

  toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }
}
