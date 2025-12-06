

-- ===============================================
-- 2. UPDATE mv_monthly_top_products
-- ===============================================

DROP MATERIALIZED VIEW IF EXISTS mv_monthly_top_products CASCADE;

CREATE MATERIALIZED VIEW mv_monthly_top_products AS
SELECT
  TO_CHAR(DATE_TRUNC('month', s.sale_date), 'YYYY-MM-DD')as mois,
    p.id as produit_id,
    p.libelle,
    fp.code_cip,
    COUNT(DISTINCT s.id) as nb_ventes,
    SUM(sl.quantity_sold) as qte_vendue,
    SUM(sl.sales_amount) as ca_genere,
    AVG(sl.sales_amount / NULLIF(sl.quantity_sold, 0)) as prix_moyen,
    NOW() as last_updated
FROM sales_line sl
INNER JOIN sales s ON sl.sales_id = s.id
INNER JOIN produit p ON sl.produit_id = p.id
LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
WHERE s.statut = 'CLOSED'
  AND s.canceled = false
  AND s.ca = 'CA'
  AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY TO_CHAR(DATE_TRUNC('month', s.sale_date), 'YYYY-MM-DD'), p.id, p.libelle, fp.code_cip;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_monthly_top_products_unique
ON mv_monthly_top_products(mois, produit_id);

-- Create indexes for ordering and filtering
CREATE INDEX idx_mv_monthly_top_products_ca ON mv_monthly_top_products(mois, ca_genere DESC);
CREATE INDEX idx_mv_monthly_top_products_qte ON mv_monthly_top_products(mois, qte_vendue DESC);




-- ===============================================
-- 8. REFRESH ALL UPDATED VIEWS
-- ===============================================

REFRESH MATERIALIZED VIEW mv_monthly_top_products;


-- ===============================================
-- 9. UPDATE REFRESH FUNCTIONS
-- ===============================================

CREATE OR REPLACE FUNCTION refresh_all_report_views()
RETURNS void AS $$
BEGIN
    -- Phase 1 views
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_alerts;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_sales_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_top_products;

    -- Phase 2 views
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_valuation;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_rotation;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_rfm;

    -- Phase 3 views

    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_abc_pareto_analysis;

    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pareto_summary;

    RAISE NOTICE 'All report materialized views (Phases 1, 2, 3) refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- COMMENTS
-- ===============================================

COMMENT ON MATERIALIZED VIEW mv_daily_sales_summary IS 'Daily sales summary with filters: statut=CLOSED, canceled=false, ca=CA';
COMMENT ON MATERIALIZED VIEW mv_monthly_top_products IS 'Monthly top products with filters: statut=CLOSED, canceled=false, ca=CA';
COMMENT ON MATERIALIZED VIEW mv_stock_rotation IS 'Stock rotation with filters: statut=CLOSED, canceled=false, ca=CA';
COMMENT ON MATERIALIZED VIEW mv_customer_rfm IS 'Customer RFM segmentation with filters: statut=CLOSED, canceled=false, ca=CA';
COMMENT ON MATERIALIZED VIEW mv_abc_pareto_analysis IS 'ABC Pareto analysis with filters: statut=CLOSED, canceled=false, ca=CA';
