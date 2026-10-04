import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ForceStockChoiceModalComponent} from './force-stock-choice-modal.component';

/** le caissier dit pourquoi il force. */
describe('ForceStockChoiceModalComponent', () => {
  let fixture: ComponentFixture<ForceStockChoiceModalComponent>;
  const activeModal = {close: jest.fn(), dismiss: jest.fn()};

  function ouvrir(options: Partial<ForceStockChoiceModalComponent>): HTMLElement {
    fixture = TestBed.createComponent(ForceStockChoiceModalComponent);
    Object.assign(fixture.componentInstance, options);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const choix = (el: HTMLElement, motif: string) => el.querySelector<HTMLElement>(`[data-motif="${motif}"]`);
  const bouton = (el: HTMLElement, selecteur: string) => el.querySelector<HTMLButtonElement>(`${selecteur} button`)!;

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [ForceStockChoiceModalComponent],
      providers: [{provide: NgbActiveModal, useValue: activeModal}],
    });
  });

  it('propose les deux motifs quand le caissier a les deux privilèges', () => {
    const el = ouvrir({canRupture: true, canEcart: true});

    expect(choix(el, 'RUPTURE_AVOIR')?.textContent).toContain('Le client sera livré plus tard');
    expect(choix(el, 'ECART_INVENTAIRE')?.textContent).toContain('Le produit est en rayon, la machine se trompe');
  });

  it("n'offre l'écart d'inventaire qu'avec son privilège", () => {
    const el = ouvrir({canRupture: true, canEcart: false});

    expect(choix(el, 'RUPTURE_AVOIR')).not.toBeNull();
    expect(choix(el, 'ECART_INVENTAIRE')).toBeNull();
  });

  it("n'offre la rupture qu'avec le forçage", () => {
    const el = ouvrir({canRupture: false, canEcart: true});

    expect(choix(el, 'RUPTURE_AVOIR')).toBeNull();
    expect(choix(el, 'ECART_INVENTAIRE')).not.toBeNull();
  });

  it('nomme le produit et la quantité saisie', () => {
    const el = ouvrir({canRupture: true, produitLibelle: 'DOLIPRANE 500MG', quantiteDemandee: 4});

    expect(el.textContent).toContain('DOLIPRANE 500MG');
    expect(el.textContent).toContain('4');
  });

  it.each(['RUPTURE_AVOIR', 'ECART_INVENTAIRE'])('ferme avec le motif %s choisi', motif => {
    const el = ouvrir({canRupture: true, canEcart: true});

    bouton(el, `[data-motif="${motif}"]`).click();

    expect(activeModal.close).toHaveBeenCalledWith(motif);
    expect(activeModal.dismiss).not.toHaveBeenCalled();
  });

  it("n'offre l'équivalent que si l'écran sait le recevoir", () => {
    expect(choix(ouvrir({canRupture: true, canEcart: true}), 'SUBSTITUER')).toBeNull();

    const el = ouvrir({canRupture: true, canEcart: true, canSubstituer: true});

    expect(choix(el, 'SUBSTITUER')?.textContent).toContain('Proposer un équivalent disponible');
    expect(choix(el, 'SUBSTITUER')?.textContent).toContain('Rien n\'est forcé');
  });

  it("propose l'équivalent même à qui n'a aucun privilège de forçage", () => {
    const el = ouvrir({canRupture: false, canEcart: false, canSubstituer: true});

    expect(choix(el, 'RUPTURE_AVOIR')).toBeNull();
    expect(choix(el, 'ECART_INVENTAIRE')).toBeNull();
    expect(choix(el, 'SUBSTITUER')).not.toBeNull();
  });

  it("ferme avec SUBSTITUER quand on choisit l'équivalent : aucun motif de forçage", () => {
    const el = ouvrir({canRupture: true, canEcart: true, canSubstituer: true});

    bouton(el, '[data-motif="SUBSTITUER"]').click();

    expect(activeModal.close).toHaveBeenCalledWith('SUBSTITUER');
    expect(activeModal.dismiss).not.toHaveBeenCalled();
  });

  it('rejette la modale sur « Annuler », sans motif', () => {
    const el = ouvrir({canRupture: true, canEcart: true});

    bouton(el, '[data-action="annuler"]').click();

    expect(activeModal.dismiss).toHaveBeenCalled();
    expect(activeModal.close).not.toHaveBeenCalled();
  });

  it('met la croix de fermeture en premier focalisable : un Entrée résiduel annule au lieu de forcer', () => {
    const el = ouvrir({canRupture: true, canEcart: true});

    expect(el.querySelector('button')?.classList).toContain('btn-close');
    expect(el.querySelector('[ngbautofocus]')).toBeNull();
  });

  it('la croix de fermeture annule, sans motif', () => {
    const el = ouvrir({canRupture: true, canEcart: true});

    el.querySelector<HTMLButtonElement>('.btn-close')!.click();

    expect(activeModal.dismiss).toHaveBeenCalled();
    expect(activeModal.close).not.toHaveBeenCalled();
  });
});
