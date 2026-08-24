import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { NavSectionLinkComponent } from './nav-section-link.component';

@Component({
  standalone: true,
  imports: [NavSectionLinkComponent],
  template: `
    <a class="pharma-nav-vertical-link">
      <app-nav-section-link code="comptabilite.balance"
                            fallbackIcon="pi pi-calculator" fallbackLabel="Balance caisse" />
    </a>
  `,
})
class HoteTest {}

function arbre(libelle: string, icon: string): INavNode[] {
  return [
    {
      id: 1, code: 'comptabilite', libelle: 'Comptabilité', targetType: 'ROUTE', ordre: 10,
      children: [{ id: 2, code: 'comptabilite.balance', libelle, icon, targetType: 'SECTION', ordre: 10 }],
    },
  ];
}

describe('AppNavSectionLink — libellé et icône lus en base', () => {
  let store: NavStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoteTest],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    store = TestBed.inject(NavStore);
  });

  it('affiche le libellé et l’icône du nœud correspondant', () => {
    store.navTree.set(arbre('Balance de caisse réelle', 'pi pi-wallet'));

    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    const lien = (f.nativeElement as HTMLElement).querySelector('.pharma-nav-vertical-link');

    expect(lien?.textContent).toContain('Balance de caisse réelle');
    expect(lien?.querySelector('i')?.className).toContain('pi-wallet');
  });

  it('suit le renommage sans rechargement de la page', () => {
    store.navTree.set(arbre('Balance caisse', 'pi pi-calculator'));
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();

    store.navTree.set(arbre('Caisse du jour', 'pi pi-calculator'));
    f.detectChanges();

    expect((f.nativeElement as HTMLElement).textContent).toContain('Caisse du jour');
  });

  it('retombe sur le libellé du gabarit quand le code est absent de l’arbre', () => {
    store.navTree.set([]);

    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    const lien = (f.nativeElement as HTMLElement).querySelector('.pharma-nav-vertical-link');

    // Un menu muet est inutilisable : le repli prime sur la fidélité à la base.
    expect(lien?.textContent).toContain('Balance caisse');
    expect(lien?.querySelector('i')?.className).toContain('pi-calculator');
  });

  it('garde le chevron, que le libellé vienne de la base ou du repli', () => {
    store.navTree.set([]);
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();

    expect((f.nativeElement as HTMLElement).querySelector('.link-arrow')).not.toBeNull();
  });

  it('rend l’icône, le libellé et le chevron dans cet ordre', () => {
    store.navTree.set(arbre('Balance caisse', 'pi pi-calculator'));
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();

    // La mise en page du lien (`gap`, chevron poussé par `margin-left: auto`) suppose ces trois
    // éléments dans l'ordre. L'hôte les laisse traverser grâce à `display: contents` — que jsdom
    // ne sait pas calculer, d'où un contrôle sur la structure plutôt que sur le style.
    const hote = (f.nativeElement as HTMLElement).querySelector('app-nav-section-link')!;
    const balises = Array.from(hote.children, e => e.tagName.toLowerCase() + (e.className ? '.' + e.className : ''));
    expect(balises).toEqual(['i.pi pi-calculator', 'span', 'span.link-arrow']);
  });
});
