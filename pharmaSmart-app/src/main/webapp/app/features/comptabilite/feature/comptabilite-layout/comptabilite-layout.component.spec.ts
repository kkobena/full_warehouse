import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { ComptabiliteLayoutComponent } from './comptabilite-layout.component';

/**
 * Vérifie que le passage à `<app-nav-sidebar>` n'a rien changé pour l'utilisateur.
 *
 * <p>Le risque de cette refonte est silencieux : si `ngbNav` cessait de voir ses onglets — parce
 * qu'un `<ng-content>` s'interposerait entre lui et eux — le menu s'afficherait vide, sans la
 * moindre erreur de compilation. C'est ce cas que ces tests ferment.
 */
const CODES = [
  { code: 'comptabilite.balance', libelle: 'Balance caisse', icon: 'pi pi-calculator' },
  { code: 'comptabilite.taxe-report', libelle: 'Rapport TVA', icon: 'pi pi-file-pdf' },
  { code: 'comptabilite.tableau-pharmacien', libelle: 'Tableau pharmacien', icon: 'pi pi-table' },
  { code: 'comptabilite.recapitulatif-caisse', libelle: 'Récapitulatif de caisse', icon: 'pi pi-chart-bar' },
  { code: 'comptabilite.raport-activite', libelle: "Rapport d'activité", icon: 'pi pi-chart-line' },
];

const PERMISSIONS = {
  canDisplay: true, canAccess: true, canCreate: false,
  canEdit: false, canDelete: false, canExport: false, canExecute: false,
};

describe('ComptabiliteLayout — menu vertical', () => {
  let fixture: ComponentFixture<ComptabiliteLayoutComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComptabiliteLayoutComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), AbilityService],
    }).compileComponents();

    // Sans arbre de navigation, `canSignal` refuse tout et le menu est vide : ce n'est pas une
    // conséquence de la refonte, c'est le fonctionnement nominal des permissions.
    const arbre: INavNode[] = [
      {
        id: 1, code: 'comptabilite', libelle: 'Comptabilité', targetType: 'ROUTE', ordre: 10,
        children: CODES.map(({ code, libelle, icon }, i) => ({
          id: i + 2, code, libelle, icon, targetType: 'SECTION' as const, ordre: (i + 1) * 10,
          permissions: PERMISSIONS,
        })),
        permissions: PERMISSIONS,
      },
    ];
    TestBed.inject(AbilityService).setFromNavTree(arbre);
    TestBed.inject(NavStore).navTree.set(arbre);

    fixture = TestBed.createComponent(ComptabiliteLayoutComponent);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('rend l’en-tête du module', () => {
    const header = element.querySelector('.pharma-nav-sidebar-header');
    expect(header?.textContent).toContain('Comptabilité');
    expect(header?.querySelector('i')?.className).toContain('pi-book');
  });

  it('rend les cinq onglets, dans l’ordre du gabarit', () => {
    const liens = element.querySelectorAll('.pharma-nav-vertical-link');
    expect(liens.length).toBe(5);
    const libelles = Array.from(liens, l => l.textContent?.replace(/[›\s]+/g, ' ').trim());
    expect(libelles).toEqual([
      'Balance caisse',
      'Rapport TVA',
      'Tableau pharmacien',
      'Récapitulatif de caisse',
      "Rapport d'activité",
    ]);
  });

  it('marque le premier onglet comme actif au chargement', () => {
    const actif = element.querySelector('.pharma-nav-vertical-link.active');
    expect(actif?.textContent).toContain('Balance caisse');
  });

  it('change d’onglet actif au clic', async () => {
    const liens = element.querySelectorAll<HTMLAnchorElement>('.pharma-nav-vertical-link');
    liens[2].click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const actif = element.querySelector('.pharma-nav-vertical-link.active');
    expect(actif?.textContent).toContain('Tableau pharmacien');
  });

  it('affiche le libellé de la base, et non celui du gabarit', () => {
    const store = TestBed.inject(NavStore);
    store.navTree.set(
      store.navTree().map(racine => ({
        ...racine,
        children: racine.children?.map(enfant =>
          enfant.code === 'comptabilite.balance' ? { ...enfant, libelle: 'Caisse du jour' } : enfant,
        ),
      })),
    );
    fixture.detectChanges();

    const liens = element.querySelectorAll('.pharma-nav-vertical-link');
    expect(liens[0].textContent).toContain('Caisse du jour');
    expect(liens[0].textContent).not.toContain('Balance caisse');
  });

  it('replie le menu et rend la largeur au contenu', () => {
    const bouton = element.querySelector<HTMLButtonElement>('.nav-sidebar-toggle button');
    expect(bouton).not.toBeNull();

    bouton!.click();
    fixture.detectChanges();

    expect(element.querySelector('.pharma-nav-sidebar.is-collapsed')).not.toBeNull();
    expect(element.querySelector('.row > .col-auto')).not.toBeNull();
    // Les cinq onglets restent : replier masque les libellés, pas la navigation.
    expect(element.querySelectorAll('.pharma-nav-vertical-link').length).toBe(5);
  });

  it('conserve la grille à deux colonnes', () => {
    expect(element.querySelector('.row > .col-lg-2')).not.toBeNull();
    expect(element.querySelector('.row > .col-lg-10')).not.toBeNull();
  });
});
