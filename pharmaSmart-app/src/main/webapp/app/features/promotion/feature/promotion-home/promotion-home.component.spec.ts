import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { IPromotion } from '../../models/promotion.model';
import { PromotionHomeComponent } from './promotion-home.component';

const SUSPENDUE = {
  id: 1,
  code: 'PRM-1',
  libelle: 'Novembre -20 %',
  type: 'PROMO_POURCENTAGE',
  valeur: 20,
  dateDebut: '2099-11-01',
  dateFin: '2099-11-30',
  statut: 'SUSPENDUE',
  etat: 'SUSPENDUE',
  priorite: 0,
  quantiteMin: 1,
  unitesRemisees: 0,
  nombreProduits: 1,
  produits: [],
} as IPromotion;

interface Droits {
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

describe('Promotions — actions protégées par les droits', () => {
  let fixture: ComponentFixture<PromotionHomeComponent>;

  async function creer(droits: Droits): Promise<HTMLElement> {
    TestBed.inject(AbilityService).setFromNavTree([
      {
        id: 1,
        code: 'promotions',
        libelle: 'Promotions',
        targetType: 'ROUTE',
        ordre: 7,
        permissions: { canDisplay: true, canAccess: true, canExport: false, canExecute: false, ...droits },
      },
    ]);
    fixture = TestBed.createComponent(PromotionHomeComponent);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .expectOne(r => r.url.endsWith('api/promotions'))
      .flush([SUSPENDUE]);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const bouton = (element: HTMLElement, libelle: string): Element | null =>
    element.querySelector(`[aria-label^="${libelle}"]`) ??
    Array.from(element.querySelectorAll('button')).find(b => b.textContent?.includes(libelle)) ??
    null;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromotionHomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateService(), AbilityService],
    }).compileComponents();
  });

  it('montre création, modification, validation et annulation à qui en a le droit', async () => {
    const element = await creer({ canCreate: true, canEdit: true, canDelete: true });
    expect(bouton(element, 'Nouvelle promotion')).not.toBeNull();
    expect(bouton(element, 'Modifier')).not.toBeNull();
    expect(bouton(element, 'Valider')).not.toBeNull();
    expect(bouton(element, 'Annuler')).not.toBeNull();
  });

  it('les cache à qui ne peut que consulter', async () => {
    const element = await creer({ canCreate: false, canEdit: false, canDelete: false });
    expect(bouton(element, 'Nouvelle promotion')).toBeNull();
    expect(bouton(element, 'Modifier')).toBeNull();
    expect(bouton(element, 'Valider')).toBeNull();
    expect(bouton(element, 'Annuler')).toBeNull();
    // La consultation reste possible.
    expect(bouton(element, 'Voir les articles')).not.toBeNull();
  });

  it('sépare création et modification', async () => {
    const element = await creer({ canCreate: false, canEdit: true, canDelete: false });
    expect(bouton(element, 'Nouvelle promotion')).toBeNull();
    expect(bouton(element, 'Modifier')).not.toBeNull();
    expect(bouton(element, 'Annuler')).toBeNull();
  });
});
