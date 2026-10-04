import { ComponentFixture, TestBed } from "@angular/core/testing";
import { IProduit } from "app/shared/model/produit.model";
import { ProduitListComponent } from "./produit-list.component";

/** Catalogue : la colonne « Favori » épingle un produit à la grille du comptoir, sans passer par un menu. */
describe("ProduitListComponent — colonne Favori", () => {
  let fixture: ComponentFixture<ProduitListComponent>;
  let composant: ProduitListComponent;

  const produit = (id: number, libelle: string, status: "ENABLE" | "DISABLE" = "ENABLE") => ({ id, libelle, status, codeCip: `CIP${id}` }) as unknown as IProduit;
  const el = () => fixture.nativeElement as HTMLElement;
  const cases = () => Array.from(el().querySelectorAll<HTMLInputElement>("td app-checkbox input[type=checkbox]"));
  /** Les cases de la colonne Favori : repérées par leur libellé, la première colonne étant la sélection. */
  const caseFavori = (libelle: string) =>
    el().querySelector<HTMLInputElement>(`input[aria-label$="${libelle} des favoris du comptoir"], input[aria-label$="${libelle} aux favoris du comptoir"]`);

  async function afficher(produits: IProduit[], inputs: Record<string, unknown> = {}): Promise<void> {
    fixture = TestBed.createComponent(ProduitListComponent);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput("produits", produits);
    fixture.componentRef.setInput("totalItems", produits.length);
    fixture.componentRef.setInput("rows", 10);
    Object.entries(inputs).forEach(([nom, valeur]) => fixture.componentRef.setInput(nom, valeur));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [ProduitListComponent] }));

  it("place la colonne Favori juste après Etat", async () => {
    await afficher([produit(1, "MASQUE")]);

    const titres = Array.from(el().querySelectorAll("thead th")).map(th => th.textContent?.trim());
    expect(titres.indexOf("Favori")).toBe(titres.indexOf("Etat") + 1);
  });

  it("coche les produits épinglés et laisse les autres décochés", async () => {
    await afficher([produit(1, "MASQUE"), produit(2, "GANTS")], { favorisIds: new Set([2]) });

    expect(caseFavori("MASQUE")?.checked).toBe(false);
    expect(caseFavori("GANTS")?.checked).toBe(true);
  });

  it("annonce l'action que la case produira", async () => {
    await afficher([produit(1, "MASQUE"), produit(2, "GANTS")], { favorisIds: new Set([2]) });

    expect(el().querySelector('input[aria-label="Épingler MASQUE aux favoris du comptoir"]')).not.toBeNull();
    expect(el().querySelector('input[aria-label="Retirer GANTS des favoris du comptoir"]')).not.toBeNull();
  });

  it("émet le produit et le nouvel état quand on coche, puis quand on décoche", async () => {
    await afficher([produit(1, "MASQUE")]);
    const emis: { produit: IProduit; favori: boolean }[] = [];
    composant.favoriChanged.subscribe(e => emis.push(e));

    caseFavori("MASQUE")!.click();
    fixture.detectChanges();
    caseFavori("MASQUE")!.click();

    expect(emis.map(e => [e.produit.id, e.favori])).toEqual([
      [1, true],
      [1, false],
    ]);
  });

  it("grise la case sans droit d'édition du catalogue : on voit l'état, on ne le change pas", async () => {
    await afficher([produit(1, "MASQUE")], { canEdit: false, favorisIds: new Set([1]) });

    expect(caseFavori("MASQUE")?.disabled).toBe(true);
    expect(caseFavori("MASQUE")?.checked).toBe(true);
  });

  it("grise la case d'un produit en veille : il ne se vend pas, il n'a rien à faire dans la grille", async () => {
    await afficher([produit(1, "MASQUE", "DISABLE")]);

    expect(caseFavori("MASQUE")?.disabled).toBe(true);
  });

  it("explique les colonnes J. stock, Marge, Etat et Favori par une icône d'information", async () => {
    await afficher([produit(1, "MASQUE")]);

    const infos = Array.from(el().querySelectorAll<HTMLElement>("thead button.th-info")).map(b => b.getAttribute("aria-label"));
    expect(infos).toEqual([
      "À propos de la colonne J. stock",
      "À propos de la colonne Marge",
      "À propos de la colonne Etat",
      "À propos de la colonne Favori",
    ]);
  });

  it("masque Etat, Favori et Statut quand le détail est ouvert, et les réaffiche à sa fermeture", async () => {
    const p1 = produit(1, "MASQUE");
    await afficher([p1]);
    const titres = () => Array.from(el().querySelectorAll("thead th")).map(th => th.textContent?.trim() ?? "");
    expect(titres().some(t => t.startsWith("Etat"))).toBe(true);

    fixture.componentRef.setInput("selectedProduit", p1);
    fixture.detectChanges();
    expect(titres().some(t => t.startsWith("Etat") || t.startsWith("Favori") || t === "Statut")).toBe(false);
    expect(caseFavori("MASQUE")).toBeNull();

    fixture.componentRef.setInput("selectedProduit", null);
    fixture.detectChanges();
    expect(titres().filter(t => t.startsWith("Etat") || t.startsWith("Favori") || t === "Statut").length).toBe(3);
  });

  it("n'a plus d'entrée de menu pour les favoris", async () => {
    await afficher([produit(1, "MASQUE")]);

    expect(el().textContent).not.toContain("favoris du comptoir");
    expect(cases().length).toBeGreaterThan(0);
  });
});
