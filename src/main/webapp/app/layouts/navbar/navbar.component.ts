import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { environment } from 'environments/environment';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';
import NavbarItem, { NavItem } from './navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faBasketShopping,
  faCoins,
  faPalette,
  faSackDollar,
  faShippingFast,
  faShoppingBag,
  faShoppingBasket,
  faStore,
  faTimes,
  faWarehouse
} from '@fortawesome/free-solid-svg-icons';
import { Theme, ThemeService } from '../../core/theme/theme.service';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  imports: [RouterModule, WarehouseCommonModule]
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
  private themeService = inject(ThemeService);
  themes: Theme[];
  selectedTheme: string;
  readonly faPalette = faPalette;


  changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  constructor() {
    this.menuStock = ['gestion-entree', 'commande', 'gestion-stock', 'produit'];
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.themes = this.themeService.getThemes();
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

 protected readonly  MENU_ITEMS: NavItem[] = [
    // Gestion Courante
    {
      translationKey: 'global.menu.menuGestionCourrante',
      faIcon: 'th-list',
      authorities: ['gestion-courant', 'ROLE_ADMIN', 'ROLE_CAISSIER', 'ROLE_VENDEUR', 'sales'],
      children: [
        {
          translationKey: 'global.menu.entities.sales',
          routerLink: '/sales',
          faIcon: 'shopping-bag',
        },
        {
          translationKey: 'global.menu.mvtCaisse',
          routerLink: '/mvt-caisse',
          faIcon: 'coins',
          authorities: ['payment', 'ROLE_ADMIN', 'mvt-caisse', 'tableau-pharmacien'],
        },
      ],
    },
    // Gestion Stock
    {
      translationKey: 'global.menu.menuGestionStock',
      faIcon: 'warehouse',
      authorities: ['gestion-stock', 'ROLE_ADMIN', 'commande', 'ROLE_RESPONSABLE_COMMANDE'],
      children: [
        {
          translationKey: 'global.menu.entities.commande',
          routerLink: '/commande',
          faIcon: 'shipping-fast',
        },
        {
          translationKey: 'global.menu.entities.produit',
          routerLink: '/produit',
          faIcon: 'box-open',
        },
        {
          translationKey: 'global.menu.entities.inventoryTransaction',
          routerLink: '/produit/transaction',
          faIcon: 'exchange-alt',
        },
        {
          translationKey: 'warehouseApp.gestionPerimes.title',
          routerLink: '/gestion-peremption',
          faIcon: 'calendar-times',
        },
        {
          translationKey: 'global.menu.ajustement', // NOTE: Create this translation key
          routerLink: '/ajustement',
          faIcon: 'sliders-h',
        },
        {
          translationKey: 'global.menu.entities.storeInventory',
          routerLink: '/store-inventory',
          faIcon: 'clipboard-list',
          authorities: ['store-inventory', 'ROLE_ADMIN'],
        },
      ],
    },
    // Référentiel
    {
      translationKey: 'global.menu.referentiel',
      faIcon: 'book',
      authorities: ['referentiel', 'ROLE_ADMIN'],
      children: [
        { translationKey: 'global.menu.entities.rayon', routerLink: '/rayon', faIcon: 'stream' },
        { translationKey: 'global.menu.entities.remise', routerLink: '/remises', faIcon: 'percent' },
        { translationKey: 'global.menu.entities.tableau', routerLink: '/tableaux', faIcon: 'table' },
        { translationKey: 'global.menu.entities.fournisseur', routerLink: '/fournisseur', faIcon: 'truck' },
      ]}]
}
