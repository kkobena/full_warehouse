-- ===============================================
-- Phase 1: Rapports Statistiques - Fondations Essentielles
-- Version: 1.1.6
-- Description: Create materialized views and indexes for reports
-- ===============================================

-- ===============================================
-- 1. STOCK ALERTS MATERIALIZED VIEW
-- ===============================================
-- Purpose: Optimize stock alerts queries (ruptures, alertes, péremptions)

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_stock_alerts AS
SELECT DISTINCT
    p.id as produit_id,
    p.libelle,
    fp.code_cip,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) as stock_quantity,
    p.qty_seuil_mini as seuil_min,
    l.expiry_date,
    CASE
        WHEN COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) = 0 THEN 'RUPTURE'
        WHEN COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) < p.qty_seuil_mini THEN 'ALERTE'
        WHEN l.expiry_date < CURRENT_DATE + INTERVAL '3 months' THEN 'PEREMPTION'
    END as alert_type,
    NOW() as last_updated
FROM produit p
LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
LEFT JOIN stock_produit sp ON p.id = sp.produit_id
LEFT JOIN order_line ol ON p.id = ol.produit_id
LEFT JOIN lot l ON ol.id = l.order_line_id AND l.expiry_date < CURRENT_DATE + INTERVAL '3 months'
WHERE p.status = 'ENABLE'
  AND (
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) = 0
    OR COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) < p.qty_seuil_mini
    OR (l.expiry_date IS NOT NULL AND l.expiry_date < CURRENT_DATE + INTERVAL '3 months')
  )
GROUP BY p.id, p.libelle, fp.code_cip, p.qty_seuil_mini, l.expiry_date;

-- Create unique index on materialized view for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_stock_alerts_unique
ON mv_stock_alerts(produit_id, COALESCE(expiry_date, CURRENT_DATE));

-- Create indexes for filtering
CREATE INDEX IF NOT EXISTS idx_mv_stock_alerts_type ON mv_stock_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_mv_stock_alerts_expiry ON mv_stock_alerts(expiry_date) WHERE expiry_date IS NOT NULL;

-- ===============================================
-- 2. DAILY SALES SUMMARY MATERIALIZED VIEW
-- ===============================================
-- Purpose: Optimize daily CA reporting and dashboard queries

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_sales_summary AS
SELECT
    DATE(s.sale_date) as sale_date,
    s.dtype as type_vente,
    COUNT(*) as nb_ventes,
    SUM(s.sales_amount) as ca_total,
    SUM(s.sales_amount - s.discount_amount) as ca_net,
    AVG(s.sales_amount) as panier_moyen,
    SUM(s.discount_amount) as total_remises,
    NOW() as last_updated
FROM sales s
WHERE s.statut = 'CLOSED'
GROUP BY DATE(s.sale_date), s.dtype;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_sales_unique
ON mv_daily_sales_summary(sale_date, type_vente);

-- Create indexes for filtering
CREATE INDEX IF NOT EXISTS idx_mv_daily_sales_date ON mv_daily_sales_summary(sale_date);
CREATE INDEX IF NOT EXISTS idx_mv_daily_sales_type ON mv_daily_sales_summary(type_vente);

-- ===============================================
-- 3. MONTHLY TOP PRODUCTS MATERIALIZED VIEW
-- ===============================================
-- Purpose: Optimize top products report queries

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_monthly_top_products AS
SELECT
    DATE_TRUNC('month', s.sale_date) as mois,
    p.id as produit_id,
    p.libelle,
    fp.code_cip,
    COUNT(DISTINCT s.id) as nb_ventes,
    SUM(sl.quantity_sold) as qte_vendue,
    SUM(sl.sales_amount) as ca_genere,
    AVG(sl.sales_amount / NULLIF(sl.quantity_sold, 0)) as prix_moyen,
    NOW() as last_updated
FROM sales_line sl
INNER JOIN sales s ON sl.sale_id = s.id
INNER JOIN produit p ON sl.produit_id = p.id
LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
WHERE s.statut = 'CLOSED'
  AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY DATE_TRUNC('month', s.sale_date), p.id, p.libelle, fp.code_cip;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_monthly_top_products_unique
ON mv_monthly_top_products(mois, produit_id);

-- Create indexes for ordering and filtering
CREATE INDEX IF NOT EXISTS idx_mv_monthly_top_products_ca ON mv_monthly_top_products(mois, ca_genere DESC);
CREATE INDEX IF NOT EXISTS idx_mv_monthly_top_products_qte ON mv_monthly_top_products(mois, qte_vendue DESC);

-- ===============================================
-- 4. PERFORMANCE INDEXES FOR REPORTS
-- ===============================================

-- Sales indexes (if not exists)
CREATE INDEX IF NOT EXISTS idx_sales_date_statut ON sales(sale_date, statut);
CREATE INDEX IF NOT EXISTS idx_sales_user_date ON sales(user_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_dtype_date ON sales(dtype, sale_date);

-- Sales lines indexes
CREATE INDEX IF NOT EXISTS idx_sales_line_produit_date ON sales_line(produit_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_line_sale_date ON sales_line(sale_id, sale_date);

-- Stock indexes
CREATE INDEX IF NOT EXISTS idx_stock_produit_qty ON stock_produit(produit_id, qty_stock);

-- Lot indexes for expiration alerts
CREATE INDEX IF NOT EXISTS idx_lot_expiry_active ON lot(expiry_date)
WHERE expiry_date IS NOT NULL AND expiry_date >= CURRENT_DATE;

-- Facture tiers payant indexes
CREATE INDEX IF NOT EXISTS idx_facture_tp_statut_date ON facture_tiers_payant(statut, invoice_date);
CREATE INDEX IF NOT EXISTS idx_facture_tp_groupe_statut ON facture_tiers_payant(groupe_tiers_payant_id, statut);

-- Cash register indexes
CREATE INDEX IF NOT EXISTS idx_cash_register_date ON cash_register(begin_time, statut);
CREATE INDEX IF NOT EXISTS idx_cash_register_user_date ON cash_register(user_id, begin_time);

-- ===============================================
-- 5. FUNCTION TO REFRESH ALL REPORT MATERIALIZED VIEWS
-- ===============================================

CREATE OR REPLACE FUNCTION refresh_all_report_views()
RETURNS void AS $$
BEGIN
    -- Refresh stock alerts view
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_alerts;

    -- Refresh daily sales summary
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_sales_summary;

    -- Refresh monthly top products
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_top_products;

    RAISE NOTICE 'All report materialized views refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- 6. INITIAL DATA REFRESH
-- ===============================================

-- Refresh all materialized views after creation
SELECT refresh_all_report_views();

-- ===============================================
-- COMMENTS FOR DOCUMENTATION
-- ===============================================

COMMENT ON MATERIALIZED VIEW mv_stock_alerts IS 'Stock alerts for ruptures, low stock, and near expiration products';
COMMENT ON MATERIALIZED VIEW mv_daily_sales_summary IS 'Daily sales summary aggregated by date and sale type';
COMMENT ON MATERIALIZED VIEW mv_monthly_top_products IS 'Monthly top-selling products by revenue and quantity';
COMMENT ON FUNCTION refresh_all_report_views() IS 'Refreshes all report materialized views - should be scheduled to run periodically';
