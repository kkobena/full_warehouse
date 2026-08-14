import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';

import SidebarComponent from './sidebar.component';
import { NavItem } from '../navbar/navbar-item.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { NavigationService } from 'app/core/config/navigation.service';
import { NavStore } from 'app/core/store/nav.store';
import { AlertBadgeService } from 'app/shared/services/alert-badge.service';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { LayoutService } from 'app/core/config/layout.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

const VENTES: NavItem = {
  id: 'ventes',
  label: 'Ventes',
  children: [{ id: 'nouvelle-vente', label: 'Nouvelle vente', routerLink: '/sales' }],
};

const STOCK: NavItem = {
  id: 'stock',
  label: 'Stock',
  children: [{ id: 'commande', label: 'Commandes', routerLink: '/commande' }],
};

describe('SidebarComponent', () => {
  let fixture: ComponentFixture<SidebarComponent>;
  let comp: SidebarComponent;
  let layoutService: LayoutService;
  let routerEvents: Subject<NavigationEnd>;

  /** Accès aux membres `protected` — le test observe le composant du dehors. */
  const call = (method: string, ...args: unknown[]): unknown =>
    (comp as unknown as Record<string, (...a: unknown[]) => unknown>)[method](...args);

  beforeEach(async () => {
    localStorage.clear();
    routerEvents = new Subject<NavigationEnd>();

    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        LayoutService,
        { provide: Router, useValue: { events: routerEvents.asObservable(), navigate: jest.fn() } },
        { provide: AccountService, useValue: { trackCurrentAccount: () => signal(null) } },
        { provide: LoginService, useValue: { logout: jest.fn() } },
        { provide: NgbModal, useValue: { open: jest.fn() } },
        { provide: NavStore, useValue: { navTree: signal([]) } },
        { provide: TauriPrinterService, useValue: { isRunningInTauri: () => false } },
        {
          provide: AlertBadgeService,
          useValue: {
            init: jest.fn(),
            ruptureCount: signal(0),
            urgentCount: signal(0),
            peremptionCount: signal(0),
          },
        },
        {
          provide: NavigationService,
          useValue: {
            hasAnyAuthority: () => false,
            buildNavItemsFromStore: () => [],
            buildUnauthenticatedNavItems: () => [],
          },
        },
      ],
    })
      .overrideTemplate(SidebarComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    comp = fixture.componentInstance;
    layoutService = TestBed.inject(LayoutService);
    fixture.detectChanges();
  });

  describe('ouverture du panneau', () => {
    it('ouvre au clic, referme au second clic sur la même entrée', () => {
      call('toggleMenu', VENTES);
      expect(comp.openMenuId()).toBe('ventes');

      call('toggleMenu', VENTES);
      expect(comp.openMenuId()).toBeNull();
    });

    it('n’ouvre qu’un panneau à la fois', () => {
      call('toggleMenu', VENTES);
      call('toggleMenu', STOCK);

      expect(comp.openMenuId()).toBe('stock');
    });

    it('distingue une activation clavier d’un clic souris', () => {
      call('toggleMenu', VENTES, { detail: 1 } as MouseEvent);
      expect(comp.openedViaKeyboard()).toBe(false);

      call('closeMenu');
      call('toggleMenu', VENTES, { detail: 0 } as MouseEvent);
      expect(comp.openedViaKeyboard()).toBe(true);
    });

    it('ouvre et prend le focus sur ArrowRight', () => {
      call('onTriggerArrowRight', VENTES);

      expect(comp.openMenuId()).toBe('ventes');
      expect(comp.openedViaKeyboard()).toBe(true);
    });
  });

  describe('fermeture', () => {
    it('ferme sur Escape et rend le focus au déclencheur', () => {
      const trigger = document.createElement('button');
      trigger.setAttribute('data-flyout-trigger', 'ventes');
      document.body.appendChild(trigger);
      call('toggleMenu', VENTES);

      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));

      expect(comp.openMenuId()).toBeNull();
      expect(document.activeElement).toBe(trigger);
      trigger.remove();
    });

    it('ignore Escape quand aucun panneau n’est ouvert', () => {
      expect(() => call('onEscape')).not.toThrow();
      expect(comp.openMenuId()).toBeNull();
    });

    it('ferme sur clic extérieur', () => {
      call('toggleMenu', VENTES);

      call('onOverlayOutsideClick', { target: document.createElement('div') } as unknown as MouseEvent);

      expect(comp.openMenuId()).toBeNull();
    });

    it('ignore le clic extérieur porté par un déclencheur, sinon le menu ne se fermerait jamais', () => {
      call('toggleMenu', VENTES);
      const trigger = document.createElement('button');
      trigger.setAttribute('data-flyout-trigger', 'stock');

      call('onOverlayOutsideClick', { target: trigger } as unknown as MouseEvent);

      // C'est `toggleMenu` du déclencheur qui décidera de la bascule.
      expect(comp.openMenuId()).toBe('ventes');
    });

    it('ferme à la navigation', () => {
      call('toggleMenu', VENTES);

      routerEvents.next(new NavigationEnd(1, '/sales', '/sales'));

      expect(comp.openMenuId()).toBeNull();
    });
  });

  describe('survol', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    it('n’ouvre pas depuis un rail au repos', () => {
      call('onTriggerEnter', VENTES);
      jest.advanceTimersByTime(1000);

      expect(comp.openMenuId()).toBeNull();
    });

    it('bascule vers un autre menu quand un panneau est déjà ouvert', () => {
      call('toggleMenu', VENTES);

      call('onTriggerEnter', STOCK);
      expect(comp.openMenuId()).toBe('ventes');

      jest.advanceTimersByTime(120);
      expect(comp.openMenuId()).toBe('stock');
    });

    it('annule la bascule si la souris ne fait que traverser', () => {
      call('toggleMenu', VENTES);
      call('onTriggerEnter', STOCK);

      call('onTriggerLeave');
      jest.advanceTimersByTime(1000);

      expect(comp.openMenuId()).toBe('ventes');
    });
  });

  describe('rail', () => {
    it('reste ouvert après une navigation sur poste fixe', () => {
      layoutService.setSidebarCollapsed(false);
      comp['isMobileSignal'].set(false);

      call('onMenuItemClick');

      expect(layoutService.isSidebarCollapsed()).toBe(false);
    });

    it('se replie après une navigation sur mobile', () => {
      layoutService.setSidebarCollapsed(false);
      comp['isMobileSignal'].set(true);

      call('onMenuItemClick');

      expect(layoutService.isSidebarCollapsed()).toBe(true);
    });

    it('ferme le panneau après une navigation depuis le panneau', () => {
      comp['isMobileSignal'].set(false);
      call('toggleMenu', VENTES);

      call('onFlyoutNavigate');

      expect(comp.openMenuId()).toBeNull();
    });

    it('exécute le handler d’une entrée simple', () => {
      const click = jest.fn();

      call('onMenuItemClick', click);

      expect(click).toHaveBeenCalled();
    });
  });
});
