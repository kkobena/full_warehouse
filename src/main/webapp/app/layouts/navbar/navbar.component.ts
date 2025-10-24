import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { environment } from 'environments/environment';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { NavItem } from './navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faBook,
  faBoxes,
  faBoxOpen,
  faBuilding,
  faCalendarTimes,
  faCashRegister,
  faClipboardList,
  faCog,
  faCogs,
  faCoins,
  faDollarSign,
  faExclamationTriangle,
  faEye,
  faFileInvoice,
  faKeyboard,
  faLink,
  faMapMarker,
  faMoneyBill,
  faMoneyCheckAlt,
  faPalette,
  faPercent,
  faPills,
  faSackDollar,
  faShippingFast,
  faShoppingBag,
  faShoppingBasket,
  faSlidersH,
  faStore,
  faStream,
  faTable,
  faThList,
  faTimes,
  faTruck,
  faTruckFast,
  faUsers,
  faWallet,
  faServer, faBars
} from '@fortawesome/free-solid-svg-icons';
import { Theme, ThemeService } from '../../core/theme/theme.service';
import { TranslateService } from '@ngx-translate/core';
import { Authority } from '../../shared/constants/authority.constants';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsDialogComponent } from '../../shared/settings/app-settings-dialog.component';
import { ShortcutsHelpDialogComponent } from '../../shared/shortcuts/shortcuts-help-dialog.component';
import { LayoutService } from '../../core/config/layout.service';

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
  navItems: NavItem[] = [];
  protected menuStock: string[] = [];
  protected readonly faTimes = faTimes;
  private loginService = inject(LoginService);
  private router = inject(Router);
  private themeService = inject(ThemeService);
  private translate = inject(TranslateService);
  private modalService = inject(NgbModal);
  protected layoutService = inject(LayoutService);

  themes: Theme[];
  selectedTheme: string;
  readonly faPalette = faPalette;
  readonly faServer = faServer;


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
    effect(() => {
      this.navItems = this.buildNavItem();
    });
  }

  ngOnInit(): void {
    this.themes = this.themeService.getThemes();

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

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: 'lg', backdrop: 'static' });
  }

  protected openShortcutsHelp(): void {
    this.modalService.open(ShortcutsHelpDialogComponent, { size: 'xl', backdrop: 'static', scrollable: true });
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

  private buildNavItem(): NavItem[] {
    const allItems: NavItem[] = [
     /* {
        label: this.translateLabel('nouvelleVente'),
        faIcon: faBasketShopping,
        authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER],
        routerLink: '/sales/false/new'
      },*/

      {
        label: this.translateLabel('menuGestionCourrante'),
        faIcon: faThList,
        authorities: [Authority.GESTION_COURANT, Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.SALES],
        children: [
          {
            label:this.translateLabel('entities.sales'),
            routerLink: '/sales',
            faIcon: faShoppingBag
          },
          {
            label: this.translateLabel('mvtCaisse'),
            routerLink: '/mvt-caisse',
            faIcon: faCoins,
            authorities: [Authority.PAYMENT, Authority.ADMIN, Authority.MVT_CAISSE, Authority.BALANCE_CAISSE, Authority.TABLEAU_PHARMACIEN]
          }
        ]
      },
      // Gestion Stock
      {
        label: this.translateLabel('menuGestionStock'),
        faIcon: faTruckFast,
        authorities: [Authority.ROLE_RESPONSABLE_COMMANDE, Authority.GESTION_STOCK, Authority.GESTION_ENTREE_STOCK, Authority.ADMIN, Authority.COMMANDE],
        children: [
          {
            label: this.translateLabel('entities.produit'),
            routerLink: '/produit',
            faIcon: faBoxOpen
          },
          {
            label: this.translateLabel('entities.commande'),
            routerLink: '/commande',
            faIcon: faShippingFast
          },

          {
            label: this.translateLabel('entities.inventoryTransaction'),
            routerLink: '/produit/transaction',
            faIcon: faEye
          },

          {
            label: this.translateLabel('ajustement'),
            routerLink: '/ajustement',
            faIcon: faSlidersH
          },
          {
            label: this.translateFullLabel('gestionPerimes.title'),
            routerLink: '/gestion-peremption',
            faIcon: faCalendarTimes
          },
          {
            label: this.translateLabel('entities.storeInventory'),
            routerLink: '/store-inventory',
            faIcon: faClipboardList,
            authorities: [Authority.STORE_INVENTORY, Authority.ADMIN]
          }
        ]
      },


      {
        label: this.translateLabel('facturation.title'),
        faIcon: faWallet,
        authorities: [Authority.ADMIN, Authority.GESTION_FACTURATION],
        children: [
          {
            label: this.translateLabel('facturation.factures'),
            routerLink: '/edition-factures',
            faIcon: faFileInvoice
          },
          {
            label: this.translateLabel('facturation.reglements'),
            routerLink: '/reglement-facture',
            faIcon: faMoneyBill

          },
          {
            label: this.translateLabel('facturation.differes'),
            routerLink: '/gestion-differe',
            faIcon: faMoneyCheckAlt

          },
          {
            label: this.translateLabel('facturation.tiersPayant'),
            routerLink: '/tiers-payant',
            faIcon: faLink
          }, {
            label: this.translateLabel('facturation.client'),
            routerLink: '/customer',
            faIcon: faUsers
          }
        ]
      },
      // Référentiel
      {
        label: this.translateLabel('referentiel'),
        faIcon: faBook,
        authorities: [Authority.REFERENTIEL, Authority.ADMIN],
        children: [
          { label: this.translateLabel('entities.rayon'), routerLink: '/rayon', faIcon: faStream },
          {
            label: this.translateLabel('entities.remise'),
            routerLink: '/remises',
            authorities: [Authority.ADMIN, Authority.REMISE],
            faIcon: faPercent
          },
          { label: this.translateLabel('entities.tableau'), routerLink: '/tableaux', faIcon: faTable },
          { label: this.translateLabel('entities.fournisseur'), routerLink: '/fournisseur', faIcon: faTruck },
          { label: this.translateLabel('entities.tva'), routerLink: '/tva', faIcon: faDollarSign },
          {
            label: this.translateLabel('entities.formeProduit'),
            routerLink: '/forme-produit',
            faIcon: faPills
          },
          {
            label: this.translateLabel('entities.familleProduit'),
            routerLink: '/famille-produit',
            faIcon: faBoxes
          },
          { label: this.translateLabel('gammeProduit'), routerLink: '/gamme-produit', faIcon: faMapMarker },
          { label: this.translateLabel('laboratoire'), routerLink: '/laboratoire', faIcon: faBuilding },
          {
            label: this.translateLabel('motifAjustement'),
            routerLink: '/motif-ajustement',
            faIcon: faExclamationTriangle
          },
          {
            label: this.translateLabel('parametre'),
            routerLink: '/parametre',
            authorities: [Authority.ADMIN, 'parametre'],
            faIcon: faCog
          }
        ]
      },

      {
        label: this.translateLabel('admin.main'),
        faIcon: faCogs,
        authorities: [Authority.ADMIN, Authority.MENU_ADMIN, Authority.USER_MANAGEMENT, Authority.MAGASIN],
        children: [
          {
            label: this.translateLabel('entities.magasin'),
            routerLink: '/magasin',
            faIcon: faStore
          },
          {
            label: this.translateLabel('admin.userManagement'),
            routerLink: '/admin/user-management',
            faIcon: faUsers

          },
          {
            label: this.translateLabel('entities.menu'),
            routerLink: '/menu',
            faIcon: faCogs

          }


        ]
      },
      {
        label: this.translateLabel('account.main'),
        faIcon: 'user',
        children: [
          {
            label: this.translateLabel('account.settings'),
            routerLink: '/account/settings',
            faIcon: 'wrench'
          },
          {
            authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.MY_CASH_REGISTER],
            label: this.translateLabel('account.cashRegister'),
            routerLink: '/my-cash-register',
            faIcon: faCashRegister

          },
          {
            label: this.translateLabel('account.password'),
            routerLink: '/account/password',
            faIcon: 'lock'

          },
          {
            label: 'Raccourcis Clavier',
            faIcon: faKeyboard,
            click: () => this.openShortcutsHelp()
          },
          {
            label: 'Menu vertical',
            faIcon: faBars,
            click: () => this.layoutService.toggleLayout()
          },
          {
            label: this.translateLabel('account.logout'),
            faIcon: 'sign-out-alt',
            click: () => this.logout()

          }

        ]
      }
    ];

    if (this.account()) {
      const filterByAuthority = (items: NavItem[]): NavItem[] => {
        return items
          .filter(item => !item.authorities || this.hasAnyAuthority(item.authorities))
          .map(item => {
            if (item.children) {
              item.children = filterByAuthority(item.children);
            }
            return item;
          })
          .filter(item => item.children ? item.children.length > 0 : true);
      };

      return filterByAuthority(allItems);
    }
    return [
      {
        label: this.translateLabel('account.main'),
        faIcon: 'user',
        children: [
          {
            label: 'Paramètres Serveur',
            faIcon: faServer,
            click: () => this.openAppSettings()
          },
          {
            label: 'Raccourcis Clavier',
            faIcon: faKeyboard,
            click: () => this.openShortcutsHelp()
          },
          {
            label: 'Menu vertical',
            faIcon: faBars,
            click: () => this.layoutService.toggleLayout()
          },
          {
            label: this.translateLabel('account.login'),
            faIcon: 'sign-out-alt',
            click: () => this.login()

          }

        ]
      }
    ];
  }

  private translateLabel(key: string): string {
    return this.translate.instant(`global.menu.${key}`);
  }

  private translateFullLabel(key: string): string {
    return this.translate.instant(`warehouseApp.${key}`);
  }

  protected readonly faSideBare = faBars;
}
