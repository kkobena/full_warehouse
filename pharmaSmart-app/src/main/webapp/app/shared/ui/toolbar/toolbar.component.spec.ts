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

@Component({
  standalone: true,
  imports: [ToolbarComponent],
  template: `
    <app-toolbar title="Ventes">
      <ng-container ngProjectAs="[toolbarActions]">
        <input id="champ" />
        <button id="a">A</button>
        <button id="b" disabled>B</button>
        <button id="c">C</button>
        <button id="d">D</button>
      </ng-container>
    </app-toolbar>
  `,
})
class HoteAvecActions {}

describe('AppToolbar — navigation aux flèches', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoteAvecActions],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  function monter(): HTMLElement {
    const f = TestBed.createComponent(HoteAvecActions);
    f.detectChanges();
    document.body.appendChild(f.nativeElement);
    return f.nativeElement as HTMLElement;
  }

  function appuyer(el: HTMLElement, key: string): void {
    el.focus();
    el.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
  }

  it('expose la zone d’actions comme une barre d’outils nommée', () => {
    const barre = monter().querySelector('.pharma-toolbar-actions')!;
    expect(barre.getAttribute('role')).toBe('toolbar');
    expect(barre.getAttribute('aria-label')).toBe('Actions : Ventes');
  });

  it('passe au bouton suivant actif, et boucle', () => {
    const hote = monter();
    appuyer(hote.querySelector('#a')!, 'ArrowRight');
    expect(document.activeElement?.id).toBe('c');
    appuyer(hote.querySelector('#d')!, 'ArrowRight');
    expect(document.activeElement?.id).toBe('a');
  });

  it('va au premier et au dernier avec Début et Fin', () => {
    const hote = monter();
    appuyer(hote.querySelector('#c')!, 'End');
    expect(document.activeElement?.id).toBe('d');
    appuyer(hote.querySelector('#d')!, 'Home');
    expect(document.activeElement?.id).toBe('a');
  });

  it('laisse les flèches au champ de saisie', () => {
    const hote = monter();
    appuyer(hote.querySelector('#champ')!, 'ArrowRight');
    expect(document.activeElement?.id).toBe('champ');
  });
});
