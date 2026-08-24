import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { DeclarationCaLayoutComponent } from './declaration-ca-layout.component';

/** Les onze onglets actifs ; `audit` reste commenté dans le gabarit (« pas ok pour le moment »). */
const SECTIONS = [
  { code: 'declaration-ca.exclusion-rayon', libelle: 'Rayons', icon: 'pi pi-th-large' },
  { code: 'declaration-ca.exclusion-tp', libelle: 'Tiers-payants', icon: 'pi pi-users' },
  { code: 'declaration-ca.parametres', libelle: 'Unités gratuites', icon: 'pi pi-gift' },
  { code: 'declaration-ca.journal-tp', libelle: 'Ventes tiers-payant exclues', icon: 'pi pi-list' },
  { code: 'declaration-ca.journal-ug', libelle: 'Unités gratuites vendues', icon: 'pi pi-list' },
  { code: 'declaration-ca.journal-rayon', libelle: 'Produits de rayons exclus', icon: 'pi pi-list' },
  { code: 'declaration-ca.ponction', libelle: 'Ponction', icon: 'pi pi-percentage' },
  { code: 'declaration-ca.ponction-historique', libelle: 'Historique des ponctions', icon: 'pi pi-history' },
  { code: 'declaration-ca.balance-reelle', libelle: 'Balance caisse (CA encaissé)', icon: 'pi pi-calculator' },
  { code: 'declaration-ca.taxe-report-reel', libelle: 'Rapport TVA (CA encaissé)', icon: 'pi pi-file-pdf' },
  { code: 'declaration-ca.tableau-pharmacien-reel', libelle: 'Tableau pharmacien (CA encaissé)', icon: 'pi pi-table' },
];

const PERMISSIONS = {
  canDisplay: true, canAccess: true, canCreate: false,
  canEdit: false, canDelete: false, canExport: false, canExecute: false,
};

describe('DeclarationCaLayout — menu vertical', () => {
  let fixture: ComponentFixture<DeclarationCaLayoutComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationCaLayoutComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), AbilityService],
    }).compileComponents();

    const arbre: INavNode[] = [
      {
        id: 1, code: 'declaration-ca', libelle: 'Retraitement du CA', targetType: 'ROUTE', ordre: 10,
        children: SECTIONS.map((s, i) => ({
          id: i + 2, ...s, targetType: 'SECTION' as const, ordre: (i + 1) * 10, permissions: PERMISSIONS,
        })),
        permissions: PERMISSIONS,
      },
    ];
    TestBed.inject(AbilityService).setFromNavTree(arbre);
    TestBed.inject(NavStore).navTree.set(arbre);

    fixture = TestBed.createComponent(DeclarationCaLayoutComponent);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('rend l’en-tête du module', () => {
    const header = element.querySelector('.pharma-nav-sidebar-header');
    expect(header?.textContent).toContain('Retraitement du CA');
    expect(header?.querySelector('i.pi-percentage')).not.toBeNull();
  });

  it('rend les onze onglets actifs', () => {
    expect(element.querySelectorAll('.pharma-nav-vertical-link').length).toBe(11);
  });

  it('affiche les libellés de la base', () => {
    const liens = element.querySelectorAll('.pharma-nav-vertical-link');
    const libelles = Array.from(liens, l => l.textContent?.replace(/[›\s]+/g, ' ').trim());
    expect(libelles).toEqual(SECTIONS.map(s => s.libelle));
  });

  it('suit un renommage sans rechargement', () => {
    const store = TestBed.inject(NavStore);
    store.navTree.set(
      store.navTree().map(racine => ({
        ...racine,
        children: racine.children?.map(e =>
          e.code === 'declaration-ca.ponction' ? { ...e, libelle: 'Prélèvement' } : e,
        ),
      })),
    );
    fixture.detectChanges();

    const libelles = Array.from(element.querySelectorAll('.pharma-nav-vertical-link'), l => l.textContent);
    expect(libelles.some(l => l?.includes('Prélèvement'))).toBe(true);
  });

  it('replie le menu et rend la largeur au contenu', () => {
    element.querySelector<HTMLButtonElement>('.nav-sidebar-toggle button')!.click();
    fixture.detectChanges();

    expect(element.querySelector('.pharma-nav-sidebar.is-collapsed')).not.toBeNull();
    expect(element.querySelector('.row > .col-auto')).not.toBeNull();
    expect(element.querySelectorAll('.pharma-nav-vertical-link').length).toBe(11);
  });

  it('conserve les largeurs de colonnes propres à cet écran', () => {
    expect(element.querySelector('.row > .col-lg-3')).not.toBeNull();
    expect(element.querySelector('.row > .col-lg-9')).not.toBeNull();
  });
});
