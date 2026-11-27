import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { JournalRemisesLayoutComponent } from './journal-remises-layout.component';

const SECTIONS = [
  { code: 'journal-remises.synthese', libelle: 'Synthèse', icon: 'pi pi-chart-bar' },
  { code: 'journal-remises.promotions', libelle: 'Promotions', icon: 'pi pi-tags' },
  { code: 'journal-remises.remises', libelle: 'Remises produit', icon: 'pi pi-percentage' },
  { code: 'journal-remises.ug', libelle: 'UG vendues', icon: 'pi pi-gift' },
];

const PERMISSIONS = {
  canDisplay: true,
  canAccess: true,
  canCreate: false,
  canEdit: false,
  canDelete: false,
  canExport: true,
  canExecute: false,
};

function arbre(sections: typeof SECTIONS): INavNode[] {
  return [
    {
      id: 1,
      code: 'journal-remises',
      libelle: 'Journal des remises',
      targetType: 'ROUTE',
      ordre: 1,
      children: sections.map((s, i) => ({
        id: i + 2,
        ...s,
        targetType: 'SECTION' as const,
        ordre: (i + 1) * 10,
        permissions: PERMISSIONS,
      })),
      permissions: PERMISSIONS,
    },
  ];
}

describe('Journal des remises — onglets', () => {
  let fixture: ComponentFixture<JournalRemisesLayoutComponent>;

  function creer(sections: typeof SECTIONS): HTMLElement {
    const nav = arbre(sections);
    TestBed.inject(AbilityService).setFromNavTree(nav);
    TestBed.inject(NavStore).navTree.set(nav);
    fixture = TestBed.createComponent(JournalRemisesLayoutComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalRemisesLayoutComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateService(), AbilityService],
    }).compileComponents();
  });

  it('rend les quatre onglets, libellés de la base', () => {
    const element = creer(SECTIONS);
    const liens = Array.from(element.querySelectorAll('.pharma-nav-vertical-link')).map(a => a.textContent?.replace('›', '').trim());
    expect(liens).toEqual(['Synthèse', 'Promotions', 'Remises produit', 'UG vendues']);
    expect(element.querySelector('.pharma-nav-sidebar-header')?.textContent).toContain('Journal des remises');
  });

  it("masque l'onglet que le rôle ne voit pas", () => {
    const element = creer(SECTIONS.filter(s => s.code !== 'journal-remises.ug'));
    expect(element.querySelectorAll('.pharma-nav-vertical-link').length).toBe(3);
  });
});
