import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, throwError } from 'rxjs';
import { ProduitSearch } from '../../../../shared/model';
import { SubstitutPropose, SubstitutsApiService } from '../../data-access/services/substituts-api.service';
import { OrigineSubstitution, SubstitutsModalComponent } from './substituts-modal.component';

/** Équivalents d'un produit au comptoir : prix comparé, stock, nature du lien, et le choix qui ferme la fenêtre. */
describe('SubstitutsModalComponent', () => {
  let fixture: ComponentFixture<SubstitutsModalComponent>;
  const api = { lireDisponibles: jest.fn() };
  const activeModal = { close: jest.fn(), dismiss: jest.fn() };

  const produit = (id: number, libelle: string, regularUnitPrice: number, totalQuantity: number, extra: Partial<ProduitSearch> = {}) =>
    ({ id, libelle, regularUnitPrice, totalQuantity, ...extra }) as ProduitSearch;
  const proposition = (p: ProduitSearch, typeSubstitut: SubstitutPropose['typeSubstitut'] = 'GENERIQUE'): SubstitutPropose => ({
    produit: p,
    typeSubstitut,
    typeSubstitutLibelle: typeSubstitut === 'GENERIQUE' ? 'Générique' : 'Thérapeutique',
  });
  const el = () => fixture.nativeElement as HTMLElement;
  const lignes = () => Array.from(el().querySelectorAll<HTMLElement>('li.proposition'));
  const texte = (ligne: HTMLElement) => (ligne.textContent ?? '').replace(/\s+/g, ' ');

  function ouvrir(origine: OrigineSubstitution, propositions: SubstitutPropose[]): void {
    api.lireDisponibles.mockReturnValue(of(propositions));
    fixture = TestBed.createComponent(SubstitutsModalComponent);
    fixture.componentInstance.origine = origine;
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [SubstitutsModalComponent],
      providers: [
        { provide: SubstitutsApiService, useValue: api },
        { provide: NgbActiveModal, useValue: activeModal },
      ],
    });
  });

  it("demande les équivalents du produit d'origine", () => {
    ouvrir({ id: 42, libelle: 'SPASFON', prixUnitaire: 1000, quantite: 2 }, []);

    expect(api.lireDisponibles).toHaveBeenCalledWith(42);
    expect(texte(el())).toContain('SPASFON');
  });

  it("dit clairement qu'il n'y a rien en stock", () => {
    ouvrir({ id: 1, libelle: 'SPASFON' }, []);

    expect(lignes()).toHaveLength(0);
    expect(texte(el())).toContain("Aucun équivalent n'est en stock");
  });

  it('signale une erreur de chargement sans bloquer la fenêtre', () => {
    api.lireDisponibles.mockReturnValue(throwError(() => new Error('réseau')));
    fixture = TestBed.createComponent(SubstitutsModalComponent);
    fixture.componentInstance.origine = { id: 1, libelle: 'SPASFON' };
    fixture.detectChanges();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain("n'ont pas pu être chargés");
  });

  it("compare le prix à celui de l'original, avec un signe et pas seulement une couleur", () => {
    ouvrir({ id: 1, libelle: 'SPASFON', prixUnitaire: 1000 }, [
      proposition(produit(2, 'SPASFON LYOC', 800, 5)),
      proposition(produit(3, 'PHLOROGLUCINOL', 1200, 5)),
      proposition(produit(4, 'MEME PRIX', 1000, 5)),
    ]);
    const [moinsCher, plusCher, memePrix] = lignes();

    expect(texte(moinsCher)).toMatch(/−\s?200/);
    expect(moinsCher.querySelector('.moins-cher')).not.toBeNull();
    expect(texte(plusCher)).toMatch(/\+\s?200/);
    expect(plusCher.querySelector('.plus-cher')).not.toBeNull();
    expect(texte(memePrix)).toContain('même prix');
  });

  it("n'affiche aucun écart quand le prix de l'original est inconnu", () => {
    ouvrir({ id: 1, libelle: 'SPASFON' }, [proposition(produit(2, 'SPASFON LYOC', 800, 5))]);

    expect(lignes()[0].querySelector('.ecart')).toBeNull();
  });

  it("distingue le substitut thérapeutique, qui relève de l'avis du pharmacien", () => {
    ouvrir({ id: 1, libelle: 'SPASFON' }, [
      proposition(produit(2, 'GENERIQUE', 800, 5), 'GENERIQUE'),
      proposition(produit(3, 'THERAPEUTIQUE', 900, 5), 'THERAPEUTIQUE'),
    ]);
    const [generique, therapeutique] = lignes();

    expect(texte(generique)).not.toContain('pharmacien');
    expect(texte(therapeutique)).toContain('À valider par le pharmacien');
  });

  it('signale un stock qui ne couvre pas la quantité demandée', () => {
    ouvrir({ id: 1, libelle: 'SPASFON', quantite: 4 }, [
      proposition(produit(2, 'SUFFISANT', 800, 10)),
      proposition(produit(3, 'INSUFFISANT', 800, 2)),
    ]);
    const [suffisant, insuffisant] = lignes();

    expect(suffisant.querySelector('.stock-juste')).toBeNull();
    expect(insuffisant.querySelector('.stock-juste')).not.toBeNull();
    expect(texte(insuffisant)).toContain('ne couvre pas la quantité demandée');
  });

  it('ferme avec le produit choisi, au format de la recherche', () => {
    const retenu = produit(2, 'SPASFON LYOC', 800, 5);
    ouvrir({ id: 1, libelle: 'SPASFON' }, [proposition(retenu)]);

    lignes()[0].querySelector<HTMLButtonElement>('button[aria-label="Choisir SPASFON LYOC"]')?.click();

    expect(activeModal.close).toHaveBeenCalledWith(retenu);
  });

  it('se ferme sans choix sur « Fermer »', () => {
    ouvrir({ id: 1, libelle: 'SPASFON' }, []);

    el().querySelector<HTMLButtonElement>('[data-action="fermer"] button, button[data-action="fermer"]')?.click();
    el().querySelector<HTMLButtonElement>('button.btn-close')?.click();

    expect(activeModal.dismiss).toHaveBeenCalledWith('fermer');
    expect(activeModal.close).not.toHaveBeenCalled();
  });
});
