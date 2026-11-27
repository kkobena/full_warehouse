-- Promotions, phase 2 (docs/PLAN-GESTION-PROMOTIONS.md) : la remise dès un montant d'achat
-- (« -10 % dès 20 000 F »).
--
-- Le seuil se lit sur le prix de référence des articles de la promotion présents dans le panier ;
-- atteint, le taux (valeur) vaut pour tout ce montant. Une promotion de panier, mais sans objet de
-- remise au niveau de la vente : la remise est répartie au plus fort reste sur les lignes, qui gardent
-- leur base de TVA et leur lien à la promotion (sales_line.promotion_id).

ALTER TABLE promotion DROP CONSTRAINT IF EXISTS promotion_type_ck;
ALTER TABLE promotion ADD CONSTRAINT promotion_type_ck
  CHECK (type IN ('PROMO_POURCENTAGE', 'PROMO_MONTANT', 'PROMO_PRIX_FIXE', 'PROMO_OFFRE_NM', 'PROMO_PACK',
                  'PROMO_SEUIL_MONTANT'));

ALTER TABLE promotion ADD COLUMN IF NOT EXISTS seuil_montant INTEGER;

-- Un seuil positif et un taux entre 1 et 99 % ; les plafonds en unités n'ont pas de sens pour une
-- remise de panier.
ALTER TABLE promotion ADD CONSTRAINT promotion_seuil_montant_ck
  CHECK (type <> 'PROMO_SEUIL_MONTANT'
         OR (seuil_montant IS NOT NULL AND seuil_montant >= 1
             AND valeur IS NOT NULL AND valeur BETWEEN 1 AND 99
             AND quantite_max_par_vente IS NULL AND quantite_max_totale IS NULL));

COMMENT ON COLUMN promotion.seuil_montant IS
  'Montant d''achat, en F, des articles de la promotion qui declenche la remise (PROMO_SEUIL_MONTANT) ; nul pour les autres mecaniques.';
COMMENT ON COLUMN promotion.valeur IS
  'Taux en % (PROMO_POURCENTAGE, PROMO_SEUIL_MONTANT), montant unitaire en F (PROMO_MONTANT), nombre d''unites offertes M (PROMO_OFFRE_NM), prix du pack en F (PROMO_PACK) ; nul pour PROMO_PRIX_FIXE, dont le prix est par produit.';
