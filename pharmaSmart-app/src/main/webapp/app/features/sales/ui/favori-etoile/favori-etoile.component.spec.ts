import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AbilityService } from 'app/core/auth/ability.service';
import { ProduitSearch } from 'app/shared/model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ProduitsFavorisApiService } from '../../../products/data-access/services/produits-favoris-api.service';
import { FavorisComptoirService } from '../../data-access/services/favoris-comptoir.service';
import { FavoriEtoileComponent } from './favori-etoile.component';

/** Étoile favori : épingler ou retirer depuis la recherche et le panier, sans passer par le catalogue. */
describe('FavoriEtoileComponent', () => {
  let fixture: ComponentFixture<FavoriEtoileComponent>;
  const api = { lister: jest.fn(), ajouter: jest.fn(), retirer: jest.fn() };
  const notification = { success: jest.fn(), error: jest.fn() };
  let droit = true;

  const favori = (id: number) => ({ id, libelle: `P${id}` }) as ProduitSearch;
  const bouton = () => (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.favori-etoile');

  function afficher(produitId: number, favoris: ProduitSearch[] = []): FavorisComptoirService {
    api.lister.mockReturnValue(of(favoris));
    const service = TestBed.inject(FavorisComptoirService);
    service.charger();
    fixture = TestBed.createComponent(FavoriEtoileComponent);
    fixture.componentRef.setInput('produitId', produitId);
    fixture.componentRef.setInput('libelle', 'DOLIPRANE');
    fixture.detectChanges();
    return service;
  }

  beforeEach(() => {
    jest.clearAllMocks();
    droit = true;
    TestBed.configureTestingModule({
      imports: [FavoriEtoileComponent],
      providers: [
        { provide: ProduitsFavorisApiService, useValue: api },
        { provide: NotificationService, useValue: notification },
        { provide: AbilityService, useValue: { canSignal: () => signal(droit) } },
      ],
    });
  });

  it("n'affiche aucune étoile à qui n'a pas le droit de gérer les favoris", () => {
    droit = false;
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [FavoriEtoileComponent],
      providers: [
        { provide: ProduitsFavorisApiService, useValue: api },
        { provide: NotificationService, useValue: notification },
        { provide: AbilityService, useValue: { canSignal: () => signal(false) } },
      ],
    });
    afficher(1);
    expect(bouton()).toBeNull();
  });

  it("annonce l'action que le clic produira, et son état", () => {
    afficher(1, [favori(2)]);
    expect(bouton()?.getAttribute('aria-label')).toBe('Épingler DOLIPRANE aux favoris');
    expect(bouton()?.getAttribute('aria-pressed')).toBe('false');

    afficher(2, [favori(2)]);
    expect(bouton()?.getAttribute('aria-label')).toBe('Retirer DOLIPRANE des favoris');
    expect(bouton()?.getAttribute('aria-pressed')).toBe('true');
  });

  it("épingle au clic, relit la grille et le dit à l'utilisateur", () => {
    api.ajouter.mockReturnValue(of(undefined));
    afficher(1);

    bouton()!.click();

    expect(api.ajouter).toHaveBeenCalledWith(1);
    expect(api.lister).toHaveBeenCalledTimes(2);
    expect(notification.success).toHaveBeenCalledWith(expect.stringContaining('ajouté aux favoris'), 'Favoris du comptoir');
  });

  it('retire au clic quand le produit est déjà épinglé', () => {
    api.retirer.mockReturnValue(of(undefined));
    afficher(2, [favori(2)]);

    bouton()!.click();

    expect(api.retirer).toHaveBeenCalledWith(2);
    expect(api.ajouter).not.toHaveBeenCalled();
  });

  it("dit pourquoi un refus (grille pleine) et ne relit rien", () => {
    api.ajouter.mockReturnValue(throwError(() => ({ status: 400, error: { message: 'La grille est pleine : 12 favoris au maximum.' } })));
    afficher(1);

    bouton()!.click();

    expect(notification.error).toHaveBeenCalledWith('La grille est pleine : 12 favoris au maximum.', 'Favoris du comptoir');
    expect(api.lister).toHaveBeenCalledTimes(1);
  });

  it('ne laisse pas le clic remonter à la ligne du panier', () => {
    api.ajouter.mockReturnValue(of(undefined));
    afficher(1);
    const surLigne = jest.fn();
    (fixture.nativeElement as HTMLElement).addEventListener('click', surLigne);

    bouton()!.click();

    expect(surLigne).not.toHaveBeenCalled();
  });
});
