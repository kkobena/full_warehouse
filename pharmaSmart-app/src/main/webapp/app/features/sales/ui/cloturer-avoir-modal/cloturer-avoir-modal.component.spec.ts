import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { ErrorService } from 'app/shared/error.service';
import { AvoirClientApiService, IAvoirClientDocument } from '../../data-access/services/avoir-client-api.service';
import { CloturerAvoirModalComponent } from './cloturer-avoir-modal.component';

/** Clôture d'un avoir : une remise de produit se compte en unités, jamais en montant. */
describe('CloturerAvoirModalComponent — remise du produit', () => {
  let fixture: ComponentFixture<CloturerAvoirModalComponent>;
  let composant: any;
  const api = { cloturerAvoir: jest.fn() };
  const modal = { close: jest.fn(), dismiss: jest.fn() };

  /** 4 unités à 500 F, dont 1 déjà remise. */
  const document_: IAvoirClientDocument = { id: 7, quantite: 4, quantiteRemise: 1, quantiteRestante: 3, montant: 2000, montantUtilise: 500, montantRestant: 1500 };
  const el = () => fixture.nativeElement as HTMLElement;

  function ouvrir(): void {
    api.cloturerAvoir.mockReturnValue(of(new HttpResponse({ body: document_ })));
    fixture = TestBed.createComponent(CloturerAvoirModalComponent);
    fixture.componentInstance.document = document_;
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [CloturerAvoirModalComponent],
      providers: [
        { provide: AvoirClientApiService, useValue: api },
        { provide: NgbActiveModal, useValue: modal },
        { provide: ErrorService, useValue: { getErrorMessage: (_e: unknown, repli: string) => repli } },
      ],
    });
    ouvrir();
  });

  it("propose le montant partiel pour les modes en argent, pas pour la remise du produit", () => {
    composant.choisirMode('REMBOURSEMENT_ESPECES');
    fixture.detectChanges();
    expect(el().querySelector('#partialUsageCheck')).not.toBeNull();

    composant.choisirMode('RETOUR_PRODUIT');
    fixture.detectChanges();
    expect(el().querySelector('#partialUsageCheck')).toBeNull();
    expect(el().querySelectorAll('app-radio').length).toBe(2);
  });

  it('remet tout par défaut : aucune quantité envoyée, le serveur prend toutes les unités restantes', () => {
    composant.choisirMode('RETOUR_PRODUIT');
    composant.confirm();

    expect(api.cloturerAvoir).toHaveBeenCalledWith(7, { modeCloture: 'RETOUR_PRODUIT', commentaire: undefined, montantUtilise: undefined, quantiteRemise: undefined });
  });

  it('envoie la quantité d\'une remise partielle, jamais un montant', () => {
    composant.choisirMode('RETOUR_PRODUIT');
    composant.remise.set('partie');
    composant.quantiteRemise.set(2);
    composant.confirm();

    expect(api.cloturerAvoir).toHaveBeenCalledWith(7, expect.objectContaining({ quantiteRemise: 2, montantUtilise: undefined }));
  });

  it('refuse une quantité nulle ou supérieure à ce qui reste', () => {
    composant.choisirMode('RETOUR_PRODUIT');
    composant.remise.set('partie');

    for (const k of [null, 0, 4]) {
      composant.quantiteRemise.set(k);
      expect(composant.canConfirm()).toBe(false);
    }
    composant.quantiteRemise.set(3);
    expect(composant.canConfirm()).toBe(true);
  });

  it('impute le prix des unités remises, et tout le solde pour la dernière', () => {
    composant.choisirMode('RETOUR_PRODUIT');
    composant.remise.set('partie');

    composant.quantiteRemise.set(2);
    expect(composant.montantEffectif()).toBe(1000);
    composant.quantiteRemise.set(3);
    expect(composant.montantEffectif()).toBe(1500);
  });

  it("change de mode : l'autre choix repart de zéro", () => {
    composant.choisirMode('RETOUR_PRODUIT');
    composant.remise.set('partie');
    composant.quantiteRemise.set(2);

    composant.choisirMode('BON_AVOIR');

    expect(composant.remise()).toBe('tout');
    expect(composant.quantiteRemise()).toBeNull();
  });
});
