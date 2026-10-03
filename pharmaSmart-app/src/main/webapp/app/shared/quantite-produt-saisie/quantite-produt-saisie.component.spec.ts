import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideTranslateService} from '@ngx-translate/core';
import {QuantiteProdutSaisieComponent} from './quantite-produt-saisie.component';

/** Saisie de la quantité au comptoir : boutons − / +, et alerte AVANT l'ajout quand le stock ne suffit pas. */
describe('QuantiteProdutSaisieComponent', () => {
  let fixture: ComponentFixture<QuantiteProdutSaisieComponent>;

  const el = () => fixture.nativeElement as HTMLElement;
  const bouton = (libelle: string) => el().querySelector<HTMLButtonElement>(`button[aria-label="${libelle}"]`);
  const alerte = () => el().querySelector('[data-testid], .stock-alerte');

  function creer(inputs: Record<string, unknown> = {}): void {
    fixture = TestBed.createComponent(QuantiteProdutSaisieComponent);
    Object.entries(inputs).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QuantiteProdutSaisieComponent],
      providers: [provideTranslateService()],
    });
  });

  it("n'affiche pas les boutons − et + par défaut : le composant sert aussi hors vente", () => {
    creer();

    expect(bouton('Augmenter la quantité')).toBeNull();
    expect(bouton('Diminuer la quantité')).toBeNull();
  });

  it('ajoute et retire une unité avec les boutons, sans descendre sous 1', () => {
    creer({showSteppers: true});
    const composant = fixture.componentInstance;

    bouton('Augmenter la quantité')!.click();
    bouton('Augmenter la quantité')!.click();
    expect(composant.quantite()).toBe(2);

    bouton('Diminuer la quantité')!.click();
    bouton('Diminuer la quantité')!.click();
    expect(composant.quantite()).toBe(1);
  });

  it("désactive les boutons tant qu'aucun produit n'est sélectionné", () => {
    creer({showSteppers: true, isValid: false});

    expect(bouton('Augmenter la quantité')!.disabled).toBe(true);
  });

  it("avertit avant l'ajout quand la quantité saisie dépasse le stock", () => {
    creer({stockDisponible: 3});

    fixture.componentInstance.quantite.set(5);
    fixture.detectChanges();

    expect(el().textContent).toContain('Stock rayon : 3');
    expect(el().querySelector('input')!.classList).toContain('border-warning');
  });

  it("ne dit rien quand la quantité tient dans le stock, ou quand le stock n'est pas connu", () => {
    creer({stockDisponible: 3});
    fixture.componentInstance.quantite.set(3);
    fixture.detectChanges();
    expect(el().textContent).not.toContain('Stock rayon');

    fixture.componentRef.setInput('stockDisponible', null);
    fixture.componentInstance.quantite.set(50);
    fixture.detectChanges();
    expect(el().textContent).not.toContain('Stock rayon');
    expect(alerte()).toBeNull();
  });
});
