import {
  bilanDisponible,
  bilanPerimetre,
  estDeLigne,
  variation,
  libelleOffre,
  libelleSeuil,
  prixApresPromotion,
  prixDansLePack,
} from './promotion.model';

describe('prixApresPromotion', () => {
  it('arrondit le pourcentage au supérieur, comme le serveur', () => {
    expect(prixApresPromotion('PROMO_POURCENTAGE', 20, 1_000)).toBe(800);
    // 333 × 15 % = 49,95 → 50 de remise
    expect(prixApresPromotion('PROMO_POURCENTAGE', 15, 333)).toBe(283);
  });

  it('ne descend jamais sous zéro pour un montant', () => {
    expect(prixApresPromotion('PROMO_MONTANT', 200, 1_000)).toBe(800);
    expect(prixApresPromotion('PROMO_MONTANT', 5_000, 100)).toBe(0);
  });

  it("garde le prix fixe s'il est sous le prix, le prix sinon", () => {
    expect(prixApresPromotion('PROMO_PRIX_FIXE', null, 1_000, 750)).toBe(750);
    expect(prixApresPromotion('PROMO_PRIX_FIXE', null, 1_000, 1_200)).toBe(1_000);
    expect(prixApresPromotion('PROMO_PRIX_FIXE', null, 1_000, null)).toBe(1_000);
  });

  it("donne pour une offre N+M le prix moyen d'une unité du groupe, comme le plancher du serveur", () => {
    // 2 achetés + 1 offert : 3 000 → 2 000.
    expect(prixApresPromotion('PROMO_OFFRE_NM', 1, 3_000, null, 2)).toBe(2_000);
    // 1 + 1 : moitié prix ; remise arrondie au supérieur.
    expect(prixApresPromotion('PROMO_OFFRE_NM', 1, 1_001, null, 1)).toBe(500);
  });
});

describe('prixDansLePack', () => {
  it('donne à chaque article sa part de la remise du pack, comme le serveur', () => {
    // 5 000 + 4 500 = 9 500, pack à 8 000 : 1 500 de remise, au prorata.
    expect(prixDansLePack(5_000, 9_500, 8_000)).toBe(4_211);
    expect(prixDansLePack(4_500, 9_500, 8_000)).toBe(3_790);
    // Un pack plus cher que ses articles ne remise rien.
    expect(prixDansLePack(5_000, 9_500, 10_000)).toBe(5_000);
  });
});

describe('libelleOffre', () => {
  it('accorde au pluriel, comme l’étiquette', () => {
    expect(libelleOffre(2, 1)).toBe('2 achetés + 1 offert');
    expect(libelleOffre(1, 2)).toBe('1 acheté + 2 offerts');
  });
});

describe('bilanPerimetre', () => {
  it('résume un ajout en bloc : ajoutés, déjà présents, dont à prix administré', () => {
    expect(bilanPerimetre(12, 2, ['PARACETAMOL 500', 'IBUPROFENE 400', 'AMOXICILLINE', 'DOLIPRANE'])).toBe(
      '12 article(s) ajouté(s) · 2 déjà dans la promotion · dont 4 à prix administré : PARACETAMOL 500, IBUPROFENE 400, AMOXICILLINE….',
    );
    expect(bilanPerimetre(0, 3, [])).toBe('Aucun article ajouté · 3 déjà dans la promotion.');
  });
});

describe('remise dès un montant d’achat', () => {
  it('se dit comme sur l’étiquette, et remise chaque unité de son taux une fois le seuil atteint', () => {
    expect(libelleSeuil(10, 20_000, v => `${v} F`)).toBe('−10 % dès 20000 F d’achat'.replace('’', "'"));
    expect(prixApresPromotion('PROMO_SEUIL_MONTANT', 10, 3_000)).toBe(2_700);
  });
});

describe('estDeLigne', () => {
  it('ne laisse cumuler que les mécaniques de ligne', () => {
    expect(estDeLigne('PROMO_POURCENTAGE')).toBe(true);
    expect(estDeLigne('PROMO_PRIX_FIXE')).toBe(true);
    expect(estDeLigne('PROMO_PACK')).toBe(false);
    expect(estDeLigne('PROMO_SEUIL_MONTANT')).toBe(false);
  });
});

describe('variation', () => {
  it('donne l’écart en pourcentage de la période précédente', () => {
    expect(variation(1_000, 1_250)).toBe('+25 %');
    expect(variation(1_000, 920)).toBe('−8 %');
    expect(variation(1_000, 1_000)).toBe('=');
    expect(variation(0, 0)).toBe('=');
    expect(variation(0, 500)).toBe('nouveau');
  });
});

describe('bilanDisponible', () => {
  it('se lit dès le premier jour de l’opération', () => {
    expect(bilanDisponible({ dateDebut: '2026-11-27' }, '2026-11-27')).toBe(true);
    expect(bilanDisponible({ dateDebut: '2026-11-28' }, '2026-11-27')).toBe(false);
  });
});
