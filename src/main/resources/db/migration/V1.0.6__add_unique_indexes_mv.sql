-- Add unique indexes for materialized views to enable CONCURRENT refresh
-- Required for: REFRESH MATERIALIZED VIEW CONCURRENTLY

-- Unique index on mv_product_profitability
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_product_profitability_produit_id
    ON mv_product_profitability (produit_id);
