import {inject, Injectable} from '@angular/core';
import {AccountService} from 'app/core/auth/account.service';
import {TranslateService} from '@ngx-translate/core';
import {NavItem} from 'app/layouts/navbar/navbar-item.model';
import {Authority} from 'app/shared/constants/authority.constants';
import {PeremptionAlertService} from '../../shared/services/peremption-alert.service';
import {NavStore} from 'app/core/store/nav.store';
import {INavNode} from 'app/shared/model/nav-item.model';
import {IconProp} from '@fortawesome/fontawesome-svg-core';
import {
  faBook,
  faBoxes,
  faBoxOpen,
  faBuilding,
  faCalendarTimes,
  faCashRegister,
  faChartBar,
  faClipboardList,
  faCog,
  faCogs,
  faCoins,
  faDesktop,
  faDollarSign,
  faExclamationTriangle,
  faFileInvoice,
  faLink,
  faMapMarker,
  faMoneyBill,
  faMoneyCheckAlt,
  faPercent,
  faPills,
  faSchoolCircleExclamation,
  faSdCard,
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
  private readonly accountService = inject(AccountService);
  private readonly translate = inject(TranslateService);
  private readonly peremptionAlertService = inject(PeremptionAlertService);
  private readonly navStore = inject(NavStore);

  /**
   * Construit le menu depuis le NavStore dynamique (Phase 2+).
   * Remplace progressivement buildNavItems() qui reste disponible pour compatibilité.
   */
  buildNavItemsFromStore(options: NavigationOptions = {}): NavItem[] {
    const tree = this.navStore.navTree();
    if (!tree.length) {
      // Fallback sur la méthode statique si le store n'est pas encore chargé
      return this.buildNavItems(options);
    }
    const items = this.mapNodesToNavItems(tree);
    items.push(this.buildAccountMenu(options));
    return items;
  }

  buildNavItems(options: NavigationOptions = {}): NavItem[] {
    const account = this.accountService.trackCurrentAccount()();
    const allItems: NavItem[] = [];

    // ── Accès directs par rôle ────────────────────────────────────────────
    if (account) {
      const isAdmin = this.hasAnyAuthority([Authority.ADMIN], account.authorities);
      const isCaissier = this.hasAnyAuthority([Authority.ROLE_CAISSIER], account.authorities);
      const isVendeur = this.hasAnyAuthority([Authority.ROLE_VENDEUR], account.authorities);
      const isRespCmd = this.hasAnyAuthority([Authority.ROLE_RESPONSABLE_COMMANDE], account.authorities);
      const isPharmacien = this.hasAnyAuthority([Authority.ROLE_PHARMACIEN, Authority.HOME_DASHBOARD], account.authorities);

      // Nouvelle Vente — Caissier/Admin → comptant ; Vendeur → prévente uniquement
      if (isCaissier || isAdmin || this.hasAnyAuthority([Authority.SALES], account.authorities)) {
        allItems.push({
          label: 'Nouvelle Vente',
          faIcon: faShoppingBag,
          routerLink: '/sales-home',
          badgeSeverity: 'success',
        });
      } else if (isVendeur) {
        allItems.push({
          label: 'Nouvelle Prévente',
          faIcon: faShoppingBag,
          routerLink: '/sales-home/prevente',
          badgeSeverity: 'success',
        });
      }

      // Mon Tableau de Bord — Resp. Commande → CommmandeHome ; autres rôles → /
      if (!isAdmin) {
        if (isRespCmd) {
          allItems.push({
            label: 'Tableau de Bord Appro',
            faIcon: faChartBar,
            routerLink: '/commande',
          });
        } else if (isCaissier || isVendeur || isPharmacien) {
          allItems.push({
            label: 'Mon Tableau de Bord',
            faIcon: faChartBar,
            routerLink: '/',
          });
        }
      }
    }

    // Gestion Courante
    allItems.push({
      label: this.translateLabel('menuGestionCourante'),
      faIcon: faThList,
      authorities: [Authority.GESTION_COURANT, Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.SALES],
      children: [

        {
          label: this.translateLabel('entities.sales'),
          routerLink: '/sales-home/gestion',
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
    const perimesCount = this.peremptionAlertService.urgentCount();
    allItems.push({
      label: this.translateLabel('menuGestionStock'),
      faIcon: faTruckFast,
      badge: perimesCount,
      badgeSeverity: perimesCount > 0 ? 'danger' : undefined,
      authorities: [
        Authority.ROLE_RESPONSABLE_COMMANDE,
        Authority.GESTION_STOCK,
        Authority.GESTION_ENTREE_STOCK,
        Authority.ADMIN,
        Authority.COMMANDE,
      ],
      children: [
        {
          label: 'Catalogue produits',
          routerLink: '/produits',
          faIcon: faBoxOpen,
        },
        {
          label: this.translateLabel('entities.commande'),
          routerLink: '/commande',
          faIcon: faShippingFast,
        },

        {
          label: 'Ajustements de stock',
          routerLink: '/features-ajustement',
          faIcon: faSlidersH,
        },
        {
          label: this.translateFullLabel('gestionPerimes.title'),
          routerLink: '/gestion-peremption',
          faIcon: faCalendarTimes,
          badge: perimesCount,
          badgeSeverity: 'danger',
        }, {
          label: this.translateLabel('entities.storeInventory'),
          routerLink: '/inventaire',
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
          routerLink: '/facturation',
          faIcon: faFileInvoice,
        },
        {
          label: this.translateLabel('facturation.differes'),
          routerLink: '/differes',
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

    // Référentiel — regroupé en 3 groupes (AX8 : Miller's Law)
    allItems.push({
      label: this.translateLabel('referentiel'),
      faIcon: faBook,
      authorities: [Authority.REFERENTIEL, Authority.ADMIN, Authority.ROLE_PHARMACIEN],
      children: [
        // ── Groupe Produits ──────────────────────────────────────────
        {label: 'Produits', groupLabel: 'Produits'} as any,
        {label: this.translateLabel('entities.rayon'), routerLink: '/rayon', faIcon: faStream},
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
        {
          label: this.translateLabel('gammeProduit'),
          routerLink: '/gamme-produit',
          faIcon: faMapMarker
        },
        {label: this.translateLabel('laboratoire'), routerLink: '/laboratoire', faIcon: faBuilding},
        // ── Groupe Commercial ─────────────────────────────────────────
        {label: '__divider__', divider: true} as any,
        {label: 'Commercial', groupLabel: 'Commercial'} as any,
        {
          label: this.translateLabel('entities.remise'),
          routerLink: '/remises',
          authorities: [Authority.ADMIN, Authority.REMISE],
          faIcon: faPercent,
        },
        {label: this.translateLabel('entities.tableau'), routerLink: '/tableaux', faIcon: faTable},
        {label: this.translateLabel('entities.tva'), routerLink: '/tva', faIcon: faDollarSign},
        {label: 'Modes de paiement', routerLink: '/mode-payments', faIcon: faSdCard},
        // ── Groupe Organisation ───────────────────────────────────────
        {label: '__divider__', divider: true} as any,
        {label: 'Organisation', groupLabel: 'Organisation'} as any,
        {
          label: this.translateLabel('entities.fournisseur'),
          routerLink: '/fournisseur',
          faIcon: faTruck
        },
        {
          label: this.translateLabel('motifAjustement'),
          routerLink: '/motif-ajustement',
          faIcon: faExclamationTriangle
        },
        {
          label: 'Motif Retour Produit',
          routerLink: '/motif-retour-produit',
          faIcon: faSchoolCircleExclamation
        },
        {
          label: this.translateLabel('parametre'),
          routerLink: '/parametre',
          authorities: [Authority.ADMIN, 'parametre'],
          faIcon: faCog,
        },
      ],
    });

    // Rapports & Statistiques
    // ── Matrice visibilité ─────────────────────────────────────────────────
    // CA                 → Admin + Resp. Commande (impact commandes / achats)
    // Stock & Inventaire → Admin + Resp. Commande (cœur de métier)
    // Clients/Fourn.     → Admin + Resp. Commande (performance fournisseurs)

    allItems.push({
      label: 'Rapports & Statistiques',
      faIcon: faChartBar,
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE, Authority.ROLE_CAISSIER],
      children: [
        {
          label: "Chiffre d'Affaires",
          routerLink: '/reports/sales',
          faIcon: faMoneyBill,
          authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
        },
        {
          label: 'Stock & Inventaire',
          routerLink: '/reports/stock',
          faIcon: faBoxes,
          authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
        },
        {
          label: 'Clients & Fournisseurs',
          routerLink: '/reports/partners',
          faIcon: faUsers,
          authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
        }
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
      {
        label: 'Menus; Accès',
        routerLink: '/admin/nav-manager',
        faIcon: faSlidersH,
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

    if (options.additionalAccountMenuItems) {
      accountChildren.push(...options.additionalAccountMenuItems);
    }

    allItems.push({
      label: this.translateLabel('account.main'),
      faIcon: 'user',
      children: accountChildren,
    });

    if (account) {
      return this.filterByAuthority(allItems, account.authorities);
    }

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

  /** Mappe récursivement les INavNode → NavItem (pour buildNavItemsFromStore). */
  private mapNodesToNavItems(nodes: INavNode[]): NavItem[] {
    return nodes
      .filter(n => n.permissions?.canDisplay !== false)
      .sort((a, b) => a.ordre - b.ordre)
      .map(n => ({
        label: n.libelle,
        routerLink: n.targetType === 'ROUTE' ? n.routerLink : undefined,
        faIcon: this.primeIconToFa(n.icon) as IconProp,
        children: n.children?.length ? this.mapNodesToNavItems(n.children) : undefined,
      } as NavItem));
  }

  /** Construit le menu Compte (toujours présent, non issu du NavStore). */
  private buildAccountMenu(options: NavigationOptions): NavItem {
    const accountChildren: NavItem[] = [
      { label: this.translateLabel('account.settings'), routerLink: '/account/settings', faIcon: 'wrench' },
      {
        authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR, Authority.MY_CASH_REGISTER],
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

  /** Table de correspondance PrimeIcons → FontAwesome (extensible). */
  private primeIconToFa(primeIcon?: string): IconProp {
    const map: Record<string, IconProp> = {
      'pi pi-shopping-bag': faShoppingBag,
      'pi pi-list': faThList,
      'pi pi-truck': faTruck,
      'pi pi-wallet': faWallet,
      'pi pi-book': faBook,
      'pi pi-chart-bar': faChartBar,
      'pi pi-cog': faCog,
      'pi pi-cogs': faCogs,
      'pi pi-box': faBoxOpen,
      'pi pi-send': faShippingFast,
      'pi pi-calendar-times': faCalendarTimes,
      'pi pi-sliders-h': faSlidersH,
      'pi pi-clipboard': faClipboardList,
      'pi pi-file-pdf': faFileInvoice,
      'pi pi-users': faUsers,
      'pi pi-user': faUsers,
      'pi pi-building': faBuilding,
      'pi pi-chart-line': faChartBar,
      'pi pi-coins': faCoins,
    };
    return (primeIcon && map[primeIcon]) || (faCog as IconProp);
  }
}
