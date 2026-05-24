import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {NavItem} from 'app/layouts/navbar/navbar-item.model';
import {PeremptionAlertService} from '../../shared/services/peremption-alert.service';
import {NavStore} from 'app/core/store/nav.store';
import {INavNode} from 'app/shared/model/nav-item.model';
import {IconProp} from '@fortawesome/fontawesome-svg-core';
import {
  faAlignJustify,
  faArrowsAltH,
  faArrowsRotate,
  faBook,
  faBookmark,
  faBoxOpen,
  faBuilding,
  faCalculator,
  faCalendarTimes,
  faCashRegister,
  faChartBar,
  faClipboardList,
  faClock,
  faCog,
  faCogs,
  faCoins,
  faCreditCard,
  faDesktop,
  faDollarSign,
  faExclamationTriangle,
  faFileAlt,
  faFileCirclePlus,
  faFileInvoice,
  faFilePen,
  faLightbulb,
  faLock,
  faMoneyBill,
  faPercent,
  faRotateLeft,
  faSdCard,
  faShield,
  faShippingFast,
  faShoppingBag,
  faShoppingCart,
  faSitemap,
  faSlidersH,
  faSortAmountDown,
  faStar,
  faStore,
  faStream,
  faTable,
  faTableCells,
  faTags,
  faThList,
  faTrash,
  faTruck,
  faUsers,
  faWallet,
} from '@fortawesome/free-solid-svg-icons';

export interface NavigationOptions {
  includeNewSale?: boolean;
  additionalAccountMenuItems?: NavItem[];
}

@Injectable({
  providedIn: 'root',
})
export class NavigationService {
  private readonly translate = inject(TranslateService);
  private readonly peremptionAlertService = inject(PeremptionAlertService);
  private readonly navStore = inject(NavStore);

  /**
   * Construit le menu depuis le NavStore dynamique.
   */
  buildNavItemsFromStore(options: NavigationOptions = {}): NavItem[] {
    const tree = this.navStore.navTree();
    if (!tree.length) {
      return [];
    }
    const items = this.mapNodesToNavItems(tree);
    items.push(this.buildAccountMenu(options));
    return items;
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

  /** Mappe récursivement les INavNode → NavItem. */
  private mapNodesToNavItems(nodes: INavNode[]): NavItem[] {
    return nodes
      .filter(n => n.permissions?.canDisplay !== false)
      .filter(n => n.targetType !== 'SECTION' && n.targetType !== 'ACTION')
      .sort((a, b) => a.ordre - b.ordre)
      .map(n => {
        const perimesCount = n.code === 'peremptions' ? this.peremptionAlertService.urgentCount() : 0;
        return {
          label: n.libelle,
          routerLink: n.targetType === 'ROUTE' ? n.routerLink : undefined,
          faIcon: this.primeIconToFa(n.icon) as IconProp,
          badge: perimesCount || undefined,
          badgeSeverity: perimesCount > 0 ? 'danger' : undefined,
          children: n.children?.length ? this.mapNodesToNavItems(n.children) : undefined,
        } as NavItem;
      });
  }

  /** Construit le menu Compte (toujours présent, non issu du NavStore). */
  private buildAccountMenu(options: NavigationOptions): NavItem {
    const accountChildren: NavItem[] = [
      { label: this.translateLabel('account.settings'), routerLink: '/account/settings', faIcon: 'wrench' },
      {
        label: this.translateLabel('account.cashRegister'),
        routerLink: '/my-cash-register',
        faIcon: faCashRegister,
      },
      { label: this.translateLabel('account.password'), routerLink: '/account/password', faIcon: 'lock' },
    ];
    if (options.additionalAccountMenuItems) {
      accountChildren.push(...options.additionalAccountMenuItems);
    }
    return { label: this.translateLabel('account.main'), faIcon: 'user', children: accountChildren };
  }

  private translateLabel(key: string): string {
    return this.translate.instant(`global.menu.${key}`);
  }

  /** Table de correspondance PrimeIcons → FontAwesome. */
  private primeIconToFa(primeIcon?: string): IconProp {
    const map: Record<string, IconProp> = {
      // Navigation & listes
      'pi pi-list':              faThList,
      'pi pi-align-justify':     faAlignJustify,
      'pi pi-th-large':          faTableCells,
      'pi pi-table':             faTable,
      'pi pi-sitemap':           faSitemap,
      // Ventes & caisse
      'pi pi-shopping-bag':      faShoppingBag,
      'pi pi-shopping-cart':     faShoppingCart,
      'pi pi-shop':              faStore,
      'pi pi-wallet':            faWallet,
      'pi pi-coins':             faCoins,
      'pi pi-money-bill':        faMoneyBill,
      'pi pi-dollar':            faDollarSign,
      'pi pi-credit-card':       faCreditCard,
      'pi pi-calculator':        faCalculator,
      'pi pi-percentage':        faPercent,
      // Stock & livraison
      'pi pi-truck':             faTruck,
      'pi pi-send':              faShippingFast,
      'pi pi-box':               faBoxOpen,
      // Fichiers & documents
      'pi pi-file-pdf':          faFileInvoice,
      'pi pi-file-edit':         faFilePen,
      'pi pi-file-plus':         faFileCirclePlus,
      'pi pi-file-minus':        faFileAlt,
      'pi pi-clipboard':         faClipboardList,
      // Temps & état
      'pi pi-clock':             faClock,
      'pi pi-calendar-times':    faCalendarTimes,
      'pi pi-history':           faClock,
      'pi pi-bookmark':          faBookmark,
      // Actions & contrôles
      'pi pi-refresh':           faArrowsRotate,
      'pi pi-sync':              faArrowsRotate,
      'pi pi-replay':            faRotateLeft,
      'pi pi-undo':              faRotateLeft,
      'pi pi-arrows-h':          faArrowsAltH,
      'pi pi-sort-amount-down':  faSortAmountDown,
      'pi pi-sliders-h':         faSlidersH,
      'pi pi-trash':             faTrash,
      'pi pi-lock':              faLock,
      // Personnes & organisations
      'pi pi-users':             faUsers,
      'pi pi-user':              faUsers,
      'pi pi-building':          faBuilding,
      'pi pi-shield':            faShield,
      // Référentiel
      'pi pi-book':              faBook,
      'pi pi-tags':              faTags,
      'pi pi-star-fill':         faStar,
      'pi pi-lightbulb':         faLightbulb,
      // Rapports
      'pi pi-chart-bar':         faChartBar,
      'pi pi-chart-line':        faChartBar,
      // Admin & config
      'pi pi-cog':               faCog,
      'pi pi-cogs':              faCogs,
      'pi pi-desktop':           faDesktop,
      // Alertes
      'pi pi-exclamation-triangle': faExclamationTriangle,
      // Flux
      'pi pi-stream':            faStream,
      'pi pi-sd-card':           faSdCard,
    };
    return (primeIcon && map[primeIcon]) || (faCog as IconProp);
  }
}
