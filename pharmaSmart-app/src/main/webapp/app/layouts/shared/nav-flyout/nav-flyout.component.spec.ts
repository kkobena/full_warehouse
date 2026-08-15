import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DOWN_ARROW, TAB, UP_ARROW } from '@angular/cdk/keycodes';

import { NavFlyoutComponent } from './nav-flyout.component';
import { NavItem } from '../../navbar/navbar-item.model';

describe('NavFlyoutComponent', () => {
  let fixture: ComponentFixture<NavFlyoutComponent>;
  let comp: NavFlyoutComponent;

  /** Monte le panneau pour l'entrée parente donnée. */
  const mount = (item: NavItem, options: { autoFocus?: boolean } = {}): void => {
    fixture.componentRef.setInput('item', item);
    fixture.componentRef.setInput('autoFocus', options.autoFocus ?? false);
    fixture.detectChanges();
  };

  const links = (): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll('.flyout-link'));

  const labels = (): string[] => links().map(link => link.textContent!.trim());

  const parent = (children: NavItem[]): NavItem => ({ id: 'ventes', label: 'Ventes', children });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavFlyoutComponent],
      // Route attrape-tout : les lignes portent de vrais `routerLink` et un clic
      // déclenche une navigation réelle, qui échouerait sans route déclarée.
      providers: [provideRouter([{ path: '**', children: [] }])],
    }).compileComponents();

    fixture = TestBed.createComponent(NavFlyoutComponent);
    comp = fixture.componentInstance;
  });

  describe('rendu', () => {
    it('rend une ligne par enfant, avec le libellé du parent en en-tête', () => {
      mount(
        parent([
          { id: 'nouvelle-vente', label: 'Nouvelle vente', routerLink: '/sales' },
          { id: 'ventes-du-jour', label: 'Ventes du jour', routerLink: '/sales/today' },
        ]),
      );

      expect(labels()).toEqual(['Nouvelle vente', 'Ventes du jour']);
      expect(fixture.nativeElement.querySelector('.flyout-header-label').textContent.trim()).toBe('Ventes');
    });

    it('rend les intertitres et les séparateurs', () => {
      mount(
        parent([
          { id: 'g1', label: '', groupLabel: 'GESTION' },
          { id: 'nouvelle-vente', label: 'Nouvelle vente', routerLink: '/sales' },
          { id: 'sep', label: '', divider: true },
          { id: 'g2', label: '', groupLabel: 'SUIVI' },
          { id: 'stats', label: 'Statistiques', routerLink: '/stats' },
        ]),
      );

      const groups = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.flyout-group'));
      expect(groups.map(g => g.textContent!.trim())).toEqual(['GESTION', 'SUIVI']);
      expect(fixture.nativeElement.querySelectorAll('.flyout-divider')).toHaveLength(1);
      expect(labels()).toEqual(['Nouvelle vente', 'Statistiques']);
    });

    it('aplatit un 3ᵉ niveau : le parent intermédiaire devient un intertitre', () => {
      mount(
        parent([
          { id: 'nouvelle-vente', label: 'Nouvelle vente', routerLink: '/sales' },
          {
            id: 'rapports',
            label: 'Rapports',
            children: [
              { id: 'par-produit', label: 'Par produit', routerLink: '/reports/products' },
              { id: 'par-vendeur', label: 'Par vendeur', routerLink: '/reports/sellers' },
            ],
          },
        ]),
      );

      const groups = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.flyout-group'));
      expect(groups.map(g => g.textContent!.trim())).toEqual(['Rapports']);
      // Aucune ligne perdue, et aucune cascade : tout est dans le même panneau.
      expect(labels()).toEqual(['Nouvelle vente', 'Par produit', 'Par vendeur']);
    });

    it('affiche les badges et plafonne au-delà de 99', () => {
      mount(
        parent([
          { id: 'avoirs', label: 'Avoirs', routerLink: '/avoirs', badge: 3 },
          { id: 'commandes', label: 'Commandes', routerLink: '/commande', badge: 150 },
        ]),
      );

      const badges = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.flyout-link .flyout-badge'));
      expect(badges.map(b => b.textContent!.trim())).toEqual(['3', '99+']);
    });

    it('rend un bouton pour une action sans routerLink, un lien sinon', () => {
      mount(
        parent([
          { id: 'logout', label: 'Se déconnecter', click: () => undefined },
          { id: 'settings', label: 'Paramètres', routerLink: '/account/settings' },
        ]),
      );

      expect(links().map(l => l.tagName)).toEqual(['BUTTON', 'A']);
    });
  });

  describe('activation d’une ligne', () => {
    it('déclenche le handler et émet navigate', () => {
      const click = jest.fn();
      const action: NavItem = { id: 'logout', label: 'Se déconnecter', click };
      const navigate = jest.fn();
      mount(parent([action]));
      comp.navigate.subscribe(navigate);

      links()[0].click();

      expect(click).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(action);
    });

    it('émet navigate sans exiger de handler sur un lien', () => {
      const navigate = jest.fn();
      mount(parent([{ id: 'settings', label: 'Paramètres', routerLink: '/account/settings' }]));
      comp.navigate.subscribe(navigate);

      links()[0].click();

      expect(navigate).toHaveBeenCalledTimes(1);
    });
  });

  describe('clavier', () => {
    /**
     * Le `FocusKeyManager` du CDK lit `event.keyCode`, que le constructeur
     * `KeyboardEvent` ne dérive pas de `key` — il faut le fournir explicitement.
     */
    const press = (key: string, keyCode?: number): void => {
      links()[0].dispatchEvent(new KeyboardEvent('keydown', { key, keyCode, bubbles: true }));
      fixture.detectChanges();
    };

    beforeEach(() => {
      mount(
        parent([
          { id: 'a', label: 'Alpha', routerLink: '/a' },
          { id: 'b', label: 'Bravo', routerLink: '/b' },
        ]),
      );
    });

    it('ferme sur Escape', () => {
      const close = jest.fn();
      comp.close.subscribe(close);

      press('Escape');

      expect(close).toHaveBeenCalled();
    });

    it('ferme sur ArrowLeft — retour au rail', () => {
      const close = jest.fn();
      comp.close.subscribe(close);

      press('ArrowLeft');

      expect(close).toHaveBeenCalled();
    });

    it('n’escalade pas Escape : la sidebar ne le traite pas deux fois', () => {
      const onDocument = jest.fn();
      document.addEventListener('keydown', onDocument);

      press('Escape');

      expect(onDocument).not.toHaveBeenCalled();
      document.removeEventListener('keydown', onDocument);
    });

    it('déplace le focus à la ligne suivante sur ArrowDown', () => {
      press('ArrowDown', DOWN_ARROW);

      expect(document.activeElement).toBe(links()[0]);

      press('ArrowDown', DOWN_ARROW);

      expect(document.activeElement).toBe(links()[1]);
    });

    it('revient à la ligne précédente sur ArrowUp, avec bouclage', () => {
      press('ArrowUp', UP_ARROW);

      expect(document.activeElement).toBe(links()[1]);
    });

    it('applique le roving tabindex : une seule ligne atteignable par Tab', () => {
      expect(links().map(l => l.tabIndex)).toEqual([0, -1]);

      press('ArrowDown', DOWN_ARROW);
      press('ArrowDown', DOWN_ARROW);

      expect(links().map(l => l.tabIndex)).toEqual([-1, 0]);
    });

    it('ferme sur Tab — un menu ne piège pas la tabulation', () => {
      const close = jest.fn();
      comp.close.subscribe(close);

      press('Tab', TAB);

      expect(close).toHaveBeenCalled();
    });
  });

  describe('plein écran', () => {
    it('n’affiche les boutons retour et fermeture qu’en mode plein écran', () => {
      mount(parent([{ id: 'a', label: 'Alpha', routerLink: '/a' }]));
      expect(fixture.nativeElement.querySelector('.flyout-back')).toBeNull();

      fixture.componentRef.setInput('fullscreen', true);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.flyout-back')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('.flyout-close')).not.toBeNull();
    });
  });
});
