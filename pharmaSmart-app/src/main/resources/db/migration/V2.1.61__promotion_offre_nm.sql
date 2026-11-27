-- Promotions, phase 2 (docs/PLAN-GESTION-PROMOTIONS.md §6) : l'offre N+M, « N achetés, M offerts ».
-- Aucune colonne nouvelle : quantite_min porte N (les unites achetees) et valeur M (les offertes).
-- Les unites offertes sont les moins cheres de chaque groupe, tous articles de la promotion
-- confondus ; la remise du groupe se repartit au plus fort reste sur les lignes qui y participent.
-- sales_line.quantity_ug n'est PAS employe : il porte les unites gratuites du fournisseur (§7.5).

ALTER TABLE promotion DROP CONSTRAINT IF EXISTS promotion_type_ck;
ALTER TABLE promotion ADD CONSTRAINT promotion_type_ck
  CHECK (type IN ('PROMO_POURCENTAGE', 'PROMO_MONTANT', 'PROMO_PRIX_FIXE', 'PROMO_OFFRE_NM'));

-- Une offre N+M offre au moins une unite.
ALTER TABLE promotion ADD CONSTRAINT promotion_offre_nm_ck
  CHECK (type <> 'PROMO_OFFRE_NM' OR (valeur IS NOT NULL AND valeur >= 1));

COMMENT ON COLUMN promotion.valeur IS
  'Taux en % (PROMO_POURCENTAGE), montant unitaire en F (PROMO_MONTANT), nombre d''unites offertes M (PROMO_OFFRE_NM) ; nul pour PROMO_PRIX_FIXE, dont le prix est par produit.';
COMMENT ON COLUMN promotion.quantite_min IS
  'Quantite qui declenche la promotion ; pour PROMO_OFFRE_NM, nombre d''unites achetees N (le groupe compte N+M unites).';
