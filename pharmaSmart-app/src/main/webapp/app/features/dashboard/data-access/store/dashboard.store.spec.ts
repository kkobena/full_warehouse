import { TestBed } from '@angular/core/testing';
import { DashboardStore } from './dashboard.store';
import { findWidgetDefinition } from '../../widgets/widget-registry';
import { DashboardItem } from '../../models/dashboard.model';

describe('DashboardStore', () => {
  let store: InstanceType<typeof DashboardStore>;
  const note = findWidgetDefinition('note')!;
  const titre = findWidgetDefinition('titre-section')!;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [DashboardStore] });
    store = TestBed.inject(DashboardStore);
  });

  it('ouvre un dashboard vierge en édition, un dashboard rempli en lecture', () => {
    store.open(null, []);
    expect(store.editMode()).toBe(true);

    store.open({ id: 1, name: 'A' }, [item('x')]);
    expect(store.editMode()).toBe(false);
    expect(store.dirty()).toBe(false);
  });

  it('ajoute un widget sous les tuiles existantes et marque le dashboard modifié', () => {
    store.open({ id: 1 }, [item('x', { y: 2, h: 3 })]);
    const ajoute = store.addWidget(note);
    expect(ajoute).toMatchObject({ x: 0, y: 5, w: note.defaultSize.w, h: note.defaultSize.h, widgetKey: 'note' });
    expect(store.items()).toHaveLength(2);
    expect(store.dirty()).toBe(true);
  });

  it('le tassement des tuiles par la grille ne compte pas comme une modification', () => {
    store.open({ id: 1 }, [item('x', { y: 4 })]);
    store.applyPositions([{ id: 'x', x: 0, y: 0, w: 4, h: 3 }]);
    expect(store.items()[0].y).toBe(0);
    expect(store.dirty()).toBe(false);

    store.markDirty();
    expect(store.dirty()).toBe(true);
  });

  it('fusionne les paramètres modifiés sur place', () => {
    store.open({ id: 1 }, [item('x', { params: { text: 'a', autre: 'b' } })]);
    store.updateParams('x', { text: 'nouveau' });
    expect(store.items()[0].params).toEqual({ text: 'nouveau', autre: 'b' });
    expect(store.dirty()).toBe(true);
  });

  it('ne propose au catalogue que les widgets autorisés, et signale ceux déjà posés', () => {
    store.open(null, [item('x', { widgetKey: 'note' })]);
    store.setAllowed([{ key: 'note', licensed: true }]);
    expect(store.catalogue().map(e => e.definition.key)).toEqual(['note']);
    expect(store.catalogue()[0].alreadyAdded).toBe(true);
    expect(store.catalogue().some(e => e.definition === titre)).toBe(false);
  });

  it('calcule la disponibilité de chaque tuile selon les droits', () => {
    store.open(null, [item('a', { widgetKey: 'note' }), item('b', { widgetKey: 'titre-section' }), item('c', { widgetKey: 'supprime' })]);
    store.setAllowed([
      { key: 'note', licensed: true },
      { key: 'titre-section', licensed: false },
    ]);
    const dispo = store.availability();
    expect(dispo.get('a')).toBe('OK');
    expect(dispo.get('b')).toBe('NON_SOUSCRIT');
    expect(dispo.get('c')).toBe('INCONNU');

    store.setAllowed([]);
    expect(store.availability().get('a')).toBe('NON_AUTORISE');
  });

  it('un enregistrement remet le dashboard à l’état non modifié', () => {
    store.open(null, []);
    store.addWidget(note);
    store.markSaved({ id: 7, name: 'Mien' });
    expect(store.dirty()).toBe(false);
    expect(store.isSaved()).toBe(true);
  });

  it('les réglages remplacent titre et paramètres ; un titre vide revient au libellé du widget', () => {
    store.open({ id: 1 }, [item('x', { title: 'Ancien', params: { limite: '10', viz: 'TABLE' } })]);
    store.updateSettings('x', '  ', { limite: '20' });
    expect(store.items()[0].title).toBeUndefined();
    expect(store.items()[0].params).toEqual({ limite: '20' });
    expect(store.dirty()).toBe(true);
  });

  it('duplique une tuile sous l’original, avec ses propres paramètres', () => {
    store.open({ id: 1 }, [item('x', { y: 1, h: 3, params: { limite: '5' } })]);
    const copie = store.duplicateItem('x')!;
    expect(copie.id).not.toBe('x');
    expect(copie.y).toBe(4);
    copie.params!['limite'] = '99';
    expect(store.items()[0].params).toEqual({ limite: '5' });
    expect(store.items()).toHaveLength(2);
  });

  function item(id: string, patch: Partial<DashboardItem> = {}): DashboardItem {
    return { id, x: 0, y: 0, w: 4, h: 3, widgetKey: 'note', ...patch };
  }
});
