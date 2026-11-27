-- Journal des remises, phase 1 (docs/PLAN-JOURNAL-REMISES.md) : la part de la promotion dans la
-- remise d'une ligne.
--
-- Une ligne qui cumule une promotion cumulable et la remise produit ne gardait que la somme des
-- deux (discount_amount) : le journal ne pourrait dire ce qu'a coûté la promotion, ni ce qu'a coûté
-- la grille. Le moteur enregistre désormais la part de la promotion ; celle de la remise produit
-- s'en déduit : discount_amount - remise_promotion.
--
-- Pas de reprise : une ligne promue déjà enregistrée (remise_promotion à 0) est imputée en entier à
-- la promotion par le journal.
ALTER TABLE sales_line
    ADD COLUMN IF NOT EXISTS remise_promotion integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN sales_line.remise_promotion IS
    'Part de la promotion (promotion_id) dans discount_amount ; le reste est la remise produit.';
