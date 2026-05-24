

DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation CASCADE;

CREATE MATERIALIZED VIEW mv_stock_valuation AS
SELECT
    p.id                                                                        AS produit_id,
    COALESCE(st.magasin_id, 0)::integer                                         AS magasin_id,
    p.libelle,
    fp.code_cip,
    f.libelle                                                                   AS categorie,
    f.id                                                                        AS categorie_id,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint)                          AS stock_quantity,
    fp.prix_achat                                                               AS purchase_price,
    fp.prix_uni                                                                 AS sales_price,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat          AS total_purchase_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni            AS total_sales_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni
        - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat   AS potential_margin,
    CASE
        WHEN fp.prix_uni > 0 THEN ROUND(
            ((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
        ELSE 0::numeric
    END                                                                         AS margin_percentage,
    NOW()                                                                       AS last_updated
FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN stock_produit sp      ON p.id = sp.produit_id
    LEFT JOIN storage st            ON sp.storage_id = st.id
    LEFT JOIN famille_produit f     ON p.famille_id = f.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, f.id, fp.prix_achat, fp.prix_uni, st.magasin_id
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

COMMENT ON MATERIALIZED VIEW mv_stock_valuation
    IS 'Stock valuation per product per magasin. magasin_id=0 means no branch assigned. Filter by magasin_id > 0 for branch-level reports.';

-- Simple column-based unique index — works on all PostgreSQL versions for CONCURRENT refresh
CREATE UNIQUE INDEX idx_mv_stock_valuation_unique
    ON mv_stock_valuation (produit_id, magasin_id);

CREATE INDEX idx_mv_stock_valuation_magasin
    ON mv_stock_valuation (magasin_id);

CREATE INDEX idx_mv_stock_valuation_category
    ON mv_stock_valuation (categorie_id);

CREATE INDEX idx_mv_stock_valuation_value
    ON mv_stock_valuation (total_sales_value DESC);

REFRESH MATERIALIZED VIEW mv_stock_valuation;

-- ── mv_stock_valuation_by_rayon ───────────────────────────────────────────────

DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation_by_rayon CASCADE;

CREATE MATERIALIZED VIEW mv_stock_valuation_by_rayon AS
SELECT
    p.id                                                                        AS produit_id,
    COALESCE(st.magasin_id, 0)::integer                                         AS magasin_id,
    p.libelle,
    fp.code_cip,
    f.libelle                                                                   AS categorie,
    f.id                                                                        AS categorie_id,
    r.libelle                                                                   AS rayon,
    COALESCE(r.id, 0)::integer                                                  AS rayon_id,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint)                          AS stock_quantity,
    fp.prix_achat                                                               AS purchase_price,
    fp.prix_uni                                                                 AS sales_price,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat          AS total_purchase_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni            AS total_sales_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni
        - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat   AS potential_margin,
    CASE
        WHEN fp.prix_uni > 0 THEN ROUND(
            ((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
        ELSE 0::numeric
    END                                                                         AS margin_percentage,
    NOW()                                                                       AS last_updated
FROM produit p
    JOIN  fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    JOIN  stock_produit sp       ON p.id = sp.produit_id
    JOIN  storage st             ON sp.storage_id = st.id
    LEFT JOIN famille_produit f  ON p.famille_id = f.id
    LEFT JOIN rayon_produit rp   ON p.id = rp.produit_id
    LEFT JOIN rayon r            ON rp.rayon_id = r.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, f.id, fp.prix_achat, fp.prix_uni,
         r.libelle, r.id, st.magasin_id
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

COMMENT ON MATERIALIZED VIEW mv_stock_valuation_by_rayon
    IS 'Stock valuation per product per rayon per magasin. magasin_id/rayon_id=0 means no assignment.';

-- Simple column-based unique index — works on all PostgreSQL versions for CONCURRENT refresh
CREATE UNIQUE INDEX idx_mv_stock_valuation_by_rayon_unique
    ON mv_stock_valuation_by_rayon (produit_id, rayon_id, magasin_id);

CREATE INDEX idx_mv_stock_valuation_by_rayon_magasin
    ON mv_stock_valuation_by_rayon (magasin_id);

CREATE INDEX idx_mv_stock_valuation_by_rayon_category
    ON mv_stock_valuation_by_rayon (categorie_id);

CREATE INDEX idx_mv_stock_valuation_by_rayon_rayon
    ON mv_stock_valuation_by_rayon (rayon_id);

CREATE INDEX idx_mv_stock_valuation_by_rayon_value
    ON mv_stock_valuation_by_rayon (total_sales_value DESC);

REFRESH MATERIALIZED VIEW mv_stock_valuation_by_rayon;

UPDATE nav_item SET ordre = 6 WHERE code = 'rapports';

UPDATE nav_item SET ordre = 5,niveau=1,target_type='GROUP' WHERE code = 'comptabilite';

