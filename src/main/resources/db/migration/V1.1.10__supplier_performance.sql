-- ===============================================
-- Phase 2: Supplier Performance Report
-- Version: 1.1.10
-- Description: Create materialized view for supplier performance analysis
-- ===============================================

-- ===============================================
-- 1. SUPPLIER PERFORMANCE MATERIALIZED VIEW
-- ===============================================
-- Purpose: Analyze supplier performance with delivery metrics and purchase volumes
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_supplier_performance AS
WITH supplier_orders AS (
    SELECT
        f.id as fournisseur_id,
        f.libelle as fournisseur_name,
        f.code as fournisseur_code,
        c.id as commande_id,
        c.order_date,
        c.receipt_date,
        c.final_amount,
        c.order_status,
        -- Calcul du délai de livraison en jours
        CASE
            WHEN c.receipt_date IS NOT NULL AND c.order_date IS NOT NULL THEN
                EXTRACT(DAY FROM AGE(c.receipt_date, c.order_date))
            ELSE NULL
        END as delivery_days,
        COALESCE(SUM(ol.quantity_received), 0) as total_received,
        COALESCE(SUM(ol.quantity_requested), 0) as total_requested
    FROM fournisseur f
    LEFT JOIN commande c ON f.id = c.fournisseur_id
    LEFT JOIN order_line ol ON c.id = ol.commande_id AND c.order_date = ol.order_date
    WHERE c.order_status ='RECEIVED'
    GROUP BY f.id, f.libelle, f.code, c.id, c.order_date, c.receipt_date, c.final_amount, c.order_status
),
recent_30d AS (
    SELECT
        fournisseur_id,
        COUNT(DISTINCT commande_id) as nb_orders_30d,
        SUM(final_amount) as purchase_amount_30d
    FROM supplier_orders
    WHERE order_date >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY fournisseur_id
),
recent_12m AS (
    SELECT
        fournisseur_id,
        COUNT(DISTINCT commande_id) as nb_orders_12m,
        SUM(final_amount) as purchase_amount_12m
    FROM supplier_orders
    WHERE order_date >= CURRENT_DATE - INTERVAL '12 months'
    GROUP BY fournisseur_id
),
delivery_metrics AS (
    SELECT
        fournisseur_id,
        ROUND(AVG(delivery_days), 0) as avg_delivery_days,
        MIN(delivery_days) as min_delivery_days,
        MAX(delivery_days) as max_delivery_days,
        CASE
            WHEN SUM(total_requested) > 0 THEN
                ROUND((SUM(total_received)::numeric / SUM(total_requested)::numeric) * 100, 2)
            ELSE 0
        END as conformity_rate_pct
    FROM supplier_orders
    WHERE delivery_days IS NOT NULL
      AND order_date >= CURRENT_DATE - INTERVAL '12 months'
    GROUP BY fournisseur_id
)
SELECT
  f.id as fournisseur_id,
  f.libelle as fournisseur_name,
  f.code as fournisseur_code,
  f.phone,
  f.mobile,
  COALESCE(r30.nb_orders_30d, 0) as nb_orders_last_30_days,
  COALESCE(r30.purchase_amount_30d, 0) as purchase_amount_last_30_days,
  COALESCE(r12.nb_orders_12m, 0) as nb_orders_last_12_months,
  COALESCE(r12.purchase_amount_12m, 0) as purchase_amount_last_12_months,
  COALESCE(dm.avg_delivery_days, 0) as avg_delivery_days,
  COALESCE(dm.min_delivery_days, 0) as min_delivery_days,
  COALESCE(dm.max_delivery_days, 0) as max_delivery_days,
  COALESCE(dm.conformity_rate_pct, 0) as conformity_rate_pct,
  CASE
    WHEN r12.purchase_amount_12m > 0 THEN
      ROUND(
        (LEAST(r12.purchase_amount_12m / 10000000.0, 1) * 40) +
        (GREATEST(1 - (COALESCE(dm.avg_delivery_days, 30) / 30.0), 0) * 30) +
        (COALESCE(dm.conformity_rate_pct, 0) * 0.3),
        2
      )
    ELSE 0
    END as performance_score,
  NOW() as last_updated
FROM fournisseur f
       LEFT JOIN recent_30d r30 ON f.id = r30.fournisseur_id
       LEFT JOIN recent_12m r12 ON f.id = r12.fournisseur_id
       LEFT JOIN delivery_metrics dm ON f.id = dm.fournisseur_id
WHERE COALESCE(r12.nb_orders_12m, 0) > 0; -- Only suppliers with orders in last 12 months

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_supplier_performance_unique
ON mv_supplier_performance(fournisseur_id);

-- Create indexes for filtering and sorting
CREATE INDEX IF NOT EXISTS idx_mv_supplier_performance_name ON mv_supplier_performance(fournisseur_name);
CREATE INDEX IF NOT EXISTS idx_mv_supplier_performance_purchase ON mv_supplier_performance(purchase_amount_last_12_months DESC);
CREATE INDEX IF NOT EXISTS idx_mv_supplier_performance_score ON mv_supplier_performance(performance_score DESC);
CREATE INDEX IF NOT EXISTS idx_mv_supplier_performance_delivery ON mv_supplier_performance(avg_delivery_days);

-- ===============================================
-- 2. FUNCTION TO REFRESH SUPPLIER PERFORMANCE VIEW
-- ===============================================

CREATE OR REPLACE FUNCTION refresh_supplier_performance_view()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supplier_performance;
    RAISE NOTICE 'Supplier performance view refreshed at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- 3. UPDATE GLOBAL REFRESH FUNCTION
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
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supplier_performance;

    RAISE NOTICE 'All report materialized views (Phase 1 + 2 + Supplier Performance) refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- 4. INITIAL DATA REFRESH
-- ===============================================

-- Refresh supplier performance materialized view after creation
SELECT refresh_supplier_performance_view();

-- ===============================================
-- COMMENTS FOR DOCUMENTATION
-- ===============================================

COMMENT ON MATERIALIZED VIEW mv_supplier_performance IS 'Supplier performance analysis with delivery metrics, purchase volumes, and conformity rates';
COMMENT ON FUNCTION refresh_supplier_performance_view() IS 'Refreshes supplier performance materialized view';
