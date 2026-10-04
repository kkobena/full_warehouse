import { HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { of, throwError } from 'rxjs';
import { AbilityService } from 'app/core/auth/ability.service';
import { ErrorService } from 'app/shared/error.service';
import { AvoirClientApiService, IAvoirClientDocument } from '../../data-access/services/avoir-client-api.service';
import { RetourClientApiService } from '../../data-access/services/retour-client-api.service';
import { CloturerAvoirModalComponent } from '../cloturer-avoir-modal/cloturer-avoir-modal.component';
import { ApresVenteModalComponent } from './apres-vente-modal.component';

/** Après-vente du comptoir : retour client ou clôture d'avoir, selon les droits, sans quitter la vente. */
describe('ApresVenteModalComponent', () => {
  let fixture: ComponentFixture<ApresVenteModalComponent>;
  const avoirApi = { queryDocuments: jest.fn() };
  const retourApi = { findSaleByRef: jest.fn() };
  const modalService = { open: jest.fn() };
  const droits: Record<string, boolean> = {};

  const avoir: IAvoirClientDocument = { id: 7, reference: 'AV-7', produitLibelle: 'DOLIPRANE', quantite: 2, quantiteRestante: 2, montant: 1000, montantRestant: 1000 };
  const el = () => fixture.nativeElement as HTMLElement;
  const boutons = () => Array.from(el().querySelectorAll<HTMLButtonElement>('button'));
  const bouton = (texte: string) => boutons().find(b => b.textContent?.includes(texte)) ?? Array.from(el().querySelectorAll<HTMLElement>('a')).find(a => a.textContent?.includes(texte)) as HTMLButtonElement | undefined;

  function ouvrir(retour: boolean, cloture: boolean): void {
    droits['ventes.retours-client.create'] = retour;
    droits['ventes.avoirs.cloturer'] = cloture;
    avoirApi.queryDocuments.mockReturnValue(of(new HttpResponse({ body: [avoir] })));
    fixture = TestBed.createComponent(ApresVenteModalComponent);
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    modalService.open.mockReturnValue({ componentInstance: {}, result: Promise.resolve() });
    TestBed.configureTestingModule({
      imports: [ApresVenteModalComponent],
      providers: [
        { provide: AvoirClientApiService, useValue: avoirApi },
        { provide: RetourClientApiService, useValue: retourApi },
        { provide: NgbModal, useValue: modalService },
        { provide: NgbActiveModal, useValue: { dismiss: jest.fn() } },
        { provide: ErrorService, useValue: { getErrorMessage: (e: any, repli: string) => e?.error?.message ?? repli } },
        { provide: AbilityService, useValue: { canSignal: (_a: string, sujet: string) => signal(!!droits[sujet]) } },
      ],
    });
  });

  it("n'affiche que l'onglet permis par les droits", () => {
    ouvrir(false, true);
    expect(bouton('Retour client')).toBeUndefined();
    expect(bouton('Avoir à clôturer')).toBeDefined();
    expect(el().textContent).toContain('DOLIPRANE');
  });

  it("ouvre l'onglet des avoirs en premier quand les deux droits sont accordés", () => {
    ouvrir(true, true);
    const onglets = Array.from(el().querySelectorAll('a.nav-link')).map(a => a.textContent?.trim());
    expect(onglets).toEqual(['Avoir à clôturer', 'Retour client']);
    expect(el().querySelector('input[aria-label="Rechercher un avoir"]')).not.toBeNull();
    expect(el().querySelector('input[aria-label="Numéro de la vente"]')).toBeNull();
  });

  it("donne le focus au champ de recherche affiché, à l'ouverture puis à chaque changement d'onglet", async () => {
    jest.useFakeTimers();
    // jsdom ne calcule aucune mise en page : `offsetParent` y est toujours nul, ce que la directive lit comme « masqué ».
    const original = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetParent')!;
    Object.defineProperty(HTMLElement.prototype, 'offsetParent', { configurable: true, get: function (this: HTMLElement) { return this.parentElement; } });
    ouvrir(true, true);
    document.body.appendChild(el());
    fixture.detectChanges();
    await fixture.whenStable();
    jest.runAllTimers();
    expect(document.activeElement?.getAttribute('aria-label')).toBe('Rechercher un avoir');

    (el().querySelectorAll('a.nav-link')[1] as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    jest.runAllTimers();
    expect(document.activeElement?.getAttribute('aria-label')).toBe('Numéro de la vente');
    Object.defineProperty(HTMLElement.prototype, 'offsetParent', original);
    jest.useRealTimers();
    el().remove();
  });

  it("annonce l'absence de droit plutôt qu'une fenêtre vide", () => {
    ouvrir(false, false);
    expect(el().textContent).toContain("Vous n'avez le droit ni de retourner");
    expect(bouton('Rechercher')).toBeUndefined();
  });

  it("met bien en évidence l'absence d'avoir", () => {
    droits['ventes.retours-client.create'] = false;
    droits['ventes.avoirs.cloturer'] = true;
    avoirApi.queryDocuments.mockReturnValue(of(new HttpResponse({ body: [] })));
    fixture = TestBed.createComponent(ApresVenteModalComponent);
    fixture.detectChanges();

    const vide = el().querySelector('.etat-vide');
    expect(vide?.getAttribute('role')).toBe('status');
    expect(vide?.textContent).toContain('Aucun avoir ouvert pour cette recherche.');
  });

  it("met en évidence l'absence de vente pour un numéro inconnu, comme pour les avoirs", () => {
    retourApi.findSaleByRef.mockReturnValue(throwError(() => ({ status: 400, error: { message: 'Vente introuvable : 123' } })));
    ouvrir(true, false);
    const composant = fixture.componentInstance as any;
    composant.reference.set('123');
    composant.chercherVente();
    fixture.detectChanges();

    const vide = el().querySelector('.etat-vide');
    expect(vide?.getAttribute('role')).toBe('status');
    expect(vide?.textContent).toContain('Aucune vente trouvée pour « 123 »');
  });

  it('ouvre la clôture avec le mode déjà choisi pour les deux gestes rapides', () => {
    const doc = { componentInstance: {} as Record<string, unknown>, result: Promise.resolve() };
    modalService.open.mockReturnValue(doc);
    ouvrir(false, true);

    bouton('Remettre le produit')!.click();
    expect(modalService.open).toHaveBeenCalledWith(CloturerAvoirModalComponent, expect.anything());
    expect(doc.componentInstance['document']).toBe(avoir);
    expect(doc.componentInstance['modeInitial']).toBe('RETOUR_PRODUIT');

    bouton('Rembourser en espèces')!.click();
    expect(doc.componentInstance['modeInitial']).toBe('REMBOURSEMENT_ESPECES');
  });

  it('ouvre « Autres modes » sans mode imposé', () => {
    const doc = { componentInstance: {} as Record<string, unknown>, result: Promise.resolve() };
    modalService.open.mockReturnValue(doc);
    ouvrir(false, true);

    bouton('Autres modes')!.click();

    expect(doc.componentInstance['modeInitial']).toBeUndefined();
  });
});
