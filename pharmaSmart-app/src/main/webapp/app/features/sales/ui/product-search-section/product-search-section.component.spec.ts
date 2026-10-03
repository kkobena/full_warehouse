import {Component} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ProduitSearch} from '../../../../shared/model';
import {ProductSearchComponent} from '../product-search/product-search.component';
import {ProductSearchSectionComponent} from './product-search-section.component';

@Component({selector: 'app-product-search', template: ''})
class ProductSearchStub {}

/** Saisie de la quantité au comptoir : boutons − / +, Entrée, et alerte AVANT l'ajout quand le stock ne suffit pas. */
describe('ProductSearchSectionComponent — saisie de la quantité', () => {
  let fixture: ComponentFixture<ProductSearchSectionComponent>;
  let composant: ProductSearchSectionComponent;

  const el = () => fixture.nativeElement as HTMLElement;
  const champ = () => el().querySelector<HTMLInputElement>('input[id="quantiteSaisie"]')!;
  const bouton = (libelle: string) => el().querySelector<HTMLButtonElement>(`button[aria-label="${libelle}"]`)!;
  const produit = (totalQuantity: number) => ({totalQuantity}) as ProduitSearch;

  function creer(inputs: Record<string, unknown> = {}): void {
    fixture = TestBed.createComponent(ProductSearchSectionComponent);
    composant = fixture.componentInstance;
    Object.entries({selectedProduct: produit(3), ...inputs}).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
  }

  // ngModel écrit la valeur au tour suivant : deux passages de détection pour qu'elle atteigne le champ.
  async function stabiliser(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function saisir(valeur: string): Promise<void> {
    champ().value = valeur;
    champ().dispatchEvent(new Event('input'));
    champ().dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({imports: [ProductSearchSectionComponent]}).overrideComponent(ProductSearchSectionComponent, {
      remove: {imports: [ProductSearchComponent]},
      add: {imports: [ProductSearchStub]},
    });
  });

  it('avertit avant l\'ajout quand la quantité saisie dépasse le stock rayon', async () => {
    creer();

    await saisir('5');

    expect(el().textContent).toContain('Stock rayon : 3');
    expect(champ().classList).toContain('border-warning');
  });

  it("ne dit rien quand la quantité tient dans le stock", async () => {
    creer();

    await saisir('3');

    expect(el().textContent).not.toContain('Stock rayon');
  });

  it('émet la quantité saisie à l\'appui sur Entrée et au clic sur le bouton d\'ajout', async () => {
    creer();
    const ajout = jest.fn();
    composant.addQuantite.subscribe(ajout);

    await saisir('2');
    champ().dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', bubbles: true}));
    bouton('Ajouter au panier').click();

    expect(ajout).toHaveBeenCalledTimes(2);
    expect(ajout).toHaveBeenCalledWith(2);
  });

  it("n'émet rien quand aucun produit n'est sélectionné", async () => {
    creer({selectedProduct: null});
    const ajout = jest.fn();
    composant.addQuantite.subscribe(ajout);

    composant.resetQuantity(2);
    fixture.detectChanges();
    bouton('Ajouter au panier').click();

    expect(ajout).not.toHaveBeenCalled();
  });

  it('resetQuantity remplit ou vide le champ', async () => {
    creer();

    composant.resetQuantity(4);
    await stabiliser();
    expect(champ().value).toBe('4');

    composant.resetQuantity(0);
    await stabiliser();
    expect(champ().value).toBe('');
  });
});
