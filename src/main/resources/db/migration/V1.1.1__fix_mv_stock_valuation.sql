-- Fix mv_stock_valuation to aggregate across all storage locations per product
-- This resolves the duplicate key constraint violation during concurrent refresh

-- Drop existing view and indexes
DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation CASCADE;

-- Recreate the view without storage_location to ensure one row per product
CREATE MATERIALIZED VIEW mv_stock_valuation AS
SELECT
    p.id AS produit_id,
    p.libelle,
    fp.code_cip,
    f.libelle AS categorie,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) AS stock_quantity,
    fp.prix_achat AS purchase_price,
    fp.prix_uni AS sales_price,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS total_purchase_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni AS total_sales_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni -
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS potential_margin,
    CASE
        WHEN fp.prix_uni > 0 THEN ROUND(
            ((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
        ELSE 0::numeric
    END AS margin_percentage,
    NOW() AS last_updated
FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN stock_produit sp ON p.id = sp.produit_id
    LEFT JOIN famille_produit f ON p.famille_id = f.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat, fp.prix_uni
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

COMMENT ON MATERIALIZED VIEW mv_stock_valuation IS 'Stock valuation aggregated across all storage locations with purchase and sales prices for financial reporting';

-- Recreate indexes
CREATE UNIQUE INDEX idx_mv_stock_valuation_unique
    ON mv_stock_valuation (produit_id);

CREATE INDEX idx_mv_stock_valuation_category
    ON mv_stock_valuation (categorie);

CREATE INDEX idx_mv_stock_valuation_value
    ON mv_stock_valuation (total_sales_value DESC);

-- Initial refresh
REFRESH MATERIALIZED VIEW mv_stock_valuation;
