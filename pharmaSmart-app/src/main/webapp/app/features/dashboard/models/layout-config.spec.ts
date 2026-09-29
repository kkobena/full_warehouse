import { parseLayoutConfig, serializeLayoutConfig } from './layout-config';
import { DashboardItem } from './dashboard.model';

describe('layout-config', () => {
  const note: DashboardItem = { id: 'a', x: 0, y: 0, w: 4, h: 2, widgetKey: 'note', params: { text: 'Bonjour' } };

  it('relit ce qu’il a écrit', () => {
    const { config, legacyItemsDropped } = parseLayoutConfig(serializeLayoutConfig([note]));
    expect(config.version).toBe(2);
    expect(config.items).toEqual([{ ...note, title: undefined }]);
    expect(legacyItemsDropped).toBe(0);
  });

  it('écarte les tuiles de l’ancien format, qui n’avaient pas de widgetKey', () => {
    const ancien = JSON.stringify({
      items: [
        { id: 'w1', x: 0, y: 0, w: 4, h: 3, widget: { type: 'KPI_CARD', title: 'Widget' } },
        { id: 'w2', x: 4, y: 0, w: 4, h: 3, widgetKey: 'note' },
      ],
    });
    const { config, legacyItemsDropped } = parseLayoutConfig(ancien);
    expect(config.items.map(i => i.id)).toEqual(['w2']);
    expect(legacyItemsDropped).toBe(1);
  });

  it('rend un dashboard vide sur une configuration absente ou illisible', () => {
    expect(parseLayoutConfig(undefined).config.items).toEqual([]);
    expect(parseLayoutConfig('{pas du json').config.items).toEqual([]);
    expect(parseLayoutConfig('{"items": "non"}').config.items).toEqual([]);
  });

  it('corrige les positions invalides et attribue un identifiant manquant', () => {
    const { config } = parseLayoutConfig(JSON.stringify({ items: [{ widgetKey: 'note', x: -3, y: 'haut', w: 2.6 }] }));
    const [item] = config.items;
    expect(item.id).toMatch(/^w-/);
    expect(item).toMatchObject({ x: 0, y: 0, w: 3, h: 3 });
  });
});
