-- Promotions, phase 2 (docs/PLAN-GESTION-PROMOTIONS.md, invariant 7) : le cumul avec la remise du
-- caissier.
--
-- Une promotion cumulable s'applique aussi à une ligne déjà remisée par la remise produit (grille)
-- choisie sur la vente : la remise du caissier d'abord, la promotion ensuite, sur le prix déjà remisé.
-- Une ligne porte toujours au plus une promotion (invariant 5) : pas de cumul entre promotions.
--
-- Réservé aux mécaniques de ligne : une offre N+M, un pack ou une remise de panier répartissent leur
-- remise sur des prix de référence, qu'une remise préalable fausserait.
--
-- Faux par défaut, et il le reste : cumuler par défaut est la première cause documentée de dérive de
-- marge (§7, point 12).

ALTER TABLE promotion ADD COLUMN IF NOT EXISTS cumulable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE promotion ADD CONSTRAINT promotion_cumulable_ck
  CHECK (NOT cumulable OR type IN ('PROMO_POURCENTAGE', 'PROMO_MONTANT', 'PROMO_PRIX_FIXE'));

COMMENT ON COLUMN promotion.cumulable IS
  'S''applique aussi sur une ligne deja remisee par la remise produit de la vente, apres elle ; mecaniques de ligne seulement. Plancher de marge controle au comptoir sur la remise cumulee.';
