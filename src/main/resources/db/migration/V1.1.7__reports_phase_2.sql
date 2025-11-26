-- ===============================================
-- Phase 2: Rapports Statistiques - Optimisation Opérationnelle
-- Version: 1.1.7
-- Description: Create materialized views and indexes for advanced reports
-- ===============================================

-- ===============================================
-- 1. STOCK VALUATION MATERIALIZED VIEW
-- ===============================================
-- Purpose: Calculate stock value (purchase vs sales price) for financial reporting

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_stock_valuation AS
SELECT
    p.id as produit_id,
    p.libelle,
    fp.code_cip,
    f.libelle as categorie,
    s.libelle as storage_location,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) as stock_quantity,
    fp.prix_achat as purchase_price,
    fp.prix_uni as sales_price,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat as total_purchase_value,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_uni as total_sales_value,
    (COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_uni) -
    (COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat) as potential_margin,
    CASE
        WHEN fp.prix_uni > 0 THEN
            ROUND(((fp.prix_uni - fp.prix_achat) / fp.prix_uni) * 100, 2)
        ELSE 0
    END as margin_percentage,
    NOW() as last_updated
FROM produit p
LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
LEFT JOIN stock_produit sp ON p.id = sp.produit_id
LEFT JOIN famille_produit f ON p.famille_id = f.id
LEFT JOIN storage s ON sp.storage_id = s.id
WHERE p.status = 'ENABLE'
  AND COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) > 0
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, s.libelle,
         fp.prix_achat, fp.prix_uni;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_stock_valuation_unique
ON mv_stock_valuation(produit_id);

-- Create indexes for filtering and sorting
CREATE INDEX IF NOT EXISTS idx_mv_stock_valuation_category ON mv_stock_valuation(categorie);
CREATE INDEX IF NOT EXISTS idx_mv_stock_valuation_storage ON mv_stock_valuation(storage_location);
CREATE INDEX IF NOT EXISTS idx_mv_stock_valuation_value ON mv_stock_valuation(total_sales_value DESC);

-- ===============================================
-- 2. STOCK ROTATION MATERIALIZED VIEW
-- ===============================================
-- Purpose: Calculate stock rotation rates using ABC analysis with Z-score

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_stock_rotation AS
WITH product_sales AS (
    SELECT
        p.id as produit_id,
        p.libelle,
        fp.code_cip,
        f.libelle as categorie,
        COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) as stock_quantity,
        fp.prix_achat as unit_cost,
        COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat as stock_value,
        COALESCE(sales_30d.ca, 0) as ca_last_30_days,
        COALESCE(sales_30d.qty_sold, 0) as qty_sold_last_30_days,
        COALESCE(sales_30d.nb_sales, 0) as nb_sales_last_30_days,
        COALESCE(sales_12m.ca, 0) as ca_last_12_months,
        COALESCE(sales_12m.qty_sold, 0) as qty_sold_last_12_months,
        CASE
            WHEN COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat > 0 THEN
                ROUND((COALESCE(sales_12m.ca, 0) / (COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat)), 2)
            ELSE 0
        END as rotation_rate_annual,
        CASE
            WHEN COALESCE(sales_12m.ca, 0) > 0 AND (COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat) > 0 THEN
                ROUND(365 / NULLIF((COALESCE(sales_12m.ca, 0) / (COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * fp.prix_achat)), 0), 0)
            ELSE 999
        END as avg_days_in_stock
    FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN stock_produit sp ON p.id = sp.produit_id
    LEFT JOIN famille_produit f ON p.famille_id = f.id
    LEFT JOIN (
        SELECT
            sl.produit_id,
            SUM(sl.sales_amount) as ca,
            SUM(sl.quantity_sold) as qty_sold,
            COUNT(DISTINCT s.id) as nb_sales
        FROM sales_line sl
        INNER JOIN sales s ON sl.sale_id = s.id
        WHERE s.statut = 'CLOSED'
          AND s.sale_date >= CURRENT_DATE - INTERVAL '30 days'
        GROUP BY sl.produit_id
    ) sales_30d ON p.id = sales_30d.produit_id
    LEFT JOIN (
        SELECT
            sl.produit_id,
            SUM(sl.sales_amount) as ca,
            SUM(sl.quantity_sold) as qty_sold
        FROM sales_line sl
        INNER JOIN sales s ON sl.sale_id = s.id
        WHERE s.statut = 'CLOSED'
          AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
        GROUP BY sl.produit_id
    ) sales_12m ON p.id = sales_12m.produit_id
    WHERE p.status = 'ENABLE'
    GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat,
             sales_30d.ca, sales_30d.qty_sold, sales_30d.nb_sales,
             sales_12m.ca, sales_12m.qty_sold
),
sales_stats AS (
    SELECT
        AVG(ca_last_12_months) as avg_ca,
        STDDEV_POP(ca_last_12_months) as stddev_ca
    FROM product_sales
    WHERE ca_last_12_months > 0
)
SELECT
    ps.produit_id,
    ps.libelle,
    ps.code_cip,
    ps.categorie,
    ps.stock_quantity,
    ps.unit_cost,
    ps.stock_value,
    ps.ca_last_30_days,
    ps.qty_sold_last_30_days,
    ps.nb_sales_last_30_days,
    ps.ca_last_12_months,
    ps.qty_sold_last_12_months,
    ps.rotation_rate_annual,
    ps.avg_days_in_stock,
    -- ABC Classification using Z-score
    CASE
        WHEN ps.ca_last_12_months = 0 THEN 'C'
        WHEN ss.stddev_ca = 0 OR ss.stddev_ca IS NULL THEN 'B'
        WHEN (ps.ca_last_12_months - ss.avg_ca) / NULLIF(ss.stddev_ca, 0) >= 1.96 THEN 'A'
        WHEN (ps.ca_last_12_months - ss.avg_ca) / NULLIF(ss.stddev_ca, 0) >= 1.65 THEN 'B'
        ELSE 'C'
    END as categorie_abc,
    NOW() as last_updated
FROM product_sales ps
CROSS JOIN sales_stats ss;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_stock_rotation_unique
ON mv_stock_rotation(produit_id);

-- Create indexes for filtering
CREATE INDEX IF NOT EXISTS idx_mv_stock_rotation_category ON mv_stock_rotation(categorie);
CREATE INDEX IF NOT EXISTS idx_mv_stock_rotation_abc ON mv_stock_rotation(categorie_abc);
CREATE INDEX IF NOT EXISTS idx_mv_stock_rotation_rate ON mv_stock_rotation(rotation_rate_annual DESC);

-- ===============================================
-- 3. CUSTOMER RFM SEGMENTATION MATERIALIZED VIEW
-- ===============================================
-- Purpose: Customer segmentation using RFM (Recency, Frequency, Monetary) analysis

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_customer_rfm AS
WITH customer_metrics AS (
    SELECT
        c.id as customer_id,
        c.first_name || ' ' || c.last_name as customer_name,
        c.phone,
        MAX(s.sale_date) as last_purchase_date,
        EXTRACT(DAY FROM (CURRENT_DATE - MAX(s.sale_date))) as days_since_last_purchase,
        COUNT(DISTINCT s.id) as nb_purchases_last_year,
        SUM(s.sales_amount) as total_spent_last_year,
        AVG(s.sales_amount) as avg_basket_value
    FROM customer c
    LEFT JOIN sales s ON c.id = s.customer_id
    WHERE s.sale_date >= CURRENT_DATE - INTERVAL '1 year'
      AND s.statut = 'CLOSED'
    GROUP BY c.id, c.first_name, c.last_name, c.phone
),
rfm_scores AS (
    SELECT
        *,
        -- Recency Score (1-5, lower days = higher score)
        CASE
            WHEN days_since_last_purchase <= 30 THEN 5
            WHEN days_since_last_purchase <= 60 THEN 4
            WHEN days_since_last_purchase <= 90 THEN 3
            WHEN days_since_last_purchase <= 180 THEN 2
            ELSE 1
        END as recency_score,

        -- Frequency Score (1-5, more purchases = higher score)
        CASE
            WHEN nb_purchases_last_year >= 20 THEN 5
            WHEN nb_purchases_last_year >= 10 THEN 4
            WHEN nb_purchases_last_year >= 5 THEN 3
            WHEN nb_purchases_last_year >= 2 THEN 2
            ELSE 1
        END as frequency_score,

        -- Monetary Score (1-5, higher spending = higher score)
        CASE
            WHEN total_spent_last_year >= 500000 THEN 5
            WHEN total_spent_last_year >= 200000 THEN 4
            WHEN total_spent_last_year >= 100000 THEN 3
            WHEN total_spent_last_year >= 50000 THEN 2
            ELSE 1
        END as monetary_score
    FROM customer_metrics
)
SELECT
    customer_id,
    customer_name,
    phone,
    last_purchase_date,
    days_since_last_purchase,
    nb_purchases_last_year,
    total_spent_last_year,
    avg_basket_value,
    recency_score,
    frequency_score,
    monetary_score,
    -- RFM Segment (e.g., 555 = best customers)
    (recency_score * 100 + frequency_score * 10 + monetary_score) as rfm_segment,

    -- Customer Classification
    CASE
        WHEN recency_score >= 4 AND frequency_score >= 4 AND monetary_score >= 4 THEN 'CHAMPION'
        WHEN recency_score >= 4 AND frequency_score >= 3 THEN 'LOYAL'
        WHEN recency_score >= 4 AND monetary_score >= 4 THEN 'BIG_SPENDER'
        WHEN recency_score >= 4 THEN 'ACTIVE'
        WHEN recency_score = 3 THEN 'AT_RISK'
        WHEN recency_score <= 2 AND frequency_score >= 3 THEN 'NEED_ATTENTION'
        ELSE 'INACTIVE'
    END as customer_classification,

    NOW() as last_updated
FROM rfm_scores;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_customer_rfm_unique
ON mv_customer_rfm(customer_id);

-- Create indexes for filtering and analysis
CREATE INDEX IF NOT EXISTS idx_mv_customer_rfm_classification ON mv_customer_rfm(customer_classification);
CREATE INDEX IF NOT EXISTS idx_mv_customer_rfm_segment ON mv_customer_rfm(rfm_segment DESC);
CREATE INDEX IF NOT EXISTS idx_mv_customer_rfm_recency ON mv_customer_rfm(recency_score DESC);
CREATE INDEX IF NOT EXISTS idx_mv_customer_rfm_frequency ON mv_customer_rfm(frequency_score DESC);
CREATE INDEX IF NOT EXISTS idx_mv_customer_rfm_monetary ON mv_customer_rfm(monetary_score DESC);

-- ===============================================
-- 4. PERFORMANCE INDEXES FOR PHASE 2 REPORTS
-- ===============================================

-- Commande (order) indexes for supplier performance
CREATE INDEX IF NOT EXISTS idx_commande_fournisseur_date ON commande(fournisseur_id, created_at);
CREATE INDEX IF NOT EXISTS idx_commande_statut_date ON commande(statut, created_at);
CREATE INDEX IF NOT EXISTS idx_commande_order_date ON commande(order_date) WHERE order_date IS NOT NULL;

-- Order line indexes
CREATE INDEX IF NOT EXISTS idx_order_line_commande ON order_line(commande_id);
CREATE INDEX IF NOT EXISTS idx_order_line_produit ON order_line(produit_id);

-- Stock produit indexes for valuation
CREATE INDEX IF NOT EXISTS idx_stock_produit_storage ON stock_produit(storage_id, produit_id);

-- Customer indexes for segmentation
CREATE INDEX IF NOT EXISTS idx_customer_status ON customer(code) WHERE code IS NOT NULL;

-- Sales indexes for rotation analysis (if not exists from Phase 1)
CREATE INDEX IF NOT EXISTS idx_sales_customer_date ON sales(customer_id, sale_date) WHERE statut = 'CLOSED';

-- ===============================================
-- 5. FUNCTION TO REFRESH PHASE 2 MATERIALIZED VIEWS
-- ===============================================

CREATE OR REPLACE FUNCTION refresh_phase2_report_views()
RETURNS void AS $$
BEGIN
    -- Refresh stock valuation view
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_valuation;
    RAISE NOTICE 'Stock valuation view refreshed at %', NOW();

    -- Refresh stock rotation view
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_rotation;
    RAISE NOTICE 'Stock rotation view refreshed at %', NOW();

    -- Refresh customer RFM view
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_rfm;
    RAISE NOTICE 'Customer RFM view refreshed at %', NOW();

    RAISE NOTICE 'All Phase 2 report materialized views refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- 6. UPDATE GLOBAL REFRESH FUNCTION
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

    RAISE NOTICE 'All report materialized views (Phase 1 + 2) refreshed successfully at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- ===============================================
-- 7. INITIAL DATA REFRESH
-- ===============================================

-- Refresh all Phase 2 materialized views after creation
SELECT refresh_phase2_report_views();

-- ===============================================
-- COMMENTS FOR DOCUMENTATION
-- ===============================================

COMMENT ON MATERIALIZED VIEW mv_stock_valuation IS 'Stock valuation with purchase and sales prices for financial reporting';
COMMENT ON MATERIALIZED VIEW mv_stock_rotation IS 'Stock rotation analysis with movement classification (slow/fast/normal)';
COMMENT ON MATERIALIZED VIEW mv_customer_rfm IS 'Customer segmentation using RFM (Recency, Frequency, Monetary) analysis';
COMMENT ON FUNCTION refresh_phase2_report_views() IS 'Refreshes all Phase 2 report materialized views';
