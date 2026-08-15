import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { faBars } from '@fortawesome/free-solid-svg-icons';

import NavbarComponent from './navbar.component';
import { NavItem } from './navbar-item.model';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
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

describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let comp: NavbarComponent;
  let routerEvents: Subject<NavigationEnd>;
  let currentAccount: ReturnType<typeof signal<Account | null>>;

  /** Accès aux membres `protected` — le test observe le composant du dehors. */
  const call = (method: string, ...args: unknown[]): unknown =>
    (comp as unknown as Record<string, (...a: unknown[]) => unknown>)[method](...args);

  /**
   * Monte le composant. `render: false` neutralise le gabarit (idiome du dépôt)
   * pour tester la logique sans instancier le CDK Overlay ; `render: true` monte
   * le vrai gabarit et exige donc un routeur réel pour les `routerLink`.
   */
  const setup = (options: { items?: NavItem[]; render?: boolean } = {}): void => {
    localStorage.clear();
    routerEvents = new Subject<NavigationEnd>();
    currentAccount = signal<Account | null>(null);
    const items = options.items ?? [];

    const builder = TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        LayoutService,
        options.render
          ? provideRouter([{ path: '**', children: [] }])
          : { provide: Router, useValue: { events: routerEvents.asObservable(), navigate: jest.fn() } },
        { provide: AccountService, useValue: { trackCurrentAccount: () => currentAccount } },
        { provide: LoginService, useValue: { logout: jest.fn() } },
        { provide: NgbModal, useValue: { open: jest.fn() } },
        { provide: NavStore, useValue: { navTree: signal([]) } },
        { provide: TauriPrinterService, useValue: { isRunningInTauri: () => false } },
        { provide: AlertBadgeService, useValue: { init: jest.fn() } },
        {
          provide: TranslateService,
          useValue: { onTranslationChange: new Subject(), get: () => of('Pharma Smart') },
        },
        {
          provide: NavigationService,
          useValue: {
            hasAnyAuthority: () => false,
            buildNavItems: () => items,
            applyNavBadges: jest.fn(),
          },
        },
      ],
    });

    if (!options.render) {
      builder.overrideTemplate(NavbarComponent, '');
    }

    fixture = builder.createComponent(NavbarComponent);
    comp = fixture.componentInstance;
    // Le gabarit réel contient <fa-icon icon="bars"> : l'icône doit être connue.
    TestBed.inject(FaIconLibrary).addIcons(faBars);
    fixture.detectChanges();
  };

  describe('ouverture du panneau', () => {
    beforeEach(() => setup());

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

    it('ouvre et prend le focus sur ArrowDown — la navbar descend, le rail va à droite', () => {
      call('onTriggerArrowDown', VENTES);

      expect(comp.openMenuId()).toBe('ventes');
      expect(comp.openedViaKeyboard()).toBe(true);
    });
  });

  describe('fermeture', () => {
    beforeEach(() => setup());

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

      expect(comp.openMenuId()).toBe('ventes');
    });

    it('ferme à la navigation', () => {
      call('toggleMenu', VENTES);

      routerEvents.next(new NavigationEnd(1, '/sales', '/sales'));

      expect(comp.openMenuId()).toBeNull();
    });

    it('referme le panneau et replie la navbar après navigation depuis le panneau', () => {
      call('toggleNavbar');
      call('toggleMenu', VENTES);

      call('onFlyoutNavigate');

      expect(comp.openMenuId()).toBeNull();
      expect(comp['isNavbarCollapsed']()).toBe(true);
    });
  });

  describe('survol', () => {
    beforeEach(() => {
      setup();
      jest.useFakeTimers();
    });
    afterEach(() => jest.useRealTimers());

    it('n’ouvre pas depuis une barre au repos', () => {
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

  describe('entrées simples', () => {
    beforeEach(() => setup());

    it('exécute le handler puis replie la navbar', () => {
      const click = jest.fn();
      call('toggleNavbar');

      call('onLeafClick', { id: 'cahier-recette', label: 'Guide', click });

      expect(click).toHaveBeenCalled();
      expect(comp['isNavbarCollapsed']()).toBe(true);
    });

    it('tolère une entrée sans handler', () => {
      expect(() => call('onLeafClick', { id: 'ventes', label: 'Ventes', routerLink: '/sales' })).not.toThrow();
    });

    it('reconnaît le menu Compte par son identifiant, pas par son libellé', () => {
      expect(call('isAccountMenu', { id: 'account', label: 'Traduction quelconque' })).toBe(true);
      expect(call('isAccountMenu', { id: 'ventes', label: 'Mon compte client' })).toBe(false);
    });
  });

  describe('rendu', () => {
    const links = (): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll('.nav-link'));

    it('rend un déclencheur de panneau pour une entrée à enfants', () => {
      setup({ render: true, items: [VENTES] });

      const trigger = fixture.nativeElement.querySelector('[data-flyout-trigger="ventes"]');
      expect(trigger.tagName).toBe('BUTTON');
      expect(trigger.getAttribute('aria-haspopup')).toBe('menu');
      expect(trigger.getAttribute('aria-expanded')).toBe('false');
    });

    it('rend un lien pour une entrée simple qui navigue', () => {
      setup({ render: true, items: [{ id: 'ventes', label: 'Ventes', routerLink: '/sales' }] });

      expect(links().map(l => l.tagName)).toEqual(['A']);
    });

    it('rend un bouton pour une action sans cible — un <a> sans href serait hors du parcours clavier', () => {
      setup({ render: true, items: [{ id: 'cahier-recette', label: 'Guide', click: () => undefined }] });

      const leaf = links()[0];
      expect(leaf.tagName).toBe('BUTTON');
      // Le fond du problème : un <a> sans href hérite d'un tabIndex de -1.
      expect(leaf.tabIndex).toBe(0);
    });

    it('affiche le badge et le plafonne au-delà de 99', () => {
      setup({
        render: true,
        items: [{ id: 'facturation', label: 'Facturation', routerLink: '/facturation', badge: 150 }],
      });

      expect(fixture.nativeElement.querySelector('.navbar-badge').textContent.trim()).toBe('99+');
    });

    it('substitue le nom de l’utilisateur au libellé du menu Compte', () => {
      setup({
        render: true,
        items: [{ id: 'account', label: 'Compte', children: [{ id: 'account.logout', label: 'Se déconnecter' }] }],
      });
      currentAccount.set({ lastName: 'Kobena', authorities: [] } as unknown as Account);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-flyout-trigger="account"]').textContent).toContain('Kobena');
    });
  });
});
