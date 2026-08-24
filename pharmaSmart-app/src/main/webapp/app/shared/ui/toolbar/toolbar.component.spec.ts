import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { ToolbarComponent } from './toolbar.component';

@Component({
  standalone: true,
  imports: [ToolbarComponent],
  template: `<app-toolbar code="comptabilite.balance" title="Balance de caisse" />`,
})
class HoteAvecCode {}

@Component({
  standalone: true,
  imports: [ToolbarComponent],
  template: `<app-toolbar title="Écran sans entrée de menu" />`,
})
class HoteSansCode {}

function arbre(libelle: string, titreLong?: string): INavNode[] {
  return [
    {
      id: 1, code: 'comptabilite', libelle: 'Comptabilité', targetType: 'ROUTE', ordre: 10,
      children: [{ id: 2, code: 'comptabilite.balance', libelle, titreLong, targetType: 'SECTION', ordre: 10 }],
    },
  ];
}

describe('AppToolbar — titre issu de la navigation', () => {
  let store: NavStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoteAvecCode, HoteSansCode],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    store = TestBed.inject(NavStore);
  });

  function titreAffiche(hote: typeof HoteAvecCode | typeof HoteSansCode): string {
    const f = TestBed.createComponent(hote);
    f.detectChanges();
    return (f.nativeElement as HTMLElement).querySelector('.pharma-toolbar-title')!.textContent!.trim();
  }

  it('préfère le titre long au libellé du menu', () => {
    store.navTree.set(arbre('Balance caisse', 'Balance de caisse détaillée'));
    expect(titreAffiche(HoteAvecCode)).toBe('Balance de caisse détaillée');
  });

  it('retombe sur le libellé du menu quand aucun titre long n’est saisi', () => {
    // Cas courant : `titre_long` n'est renseigné que là où les deux valeurs diffèrent.
    store.navTree.set(arbre('Balance caisse'));
    expect(titreAffiche(HoteAvecCode)).toBe('Balance caisse');
  });

  it('retombe sur le titre du gabarit quand le code est absent de l’arbre', () => {
    // Item désactivé, installation plus ancienne, arbre pas encore chargé : un écran sans titre
    // serait pire qu'un titre périmé.
    store.navTree.set([]);
    expect(titreAffiche(HoteAvecCode)).toBe('Balance de caisse');
  });

  it('laisse intact un écran qui ne déclare pas de code', () => {
    store.navTree.set(arbre('Balance caisse', 'Balance de caisse détaillée'));
    expect(titreAffiche(HoteSansCode)).toBe('Écran sans entrée de menu');
  });

  it('suit un renommage sans rechargement', () => {
    store.navTree.set(arbre('Balance caisse'));
    const f = TestBed.createComponent(HoteAvecCode);
    f.detectChanges();

    store.navTree.set(arbre('Caisse du jour'));
    f.detectChanges();

    expect((f.nativeElement as HTMLElement).querySelector('.pharma-toolbar-title')!.textContent!.trim())
      .toBe('Caisse du jour');
  });
});
