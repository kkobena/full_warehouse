import { inject, Injectable } from '@angular/core';
import { AccountService } from 'app/core/auth/account.service';
import { TranslateService } from '@ngx-translate/core';
import { NavItem } from 'app/layouts/navbar/navbar-item.model';
import { Authority } from 'app/shared/constants/authority.constants';
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
  faWallet, faDesktop, faSchoolCircleExclamation
} from '@fortawesome/free-solid-svg-icons';

export interface NavigationOptions {
  includeNewSale?: boolean;
  additionalAccountMenuItems?: NavItem[];
  additionalAdminMenuItems?: NavItem[];
}

@Injectable({
  providedIn: 'root',
})
export class NavigationService {
  private accountService = inject(AccountService);
  private translate = inject(TranslateService);

  buildNavItems(options: NavigationOptions = {}): NavItem[] {
    const account = this.accountService.trackCurrentAccount()();
    const allItems: NavItem[] = [];

    // Add "Nouvelle Vente" (New Sale) if requested
    if (options.includeNewSale) {
      allItems.push({
        label: this.translateLabel('nouvelleVente'),
        faIcon: faBasketShopping,
        authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER],
        routerLink: '/sales/false/new',
      });
    }

    // Gestion Courante
    allItems.push({
      label: this.translateLabel('menuGestionCourrante'),
      faIcon: faThList,
      authorities: [Authority.GESTION_COURANT, Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.SALES],
      children: [
        {
          label: this.translateLabel('entities.sales'),
          routerLink: '/sales',
          faIcon: faShoppingBag,
        },
        {
          label: this.translateLabel('mvtCaisse'),
          routerLink: '/mvt-caisse',
          faIcon: faCoins,
          authorities: [Authority.PAYMENT, Authority.ADMIN, Authority.MVT_CAISSE, Authority.BALANCE_CAISSE, Authority.TABLEAU_PHARMACIEN],
        },
      ],
    });

    // Gestion Stock
    allItems.push({
      label: this.translateLabel('menuGestionStock'),
      faIcon: faTruckFast,
      authorities: [
        Authority.ROLE_RESPONSABLE_COMMANDE,
        Authority.GESTION_STOCK,
        Authority.GESTION_ENTREE_STOCK,
        Authority.ADMIN,
        Authority.COMMANDE,
      ],
      children: [
        {
          label: this.translateLabel('entities.produit'),
          routerLink: '/produit',
          faIcon: faBoxOpen,
        },
        {
          label: this.translateLabel('entities.commande'),
          routerLink: '/commande',
          faIcon: faShippingFast,
        },
        {
          label: this.translateLabel('entities.inventoryTransaction'),
          routerLink: '/produit/transaction',
          faIcon: faEye,
        },
        {
          label: this.translateLabel('ajustement'),
          routerLink: '/ajustement',
          faIcon: faSlidersH,
        },
        {
          label: this.translateFullLabel('gestionPerimes.title'),
          routerLink: '/gestion-peremption',
          faIcon: faCalendarTimes,
        },
        {
          label: this.translateLabel('entities.storeInventory'),
          routerLink: '/store-inventory',
          faIcon: faClipboardList,
          authorities: [Authority.STORE_INVENTORY, Authority.ADMIN],
        },
        {
          label: this.translateLabel('entities.depot'),
          routerLink: '/depot',
          faIcon: faBuilding,
        },
      ],
    });

    // Facturation
    allItems.push({
      label: this.translateLabel('facturation.title'),
      faIcon: faWallet,
      authorities: [Authority.ADMIN, Authority.GESTION_FACTURATION],
      children: [
        {
          label: this.translateLabel('facturation.factures'),
          routerLink: '/edition-factures',
          faIcon: faFileInvoice,
        },
        {
          label: this.translateLabel('facturation.reglements'),
          routerLink: '/reglement-facture',
          faIcon: faMoneyBill,
        },
        {
          label: this.translateLabel('facturation.differes'),
          routerLink: '/gestion-differe',
          faIcon: faMoneyCheckAlt,
        },
        {
          label: this.translateLabel('facturation.tiersPayant'),
          routerLink: '/tiers-payant',
          faIcon: faLink,
        },
        {
          label: this.translateLabel('facturation.client'),
          routerLink: '/customer',
          faIcon: faUsers,
        },
      ],
    });

    // Référentiel
    allItems.push({
      label: this.translateLabel('referentiel'),
      faIcon: faBook,
      authorities: [Authority.REFERENTIEL, Authority.ADMIN],
      children: [
        { label: this.translateLabel('entities.rayon'), routerLink: '/rayon', faIcon: faStream },
        {
          label: this.translateLabel('entities.remise'),
          routerLink: '/remises',
          authorities: [Authority.ADMIN, Authority.REMISE],
          faIcon: faPercent,
        },
        { label: this.translateLabel('entities.tableau'), routerLink: '/tableaux', faIcon: faTable },
        { label: this.translateLabel('entities.fournisseur'), routerLink: '/fournisseur', faIcon: faTruck },
        { label: this.translateLabel('entities.tva'), routerLink: '/tva', faIcon: faDollarSign },
        {
          label: this.translateLabel('entities.formeProduit'),
          routerLink: '/forme-produit',
          faIcon: faPills,
        },
        {
          label: this.translateLabel('entities.familleProduit'),
          routerLink: '/famille-produit',
          faIcon: faBoxes,
        },
        { label: this.translateLabel('gammeProduit'), routerLink: '/gamme-produit', faIcon: faMapMarker },
        { label: this.translateLabel('laboratoire'), routerLink: '/laboratoire', faIcon: faBuilding },

        {
          label: this.translateLabel('motifAjustement'),
          routerLink: '/motif-ajustement',
          faIcon: faExclamationTriangle,
        },
        {
          label:'Motif Retour Produit',
          routerLink: '/motif-retour-produit',
          faIcon: faSchoolCircleExclamation,
        },



        {
          label: this.translateLabel('parametre'),
          routerLink: '/parametre',
          authorities: [Authority.ADMIN, 'parametre'],
          faIcon: faCog,
        },
      ],
    });

    // Admin Section
    const adminChildren: NavItem[] = [
      {
        label: this.translateLabel('entities.magasin'),
        routerLink: '/magasin',
        faIcon: faStore,
      },

      {
        label: this.translateLabel('admin.userManagement'),
        routerLink: '/admin/user-management',
        faIcon: faUsers,
      },
      {
        label: 'Poste de Travail',
        routerLink: '/poste',
        faIcon: faDesktop,
      },
      {
        label: this.translateLabel('entities.menu'),
        routerLink: '/menu',
        faIcon: faCogs,
      },
    ];

    // Add additional admin menu items if provided
    if (options.additionalAdminMenuItems) {
      adminChildren.push(...options.additionalAdminMenuItems);
    }

    allItems.push({
      label: this.translateLabel('admin.main'),
      faIcon: faCogs,
      authorities: [Authority.ADMIN, Authority.MENU_ADMIN, Authority.USER_MANAGEMENT, Authority.MAGASIN],
      children: adminChildren,
    });

    // Account Section
    const accountChildren: NavItem[] = [
      {
        label: this.translateLabel('account.settings'),
        routerLink: '/account/settings',
        faIcon: 'wrench',
      },
      {
        authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.MY_CASH_REGISTER],
        label: this.translateLabel('account.cashRegister'),
        routerLink: '/my-cash-register',
        faIcon: faCashRegister,
      },
      {
        label: this.translateLabel('account.password'),
        routerLink: '/account/password',
        faIcon: 'lock',
      },
    ];

    // Add additional account menu items if provided
    if (options.additionalAccountMenuItems) {
      accountChildren.push(...options.additionalAccountMenuItems);
    }

    allItems.push({
      label: this.translateLabel('account.main'),
      faIcon: 'user',
      children: accountChildren,
    });

    // Filter by authority if user is authenticated
    if (account) {
      return this.filterByAuthority(allItems, account.authorities);
    }

    // Return unauthenticated menu items
    return [];
  }

  /**
   * Build unauthenticated navigation items
   */
  buildUnauthenticatedNavItems(additionalItems: NavItem[] = []): NavItem[] {
    return [
      {
        label: this.translateLabel('account.main'),
        faIcon: 'user',
        children: additionalItems,
      },
    ];
  }

  /**
   * Check if user has any of the specified authorities
   */
  hasAnyAuthority(authorities: string[] | string, userAuthorities: string[]): boolean {
    if (!Array.isArray(authorities)) {
      authorities = [authorities];
    }
    return userAuthorities.some((authority: string) => authorities.includes(authority));
  }

  /**
   * Filter navigation items by user authorities
   */
  private filterByAuthority(items: NavItem[], userAuthorities: string[]): NavItem[] {
    return items
      .filter(item => !item.authorities || this.hasAnyAuthority(item.authorities, userAuthorities))
      .map(item => {
        if (item.children) {
          item.children = this.filterByAuthority(item.children, userAuthorities);
        }
        return item;
      })
      .filter(item => (item.children ? item.children.length > 0 : true));
  }

  /**
   * Translate menu label
   */
  private translateLabel(key: string): string {
    return this.translate.instant(`global.menu.${key}`);
  }

  /**
   * Translate full label
   */
  private translateFullLabel(key: string): string {
    return this.translate.instant(`warehouseApp.${key}`);
  }
}
