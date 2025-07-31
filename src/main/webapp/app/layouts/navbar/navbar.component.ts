import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { environment } from 'environments/environment';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import NavbarItem from './navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faBasketShopping,
  faCoins,
  faSackDollar,
  faShippingFast,
  faShoppingBag,
  faShoppingBasket,
  faStore,
  faTimes,
  faWarehouse,
} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  imports: [RouterModule, WarehouseCommonModule],
})
export default class NavbarComponent implements OnInit {
  protected isNavbarCollapsed = signal(true);
  protected version = '';
  protected account = inject(AccountService).trackCurrentAccount();
  protected entitiesNavbarItems: NavbarItem[] = [];

  // protected entitiesNavbarItems: any[] = [];

  protected readonly faWarehouse = faWarehouse;
  protected readonly faShoppingBag = faShoppingBag;
  protected readonly faShippingFast = faShippingFast;
  protected readonly faShoppingBasket = faShoppingBasket;
  protected readonly faStore = faStore;
  protected readonly basketShoppingPlus = faBasketShopping;
  protected menuStock: string[] = [];
  protected readonly faSackDollar = faSackDollar;
  protected faCoins = faCoins;
  protected readonly faTimes = faTimes;
  private loginService = inject(LoginService);
  private router = inject(Router);

  constructor() {
    this.menuStock = ['gestion-entree', 'commande', 'gestion-stock', 'produit'];
    const { VERSION } = environment;
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

  protected collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  protected login(): void {
    this.router.navigate(['/login']);
  }

  protected logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  protected toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }

  protected hasAnyAuthority(authorities: string[] | string): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    if (!Array.isArray(authorities)) {
      authorities = [authorities];
    }
    return userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }
}
