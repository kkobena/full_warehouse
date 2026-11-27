import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NotificationService } from '../../../../shared/services/notification.service';
import { PromotionApiService } from '../../../promotion/data-access/services/promotion-api.service';
import { IPackComptoir } from '../../../promotion/models/promotion.model';
import { SaleEvent } from '../store/sales.store';
import { SalesFacade } from '../facades/sales.facade';
import { PackComptoirService } from './pack-comptoir.service';

describe('PackComptoirService', () => {
  const lastEvent = signal<SaleEvent | null>({ type: 'PRODUCT_ADDED', seq: 7 });
  const pack: IPackComptoir = {
    promotionId: 9,
    libelle: 'Duo cheveux',
    prix: 8000,
    composants: [
      { produitId: 1, libelle: 'SHAMPOOING', regularUnitPrice: 5000, quantite: 1, itemQty: 1 },
      { produitId: 2, libelle: 'APRES-SHAMPOOING', regularUnitPrice: 4500, quantite: 2, itemQty: 1 },
    ],
  };
  let service: PackComptoirService;

  beforeEach(() => {
    lastEvent.set({ type: 'PRODUCT_ADDED', seq: 7 });
    TestBed.configureTestingModule({
      providers: [
        { provide: SalesFacade, useValue: { lastEvent } },
        { provide: PromotionApiService, useValue: { pack: () => of(pack) } },
        { provide: NotificationService, useValue: { warning: jest.fn() } },
      ],
    });
    service = TestBed.inject(PackComptoirService);
  });

  it("attend la confirmation de chaque article avant d'ajouter le suivant, sans se fier à l'ajout précédent", () => {
    const ajouts: [number, number][] = [];

    service.ajouter(pack, (produit, quantite) => ajouts.push([produit.id, quantite]));
    TestBed.tick();

    // L'évènement déjà présent (seq 7) est rejoué à l'abonnement : il ne doit pas compter.
    expect(ajouts).toEqual([[1, 1]]);

    lastEvent.set({ type: 'PRODUCT_ADDED', seq: 8 });
    TestBed.tick();

    // Le second article part à sa quantité dans le pack, et seulement après la confirmation du premier.
    expect(ajouts).toEqual([
      [1, 1],
      [2, 2],
    ]);
  });
});
