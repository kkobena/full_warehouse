DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation_by_rayon CASCADE;

CREATE MATERIALIZED VIEW mv_stock_valuation_by_rayon AS
SELECT p.id                                         AS produit_id,
       p.libelle,
       fp.code_cip,
       f.libelle                                    AS categorie,
       f.id                                         AS categorieId,
       r.libelle                                    AS rayon,
       r.id                                         AS rayon_id,
       sp.total_qty                                 AS stock_quantity,
       fp.prix_achat                                AS purchase_price,
       fp.prix_uni                                  AS sales_price,
       sp.total_qty * fp.prix_achat                 AS total_purchase_value,
       sp.total_qty * fp.prix_uni                   AS total_sales_value,
       sp.total_qty * (fp.prix_uni - fp.prix_achat) AS potential_margin,
       CASE
         WHEN fp.prix_uni > 0
           THEN ROUND(((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
         ELSE 0
         END                                        AS margin_percentage,
       NOW()                                        AS last_updated
FROM produit p
       JOIN fournisseur_produit fp
            ON p.fournisseur_produit_principal_id = fp.id
       JOIN (SELECT produit_id,
                    SUM(qty_stock + qty_ug) AS total_qty
             FROM stock_produit
             GROUP BY produit_id) sp ON p.id = sp.produit_id
       LEFT JOIN famille_produit f ON p.famille_id = f.id
       LEFT JOIN rayon_produit rp ON p.id = rp.produit_id
       LEFT JOIN rayon r ON rp.rayon_id = r.id
WHERE p.status = 'ENABLE'
  AND sp.total_qty > 0
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, f.id,
         fp.prix_achat, fp.prix_uni,
         r.libelle, r.id, sp.total_qty;



CREATE UNIQUE INDEX idx_mv_stock_valuation_by_rayon_unique
  ON mv_stock_valuation_by_rayon (produit_id, rayon_id);

CREATE INDEX idx_mv_stock_valuation_by_rayon_category
  ON mv_stock_valuation_by_rayon (categorieId);

CREATE INDEX idx_mv_stock_valuation_by_rayon_value
  ON mv_stock_valuation_by_rayon (total_sales_value DESC);

CREATE INDEX idx_produit_status
  ON produit (status)
  WHERE status = 'ENABLE';

-- Initial refresh

REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_valuation_by_rayon;


DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation CASCADE;


CREATE MATERIALIZED VIEW mv_stock_valuation AS
SELECT p.id                                                               AS produit_id,
       p.libelle,
       fp.code_cip,
       f.libelle                                                          AS categorie,
       f.id                                                               AS categorie_id,
       COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint)                 AS stock_quantity,
       fp.prix_achat                                                      AS purchase_price,
       fp.prix_uni                                                        AS sales_price,
       COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS total_purchase_value,
       COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni   AS total_sales_value,
       COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni -
       COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS potential_margin,
       CASE
         WHEN fp.prix_uni > 0 THEN ROUND(
           ((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
         ELSE 0::numeric
         END                                                              AS margin_percentage,
       NOW()                                                              AS last_updated
FROM produit p
       LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
       LEFT JOIN stock_produit sp ON p.id = sp.produit_id
       LEFT JOIN famille_produit f ON p.famille_id = f.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle,f.id, fp.prix_achat, fp.prix_uni
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

COMMENT ON MATERIALIZED VIEW mv_stock_valuation IS 'Stock valuation aggregated across all storage locations with purchase and sales prices for financial reporting';

-- Recreate indexes
CREATE UNIQUE INDEX idx_mv_stock_valuation_unique
  ON mv_stock_valuation (produit_id);

CREATE INDEX idx_mv_stock_valuation_category
  ON mv_stock_valuation (categorie_id);

CREATE INDEX idx_mv_stock_valuation_value
  ON mv_stock_valuation (total_sales_value DESC);

-- Initial refresh
REFRESH MATERIALIZED VIEW mv_stock_valuation;

