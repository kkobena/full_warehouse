import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgSelectComponent } from '@ng-select/ng-select';

import { MultiSelectComponent } from './multi-select.component';
import { SelectSearchComponent } from './select-search.component';

const RAYONS = [
  { id: 1, nom: 'Antalgiques' },
  { id: 2, nom: 'Antibiotiques' },
  { id: 3, nom: 'Dermatologie' },
];

/**
 * Ces wrappers sont des façades : on vérifie ce qu'ils **transmettent** à ng-select et
 * ce qu'ils **relaient** en retour, sans jamais piloter les rouages internes de la lib —
 * sinon le test casserait à la moindre montée de version.
 */
describe('SelectSearchComponent', () => {
  let fixture: ComponentFixture<SelectSearchComponent>;
  let component: SelectSearchComponent;

  const debugSelect = (): DebugElement => {
    fixture.detectChanges();
    return fixture.debugElement.query(By.directive(NgSelectComponent));
  };

  const ngSelect = (): NgSelectComponent => debugSelect().componentInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SelectSearchComponent] }).compileComponents();
    fixture = TestBed.createComponent(SelectSearchComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('items', RAYONS);
  });

  it('transmet les options et les liaisons à ng-select', () => {
    fixture.componentRef.setInput('bindLabel', 'nom');
    fixture.componentRef.setInput('bindValue', 'id');
    const select = ngSelect();
    expect(select.items()).toEqual(RAYONS);
    expect(select.bindLabel()).toBe('nom');
    expect(select.bindValue()).toBe('id');
  });

  it('reste en sélection simple', () => {
    expect(ngSelect().multiple()).toBe(false);
  });

  it('propage placeholder, loading et clearable', () => {
    fixture.componentRef.setInput('placeholder', 'Choisir');
    fixture.componentRef.setInput('loading', true);
    fixture.componentRef.setInput('clearable', false);
    const select = ngSelect();
    expect(select.placeholder()).toBe('Choisir');
    expect(select.loading()).toBe(true);
    expect(select.clearable()).toBe(false);
  });

  // `NgModel` applique l'état désactivé sur une microtâche : sans le flush, l'assertion
  // s'exécuterait un tick trop tôt.
  it('propage la désactivation venue du formulaire', async () => {
    component.setDisabledState(true);
    fixture.detectChanges();
    await Promise.resolve();
    expect([...debugSelect().nativeElement.classList]).toContain('ng-select-disabled');
  });

  it('applique la classe compacte', () => {
    fixture.componentRef.setInput('small', true);
    expect([...debugSelect().nativeElement.classList]).toContain('app-select-sm');
  });

  it('marque le champ invalide', () => {
    fixture.componentRef.setInput('invalid', true);
    expect([...debugSelect().nativeElement.classList]).toEqual(expect.arrayContaining(['ng-invalid', 'ng-touched']));
  });

  describe('relais des événements', () => {
    it('notifie onChange et selectionChange à la sélection', () => {
      const onChange = jest.fn();
      const selectionChange = jest.fn();
      component.registerOnChange(onChange);
      component.selectionChange.subscribe(selectionChange);

      debugSelect().triggerEventHandler('ngModelChange', RAYONS[1]);

      expect(onChange).toHaveBeenCalledWith(RAYONS[1]);
      expect(selectionChange).toHaveBeenCalledWith(RAYONS[1]);
    });

    /*
     * On émet depuis le **vrai** output de ng-select, pas via `triggerEventHandler`.
     *
     * Raison : un nom d'événement inconnu sur un élément de composant compile en simple
     * écouteur DOM. `triggerEventHandler('searchEvent', …)` trouvait donc cet écouteur et
     * le test passait au vert — alors que ng-select émet en réalité sur `searchEvent`
     * (aliasé `search` côté template) et que rien n'était branché en production.
     * Passer par l'instance rend le test sensible à cette erreur d'alias.
     */
    it('relaie le terme de recherche', () => {
      const searched = jest.fn();
      component.searched.subscribe(searched);
      ngSelect().searchEvent.emit({ term: 'anti', items: [] });
      expect(searched).toHaveBeenCalledWith('anti');
    });

    it('relaie le défilement en fin de liste', () => {
      const scrolled = jest.fn();
      component.scrolledToEnd.subscribe(scrolled);
      ngSelect().scrollToEnd.emit({ start: 0, end: 3 });
      expect(scrolled).toHaveBeenCalled();
    });

    it('notifie onTouched au blur', () => {
      const onTouched = jest.fn();
      component.registerOnTouched(onTouched);
      ngSelect().blurEvent.emit(null);
      expect(onTouched).toHaveBeenCalled();
    });
  });

  it('writeValue est répercuté sur la sélection de ng-select', async () => {
    fixture.componentRef.setInput('bindLabel', 'nom');
    component.writeValue(RAYONS[0]);
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
    expect(ngSelect().selectedItems.map(item => item.value)).toEqual([RAYONS[0]]);
  });
});

describe('MultiSelectComponent', () => {
  let fixture: ComponentFixture<MultiSelectComponent>;
  let component: MultiSelectComponent;

  const debugSelect = (): DebugElement => {
    fixture.detectChanges();
    return fixture.debugElement.query(By.directive(NgSelectComponent));
  };

  const ngSelect = (): NgSelectComponent => debugSelect().componentInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MultiSelectComponent] }).compileComponents();
    fixture = TestBed.createComponent(MultiSelectComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('items', RAYONS);
  });

  it('active la sélection multiple', () => {
    expect(ngSelect().multiple()).toBe(true);
  });

  it('laisse le panneau ouvert entre deux choix par défaut', () => {
    // Sinon l'utilisateur doit rouvrir la liste à chaque option — pénible sur un filtre.
    expect(ngSelect().closeOnSelect()).toBe(false);
  });

  it('propage maxSelectedItems et hideSelected', () => {
    fixture.componentRef.setInput('maxSelectedItems', 2);
    fixture.componentRef.setInput('hideSelected', true);
    const select = ngSelect();
    expect(select.maxSelectedItems()).toBe(2);
    expect(select.hideSelected()).toBe(true);
  });

  it('émet un tableau de valeurs', () => {
    const onChange = jest.fn();
    component.registerOnChange(onChange);
    debugSelect().triggerEventHandler('ngModelChange', [RAYONS[0], RAYONS[2]]);
    expect(onChange).toHaveBeenCalledWith([RAYONS[0], RAYONS[2]]);
  });
});
