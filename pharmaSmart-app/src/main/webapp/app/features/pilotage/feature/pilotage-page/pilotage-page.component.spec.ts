import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { ONGLETS_PILOTAGE } from '../../pilotage-onglets';
import { PilotagePageComponent } from './pilotage-page.component';

const PERMISSIONS = {
  canDisplay: true,
  canAccess: true,
  canCreate: false,
  canEdit: false,
  canDelete: false,
  canExport: true,
  canExecute: false,
};

function arbre(codesAutorises: string[]): INavNode[] {
  return [
    {
      id: 1,
      code: 'pilotage',
      libelle: 'Pilotage',
      titreLong: "Pilotage de l'officine",
      targetType: 'ROUTE',
      ordre: 1,
      permissions: PERMISSIONS,
      children: ONGLETS_PILOTAGE.map((onglet, i) => ({
        id: i + 2,
        code: onglet.code,
        libelle: onglet.libelle,
        icon: onglet.icone,
        targetType: 'SECTION' as const,
        ordre: (i + 1) * 10,
        permissions: codesAutorises.includes(onglet.code) ? PERMISSIONS : undefined,
      })),
    },
  ];
}

describe('PilotagePage — onglets', () => {
  let fixture: ComponentFixture<PilotagePageComponent>;
  let element: HTMLElement;
  let queryParams: BehaviorSubject<ParamMap>;

  function monter(codesAutorises: string[], onglet?: string): void {
    queryParams = new BehaviorSubject(convertToParamMap(onglet ? { onglet } : {}));
    TestBed.configureTestingModule({
      imports: [PilotagePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        AbilityService,
        { provide: ActivatedRoute, useValue: { queryParamMap: queryParams } },
      ],
    });
    const navigation = arbre(codesAutorises);
    TestBed.inject(AbilityService).setFromNavTree(navigation);
    TestBed.inject(NavStore).navTree.set(navigation);
    fixture = TestBed.createComponent(PilotagePageComponent);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  }

  const libellesDesOnglets = (): string[] => Array.from(element.querySelectorAll('.pharma-nav-tab-link'), l => l.textContent!.trim());

  it('nomme la page par le titre long de la navigation', () => {
    monter(ONGLETS_PILOTAGE.map(o => o.code));
    expect(element.querySelector('h1')?.textContent).toContain("Pilotage de l'officine");
  });

  it('rend les huit onglets dans une liste d’onglets nommée', () => {
    monter(ONGLETS_PILOTAGE.map(o => o.code));
    const liste = element.querySelector('[role="tablist"]');
    expect(liste?.getAttribute('aria-label')).toBe('Onglets du pilotage');
    expect(libellesDesOnglets()).toEqual(ONGLETS_PILOTAGE.map(o => o.libelle));
  });

  it('masque les onglets sans droit et ouvre le premier autorisé', () => {
    monter(['pilotage.rentabilite-remises', 'pilotage.objectifs']);
    expect(libellesDesOnglets()).toEqual(['Rentabilité & remises', 'Objectifs']);
    expect(element.querySelector('.pharma-nav-tab-link.active')?.textContent).toContain('Rentabilité & remises');
  });

  it('ouvre l’onglet demandé dans l’URL s’il est autorisé', () => {
    monter(ONGLETS_PILOTAGE.map(o => o.code), 'objectifs');
    expect(element.querySelector('.pharma-nav-tab-link.active')?.textContent).toContain('Objectifs');
  });

  it('ignore un onglet de l’URL non autorisé', () => {
    monter(['pilotage.tableau-de-bord'], 'objectifs');
    expect(element.querySelector('.pharma-nav-tab-link.active')?.textContent).toContain('Tableau de bord');
  });

  it('écrit l’onglet choisi dans l’URL', () => {
    monter(ONGLETS_PILOTAGE.map(o => o.code));
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    (element.querySelectorAll('.pharma-nav-tab-link')[2] as HTMLElement).click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { onglet: 'comparer-annees' }, replaceUrl: true }));
  });

  it('prévient quand aucun onglet n’est ouvert', () => {
    monter([]);
    expect(element.textContent).toContain('Aucun onglet du pilotage ne vous est ouvert');
  });
});
