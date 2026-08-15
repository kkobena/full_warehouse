import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { NavigationService, NavMenuActions } from './navigation.service';
import { LayoutService } from './layout.service';
import { NavStore } from 'app/core/store/nav.store';
import { AccountService } from 'app/core/auth/account.service';
import { AlertBadgeService } from 'app/shared/services/alert-badge.service';
import { PeremptionAlertService } from 'app/shared/services/peremption-alert.service';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { INavNode } from 'app/shared/model/nav-item.model';
import { NavItem } from 'app/layouts/navbar/navbar-item.model';
import { Account } from 'app/core/auth/account.model';

/** Arbre minimal renvoyé par le NavStore. */
const node = (over: Partial<INavNode>): INavNode =>
  ({ id: 1, code: 'x', libelle: 'X', targetType: 'ROUTE', ordre: 1, ...over }) as INavNode;

const account = (authorities: string[]): Account => ({ authorities }) as Account;

const noopActions: NavMenuActions = {
  onLogin: jest.fn(),
  onLogout: jest.fn(),
  onOpenConfigEditor: jest.fn(),
  onOpenAppSettings: jest.fn(),
  onOpenCahierRecette: jest.fn(),
};

describe('NavigationService', () => {
  let service: NavigationService;
  let navTree: ReturnType<typeof signal<INavNode[]>>;
  let currentAccount: ReturnType<typeof signal<Account | null>>;
  let counters: Record<string, ReturnType<typeof signal<number>>>;
  let isTauri: boolean;

  /** Aplatit l'arbre pour retrouver une entrée par son identifiant. */
  const find = (items: NavItem[], id: string): NavItem | undefined => {
    for (const item of items) {
      if (item.id === id) return item;
      const hit = item.children && find(item.children, id);
      if (hit) return hit;
    }
    return undefined;
  };

  beforeEach(() => {
    localStorage.clear();
    navTree = signal<INavNode[]>([]);
    currentAccount = signal<Account | null>(null);
    isTauri = false;
    counters = {
      rupture: signal(0),
      urgent: signal(0),
      peremption: signal(0),
      facturationOverdue: signal(0),
    };

    TestBed.configureTestingModule({
      providers: [
        NavigationService,
        LayoutService,
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: NavStore, useValue: { navTree } },
        { provide: AccountService, useValue: { trackCurrentAccount: () => currentAccount } },
        { provide: PeremptionAlertService, useValue: { urgentCount: signal(0) } },
        { provide: TauriPrinterService, useValue: { isRunningInTauri: () => isTauri } },
        {
          provide: AlertBadgeService,
          useValue: {
            ruptureCount: counters['rupture'],
            urgentCount: counters['urgent'],
            peremptionCount: counters['peremption'],
            facturationOverdueCount: counters['facturationOverdue'],
          },
        },
      ],
    });
    service = TestBed.inject(NavigationService);
  });

  describe('buildNavItems — utilisateur anonyme', () => {
    it('n’expose que le menu Compte', () => {
      const items = service.buildNavItems(noopActions);

      expect(items).toHaveLength(1);
      expect(items[0].id).toBe('account');
      expect(items[0].children?.map(c => c.id)).toEqual(['layout.toggle', 'account.login']);
    });

    it('ajoute les paramètres serveur sous Tauri seulement', () => {
      expect(find(service.buildNavItems(noopActions), 'server-settings')).toBeUndefined();

      isTauri = true;

      expect(find(service.buildNavItems(noopActions), 'server-settings')).toBeDefined();
    });
  });

  describe('buildNavItems — utilisateur connecté', () => {
    beforeEach(() => {
      currentAccount.set(account(['ROLE_USER']));
      navTree.set([node({ code: 'ventes', libelle: 'Ventes', routerLink: '/sales' })]);
    });

    it('construit l’arbre du store, puis le menu Compte', () => {
      const items = service.buildNavItems(noopActions);

      expect(items.map(i => i.id)).toEqual(['ventes', 'account']);
      expect(find(items, 'account.logout')).toBeDefined();
    });

    it('réserve le guide des fonctionnalités aux administrateurs', () => {
      expect(find(service.buildNavItems(noopActions), 'cahier-recette')).toBeUndefined();

      currentAccount.set(account(['ROLE_ADMIN']));

      expect(find(service.buildNavItems(noopActions), 'cahier-recette')).toBeDefined();
    });

    it('réserve la configuration avancée aux administrateurs sous Tauri', () => {
      currentAccount.set(account(['ROLE_ADMIN']));
      expect(find(service.buildNavItems(noopActions), 'app-config')).toBeUndefined();

      isTauri = true;

      expect(find(service.buildNavItems(noopActions), 'app-config')).toBeDefined();
    });

    it('annonce le mode vers lequel la bascule mène, pas le mode courant', () => {
      TestBed.inject(LayoutService).setLayoutMode('sidebar');
      expect(find(service.buildNavItems(noopActions), 'layout.toggle')?.label).toBe('Menu horizontal');

      TestBed.inject(LayoutService).setLayoutMode('navbar');
      expect(find(service.buildNavItems(noopActions), 'layout.toggle')?.label).toBe('Menu vertical');
    });
  });

  describe('applyNavBadges', () => {
    const tree = (): NavItem[] => [
      {
        id: 'stock',
        label: 'Stock',
        children: [
          { id: 'commande', label: 'Commandes', routerLink: '/commande' },
          { id: 'peremption', label: 'Péremptions', routerLink: '/gestion-peremption' },
        ],
      },
      { id: 'facturation', label: 'Facturation', routerLink: '/facturation' },
      { id: 'ventes', label: 'Ventes', routerLink: '/sales' },
    ];

    it('ne pose aucun badge quand les compteurs sont à zéro', () => {
      const items = tree();

      service.applyNavBadges(items);

      expect(items.every(i => i.badge === undefined)).toBe(true);
    });

    it('retient le plus grand des deux compteurs de commande', () => {
      counters['rupture'].set(3);
      counters['urgent'].set(7);
      const items = tree();

      service.applyNavBadges(items);

      expect(find(items, 'commande')?.badge).toBe(7);
    });

    it('marque les factures échues en warning, pas en danger', () => {
      counters['facturationOverdue'].set(4);
      const items = tree();

      service.applyNavBadges(items);

      expect(find(items, 'facturation')?.badge).toBe(4);
      expect(find(items, 'facturation')?.badgeSeverity).toBe('warning');
    });

    it('propage la somme des enfants sur le parent', () => {
      counters['urgent'].set(2);
      counters['peremption'].set(5);
      const items = tree();

      service.applyNavBadges(items);

      expect(find(items, 'stock')?.badge).toBe(7);
      expect(find(items, 'stock')?.badgeSeverity).toBe('danger');
    });

    it('efface un badge devenu obsolète', () => {
      counters['peremption'].set(5);
      const items = tree();
      service.applyNavBadges(items);
      expect(find(items, 'peremption')?.badge).toBe(5);

      counters['peremption'].set(0);
      service.applyNavBadges(items);

      expect(find(items, 'peremption')?.badge).toBeUndefined();
      expect(find(items, 'stock')?.badge).toBeUndefined();
    });
  });
});
