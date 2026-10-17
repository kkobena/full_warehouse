import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { of, throwError } from "rxjs";
import { ProduitEtiquetteModalComponent } from "./produit-etiquette-modal.component";
import { IFormatEtiquette, ILotProduit, ProductsApiService } from "../../data-access/services/products-api.service";
import { NotificationService } from "app/shared/services/notification.service";
import { BlobDownloadService } from "app/shared/services/blob-download.service";
import { TauriPrinterService } from "app/shared/services/tauri-printer.service";

/**
 * Le choix du type d'étiquette décide de l'endpoint appelé : une erreur ici imprime une étiquette
 * sans lot là où la traçabilité l'exigeait, ou l'inverse.
 */
describe("ProduitEtiquetteModalComponent", () => {
  let fixture: ComponentFixture<ProduitEtiquetteModalComponent>;
  let composant: ProduitEtiquetteModalComponent;

  const lots: ILotProduit[] = [
    { id: 7, numLot: "LOT7", expiryDate: new Date("2026-12-31"), currentQuantity: 12 },
    { id: 8, numLot: "LOT8", expiryDate: new Date("2027-03-31"), currentQuantity: 4 },
  ];

  const formats: IFormatEtiquette[] = [
    { code: "A4_65_HISTORIQUE", libelle: "65 étiquettes", reference: "Avery L7651", support: "PLANCHE_A4", supportLibelle: "Planche A4", largeurMm: 38, hauteurMm: 21.2, colonnes: 5, rangees: 13, capacite: 65, parDefaut: true },
    { code: "A4_21_63X38", libelle: "21 étiquettes", reference: "Avery L7160", support: "PLANCHE_A4", supportLibelle: "Planche A4", largeurMm: 63.5, hauteurMm: 38.1, colonnes: 3, rangees: 7, capacite: 21, parDefaut: false },
    { code: "ROULEAU_40X30", libelle: "Rouleau 40 × 30", reference: "die-cut 40 × 30", support: "ROULEAU", supportLibelle: "Rouleau", largeurMm: 40, hauteurMm: 30, colonnes: 1, rangees: 1, capacite: 1, parDefaut: false },
  ];

  const api = {
    getEtiquettes: jest.fn(() => of(new Blob())),
    getEtiquettesLot: jest.fn(() => of(new Blob())),
    getEtiquettesZpl: jest.fn(() => of("^XA^XZ")),
    getReglettes: jest.fn(() => of(new Blob())),
    getReglettesZpl: jest.fn(() => of("^XA^XZ")),
    imprimerPreparation: jest.fn(() => of(new Blob())),
    imprimerPreparationZpl: jest.fn(() => of("^XA^XZ")),
    getEtiquettesLotZpl: jest.fn(() => of("^XA^BXN^XZ")),
    getLots: jest.fn(() => of(lots)),
    listerFormatsEtiquette: jest.fn(() => of(formats)),
  };
  /** Le poste : hors Tauri par défaut, comme un navigateur. */
  const tauriPrinter = {
    isRunningInTauri: jest.fn(() => false),
    getAvailablePrinters: jest.fn(() => Promise.resolve([{ name: "ZD230", isDefault: true }])),
    printZpl: jest.fn(() => Promise.resolve()),
  };
  const blobDownload = { downloadPdf: jest.fn() };
  const notification = { error: jest.fn() };
  const activeModal = { close: jest.fn(), dismiss: jest.fn() };

  /** Accès aux membres protégés : le test juge le comportement, pas la visibilité. */
  const interne = (): any => composant as any;

  async function monter(lotsDuProduit: ILotProduit[] = lots): Promise<void> {
    api.getLots.mockReturnValue(of(lotsDuProduit));
    api.listerFormatsEtiquette.mockReturnValue(of(formats));
    await TestBed.configureTestingModule({
      imports: [ProduitEtiquetteModalComponent],
      providers: [
        { provide: ProductsApiService, useValue: api },
        { provide: BlobDownloadService, useValue: blobDownload },
        { provide: NotificationService, useValue: notification },
        { provide: NgbActiveModal, useValue: activeModal },
        { provide: TauriPrinterService, useValue: tauriPrinter },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProduitEtiquetteModalComponent);
    composant = fixture.componentInstance;
    composant.produit = { id: 500, libelle: "DOLIPRANE 1000MG", codeCip: "1234567" } as any;
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    tauriPrinter.isRunningInTauri.mockReturnValue(false);
    tauriPrinter.getAvailablePrinters.mockResolvedValue([{ name: "ZD230", isDefault: true }]);
    tauriPrinter.printZpl.mockResolvedValue(undefined);
    TestBed.resetTestingModule();
  });

  it("imprime l'étiquette standard par défaut", async () => {
    await monter();

    interne().onPrint();

    expect(api.getEtiquettes).toHaveBeenCalledWith(500, 1, 1, "A4_65_HISTORIQUE");
    expect(api.getEtiquettesLot).not.toHaveBeenCalled();
  });

  it("imprime l'étiquette de lot quand ce type est choisi", async () => {
    await monter();

    interne().onTypeChange("LOT");
    interne().lotId.set(8);
    interne().onPrint();

    expect(api.getEtiquettesLot).toHaveBeenCalledWith(500, 8, 1, 1, "A4_65_HISTORIQUE");
    expect(api.getEtiquettes).not.toHaveBeenCalled();
  });

  it("n'imprime pas d'étiquette de lot sans lot choisi", async () => {
    await monter([]);

    interne().onTypeChange("LOT");

    expect(interne().peutImprimer()).toBe(false);
    interne().onPrint();
    expect(api.getEtiquettesLot).not.toHaveBeenCalled();
  });

  it("n'affiche « aucun lot » qu'une fois la liste chargée", async () => {
    await monter([]);

    expect(interne().etatLots()).toBe("VIERGE");

    interne().onTypeChange("LOT");

    expect(interne().etatLots()).toBe("PRET");
  });

  it("ne charge les lots qu'une fois le type « lot » choisi", async () => {
    await monter();

    expect(api.getLots).not.toHaveBeenCalled();

    interne().onTypeChange("LOT");
    interne().onTypeChange("STANDARD");
    interne().onTypeChange("LOT");

    expect(api.getLots).toHaveBeenCalledTimes(1);
  });

  it("présélectionne le lot quand l'article n'en a qu'un", async () => {
    await monter([lots[0]]);

    interne().onTypeChange("LOT");

    expect(interne().lotId()).toBe(7);
    expect(interne().peutImprimer()).toBe(true);
  });

  it("ne présélectionne rien quand plusieurs lots coexistent", async () => {
    await monter();

    interne().onTypeChange("LOT");

    expect(interne().lotId()).toBeNull();
  });

  it("présente chaque lot avec sa péremption et son stock", async () => {
    await monter();

    interne().onTypeChange("LOT");

    expect(interne().lots()[0].libelle).toContain("LOT7");
    expect(interne().lots()[0].libelle).toContain("12 en stock");
  });

  it("l'étiquette standard reste imprimable sans aucun lot", async () => {
    await monter([]);

    expect(interne().peutImprimer()).toBe(true);
    interne().onPrint();
    expect(api.getEtiquettes).toHaveBeenCalled();
  });

  it("signale l'échec de génération sans fermer la modale", async () => {
    await monter();
    api.getEtiquettes.mockReturnValueOnce(throwError(() => new Error("500")) as any);

    interne().onPrint();

    expect(notification.error).toHaveBeenCalled();
    expect(activeModal.close).not.toHaveBeenCalled();
    expect(interne().loading()).toBe(false);
  });

  it("nomme le fichier selon le type d'étiquette", async () => {
    await monter();

    interne().onTypeChange("LOT");
    interne().lotId.set(7);
    interne().onPrint();

    expect(blobDownload.downloadPdf).toHaveBeenCalledWith(expect.anything(), "etiquettes-lot-1234567");
  });

  it("retient le format par défaut du catalogue", async () => {
    await monter();

    expect(interne().formatCode()).toBe("A4_65_HISTORIQUE");
    expect(interne().capacite()).toBe(65);
  });

  it("la capacité suit le format choisi — 65 et non 64 pour la planche historique", async () => {
    await monter();

    interne().onFormatChange("A4_21_63X38");

    expect(interne().capacite()).toBe(21);
  });

  it("ramène la case de départ à 1 si la nouvelle planche est plus petite", async () => {
    await monter();
    interne().startAt = 40;

    interne().onFormatChange("A4_21_63X38");

    expect(interne().startAt).toBe(1);
  });

  it("refuse une case de départ hors de la planche", async () => {
    await monter();

    interne().startAt = 65;
    expect(interne().peutImprimer()).toBe(true);

    interne().startAt = 66;
    expect(interne().peutImprimer()).toBe(false);
  });

  it("transmet le format choisi à l'impression", async () => {
    await monter();

    interne().onFormatChange("A4_21_63X38");
    interne().onPrint();

    expect(api.getEtiquettes).toHaveBeenCalledWith(500, 1, 1, "A4_21_63X38");
  });

  it("imprime sans format si le catalogue est injoignable", async () => {
    api.listerFormatsEtiquette.mockReturnValueOnce(throwError(() => new Error("500")) as any);
    await monter();

    expect(interne().formatCode()).toBeNull();
    expect(interne().capacite()).toBe(65);

    interne().onPrint();

    expect(api.getEtiquettes).toHaveBeenCalledWith(500, 1, 1, undefined);
  });

  it("un rouleau n'a pas de case de départ, et la quantité n'est plus bornée par la planche", async () => {
    await monter();
    interne().startAt = 40;

    interne().onFormatChange("ROULEAU_40X30");

    expect(interne().estRouleau()).toBe(true);
    // Une case de départ y ferait dérouler des étiquettes blanches.
    expect(interne().startAt).toBe(1);
    expect(interne().capacite()).toBe(1);
    expect(interne().qtyMax()).toBe(500);
  });

  it("imprime une série sur rouleau sans se faire refuser par la borne de planche", async () => {
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().qty = 24;

    expect(interne().peutImprimer()).toBe(true);
    interne().onPrint();

    expect(api.getEtiquettes).toHaveBeenCalledWith(500, 24, 1, "ROULEAU_40X30");
  });

  it("revenir à une planche rend la case de départ", async () => {
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onFormatChange("A4_21_63X38");

    expect(interne().estRouleau()).toBe(false);
    expect(interne().capacite()).toBe(21);
    expect(interne().qtyMax()).toBe(21);
  });

  it("n'offre pas le ZPL hors du poste : un navigateur ne parle pas à l'imprimante", async () => {
    await monter();

    interne().onFormatChange("ROULEAU_40X30");

    expect(interne().zplPossible()).toBe(false);
  });

  it("n'offre pas le ZPL sur une planche, même depuis le poste", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    expect(interne().zplPossible()).toBe(false);
  });

  it("envoie le flux ZPL à l'imprimante choisie plutôt que de télécharger un PDF", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onModeChange("ZPL");
    await Promise.resolve();
    interne().qty = 12;
    interne().onPrint();
    await Promise.resolve();

    expect(api.getEtiquettesZpl).toHaveBeenCalledWith(500, 12, "ROULEAU_40X30", "DPI_203");
    expect(api.getEtiquettes).not.toHaveBeenCalled();
    expect(tauriPrinter.printZpl).toHaveBeenCalledWith("^XA^XZ", "ZD230");
    expect(blobDownload.downloadPdf).not.toHaveBeenCalled();
  });

  it("envoie l'étiquette de lot en ZPL quand ce type est choisi", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onModeChange("ZPL");
    interne().onTypeChange("LOT");
    interne().lotId.set(8);
    interne().resolution.set("DPI_300");
    interne().onPrint();
    await Promise.resolve();

    expect(api.getEtiquettesLotZpl).toHaveBeenCalledWith(500, 8, 1, "ROULEAU_40X30", "DPI_300");
  });

  it("signale le refus de l'imprimante sans fermer la modale", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    tauriPrinter.printZpl.mockRejectedValueOnce(new Error("hors ligne"));
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onModeChange("ZPL");
    interne().onPrint();
    await Promise.resolve();
    await Promise.resolve();

    expect(notification.error).toHaveBeenCalled();
    expect(activeModal.close).not.toHaveBeenCalled();
    expect(interne().loading()).toBe(false);
  });

  it("revenir à une planche repasse en PDF", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onModeChange("ZPL");
    interne().onFormatChange("A4_21_63X38");
    interne().onPrint();

    expect(interne().estZpl()).toBe(false);
    expect(api.getEtiquettes).toHaveBeenCalled();
    expect(api.getEtiquettesZpl).not.toHaveBeenCalled();
  });

  it("imprime une réglette de gondole depuis la fiche produit", async () => {
    await monter();

    interne().onTypeChange("REGLETTE");
    interne().qty = 3;
    interne().onPrint();

    expect(api.getReglettes).toHaveBeenCalledWith(500, 3, 1, "A4_65_HISTORIQUE");
    expect(api.getEtiquettes).not.toHaveBeenCalled();
    expect(blobDownload.downloadPdf).toHaveBeenCalledWith(expect.anything(), "reglettes-1234567");
  });

  it("préremplit le nom du pot avec l'article, sans écraser une saisie", async () => {
    await monter();

    interne().onTypeChange("PREPARATION");
    expect(interne().preparation.libelle).toBe("DOLIPRANE 1000MG");

    interne().preparation.libelle = "Pommade salicylée 5 %";
    interne().onTypeChange("STANDARD");
    interne().onTypeChange("PREPARATION");
    expect(interne().preparation.libelle).toBe("Pommade salicylée 5 %");
  });

  it("refuse une préparation sans composition", async () => {
    await monter();

    interne().onTypeChange("PREPARATION");

    // Le nom vient de l'article, mais une étiquette de préparation sans composition n'en est pas une.
    expect(interne().peutImprimer()).toBe(false);

    interne().preparation.composition = "Acide salicylique 5 g, vaseline qsp 100 g";
    expect(interne().peutImprimer()).toBe(true);
  });

  it("envoie la préparation saisie, dates comprises", async () => {
    await monter();

    interne().onTypeChange("PREPARATION");
    interne().preparation.composition = "Acide salicylique 5 g, vaseline qsp 100 g";
    interne().preparation.mention = "À usage externe";
    interne().datePreparation = { year: 2026, month: 10, day: 16 };
    interne().onPrint();

    expect(api.imprimerPreparation).toHaveBeenCalledWith(
      expect.objectContaining({
        libelle: "DOLIPRANE 1000MG",
        composition: "Acide salicylique 5 g, vaseline qsp 100 g",
        mention: "À usage externe",
        datePreparation: "2026-10-16",
        peremption: null,
      }),
      1,
      1,
      "A4_65_HISTORIQUE"
    );
  });

  it("une réglette part aussi en ZPL depuis le poste", async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    interne().onFormatChange("ROULEAU_40X30");
    interne().onModeChange("ZPL");
    interne().onTypeChange("REGLETTE");
    interne().onPrint();
    await Promise.resolve();

    expect(api.getReglettesZpl).toHaveBeenCalledWith(500, 1, "ROULEAU_40X30", "DPI_203");
    expect(tauriPrinter.printZpl).toHaveBeenCalled();
  });
});
