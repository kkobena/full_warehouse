import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpResponse} from '@angular/common/http';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {of} from 'rxjs';
import {DciAssignationModalComponent} from './dci-assignation-modal.component';
import {DciApiService} from '../../../dci/data-access/services/dci-api.service';
import {DciService} from '../../../../entities/dci/dci.service';
import {NotificationService} from 'app/shared/services/notification.service';
import {ErrorService} from 'app/shared/error.service';
import {IProduit} from 'app/shared/model/produit.model';

/** Affectation en masse d'une molécule — PLAN-PRODUIT-DCI-N-N. */
describe('DciAssignationModalComponent', () => {
  let fixture: ComponentFixture<DciAssignationModalComponent>;
  const api = {rattacherProduits: jest.fn()};
  const activeModal = {close: jest.fn(), dismiss: jest.fn()};
  const paracetamol = {id: 7, libelle: 'PARACETAMOL'};

  const produits: IProduit[] = [
    {id: 1, libelle: 'DOLIPRANE 500', dcis: [{dciId: 7}]},
    {id: 2, libelle: 'SANS MOLECULE'},
  ];

  function ouvrir(): HTMLElement {
    fixture = TestBed.createComponent(DciAssignationModalComponent);
    fixture.componentInstance.produits = produits;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const composant = () => fixture.componentInstance as unknown as {
    dci: { set: (d: unknown) => void };
    mode: { set: (m: string) => void; (): string };
    valider: () => void;
  };

  beforeEach(() => {
    jest.clearAllMocks();
    api.rattacherProduits.mockReturnValue(of(new HttpResponse({body: 2})));
    TestBed.configureTestingModule({
      imports: [DciAssignationModalComponent],
      providers: [
        {provide: DciApiService, useValue: api},
        {provide: DciService, useValue: {queryUnpaged: jest.fn(() => of(new HttpResponse({body: []})))}},
        {provide: NotificationService, useValue: {error: jest.fn()}},
        {provide: ErrorService, useValue: {getErrorMessage: jest.fn()}},
        {provide: NgbActiveModal, useValue: activeModal},
      ],
    });
  });

  it('ajoute la molécule par défaut', () => {
    const el = ouvrir();
    composant().dci.set(paracetamol);

    composant().valider();

    expect(el.textContent).toContain('Ajouter cette molécule');
    expect(api.rattacherProduits).toHaveBeenCalledWith(7, [1, 2], 'AJOUTER');
    expect(activeModal.close).toHaveBeenCalledWith({dci: paracetamol, nombre: 2, mode: 'AJOUTER'});
  });

  it('transmet le remplacement quand il est choisi', () => {
    ouvrir();
    composant().dci.set(paracetamol);
    composant().mode.set('REMPLACER');

    composant().valider();

    expect(api.rattacherProduits).toHaveBeenCalledWith(7, [1, 2], 'REMPLACER');
  });

  it('annonce combien de produits portent déjà une molécule', () => {
    const el = ouvrir();

    expect(el.querySelector('[data-aide-mode]')?.textContent).toContain('1 produit(s)');
  });

  it("explique ce que fait le remplacement", () => {
    const el = ouvrir();
    composant().mode.set('REMPLACER');
    fixture.detectChanges();

    expect(el.querySelector('[data-aide-mode]')?.textContent).toContain('ne porteront plus que cette molécule');
  });
});
