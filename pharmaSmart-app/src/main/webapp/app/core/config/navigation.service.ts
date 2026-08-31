import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {NavItem, navItemIdFromLabel} from 'app/layouts/navbar/navbar-item.model';
import {PeremptionAlertService} from '../../shared/services/peremption-alert.service';
import {AlertBadgeService} from '../../shared/services/alert-badge.service';
import {AccountService} from '../auth/account.service';
import {TauriPrinterService} from '../../shared/services/tauri-printer.service';
import {LayoutService} from './layout.service';
import {Authority} from '../../config/authority.constants';
import {NavStore} from 'app/core/store/nav.store';
import {INavNode} from 'app/shared/model/nav-item.model';
import {IconProp} from '@fortawesome/fontawesome-svg-core';
import {
  faAlignJustify,
  faArrowsAltH,
  faArrowsRotate,
  faBars,
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
  faServer,
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

/**
 * Actions que le chrome de navigation délègue à son composant hôte.
 *
 * La bascule de mode (`layout.toggle`) n'y figure pas : elle ne dépend d'aucun
 * état du composant et est traitée directement par le service.
 */
export interface NavMenuActions {
  onLogin: () => void;
  onLogout: () => void;
  onOpenConfigEditor: () => void;
  onOpenAppSettings: () => void;
}

@Injectable({
  providedIn: 'root',
})
export class NavigationService {
  private readonly translate = inject(TranslateService);
  private readonly peremptionAlertService = inject(PeremptionAlertService);
  private readonly navStore = inject(NavStore);
  private readonly alertBadgeService = inject(AlertBadgeService);
  private readonly accountService = inject(AccountService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly layoutService = inject(LayoutService);

  /**
   * Construit l'arbre de navigation complet, identique pour la navbar
   * horizontale et le rail vertical : entrées du `NavStore`, menu Compte, et
   * entrées propres au chrome (bascule de mode, guide, configuration).
   *
   * C'est la source unique des deux barres — c'est ce qui garantit qu'elles
   * n'affichent jamais des menus différents.
   */
  buildNavItems(actions: NavMenuActions): NavItem[] {
    const account = this.accountService.trackCurrentAccount()();
    const isTauri = this.tauriPrinterService.isRunningInTauri();
    // Le libellé annonce le mode vers lequel on bascule, pas le mode courant.
    const layoutToggle: NavItem = {
      id: 'layout.toggle',
      label: this.layoutService.isSidebar() ? 'Menu horizontal' : 'Menu vertical',
      faIcon: faBars,
      click: () => this.layoutService.toggleLayout(),
    };

    if (!account) {
      const anonymousItems: NavItem[] = [
        layoutToggle,
        {
          id: 'account.login',
          label: 'Se connecter',
          faIcon: 'sign-out-alt',
          click: actions.onLogin
        },
      ];
      if (isTauri) {
        anonymousItems.unshift({
          id: 'server-settings',
          label: 'Paramètres Serveur',
          faIcon: faServer,
          click: actions.onOpenAppSettings,
        });
      }
      return this.buildUnauthenticatedNavItems(anonymousItems);
    }

    const isAdmin = this.hasAnyAuthority(Authority.ADMIN, account.authorities);
    const accountItems: NavItem[] = [
      layoutToggle,
      {
        id: 'account.logout',
        label: 'Se déconnecter',
        faIcon: 'sign-out-alt',
        click: actions.onLogout
      },
    ];
    if (isAdmin && isTauri) {
      accountItems.unshift({
        id: 'app-config',
        label: 'Configuration avancée',
        faIcon: faSlidersH,
        click: actions.onOpenConfigEditor,
      });
    }

    // « Guide des fonctionnalités » n'est plus poussé ici : il est devenu une entrée
    // nav_item du module Administration (migration V1.9.5), donc soumise au même
    // paramétrage de droits et au même ordre personnalisable que le reste du menu.
    return this.buildNavItemsFromStore({additionalAccountMenuItems: accountItems});
  }

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
        id: 'account',
        label: this.translateLabel('account.main'),
        faIcon: 'user',
        children: additionalItems,
      },
    ];
  }

  /**
   * Applique les badges d'alerte sur l'arbre, **en place**.
   *
   * Règles :
   * - `/commande`            → max(ruptures, urgents)      — danger
   * - `/gestion-peremption`  → péremptions                 — danger
   * - `/facturation`         → factures échues             — warning
   * - parents                → somme propagée des enfants  — danger
   *
   * Partagé par la navbar et la sidebar : c'est ce qui garantit des badges
   * identiques quel que soit le mode de navigation choisi.
   */
  applyNavBadges(items: NavItem[]): void {
    const ruptureCount = this.alertBadgeService.ruptureCount();
    const urgentCount = this.alertBadgeService.urgentCount();
    const peremptionCount = this.alertBadgeService.peremptionCount();
    const facturationOverdueCount = this.alertBadgeService.facturationOverdueCount();
    this.applyBadgesRecursively(items, ruptureCount, urgentCount, peremptionCount, facturationOverdueCount);
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

  private applyBadgesRecursively(
    items: NavItem[],
    ruptureCount: number,
    urgentCount: number,
    peremptionCount: number,
    facturationOverdueCount: number
  ): void {
    for (const item of items) {
      if (item.children?.length) {
        this.applyBadgesRecursively(item.children, ruptureCount, urgentCount, peremptionCount, facturationOverdueCount);
        const total = item.children.reduce((sum, c) => sum + (c.badge ?? 0), 0);
        // La couleur du parent est celle de la plus grave de ses alertes, et non « danger »
        // par principe : une rubrique dont le seul compteur est une facture echue s'affichait
        // en rouge, alors que l'ecran vise, lui, la signale en orange. La rubrique disait donc
        // une urgence que le detail dementait.
        const urgent = item.children.some(c => (c.badge ?? 0) > 0 && (c.badgeSeverity ?? 'danger') === 'danger');
        item.badge = total > 0 ? total : undefined;
        item.badgeSeverity = total > 0 ? (urgent ? 'danger' : 'warning') : undefined;
      } else if (item.routerLink === '/commande') {
        const total = Math.max(ruptureCount, urgentCount);
        item.badge = total > 0 ? total : undefined;
        item.badgeSeverity = 'danger';
      } else if (item.routerLink === '/gestion-peremption') {
        item.badge = peremptionCount > 0 ? peremptionCount : undefined;
        item.badgeSeverity = 'danger';
      } else if (item.routerLink === '/facturation') {
        item.badge = facturationOverdueCount > 0 ? facturationOverdueCount : undefined;
        item.badgeSeverity = 'warning';
      } else {
        item.badge = undefined;
        item.badgeSeverity = undefined;
      }
    }
  }

  /** Mappe récursivement les INavNode → NavItem. */
  private mapNodesToNavItems(nodes: INavNode[]): NavItem[] {
    return nodes
      .filter(n => n.permissions?.canDisplay !== false)
      .filter(n => n.targetType !== 'SECTION' && n.targetType !== 'ACTION')
      .sort((a, b) => a.ordre - b.ordre)
      .map(n => {
        const perimesCount = n.code === 'peremptions' ? this.peremptionAlertService.urgentCount() : 0;

        const children = n.children?.length ? this.mapNodesToNavItems(n.children) : undefined;
        return {
          id: n.code || navItemIdFromLabel(n.libelle),
          label: n.libelle,
          routerLink: n.targetType === 'ROUTE' ? n.routerLink : undefined,
          faIcon: this.primeIconToFa(n.icon) as IconProp,
          badge: perimesCount || undefined,
          badgeSeverity: perimesCount > 0 ? 'danger' : undefined,
          children: children?.length ? children : undefined,
        } as NavItem;
      });
  }

  /** Construit le menu Compte (toujours présent, non issu du NavStore). */
  private buildAccountMenu(options: NavigationOptions): NavItem {
    const accountChildren: NavItem[] = [
      {
        id: 'account.settings',
        label: this.translateLabel('account.settings'),
        routerLink: '/account/settings',
        faIcon: 'wrench'
      },
      {
        id: 'account.cash-register',
        label: this.translateLabel('account.cashRegister'),
        routerLink: '/my-cash-register',
        faIcon: faCashRegister,
      },
      {
        id: 'account.password',
        label: this.translateLabel('account.password'),
        routerLink: '/account/password',
        faIcon: 'lock'
      },
    ];
    if (options.additionalAccountMenuItems) {
      accountChildren.push(...options.additionalAccountMenuItems);
    }
    return {
      id: 'account',
      label: this.translateLabel('account.main'),
      faIcon: 'user',
      children: accountChildren
    };
  }

  private translateLabel(key: string): string {
    return this.translate.instant(`global.menu.${key}`);
  }

  /** Table de correspondance PrimeIcons → FontAwesome. */
  private primeIconToFa(primeIcon?: string): IconProp {
    const map: Record<string, IconProp> = {
      // Navigation & listes
      'pi pi-list': faThList,
      'pi pi-align-justify': faAlignJustify,
      'pi pi-th-large': faTableCells,
      'pi pi-table': faTable,
      'pi pi-sitemap': faSitemap,
      // Ventes & caisse
      'pi pi-shopping-bag': faShoppingBag,
      'pi pi-shopping-cart': faShoppingCart,
      'pi pi-shop': faStore,
      'pi pi-wallet': faWallet,
      'pi pi-coins': faCoins,
      'pi pi-money-bill': faMoneyBill,
      'pi pi-dollar': faDollarSign,
      'pi pi-credit-card': faCreditCard,
      'pi pi-calculator': faCalculator,
      'pi pi-percentage': faPercent,
      // Stock & livraison
      'pi pi-truck': faTruck,
      'pi pi-send': faShippingFast,
      'pi pi-box': faBoxOpen,
      // Fichiers & documents
      'pi pi-file-pdf': faFileInvoice,
      'pi pi-file-edit': faFilePen,
      'pi pi-file-plus': faFileCirclePlus,
      'pi pi-file-minus': faFileAlt,
      'pi pi-clipboard': faClipboardList,
      // Temps & état
      'pi pi-clock': faClock,
      'pi pi-calendar-times': faCalendarTimes,
      'pi pi-history': faClock,
      'pi pi-bookmark': faBookmark,
      // Actions & contrôles
      'pi pi-refresh': faArrowsRotate,
      'pi pi-sync': faArrowsRotate,
      'pi pi-replay': faRotateLeft,
      'pi pi-undo': faRotateLeft,
      'pi pi-arrows-h': faArrowsAltH,
      'pi pi-sort-amount-down': faSortAmountDown,
      'pi pi-sliders-h': faSlidersH,
      'pi pi-trash': faTrash,
      'pi pi-lock': faLock,
      // Personnes & organisations
      'pi pi-users': faUsers,
      'pi pi-user': faUsers,
      'pi pi-building': faBuilding,
      'pi pi-shield': faShield,
      // Référentiel
      'pi pi-book': faBook,
      'pi pi-tags': faTags,
      'pi pi-star-fill': faStar,
      'pi pi-lightbulb': faLightbulb,
      // Rapports
      'pi pi-chart-bar': faChartBar,
      'pi pi-chart-line': faChartBar,
      // Admin & config
      'pi pi-cog': faCog,
      'pi pi-cogs': faCogs,
      'pi pi-desktop': faDesktop,
      // Alertes
      'pi pi-exclamation-triangle': faExclamationTriangle,
      // Flux
      'pi pi-stream': faStream,
      'pi pi-sd-card': faSdCard,
    };
    return (primeIcon && map[primeIcon]) || (faCog as IconProp);
  }
}
