import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { of, throwError } from "rxjs";
import { ReceptionSequentialComponent } from "./reception-sequential.component";
import { IOrderLine } from "../../../../../shared/model/order-line.model";
import { IReceptionScanResult } from "../../../../../shared/model/reception-scan-result.model";
import { ProductsApiService } from "../../../../products/data-access/services/products-api.service";
import { CommandeService } from "../../../../../entities/commande/commande.service";
import { NotificationService } from "../../../../../shared/services/notification.service";
import { ErrorService } from "../../../../../shared/error.service";

/**
 * En saisie séquentielle l'opérateur est déjà positionné sur une ligne : un code inconnu se
 * confirme en une touche. Deux écritures différentes s'y cachent, et les confondre serait grave —
 * une ligne au code provisoire voit son code remplacé, une ligne déjà codée ne doit garder son
 * CIP intact et ne recevoir qu'un code alternatif.
 */
describe("ReceptionSequentialComponent — code inconnu", () => {
  let fixture: ComponentFixture<ReceptionSequentialComponent>;
  let composant: ReceptionSequentialComponent;
  let productsApi: { createCodeAlias: jest.Mock };
  let commandeService: { updateCip: jest.Mock };

  const ligneCodee: IOrderLine = {
    id: 1,
    produitId: 11,
    produitLibelle: "DOLIPRANE 500",
    produitCip: "CIP-1",
    quantityRequested: 10,
    quantityReceived: 0,
    provisionalCode: false,
  };

  const ligneProvisoire: IOrderLine = { ...ligneCodee, id: 2, produitId: 12, provisionalCode: true };

  const scanInconnu = (code = "07353163006031"): IReceptionScanResult =>
    ({
      found: false,
      scannedCode: code,
      dataMatrixHint: {
        scannedCode: "7353163006031",
        numLot: "ABC101",
        expiryDate: "2027-01-31",
        serialNumber: null,
        qty: 1,
        proposition: false,
      },
    }) as IReceptionScanResult;

  const monter = async (lignes: IOrderLine[]): Promise<void> => {
    productsApi = { createCodeAlias: jest.fn().mockReturnValue(of({ id: 99 })) };
    commandeService = { updateCip: jest.fn().mockReturnValue(of({})) };

    await TestBed.configureTestingModule({
      imports: [ReceptionSequentialComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductsApiService, useValue: productsApi },
        { provide: CommandeService, useValue: commandeService },
        { provide: NotificationService, useValue: { success: jest.fn(), error: jest.fn(), info: jest.fn() } },
        // ErrorService tire TranslateService : on le stubbe plutôt que de monter l'i18n.
        { provide: ErrorService, useValue: { getErrorMessage: (e: unknown) => String(e) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReceptionSequentialComponent);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput("orderLines", lignes);
    fixture.componentRef.setInput("lastScanResult", null);
    fixture.detectChanges();
  };

  /** Accès aux membres protégés : le test juge le comportement, pas la visibilité. */
  const interne = (): any => composant as any;

  afterEach(() => TestBed.resetTestingModule());

  it("mémorise un code alternatif sur une ligne déjà codée, sans toucher son CIP", async () => {
    await monter([ligneCodee]);
    const rejoue = jest.fn();
    composant.rejouerScan.subscribe(rejoue);

    fixture.componentRef.setInput("lastScanResult", scanInconnu());
    fixture.detectChanges();

    expect(interne().scanInconnu()).not.toBeNull();
    expect(interne().associationEcraseLeCip()).toBe(false);

    interne().onAssocierCodeALaLigneCourante();

    expect(productsApi.createCodeAlias).toHaveBeenCalledWith(11, "7353163006031");
    expect(rejoue).toHaveBeenCalledWith("07353163006031");
    expect(interne().scanInconnu()).toBeNull();
  });

  it("sur une ligne au code provisoire, remplace le CIP et ne crée aucun alias", async () => {
    await monter([ligneProvisoire]);
    const rejoue = jest.fn();
    composant.rejouerScan.subscribe(rejoue);

    fixture.componentRef.setInput("lastScanResult", scanInconnu("CODE-GROSSISTE"));
    fixture.detectChanges();

    // Même bandeau, même touche : seule l'écriture change.
    expect(interne().scanInconnu()).not.toBeNull();
    expect(interne().associationEcraseLeCip()).toBe(true);

    interne().onAssocierCodeALaLigneCourante();

    expect(commandeService.updateCip).toHaveBeenCalled();
    expect(productsApi.createCodeAlias).not.toHaveBeenCalled();
    expect(rejoue).toHaveBeenCalledWith("CODE-GROSSISTE");
    expect(interne().scanInconnu()).toBeNull();
  });

  it("ne réagit pas à un scan reconnu", async () => {
    await monter([ligneCodee]);

    fixture.componentRef.setInput("lastScanResult", { found: true, scannedCode: "x" } as IReceptionScanResult);
    fixture.detectChanges();

    expect(interne().scanInconnu()).toBeNull();
  });

  it("délègue au parent quand la boîte n'est pas la ligne courante", async () => {
    await monter([ligneCodee]);
    const demande = jest.fn();
    composant.autreLigneDemandee.subscribe(demande);

    fixture.componentRef.setInput("lastScanResult", scanInconnu());
    fixture.detectChanges();
    interne().onDemanderAutreLigne();

    expect(demande).toHaveBeenCalledWith(
      expect.objectContaining({ code: "7353163006031" }),
    );
    expect(interne().scanInconnu()).toBeNull();
    expect(productsApi.createCodeAlias).not.toHaveBeenCalled();
  });

  it("garde le bandeau si la mémorisation échoue, pour ne pas perdre le scan", async () => {
    await monter([ligneCodee]);
    productsApi.createCodeAlias.mockReturnValue(throwError(() => new Error("boom")));
    const rejoue = jest.fn();
    composant.rejouerScan.subscribe(rejoue);

    fixture.componentRef.setInput("lastScanResult", scanInconnu());
    fixture.detectChanges();
    interne().onAssocierCodeALaLigneCourante();

    expect(interne().scanInconnu()).not.toBeNull();
    expect(rejoue).not.toHaveBeenCalled();
  });

  describe("clavier", () => {
    it("Entrée associe le code à la ligne courante", async () => {
      await monter([ligneCodee]);
      fixture.componentRef.setInput("lastScanResult", scanInconnu());
      fixture.detectChanges();

      composant.onKeydown(new KeyboardEvent("keydown", { key: "Enter" }));

      expect(productsApi.createCodeAlias).toHaveBeenCalledWith(11, "7353163006031");
    });

    it("A demande une autre ligne", async () => {
      await monter([ligneCodee]);
      const demande = jest.fn();
      composant.autreLigneDemandee.subscribe(demande);
      fixture.componentRef.setInput("lastScanResult", scanInconnu());
      fixture.detectChanges();

      composant.onKeydown(new KeyboardEvent("keydown", { key: "a" }));

      expect(demande).toHaveBeenCalled();
    });

    it("Échap ignore le code sans rien écrire", async () => {
      await monter([ligneCodee]);
      fixture.componentRef.setInput("lastScanResult", scanInconnu());
      fixture.detectChanges();

      composant.onKeydown(new KeyboardEvent("keydown", { key: "Escape" }));

      expect(interne().scanInconnu()).toBeNull();
      expect(productsApi.createCodeAlias).not.toHaveBeenCalled();
    });

    it("sans code inconnu en attente, Entrée ne déclenche aucune association", async () => {
      await monter([ligneCodee]);

      composant.onKeydown(new KeyboardEvent("keydown", { key: "Enter" }));

      expect(productsApi.createCodeAlias).not.toHaveBeenCalled();
    });
  });
});
