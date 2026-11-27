-- Promotions : plafond total « dans la limite de N unites » (docs/PLAN-GESTION-PROMOTIONS.md, §5.3 invariant 8).
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS quantite_max_totale INTEGER;
ALTER TABLE promotion ADD CONSTRAINT promotion_quantite_max_totale_ck
  CHECK (quantite_max_totale IS NULL OR quantite_max_totale >= quantite_min);

COMMENT ON COLUMN promotion.quantite_max_totale IS
  'Unites remisees au plus, toutes ventes cloturees non annulees confondues. Nul : sans plafond.';

-- Le nombre d'unites que la promotion a remisees sur la ligne : sous un plafond, ce n'est pas la
-- quantite vendue. Le compteur du plafond total en fait la somme.
ALTER TABLE sales_line ADD COLUMN IF NOT EXISTS quantite_promo INTEGER;

COMMENT ON COLUMN sales_line.quantite_promo IS
  'Unites remisees par la promotion (promotion_id). Nul sans promotion.';

-- Les lignes deja promues : seul le plafond par vente pouvait borner leurs unites.
UPDATE sales_line sl
SET quantite_promo = LEAST(sl.quantity_requested, COALESCE(p.quantite_max_par_vente, sl.quantity_requested))
FROM promotion p
WHERE sl.promotion_id = p.id
  AND sl.quantite_promo IS NULL;
