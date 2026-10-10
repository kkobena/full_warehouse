import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnalysePilotage, Axe } from '../../models/pilotage.model';
import { TableauAnalyseComponent } from './tableau-analyse.component';

const famille: Axe = { code: 'FAMILLE', libelle: 'Famille', sources: ['LIGNES'], suivant: 'PRODUIT', filtrable: true, ordonne: false };
const produit: Axe = { ...famille, code: 'PRODUIT', libelle: 'Produit', suivant: null };

const analyse = (axe: Axe, avecReference = true): AnalysePilotage => ({
  comparaison: { periode: { du: '2026-10-01', au: '2026-10-09' }, reference: avecReference ? { du: '2025-10-01', au: '2025-10-09' } : null, aDate: true },
  source: 'LIGNES',
  indicateurs: [{ code: 'CA_TTC', libelle: 'CA TTC', definition: '', unite: 'MONTANT', sensFavorable: 'HAUSSE', droit: 'PAGE' }],
  indicateursIgnores: [],
  axe,
  axe2: null,
  total: [{ valeur: 100_000, valeurReference: 90_000, ecart: 10_000, ecartPct: 11.1 }],
  elements: [{ cle: '12', libelle: 'Antalgiques', cellules: [{ valeur: 60_000, valeurReference: 50_000, ecart: 10_000, ecartPct: 20 }], part: 60, contribution: 100 }],
  autres: { cle: null, libelle: 'Autres (3)', cellules: [{ valeur: 40_000, valeurReference: 40_000, ecart: 0, ecartPct: 0 }], part: 40, contribution: 0 },
  nombreElements: 4,
  croise: null,
});

describe("Tableau à variations de l'onglet Analyser", () => {
  let fixture: ComponentFixture<TableauAnalyseComponent>;

  function afficher(valeur: AnalysePilotage, peutVoirVentes = true): HTMLElement {
    fixture = TestBed.createComponent(TableauAnalyseComponent);
    fixture.componentRef.setInput('analyse', valeur);
    fixture.componentRef.setInput('peutVoirVentes', peutVoirVentes);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const boutons = (element: HTMLElement): string[] =>
    Array.from(element.querySelectorAll('tbody app-button button')).map(bouton => bouton.getAttribute('aria-label') ?? '');

  it('valeur complète, part, ligne « autres » sans action, total', () => {
    const element = afficher(analyse(famille));

    // Intl sépare les milliers par une espace fine insécable.
    expect(element.textContent).toMatch(/60\s000/);
    expect(element.textContent).toContain('60,0 %');
    expect(element.textContent).toContain('Autres (3)');
    expect(element.textContent).toContain('Total (4)');
    expect(boutons(element)).toEqual(['Détailler Antalgiques', "Expliquer l'écart de Antalgiques"]);
  });

  it('sur un produit, on descend aux ventes si le droit le permet', () => {
    expect(boutons(afficher(analyse(produit)))).toEqual(['Ventes de Antalgiques', "Expliquer l'écart de Antalgiques"]);
    expect(boutons(afficher(analyse(produit), false))).toEqual(["Expliquer l'écart de Antalgiques"]);
  });

  it('sans référence, ni variation ni explication', () => {
    const element = afficher(analyse(famille, false));

    expect(element.querySelector('app-variation')).toBeNull();
    expect(boutons(element)).toEqual(['Détailler Antalgiques']);
  });
});
