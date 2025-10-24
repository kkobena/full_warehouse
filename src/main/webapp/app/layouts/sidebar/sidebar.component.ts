import { Component, effect, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { NavItem } from '../navbar/navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faBasketShopping,
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
  faLink,
  faMapMarker,
  faMoneyBill,
  faMoneyCheckAlt,
  faPalette,
  faPercent,
  faPills,
  faShippingFast,
  faShoppingBag,
  faSlidersH,
  faStore,
  faStream,
  faTable,
  faThList,
  faTruck,
  faTruckFast,
  faUsers,
  faWallet,
  faServer,
  faBars,
  faChevronDown,
  faChevronRight,
  faUserCircle
} from '@fortawesome/free-solid-svg-icons';
import { Theme, ThemeService } from '../../core/theme/theme.service';
import { TranslateService } from '@ngx-translate/core';
import { Authority } from '../../shared/constants/authority.constants';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsDialogComponent } from '../../shared/settings/app-settings-dialog.component';
import { LayoutService } from '../../core/config/layout.service';
import { environment } from 'environments/environment';

@Component({
  selector: 'jhi-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, WarehouseCommonModule, FormsModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export default class SidebarComponent implements OnInit {
  protected account = inject(AccountService).trackCurrentAccount();
  protected version = '';
  navItems: NavItem[] = [];
  expandedItems = new Set<string>();

  private loginService = inject(LoginService);
  private router = inject(Router);
  private themeService = inject(ThemeService);
  private translate = inject(TranslateService);
  private modalService = inject(NgbModal);
  protected layoutService = inject(LayoutService);

  themes: Theme[];
  selectedTheme: string;


  readonly faBars = faBars;
  readonly faChevronDown = faChevronDown;
  readonly faChevronRight = faChevronRight;
  readonly faUserCircle = faUserCircle;

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
    effect(() => {
      this.navItems = this.buildNavItem();
    });
  }

  ngOnInit(): void {
   // this.themes = this.themeService.getThemes();
  }

  protected isCollapsed(): boolean {
    return this.layoutService.isSidebarCollapsed();
  }

  protected toggleSidebar(): void {
    this.layoutService.toggleSidebarCollapsed();
  }

  protected toggleItem(label: string): void {
    if (this.expandedItems.has(label)) {
      this.expandedItems.delete(label);
    } else {
      this.expandedItems.add(label);
    }
  }

  protected expandItem(label: string): void {
    this.expandedItems.add(label);
  }

  protected collapseItem(label: string): void {
    this.expandedItems.delete(label);
  }

  protected isExpanded(label: string): boolean {
    return this.expandedItems.has(label);
  }

  protected onParentMenuHover(label: string, isEntering: boolean): void {
    if (isEntering) {
      this.expandItem(label);
    } else {
      this.collapseItem(label);
    }
  }

  protected onParentMenuClick(label: string): void {
    if (this.isCollapsed()) {
      // If sidebar is collapsed, expand it and show the menu
      this.toggleSidebar();
      this.expandItem(label);
    } else {
      // If sidebar is expanded, toggle the menu item
      this.toggleItem(label);
    }
  }

  protected onMenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) {
      clickHandler();
    }
    // Collapse sidebar if it's expanded when clicking a menu item without children
    if (!this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  protected onSubmenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) {
      clickHandler();
    }
    // Collapse sidebar if it's expanded when clicking a submenu item
    if (!this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  protected changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  protected login(): void {
    this.router.navigate(['/login']);
  }

  protected logout(): void {
    this.loginService.logout();
    this.router.navigate(['']);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: 'lg', backdrop: 'static' });
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
    // Same menu structure as navbar
    const allItems: NavItem[] = [
      {
        label: this.translateLabel('nouvelleVente'),
        faIcon: faBasketShopping,
        authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER],
        routerLink: '/sales/false/new'
      },
      {
        label: this.translateLabel('menuGestionCourrante'),
        faIcon: faThList,
        authorities: [Authority.GESTION_COURANT, Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.SALES],
        children: [
          {
            label: this.translateLabel('entities.sales'),
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
          },
          {
            label: this.translateLabel('facturation.client'),
            routerLink: '/customer',
            faIcon: faUsers
          }
        ]
      },
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
          },
          {
            label: 'Paramètres Serveur',
            faIcon: faServer,
            click: () => this.openAppSettings()
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
            label: 'Menu horizontal',
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
            label: 'Menu horizontal',
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
}
