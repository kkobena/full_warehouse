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
SELECT
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
       JOIN fournisseur_produit fpp ON p.id = fpp.produit_id
       LEFT JOIN stock_produit sp ON p.id = sp.produit_id
       LEFT JOIN order_line ol ON fpp.id = ol.fournisseur_produit_id
       LEFT JOIN lot l ON ol.id = l.order_line_id AND l.expiry_date < CURRENT_DATE + INTERVAL '3 months'
WHERE p.status = 'ENABLE'
GROUP BY p.id, p.libelle, fp.code_cip, p.qty_seuil_mini, l.expiry_date,l.statut
HAVING
  COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) = 0
    OR COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) < p.qty_seuil_mini
    OR (l.expiry_date IS NOT NULL AND l.expiry_date < CURRENT_DATE + INTERVAL '3 months' AND l.statut='AVAILABLE');

-- Create unique index on materialized view for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_stock_alerts_unique
ON mv_stock_alerts(produit_id, expiry_date)  ;

-- Create indexes for filtering
CREATE INDEX IF NOT EXISTS idx_mv_stock_alerts_type ON mv_stock_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_mv_stock_alerts_expiry ON mv_stock_alerts(expiry_date) WHERE expiry_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_date_statut ON sales(sale_date, statut);
CREATE INDEX IF NOT EXISTS idx_sales_user_date ON sales(user_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_dtype_date ON sales(dtype, sale_date);

-- Sales lines indexes
CREATE INDEX IF NOT EXISTS idx_sales_line_produit_date ON sales_line(produit_id, sale_date);



-- Lot indexes for expiration alerts
CREATE INDEX IF NOT EXISTS idx_lot_expiry_active ON lot(expiry_date) WHERE statut = 'AVAILABLE';

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


    RAISE NOTICE 'All report materialized views refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;


SELECT refresh_all_report_views();

-- ===============================================
-- COMMENTS FOR DOCUMENTATION
-- ===============================================

COMMENT ON MATERIALIZED VIEW mv_stock_alerts IS 'Stock alerts for ruptures, low stock, and near expiration products';
COMMENT ON FUNCTION refresh_all_report_views() IS 'Refreshes all report materialized views - should be scheduled to run periodically';
