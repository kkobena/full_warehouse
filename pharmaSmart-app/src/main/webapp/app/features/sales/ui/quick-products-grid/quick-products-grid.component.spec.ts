import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProduitSearch } from '../../../../shared/model';
import { ProduitsFavorisApiService } from '../../../products/data-access/services/produits-favoris-api.service';
import { PromotionsEnVigueurService } from '../../../promotion/data-access/services/promotions-en-vigueur.service';
import { IPromotionArticle } from '../../../promotion/models/promotion.model';
import { QuickProductsGridComponent } from './quick-products-grid.component';

/** Grille des favoris du comptoir : une tuile par produit épinglé, un clic ajoute le produit au panier. */
describe('QuickProductsGridComponent', () => {
  let fixture: ComponentFixture<QuickProductsGridComponent>;
  let composant: QuickProductsGridComponent;
  const api = { lister: jest.fn() };
  const parProduit = signal<ReadonlyMap<number, IPromotionArticle>>(new Map());
  const promotions = {
    parProduit,
    charger: jest.fn(),
    pour: (id: number | null | undefined): IPromotionArticle | undefined => (id == null ? undefined : parProduit().get(id)),
  };
  const providers = [
    { provide: ProduitsFavorisApiService, useValue: api },
    { provide: PromotionsEnVigueurService, useValue: promotions },
  ];
  const promo = (produitId: number, prixApresPromo: number, quantiteMin = 1): IPromotionArticle => ({
    produitId,
    promotionId: 9,
    libelle: 'Opération été',
    prixReference: 1500,
    prixApresPromo,
    dateFin: '2026-12-31',
    quantiteMin,
  });

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
    parProduit.set(new Map());
    TestBed.configureTestingModule({
      imports: [QuickProductsGridComponent],
      providers,
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
      providers,
    });
    creer([produit(1, 'MASQUE', 40)]);
    expect(basculeur()?.getAttribute('aria-expanded')).toBe('false');
  });

  describe('articles en promotion', () => {
    const prixAvant = (t: HTMLElement): string | undefined => t.querySelector('.tuile-prix-avant')?.textContent ?? undefined;

    it('barre le prix normal, affiche le prix remisé et la pastille PROMO', () => {
      parProduit.set(new Map([[1, promo(1, 1200)]]));
      creer([produit(1, 'CREME SOLAIRE', 40), produit(2, 'GANTS', 10)]);
      const [enPromo, normal] = tuiles();

      expect(enPromo.classList.contains('tuile--promo')).toBe(true);
      expect(prixAvant(enPromo)).toMatch(/1\s?500/);
      expect(enPromo.querySelector('.tuile-prix')?.textContent).toMatch(/1\s?200/);
      expect(enPromo.querySelector('app-badge-promo')).not.toBeNull();
      expect(enPromo.getAttribute('aria-label')).toContain('en promotion à 1200 francs au lieu de 1500');

      expect(normal.classList.contains('tuile--promo')).toBe(false);
      expect(normal.querySelector('app-badge-promo')).toBeNull();
    });

    it("« dès 3 » : un clic n'ajoute qu'une unité, la tuile garde donc le prix normal et la seule pastille", () => {
      parProduit.set(new Map([[1, promo(1, 1200, 3)]]));
      creer([produit(1, 'CREME SOLAIRE', 40)]);
      const [tuile] = tuiles();

      expect(prixAvant(tuile)).toBeUndefined();
      expect(tuile.querySelector('.tuile-prix')?.textContent).toMatch(/1\s?500/);
      expect(tuile.querySelector('app-badge-promo')).not.toBeNull();
    });

    it('en rupture, « Rupture » prend la place de la pastille', () => {
      parProduit.set(new Map([[1, promo(1, 1200)]]));
      creer([produit(1, 'CREME SOLAIRE', 0)]);
      const [tuile] = tuiles();

      expect(tuile.querySelector('.tuile-etat')?.textContent).toContain('Rupture');
      expect(tuile.querySelector('app-badge-promo')).toBeNull();
    });

    it('relit les promotions avec la grille : une promotion validée en journée apparaît sans recharger la page', () => {
      creer([produit(1, 'MASQUE', 40)]);
      expect(promotions.charger).toHaveBeenCalledTimes(1);

      composant.recharger();

      expect(promotions.charger).toHaveBeenCalledTimes(2);
    });
  });
});
