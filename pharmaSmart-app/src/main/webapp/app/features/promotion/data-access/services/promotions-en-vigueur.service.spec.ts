import { IPromotionArticle } from '../../models/promotion.model';
import { decrirePromotion } from './promotions-en-vigueur.service';

describe('decrirePromotion', () => {
  const article: IPromotionArticle = {
    produitId: 1,
    promotionId: 5,
    libelle: 'Opération été',
    prixReference: 4495,
    prixApresPromo: 3596,
    dateFin: '2026-12-31',
    quantiteMin: 1,
  };
  const montant = (v: number): string => `${v} F`;

  it('dit la promotion, les deux prix et la date de fin', () => {
    expect(decrirePromotion(article, montant)).toBe("Opération été : 3596 F au lieu de 4495 F, jusqu'au 31/12/2026");
  });

  it('cite le seuil de quantité quand il y en a un', () => {
    expect(decrirePromotion({ ...article, quantiteMin: 3 }, montant)).toBe(
      "Opération été : 3596 F au lieu de 4495 F, dès 3 unités, jusqu'au 31/12/2026",
    );
  });

  it("dit une offre N+M en toutes lettres, sans prix moyen qui tromperait la cliente d'une seule unité", () => {
    expect(decrirePromotion({ ...article, quantiteMin: 3, offre: '2 achetés + 1 offert' }, montant)).toBe(
      "Opération été : 2 achetés + 1 offert, jusqu'au 31/12/2026",
    );
  });
});
