import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { of, throwError } from "rxjs";
import { ProduitSearch } from "app/shared/model";
import { NotificationService } from "app/shared/services/notification.service";
import { FavoriSuggere, ProduitsFavorisApiService } from "../../data-access/services/produits-favoris-api.service";
import { FavorisComptoirModalComponent } from "./favoris-comptoir-modal.component";

/** Gestion de la grille du comptoir : les épinglés dans leur ordre, les suggestions d'après les ventes, chaque geste tout de suite. */
describe("FavorisComptoirModalComponent", () => {
  let fixture: ComponentFixture<FavorisComptoirModalComponent>;
  const api = { lister: jest.fn(), suggerer: jest.fn(), ajouter: jest.fn(), retirer: jest.fn(), reordonner: jest.fn() };
  const activeModal = { close: jest.fn() };
  const notifications = { error: jest.fn() };

  const produit = (id: number, libelle: string) => ({ id, libelle, regularUnitPrice: 1000 }) as ProduitSearch;
  const suggestion = (id: number, libelle: string, nbVentes: number): FavoriSuggere => ({ produit: produit(id, libelle), nbVentes, qteVendue: nbVentes * 2 });
  const el = () => fixture.nativeElement as HTMLElement;
  const libelles = (section: string) =>
    Array.from(el().querySelectorAll(`section[aria-labelledby="${section}"] .libelle`)).map(e => e.textContent?.trim());
  const bouton = (libelle: string) => el().querySelector<HTMLButtonElement>(`button[aria-label="${libelle}"]`)!;

  function ouvrir(epingles: ProduitSearch[], suggestions: FavoriSuggere[]): void {
    api.lister.mockReturnValue(of(epingles));
    api.suggerer.mockReturnValue(of(suggestions));
    fixture = TestBed.createComponent(FavorisComptoirModalComponent);
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    api.ajouter.mockReturnValue(of(undefined));
    api.retirer.mockReturnValue(of(undefined));
    api.reordonner.mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      imports: [FavorisComptoirModalComponent],
      providers: [
        { provide: ProduitsFavorisApiService, useValue: api },
        { provide: NgbActiveModal, useValue: activeModal },
        { provide: NotificationService, useValue: notifications },
      ],
    });
  });

  it("montre les épinglés dans l'ordre de la grille et les suggestions à part", () => {
    ouvrir([produit(1, "MASQUE"), produit(2, "GANTS")], [suggestion(3, "SERUM PHYSIOLOGIQUE", 12)]);

    expect(libelles("titre-epingles")).toEqual(["MASQUE", "GANTS"]);
    expect(libelles("titre-suggestions")).toEqual(["SERUM PHYSIOLOGIQUE"]);
    expect(el().textContent).toContain("12 ventes");
  });

  it("explique pourquoi la grille n'apparaît pas quand rien n'est épinglé", () => {
    ouvrir([], [suggestion(3, "SERUM", 1)]);

    expect(el().textContent).toContain("la grille n'apparaît pas à l'écran de vente");
  });

  it("épingler une suggestion l'ajoute en dernier et la retire des suggestions", () => {
    ouvrir([produit(1, "MASQUE")], [suggestion(3, "SERUM", 5), suggestion(4, "PANSEMENT", 3)]);

    bouton("Épingler SERUM").click();
    fixture.detectChanges();

    expect(api.ajouter).toHaveBeenCalledWith(3);
    expect(libelles("titre-epingles")).toEqual(["MASQUE", "SERUM"]);
    expect(libelles("titre-suggestions")).toEqual(["PANSEMENT"]);
  });

  it("retirer un favori relit les deux listes : il peut redevenir une suggestion", () => {
    ouvrir([produit(1, "MASQUE")], []);
    api.lister.mockReturnValue(of([]));
    api.suggerer.mockReturnValue(of([suggestion(1, "MASQUE", 9)]));

    bouton("Retirer MASQUE").click();
    fixture.detectChanges();

    expect(api.retirer).toHaveBeenCalledWith(1);
    expect(libelles("titre-epingles")).toEqual([]);
    expect(libelles("titre-suggestions")).toEqual(["MASQUE"]);
  });

  it("monte et descend un favori, et envoie au serveur l'ordre complet", () => {
    ouvrir([produit(1, "MASQUE"), produit(2, "GANTS"), produit(3, "SERUM")], []);

    bouton("Monter SERUM").click();
    fixture.detectChanges();
    expect(api.reordonner).toHaveBeenLastCalledWith([1, 3, 2]);
    expect(libelles("titre-epingles")).toEqual(["MASQUE", "SERUM", "GANTS"]);

    bouton("Descendre MASQUE").click();
    fixture.detectChanges();
    expect(api.reordonner).toHaveBeenLastCalledWith([3, 1, 2]);
    expect(libelles("titre-epingles")).toEqual(["SERUM", "MASQUE", "GANTS"]);
  });

  it("grise « Monter » sur le premier et « Descendre » sur le dernier", () => {
    ouvrir([produit(1, "MASQUE"), produit(2, "GANTS")], []);

    expect(bouton("Monter MASQUE").disabled).toBe(true);
    expect(bouton("Descendre GANTS").disabled).toBe(true);
    expect(bouton("Descendre MASQUE").disabled).toBe(false);
  });

  it("laisse l'ordre affiché tel qu'il était quand le serveur refuse le déplacement", () => {
    ouvrir([produit(1, "MASQUE"), produit(2, "GANTS")], []);
    api.reordonner.mockReturnValue(throwError(() => new Error("500")));

    bouton("Monter GANTS").click();
    fixture.detectChanges();

    expect(libelles("titre-epingles")).toEqual(["MASQUE", "GANTS"]);
    expect(notifications.error).toHaveBeenCalled();
  });

  it("signale l'échec du chargement", () => {
    api.lister.mockReturnValue(throwError(() => new Error("réseau")));
    api.suggerer.mockReturnValue(of([]));
    fixture = TestBed.createComponent(FavorisComptoirModalComponent);
    fixture.detectChanges();

    expect(notifications.error).toHaveBeenCalled();
  });

  it("se ferme sur « Terminé »", () => {
    ouvrir([], []);

    el().querySelector<HTMLButtonElement>(".modal-footer button")?.click();

    expect(activeModal.close).toHaveBeenCalled();
  });
});
