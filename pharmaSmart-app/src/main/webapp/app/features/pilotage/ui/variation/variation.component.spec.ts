import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VariationComponent } from './variation.component';

describe('Variation d’un indicateur', () => {
  let fixture: ComponentFixture<VariationComponent>;

  function afficher(entrees: Record<string, unknown>): HTMLElement {
    fixture = TestBed.createComponent(VariationComponent);
    Object.entries(entrees).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('une hausse du CA est favorable, en % et dite en toutes lettres', () => {
    const element = afficher({ ecart: 1000, ecartPct: 4.1, unite: 'MONTANT', sensFavorable: 'HAUSSE' });
    expect(element.querySelector('.variation--favorable')).not.toBeNull();
    expect(element.textContent).toContain('▲');
    expect(element.textContent).toContain('en hausse de');
    expect(element.textContent).toContain('4,1 %');
  });

  it('une hausse des remises est défavorable', () => {
    const element = afficher({ ecart: 500, ecartPct: 12, unite: 'MONTANT', sensFavorable: 'BAISSE' });
    expect(element.querySelector('.variation--defavorable')).not.toBeNull();
  });

  it('un taux varie en points', () => {
    const element = afficher({ ecart: -0.4, ecartPct: null, unite: 'POURCENTAGE', sensFavorable: 'HAUSSE' });
    expect(element.textContent).toContain('0,4 pt');
    expect(element.textContent).toContain('en baisse de');
  });

  it('sans référence comparable : n.s.', () => {
    const element = afficher({ ecart: null, ecartPct: null, unite: 'MONTANT', sensFavorable: 'HAUSSE' });
    expect(element.textContent?.trim()).toBe('n.s.');
  });
});
