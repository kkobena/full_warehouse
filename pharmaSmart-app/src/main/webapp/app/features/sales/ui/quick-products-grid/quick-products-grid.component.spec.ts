import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProduitSearch } from '../../../../shared/model';
import { ProduitsFavorisApiService } from '../../../products/data-access/services/produits-favoris-api.service';
import { QuickProductsGridComponent } from './quick-products-grid.component';

/** Grille des favoris du comptoir : une tuile par produit épinglé, un clic ajoute le produit au panier. */
describe('QuickProductsGridComponent', () => {
  let fixture: ComponentFixture<QuickProductsGridComponent>;
  let composant: QuickProductsGridComponent;
  const api = { lister: jest.fn() };

  const produit = (id: number, libelle: string, totalQuantity: number, regularUnitPrice = 1500) =>
    ({ id, libelle, totalQuantity, regularUnitPrice }) as ProduitSearch;
  const el = () => fixture.nativeElement as HTMLElement;
  const tuiles = () => Array.from(el().querySelectorAll<HTMLButtonElement>('button.tuile'));
  const basculeur = () => el().querySelector<HTMLButtonElement>('button.favoris-titre');

  function creer(favoris: ProduitSearch[], inputs: Record<string, unknown> = {}): void {
    api.lister.mockReturnValue(of(favoris));
    fixture = TestBed.createComponent(QuickProductsGridComponent);
    composant = fixture.componentInstance;
    Object.entries(inputs).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [QuickProductsGridComponent],
      providers: [{ provide: ProduitsFavorisApiService, useValue: api }],
    });
  });

  it("n'occupe aucune place tant que rien n'est épinglé", () => {
    creer([]);

    expect(el().querySelector('section')).toBeNull();
    expect(tuiles()).toHaveLength(0);
  });

  it("n'occupe aucune place si la lecture des favoris échoue", () => {
    api.lister.mockReturnValue(throwError(() => new Error('réseau')));
    fixture = TestBed.createComponent(QuickProductsGridComponent);
    fixture.detectChanges();

    expect(el().querySelector('section')).toBeNull();
  });

  it('montre une tuile par favori, dans l’ordre reçu, avec son prix', () => {
    creer([produit(1, 'MASQUE CHIRURGICAL', 40, 2500), produit(2, 'GANTS NITRILE', 10, 1200)]);

    expect(tuiles().map(t => t.querySelector('.tuile-libelle')?.textContent?.trim())).toEqual(['MASQUE CHIRURGICAL', 'GANTS NITRILE']);
    expect(tuiles()[0].querySelector('.tuile-prix')?.textContent).toMatch(/2\s?500/);
    expect(el().querySelector('.favoris-nombre')?.textContent?.trim()).toBe('2');
  });

  it('signale une rupture mais laisse la tuile cliquable : l’ajout ouvre alors les équivalents', () => {
    creer([produit(1, 'MASQUE', 40), produit(2, 'GANTS', 0)]);
    const [enStock, enRupture] = tuiles();

    expect(enStock.classList.contains('tuile--rupture')).toBe(false);
    expect(enRupture.classList.contains('tuile--rupture')).toBe(true);
    expect(enRupture.querySelector('.tuile-etat')?.textContent).toContain('Rupture');
    expect(enRupture.disabled).toBe(false);
  });

  it('un clic émet le produit au format de la recherche', () => {
    creer([produit(1, 'MASQUE', 40), produit(2, 'GANTS', 10)]);
    const choisis: ProduitSearch[] = [];
    composant.productChosen.subscribe(p => choisis.push(p));

    tuiles()[1].click();

    expect(choisis.map(p => p.id)).toEqual([2]);
  });

  it('relit la grille après un clic, le stock ayant bougé', () => {
    jest.useFakeTimers();
    try {
      creer([produit(1, 'MASQUE', 40)]);
      expect(api.lister).toHaveBeenCalledTimes(1);

      tuiles()[0].click();
      jest.advanceTimersByTime(1500);

      expect(api.lister).toHaveBeenCalledTimes(2);
    } finally {
      jest.useRealTimers();
    }
  });

  it('ne répond plus pendant l’enregistrement de la vente', () => {
    creer([produit(1, 'MASQUE', 40)], { disabled: true });
    const choisis: ProduitSearch[] = [];
    composant.productChosen.subscribe(p => choisis.push(p));

    expect(tuiles()[0].disabled).toBe(true);
    // Même un appel direct ne passe pas : le garde-fou ne dépend pas de l'attribut du bouton.
    (composant as unknown as { choisir(p: ProduitSearch): void }).choisir(produit(1, 'MASQUE', 40));

    expect(choisis).toHaveLength(0);
  });

  it('se replie et se déplie, et retient ce choix pour la prochaine ouverture', () => {
    creer([produit(1, 'MASQUE', 40)]);
    expect(basculeur()?.getAttribute('aria-expanded')).toBe('true');

    basculeur()?.click();
    fixture.detectChanges();
    expect(basculeur()?.getAttribute('aria-expanded')).toBe('false');
    expect(tuiles()).toHaveLength(0);
    expect(localStorage.getItem('pharmasmart_favoris_ouverts')).toBe('0');

    // Nouvelle ouverture de l'écran : la grille reste repliée.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [QuickProductsGridComponent],
      providers: [{ provide: ProduitsFavorisApiService, useValue: api }],
    });
    creer([produit(1, 'MASQUE', 40)]);
    expect(basculeur()?.getAttribute('aria-expanded')).toBe('false');
  });
});
