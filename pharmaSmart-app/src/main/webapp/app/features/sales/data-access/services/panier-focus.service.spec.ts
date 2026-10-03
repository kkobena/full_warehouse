import {PanierFocusService} from './panier-focus.service';

/** Où va le focus après une mise à jour de ligne : la correction au clavier le garde dans la grille. */
describe('PanierFocusService', () => {
  let service: PanierFocusService;

  beforeEach(() => {
    jest.useFakeTimers();
    service = new PanierFocusService();
  });

  afterEach(() => jest.useRealTimers());

  it('ne maintient rien par défaut : le focus retourne à la recherche produit', () => {
    expect(service.consommer()).toBe(false);
  });

  it("maintient le focus dans la grille pour UNE mise à jour, puis revient au comportement par défaut", () => {
    service.demanderMaintien();

    expect(service.consommer()).toBe(true);
    expect(service.consommer()).toBe(false);
  });

  it("expire seule : une mise à jour refusée ne prive pas de focus une validation ultérieure", () => {
    service.demanderMaintien(1000);

    jest.advanceTimersByTime(1001);

    expect(service.consommer()).toBe(false);
  });
});
