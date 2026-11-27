import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IPromotionArticle } from '../../models/promotion.model';
import { BadgePromoComponent } from './badge-promo.component';

describe('BadgePromoComponent', () => {
  let fixture: ComponentFixture<BadgePromoComponent>;

  const article: IPromotionArticle = {
    produitId: 1,
    promotionId: 5,
    libelle: 'Opération été',
    prixReference: 4495,
    prixApresPromo: 3596,
    dateFin: '2026-12-31',
    quantiteMin: 3,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BadgePromoComponent] });
    fixture = TestBed.createComponent(BadgePromoComponent);
    fixture.componentRef.setInput('article', article);
    fixture.detectChanges();
  });

  it('affiche « PROMO » et dit, pour les lecteurs d’écran, la promotion et son prix', () => {
    const pastille = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('.badge-promo');

    expect(pastille?.textContent?.trim()).toBe('PROMO');
    const libelle = pastille?.getAttribute('aria-label') ?? '';
    expect(libelle).toContain('En promotion : Opération été');
    expect(libelle).toMatch(/3\s?596/);
    expect(libelle).toMatch(/au lieu de 4\s?495/);
    expect(libelle).toContain('dès 3 unités');
    expect(libelle).toContain("jusqu'au 31/12/2026");
  });
});
