import { ComponentFixture, TestBed } from "@angular/core/testing";
import { CodeResolutionComponent } from "./code-resolution.component";
import { IOrderLine } from "app/shared/model/order-line.model";

/**
 * La modale de résolution est le seul endroit où un code inconnu devient une liaison durable :
 * si elle laisse confirmer sans ligne choisie, on mémorise un code sur n'importe quoi.
 */
describe("CodeResolutionComponent", () => {
  let fixture: ComponentFixture<CodeResolutionComponent>;
  let composant: CodeResolutionComponent;

  const lignes: IOrderLine[] = [
    { id: 1, produitId: 11, produitLibelle: "DOLIPRANE 500", produitCip: "CIP-1", quantityRequested: 10, quantityReceived: 2 },
    { id: 2, produitId: 12, produitLibelle: "EFFERALGAN 1g", produitCip: "CIP-2", quantityRequested: 5, quantityReceived: 0 },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CodeResolutionComponent] }).compileComponents();
    fixture = TestBed.createComponent(CodeResolutionComponent);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput("scannedCode", "07353163006031");
    fixture.componentRef.setInput("lignes", lignes);
    fixture.componentRef.setInput("hint", {
      scannedCode: "7353163006031",
      numLot: "ABC101",
      expiryDate: "2025-01-31",
      serialNumber: null,
      qty: 3,
    });
    fixture.detectChanges();
  });

  /** Accès aux membres protégés : le test juge le comportement, pas la visibilité. */
  const interne = (): any => composant as any;

  it("n'autorise pas la confirmation avant qu'une ligne soit choisie", () => {
    expect(interne().peutConfirmer()).toBe(false);
  });

  it("n'émet rien si l'on confirme sans ligne choisie", () => {
    const valide = jest.fn();
    composant.valide.subscribe(valide);

    interne().confirmer();

    expect(valide).not.toHaveBeenCalled();
  });

  it("émet la ligne choisie et la mémorisation demandée", () => {
    const valide = jest.fn();
    composant.valide.subscribe(valide);

    interne().choisir(lignes[1]);
    interne().confirmer();

    expect(valide).toHaveBeenCalledWith({ ligne: lignes[1], memoriser: true });
  });

  it("permet de refuser la mémorisation", () => {
    const valide = jest.fn();
    composant.valide.subscribe(valide);

    interne().choisir(lignes[0]);
    interne().memoriser.set(false);
    interne().confirmer();

    expect(valide).toHaveBeenCalledWith({ ligne: lignes[0], memoriser: false });
  });

  it("filtre les lignes sur le libellé puis sur le code", () => {
    interne().filtre.set("efferalgan");
    expect(interne().lignesFiltrees()).toEqual([lignes[1]]);

    interne().filtre.set("cip-1");
    expect(interne().lignesFiltrees()).toEqual([lignes[0]]);
  });

  it("sans filtre, toutes les lignes du bon sont proposées", () => {
    expect(interne().lignesFiltrees()).toEqual(lignes);
  });

  it("réinitialise le choix entre deux scans", () => {
    interne().choisir(lignes[0]);
    interne().filtre.set("doli");
    interne().memoriser.set(false);

    composant.reinitialiser();

    expect(interne().ligneChoisie()).toBeNull();
    expect(interne().filtre()).toBe("");
    expect(interne().memoriser()).toBe(true);
  });

  it("affiche le lot et la péremption lus sur la boîte", () => {
    const texte = fixture.nativeElement.textContent as string;

    expect(texte).toContain("ABC101");
    expect(texte).toContain("31/01/2025");
    expect(texte).toContain("Lu sur la boîte");
  });

  it("signale des valeurs déduites comme à vérifier, et non comme lues", () => {
    fixture.componentRef.setInput("hint", {
      scannedCode: "LOT:ABC101/EXP:20250131",
      numLot: "ABC101",
      expiryDate: "2025-01-31",
      serialNumber: null,
      qty: 1,
      proposition: true,
    });
    fixture.detectChanges();

    const texte = fixture.nativeElement.textContent as string;

    expect(texte).toContain("Déduit de la boîte");
    expect(texte).not.toContain("Lu sur la boîte");
    expect(texte).toContain("ne suit pas le standard GS1");
  });

  it("annule sans rien émettre sur valide", () => {
    const valide = jest.fn();
    const annule = jest.fn();
    composant.valide.subscribe(valide);
    composant.annule.subscribe(annule);

    interne().annuler();

    expect(annule).toHaveBeenCalled();
    expect(valide).not.toHaveBeenCalled();
  });
});
