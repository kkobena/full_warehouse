import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ISalesLine} from '../../../../shared/model';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {PanierFocusService} from '../../data-access/services/panier-focus.service';
import {ProduitSignaux, ProduitSignauxApiService} from '../../data-access/services/produit-signaux-api.service';
import {ProductListComponent} from './product-list.component';
import {of} from 'rxjs';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

/**
 * Panier du comptoir : mode correction au clavier, quantités fusionnées, prix négocié en sous-titre.
 * Le flux de saisie rapide (Entrée dans une cellule → retour à la recherche) n'est pas modifié ici et
 * reste porté par les gestionnaires existants.
 */
describe('ProductListComponent', () => {
  let fixture: ComponentFixture<ProductListComponent>;
  let composant: ProductListComponent;
  let panierFocus: PanierFocusService;
  const confirmDialog = {onConfirm: jest.fn()};
  const signauxApi = {chargerSignaux: jest.fn()};
  const modalService = {open: jest.fn()};

  const ligne = (id: number, extra: Partial<ISalesLine> = {}): ISalesLine => ({
    id,
    code: `CIP${id}`,
    produitLibelle: `PRODUIT ${id}`,
    quantityRequested: 2,
    quantitySold: 2,
    regularUnitPrice: 1250,
    salesAmount: 2500,
    ...extra,
  });

  const dans = (jours: number): string => {
    const d = new Date();
    d.setDate(d.getDate() + jours);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  const signaux = (extra: Partial<ProduitSignaux>): ProduitSignaux => ({
    produitId: 1,
    statutLegal: null,
    typeGenerique: null,
    peremptionLot: null,
    peremptionDate: null,
    dateLimitePeremption: dans(90),
    stockRestant: 50,
    seuilMini: 0,
    ...extra,
  });

  const el = () => fixture.nativeElement as HTMLElement;
  const rangee = (id: number) => el().querySelector<HTMLElement>(`tr[data-line-id="${id}"]`)!;

  function creer(lignes: ISalesLine[], inputs: Record<string, unknown> = {}): void {
    fixture = TestBed.createComponent(ProductListComponent);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput('salesLines', lignes);
    Object.entries(inputs).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
  }

  function appuyer(cible: HTMLElement, key: string, options: KeyboardEventInit = {}): KeyboardEvent {
    const evenement = new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true, ...options});
    cible.dispatchEvent(evenement);
    return evenement;
  }

  beforeEach(() => {
    jest.clearAllMocks();
    signauxApi.chargerSignaux.mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [
        {provide: NgbConfirmDialogService, useValue: confirmDialog},
        {provide: ProduitSignauxApiService, useValue: signauxApi},
        {provide: NgbModal, useValue: modalService},
      ],
    });
    panierFocus = TestBed.inject(PanierFocusService);
  });

  describe('correction au clavier', () => {
    it('↓ et ↑ changent de ligne sans ouvrir l\'édition', () => {
      creer([ligne(1), ligne(2), ligne(3)], {selectedLineId: 1});
      const selection = jest.fn();
      composant.lineSelected.subscribe(selection);

      appuyer(rangee(1), 'ArrowDown');
      expect(selection).toHaveBeenLastCalledWith(expect.objectContaining({id: 2}));

      fixture.componentRef.setInput('selectedLineId', 3);
      appuyer(rangee(3), 'ArrowUp');
      expect(selection).toHaveBeenLastCalledWith(expect.objectContaining({id: 2}));
    });

    it('ne sort pas de la liste : ↓ sur la dernière ligne reste sur elle', () => {
      creer([ligne(1), ligne(2)], {selectedLineId: 2});
      const selection = jest.fn();
      composant.lineSelected.subscribe(selection);

      appuyer(rangee(2), 'ArrowDown');

      expect(selection).toHaveBeenLastCalledWith(expect.objectContaining({id: 2}));
    });

    it('+ ajoute une unité à la quantité demandée et garde le focus dans la grille', () => {
      creer([ligne(1, {quantityRequested: 2})], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);

      const evenement = appuyer(rangee(1), '+');

      expect(demande).toHaveBeenCalledWith({line: expect.objectContaining({id: 1}), newQty: 3});
      expect(panierFocus.consommer()).toBe(true);
      expect(evenement.defaultPrevented).toBe(true);
    });

    it('Ctrl+D ajoute aussi une unité (la ligne est unique par produit : le serveur fusionne)', () => {
      creer([ligne(1, {quantityRequested: 1, quantitySold: 1})], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);

      const evenement = appuyer(rangee(1), 'd', {ctrlKey: true});

      expect(demande).toHaveBeenCalledWith({line: expect.objectContaining({id: 1}), newQty: 2});
      expect(evenement.defaultPrevented).toBe(true); // sinon : marque-page du navigateur
    });

    it('- retire une unité, mais jamais jusqu\'à zéro : la suppression a sa propre touche', () => {
      creer([ligne(1, {quantityRequested: 3}), ligne(2, {quantityRequested: 1, quantitySold: 1})], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);

      appuyer(rangee(1), '-');
      expect(demande).toHaveBeenCalledWith({line: expect.objectContaining({id: 1}), newQty: 2});

      demande.mockClear();
      fixture.componentRef.setInput('selectedLineId', 2);
      appuyer(rangee(2), '-');
      expect(demande).not.toHaveBeenCalled();
    });

    it('Suppr demande la confirmation de suppression de la ligne sélectionnée', () => {
      creer([ligne(1), ligne(2)], {selectedLineId: 2});

      appuyer(rangee(2), 'Delete');

      expect(confirmDialog.onConfirm).toHaveBeenCalledTimes(1);
      expect(confirmDialog.onConfirm.mock.calls[0][2]).toContain('PRODUIT 2');
    });

    it('ignore les touches quand la grille n\'est pas modifiable', () => {
      creer([ligne(1)], {selectedLineId: 1, isEditable: false});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);

      appuyer(rangee(1), '+');
      appuyer(rangee(1), 'Delete');

      expect(demande).not.toHaveBeenCalled();
      expect(confirmDialog.onConfirm).not.toHaveBeenCalled();
    });

    it('n\'intercepte rien pendant la saisie dans un champ : « + » et Suppr y gardent leur sens', () => {
      creer([ligne(1)], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);
      const champ = document.createElement('input');
      rangee(1).appendChild(champ);

      appuyer(champ, '+');
      appuyer(champ, 'Delete');

      expect(demande).not.toHaveBeenCalled();
      expect(confirmDialog.onConfirm).not.toHaveBeenCalled();
    });

    it('Ctrl+↓ dans une cellule valide la valeur saisie et garde le focus dans la grille', () => {
      creer([ligne(1, {quantityRequested: 2}), ligne(2)], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);
      const champ = document.createElement('input');
      champ.dataset['col'] = 'qty';
      champ.value = '5';
      rangee(1).appendChild(champ);

      const evenement = appuyer(champ, 'ArrowDown', {ctrlKey: true});

      expect(demande).toHaveBeenCalledWith({line: expect.objectContaining({id: 1}), newQty: 5});
      expect(panierFocus.consommer()).toBe(true);
      expect(evenement.defaultPrevented).toBe(true);
    });

    it('Ctrl+↓ sans changement de valeur ne met rien à jour : on passe simplement à la ligne suivante', () => {
      creer([ligne(1, {quantityRequested: 2}), ligne(2)], {selectedLineId: 1});
      const demande = jest.fn();
      const selection = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);
      composant.lineSelected.subscribe(selection);
      const champ = document.createElement('input');
      champ.dataset['col'] = 'qty';
      champ.value = '2';
      rangee(1).appendChild(champ);

      appuyer(champ, 'ArrowDown', {ctrlKey: true});

      expect(demande).not.toHaveBeenCalled();
      expect(selection).toHaveBeenCalledWith(expect.objectContaining({id: 2}));
      expect(panierFocus.consommer()).toBe(false);
    });

    it('Ctrl+↓ avec une valeur refusée ne bouge pas et ne retient pas le focus', () => {
      creer([ligne(1, {quantityRequested: 2}), ligne(2)], {selectedLineId: 1});
      const demande = jest.fn();
      composant.quantityRequestedChanged.subscribe(demande);
      const champ = document.createElement('input');
      champ.dataset['col'] = 'qty';
      champ.value = '0';
      rangee(1).appendChild(champ);

      appuyer(champ, 'ArrowDown', {ctrlKey: true});

      expect(demande).not.toHaveBeenCalled();
      expect(panierFocus.consommer()).toBe(false);
    });
  });

  describe('pagination', () => {
    const lignes = (n: number) => Array.from({length: n}, (_, i) => ligne(i + 1));
    const pagination = () => el().querySelector('.app-table__paginator');

    it("n'apparaît pas tant que le panier ne dépasse pas le seuil (5 par défaut)", () => {
      creer(lignes(5));

      expect(pagination()).toBeNull();
    });

    it('apparaît dès que le panier dépasse le seuil', () => {
      creer(lignes(6));

      expect(pagination()).not.toBeNull();
    });

    it('suit un seuil différent, déclaré en entrée', () => {
      creer(lignes(8), {seuilPagination: 10});
      expect(pagination()).toBeNull();

      fixture.componentRef.setInput('seuilPagination', 7);
      fixture.detectChanges();
      expect(pagination()).not.toBeNull();
    });
  });

  describe('quantités', () => {
    it('affiche une seule valeur quand la quantité demandée est servie en entier', () => {
      creer([ligne(1, {quantityRequested: 3, quantitySold: 3})]);

      expect(rangee(1).querySelector('.qty-double')).toBeNull();
      expect(rangee(1).querySelector('app-editable-cell[data-col="qty"]')).not.toBeNull();
    });

    it('affiche « servie / demandée » quand le stock ne couvre pas la demande', () => {
      creer([ligne(1, {quantityRequested: 5, quantitySold: 2})]);

      const double = rangee(1).querySelector('.qty-double');
      expect(double).not.toBeNull();
      expect(double!.textContent).toContain('2');
      expect(double!.textContent).toContain('5');
    });
  });

  describe('prix négocié par tiers payant', () => {
    it('ne montre rien de plus en vente comptant : pas de régression visuelle', () => {
      creer([ligne(1, {calculationBasePrice: 980})], {saleType: 'COMPTANT'});

      expect(rangee(1).querySelector('.prix-sous-titre')).toBeNull();
    });

    it('montre le prix négocié en sous-titre, quand il diffère du prix public (assurance)', () => {
      creer([ligne(1, {calculationBasePrice: 980})], {saleType: 'ASSURANCE'});

      const sousTitre = rangee(1).querySelector('.prix-sous-titre');
      expect(sousTitre?.textContent).toContain('nég.');
      expect(sousTitre?.textContent).toContain('980');
      expect(sousTitre?.classList).toContain('prix-negocie-bas');
    });

    it('le distingue quand il est plus HAUT que le prix public', () => {
      creer([ligne(1, {calculationBasePrice: 1400})], {saleType: 'CARNET'});

      expect(rangee(1).querySelector('.prix-sous-titre')?.classList).toContain('prix-negocie-haut');
    });

    it('n\'affiche pas de sous-titre quand le prix de base est le prix public', () => {
      creer([ligne(1, {calculationBasePrice: 1250})], {saleType: 'ASSURANCE'});

      expect(rangee(1).querySelector('.prix-sous-titre')).toBeNull();
    });

    it('signale des taux négociés quand le prix ne change pas mais que le taux oui', () => {
      creer([ligne(1, {rates: [{compteTiersPayantId: 7, rate: 0.7}]})], {saleType: 'ASSURANCE'});

      expect(rangee(1).querySelector('.prix-taux')?.textContent).toContain('taux nég.');
    });

    it('donne, par tiers payant, le taux réellement appliqué à la ligne (négocié ou contractuel)', () => {
      creer([ligne(1, {rates: [{compteTiersPayantId: 7, rate: 0.7}]})], {
        saleType: 'ASSURANCE',
        tiersPayants: [
          {id: 7, tiersPayantName: 'CNPS', taux: 80, priorite: 1},
          {id: 8, tiersPayantName: 'MUTUELLE X', taux: 100, priorite: 2},
        ],
      });

      const details = composant['detailTiersPayants'](composant.salesLines()[0]);

      expect(details).toEqual([
        {nom: 'CNPS', taux: 70, negocie: true},
        {nom: 'MUTUELLE X', taux: 100, negocie: false},
      ]);
    });
  });
  describe('non remboursé et repères produit', () => {
    const cases = () => el().querySelectorAll('app-checkbox');

    it("n'affiche pas la colonne « non remboursé » sur une vente comptant", () => {
      creer([ligne(1)], {saleType: 'COMPTANT'});

      expect(cases().length).toBe(0);
    });

    it("n'affiche pas la colonne « non remboursé » sur une vente carnet", () => {
      creer([ligne(1)], {saleType: 'CARNET'});

      expect(cases().length).toBe(0);
    });

    it('affiche une case par ligne sur une vente assurance et signale le changement', () => {
      creer([ligne(1), ligne(2, {nonRembourse: true})], {saleType: 'ASSURANCE'});
      const changement = jest.fn();
      composant.nonRembourseChanged.subscribe(changement);

      expect(cases().length).toBe(2);
      expect(rangee(2).classList).toContain('ligne-non-remboursee');

      cases()[0].querySelector<HTMLInputElement>('input')!.click();

      expect(changement).toHaveBeenCalledWith({line: expect.objectContaining({id: 1}), nonRembourse: true});
    });

    it("recharge les repères des produits à chaque évolution du panier, et les affiche", () => {
      signauxApi.chargerSignaux.mockReturnValue(
        of([signaux({produitId: 7, statutLegal: 'STUPEFIANTS', typeGenerique: 'GENERIQUE', peremptionLot: 'L42', peremptionDate: dans(30)})]),
      );

      creer([ligne(1, {produitId: 7})]);
      fixture.componentRef.setInput('salesLines', [ligne(1, {produitId: 7}), ligne(2, {produitId: 7})]);
      fixture.detectChanges();

      expect(signauxApi.chargerSignaux).toHaveBeenCalledTimes(2);
      expect(signauxApi.chargerSignaux).toHaveBeenLastCalledWith([7]);
      expect(el().textContent).toContain('Stup');
      expect(el().textContent).toContain('Lot L42');
    });

    const actionPrixNegocie = () =>
      Array.from(el().querySelectorAll('button')).find(b => b.textContent?.includes('Ajouter un prix négocié'));

    it("propose « Ajouter un prix négocié » sur une vente assurance uniquement", () => {
      creer([ligne(1)], {saleType: 'COMPTANT'});
      expect(actionPrixNegocie()).toBeUndefined();

      fixture.destroy();
      creer([ligne(1)], {saleType: 'CARNET'});
      expect(actionPrixNegocie()).toBeUndefined();

      fixture.destroy();
      creer([ligne(1)], {saleType: 'ASSURANCE'});
      expect(actionPrixNegocie()).toBeDefined();
    });

    it('ouvre le formulaire de tarif avec les tiers payants de la vente, puis demande le recalcul', async () => {
      const instance: Record<string, unknown> = {};
      modalService.open.mockReturnValue({componentInstance: instance, result: Promise.resolve('saved')});
      const tiersPayants = [{id: 5, tiersPayantId: 50, tiersPayantFullName: 'CNPS'}];
      creer([ligne(1, {produitId: 9})], {saleType: 'ASSURANCE', tiersPayants});
      const ajoute = jest.fn();
      composant.prixNegocieAjoute.subscribe(ajoute);

      actionPrixNegocie()!.click();
      await Promise.resolve();

      expect(instance['tiersPayantsVente']).toEqual(tiersPayants);
      expect((instance['produit'] as {id: number}).id).toBe(9);
      expect(ajoute).toHaveBeenCalledWith(expect.objectContaining({id: 1}));
    });

    it("affiche les lots prélevés sous le libellé, et le lot en stock seulement quand aucun lot n'est attribué", () => {
      signauxApi.chargerSignaux.mockReturnValue(of([signaux({produitId: 7, peremptionLot: 'STOCK1', peremptionDate: dans(20)})]));

      creer([
        ligne(1, {produitId: 7, lots: [{id: 1, numLot: 'A123', quantity: 2, expiryDate: dans(400)}]}),
        ligne(2, {produitId: 7}),
      ]);

      expect(rangee(1).querySelector('.lots-ligne')!.textContent).toContain('A123');
      expect(rangee(1).textContent).not.toContain('Lot STOCK1');
      expect(rangee(2).querySelector('.lots-ligne')).toBeNull();
      expect(rangee(2).textContent).toContain('Lot STOCK1');
    });

    it('résume plusieurs lots par « +N » et colore un lot proche ou périmé', () => {
      signauxApi.chargerSignaux.mockReturnValue(of([signaux({produitId: 7})]));

      creer([
        ligne(1, {
          produitId: 7,
          lots: [
            {id: 1, numLot: 'PERIME', quantity: 1, expiryDate: dans(-10)},
            {id: 2, numLot: 'PROCHE', quantity: 1, expiryDate: dans(30)},
            {id: 3, numLot: 'LOIN', quantity: 1, expiryDate: dans(400)},
          ],
        }),
      ]);

      const sousTitre = rangee(1).querySelector('.lots-ligne')!;
      expect(sousTitre.querySelector('.lot-perime')!.textContent).toContain('PERIME');
      expect(sousTitre.querySelector('.lot-proche')!.textContent).toContain('PROCHE');
      expect(sousTitre.textContent).toContain('+1');
      expect(sousTitre.textContent).not.toContain('LOIN');
    });

    it('met la quantité servie en rouge quand elle est inférieure à la demandée', () => {
      creer([ligne(1, {quantitySold: 1, quantityRequested: 3}), ligne(2)]);

      expect(rangee(1).querySelector('.qty-insuffisante')!.textContent?.trim()).toBe('1');
      expect(rangee(2).querySelector('.qty-insuffisante')).toBeNull();
    });

    it('signale un stock faible (restant ≤ seuil mini) et une rupture, mais rien sans seuil', () => {
      signauxApi.chargerSignaux.mockReturnValue(
        of([
          signaux({produitId: 1, stockRestant: 3, seuilMini: 5}),
          signaux({produitId: 2, stockRestant: 0, seuilMini: 5}),
          signaux({produitId: 3, stockRestant: 3, seuilMini: 0}),
          signaux({produitId: 4, stockRestant: 9, seuilMini: 5}),
        ]),
      );

      creer([1, 2, 3, 4].map(id => ligne(id, {produitId: id})));

      expect(rangee(1).textContent).toContain('Stock faible');
      expect(rangee(2).textContent).toContain('Rupture');
      expect(rangee(3).textContent).not.toMatch(/Stock faible|Rupture/);
      expect(rangee(4).textContent).not.toMatch(/Stock faible|Rupture/);
    });
  });
});
