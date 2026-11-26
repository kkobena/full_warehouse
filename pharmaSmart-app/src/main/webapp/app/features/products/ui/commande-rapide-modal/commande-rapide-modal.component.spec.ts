import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { of, Subject } from "rxjs";

import { CommandeService } from "app/entities/commande/commande.service";
import { CommandCommonService } from "app/entities/commande/command-common.service";
import { SuggestionService } from "app/entities/commande/suggestion/suggestion.service";
import { IFournisseurProduit } from "app/shared/model/fournisseur-produit.model";
import { NotificationService } from "app/shared/services/notification.service";
import { CommandeRapideModalComponent } from "./commande-rapide-modal.component";

describe("CommandeRapideModalComponent", () => {
  let fixture: ComponentFixture<CommandeRapideModalComponent>;
  let fournisseurs$: Subject<IFournisseurProduit[]>;

  const commandeService = { createCommandeRapide: jest.fn(() => of({ body: { commandeId: 9 } })) };
  const commandCommon = { pendingOpenCommandeId: { set: jest.fn() }, navigateToCommandesAPasser: jest.fn() };
  const activeModal = { close: jest.fn(), dismiss: jest.fn() };

  /** Accès aux membres protégés : le test juge le comportement, pas la visibilité. */
  const interne = (): any => fixture.componentInstance as any;
  const champQuantite = (): HTMLInputElement | null => fixture.nativeElement.querySelector("app-input-number input");

  beforeEach(async () => {
    jest.clearAllMocks();
    fournisseurs$ = new Subject();
    await TestBed.configureTestingModule({
      imports: [CommandeRapideModalComponent],
      providers: [
        { provide: SuggestionService, useValue: { getFournisseursProduit: jest.fn(() => fournisseurs$) } },
        { provide: CommandeService, useValue: commandeService },
        { provide: CommandCommonService, useValue: commandCommon },
        { provide: NotificationService, useValue: { error: jest.fn() } },
        { provide: NgbActiveModal, useValue: activeModal },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(CommandeRapideModalComponent);
    fixture.componentInstance.produit = { id: 500, libelle: "DOLIPRANE 1000MG", totalQuantity: 3 } as any;
    fixture.detectChanges();
  });

  const charger = (list: IFournisseurProduit[]): void => {
    fournisseurs$.next(list);
    fixture.detectChanges();
  };

  it("pose le curseur sur la quantité une fois les fournisseurs chargés, valeur sélectionnée", async () => {
    // Pendant le chargement, le champ n'existe pas encore.
    expect(champQuantite()).toBeNull();

    charger([{ id: 1, principal: true, fournisseurLibelle: "LABOREX", qteMinimaleCommande: 6, prixAchat: 100 }]);

    // Le champ apparu rend l'effet de focus « sale » : c'est le planificateur qui le relance.
    await fixture.whenStable();

    const champ = champQuantite()!;
    expect(document.activeElement).toBe(champ);
    expect(champ.value).toBe("6");
    expect(champ.selectionStart).toBe(0);
    expect(champ.selectionEnd).toBe(champ.value.length);
  });

  it("commande chez le fournisseur principal la quantité minimale proposée", () => {
    charger([
      { id: 1, principal: false, fournisseurLibelle: "COPHARMED", qteMinimaleCommande: 2, prixAchat: 90 },
      { id: 2, principal: true, fournisseurLibelle: "LABOREX", qteMinimaleCommande: 6, prixAchat: 100 },
    ]);

    expect(interne().totalEstime).toBe(600);
    interne().onSubmit();

    expect(commandeService.createCommandeRapide).toHaveBeenCalledWith(3, 2, 6);
    expect(activeModal.close).toHaveBeenCalledWith(true);
  });

  it("refuse une quantité vidée", () => {
    charger([{ id: 1, principal: true, fournisseurLibelle: "LABOREX", qteMinimaleCommande: 6, prixAchat: 100 }]);

    interne().form.controls.quantite.setValue(null);
    interne().onSubmit();

    expect(commandeService.createCommandeRapide).not.toHaveBeenCalled();
  });
});
