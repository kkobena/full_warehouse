-- ===============================================
-- Add filters for canceled sales and categorieChiffreAffaire
-- Version: 1.1.9
-- Description: Update materialized views to exclude canceled sales
--              and filter by CategorieChiffreAffaire = 'CA'
-- ===============================================



DROP MATERIALIZED VIEW IF EXISTS mv_daily_sales_summary CASCADE;

CREATE MATERIALIZED VIEW mv_daily_sales_summary AS
SELECT
    s.sale_date as sale_date,
    s.dtype as type_vente,
    COUNT(*) as nb_ventes,
    SUM(s.sales_amount) as ca_total,
    SUM(s.sales_amount - s.discount_amount) as ca_net,
    AVG(s.sales_amount) as panier_moyen,
    SUM(s.discount_amount) as total_remises,
    NOW() as last_updated
FROM sales s
WHERE s.statut = 'CLOSED'
  AND s.canceled = false
  AND s.ca = 'CA'
GROUP BY s.sale_date, s.dtype;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_daily_sales_unique
ON mv_daily_sales_summary(sale_date, type_vente);

-- Create indexes for filtering
CREATE INDEX idx_mv_daily_sales_date ON mv_daily_sales_summary(sale_date);
CREATE INDEX idx_mv_daily_sales_type ON mv_daily_sales_summary(type_vente);

-- ===============================================
-- 2. UPDATE mv_monthly_top_products
-- ===============================================

DROP MATERIALIZED VIEW IF EXISTS mv_monthly_top_products CASCADE;

CREATE MATERIALIZED VIEW mv_monthly_top_products AS
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
INNER JOIN sales s ON sl.sales_id = s.id
INNER JOIN produit p ON sl.produit_id = p.id
LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
WHERE s.statut = 'CLOSED'
  AND s.canceled = false
  AND s.ca = 'CA'
  AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY DATE_TRUNC('month', s.sale_date), p.id, p.libelle, fp.code_cip;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_monthly_top_products_unique
ON mv_monthly_top_products(mois, produit_id);

-- Create indexes for ordering and filtering
CREATE INDEX idx_mv_monthly_top_products_ca ON mv_monthly_top_products(mois, ca_genere DESC);
CREATE INDEX idx_mv_monthly_top_products_qte ON mv_monthly_top_products(mois, qte_vendue DESC);

-- ===============================================
-- 3. UPDATE mv_stock_rotation (two sub-queries)
-- ===============================================

DROP MATERIALIZED VIEW IF EXISTS mv_stock_rotation CASCADE;

CREATE MATERIALIZED VIEW mv_stock_rotation AS
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
        INNER JOIN sales s ON sl.sales_id = s.id
        WHERE s.statut = 'CLOSED'
          AND s.canceled = false
          AND s.ca = 'CA'
          AND s.sale_date >= CURRENT_DATE - INTERVAL '30 days'
        GROUP BY sl.produit_id
    ) sales_30d ON p.id = sales_30d.produit_id
    LEFT JOIN (
        SELECT
            sl.produit_id,
            SUM(sl.sales_amount) as ca,
            SUM(sl.quantity_sold) as qty_sold
        FROM sales_line sl
        INNER JOIN sales s ON sl.sales_id = s.id
        WHERE s.statut = 'CLOSED'
          AND s.canceled = false
          AND s.ca = 'CA'
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
CREATE UNIQUE INDEX idx_mv_stock_rotation_unique
ON mv_stock_rotation(produit_id);

-- Create indexes for filtering
CREATE INDEX idx_mv_stock_rotation_category ON mv_stock_rotation(categorie);
CREATE INDEX idx_mv_stock_rotation_abc ON mv_stock_rotation(categorie_abc);
CREATE INDEX idx_mv_stock_rotation_rate ON mv_stock_rotation(rotation_rate_annual DESC);

-- ===============================================
-- 4. UPDATE mv_customer_rfm
-- ===============================================

DROP MATERIALIZED VIEW IF EXISTS mv_customer_rfm CASCADE;

CREATE MATERIALIZED VIEW mv_customer_rfm AS
WITH customer_metrics AS (
    SELECT
        c.id as customer_id,
        c.first_name || ' ' || c.last_name as customer_name,
        c.phone,
        MAX(s.sale_date) as last_purchase_date,
          EXTRACT(DAY FROM AGE(CURRENT_DATE, MAX(s.sale_date))) as days_since_last_purchase,
        COUNT(DISTINCT s.id) as nb_purchases_last_year,
        SUM(s.sales_amount) as total_spent_last_year,
        AVG(s.sales_amount) as avg_basket_value
    FROM customer c
    LEFT JOIN sales s ON c.id = s.customer_id
    WHERE s.sale_date >= CURRENT_DATE - INTERVAL '1 year'
      AND s.statut = 'CLOSED'
      AND s.canceled = false
      AND s.ca = 'CA'
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
CREATE UNIQUE INDEX idx_mv_customer_rfm_unique
ON mv_customer_rfm(customer_id);

-- Create indexes for filtering and analysis
CREATE INDEX idx_mv_customer_rfm_classification ON mv_customer_rfm(customer_classification);
CREATE INDEX idx_mv_customer_rfm_segment ON mv_customer_rfm(rfm_segment DESC);
CREATE INDEX idx_mv_customer_rfm_recency ON mv_customer_rfm(recency_score DESC);
CREATE INDEX idx_mv_customer_rfm_frequency ON mv_customer_rfm(frequency_score DESC);
CREATE INDEX idx_mv_customer_rfm_monetary ON mv_customer_rfm(monetary_score DESC);

-- ===============================================
-- 6. UPDATE mv_abc_pareto_analysis
-- ===============================================

DROP MATERIALIZED VIEW IF EXISTS mv_pareto_summary CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_abc_pareto_analysis CASCADE;

CREATE MATERIALIZED VIEW mv_abc_pareto_analysis AS
WITH product_sales AS (
    SELECT
        p.id as produit_id,
        p.libelle,
        fp.code_cip,
        f.libelle as categorie,

        -- CA généré
        SUM(sl.sales_amount) as ca_total,

        -- Quantité vendue
        SUM(sl.quantity_sold) as qte_vendue,

        -- Nombre de ventes
        COUNT(DISTINCT sl.sales_id) as nb_ventes

    FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN famille_produit f ON p.famille_id = f.id
    INNER JOIN sales_line sl ON p.id = sl.produit_id
    INNER JOIN sales s ON sl.sales_id = s.id

    WHERE s.statut = 'CLOSED'
      AND s.canceled = false
      AND s.ca = 'CA'
      AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
      AND p.status = 'ENABLE'

    GROUP BY p.id, p.libelle, fp.code_cip, f.libelle

    HAVING SUM(sl.sales_amount) > 0
),
total_ca AS (
    SELECT SUM(ca_total) as ca_global
    FROM product_sales
),
cumulative_ca AS (
    SELECT
        ps.*,
        tc.ca_global,
        SUM(ps.ca_total) OVER (ORDER BY ps.ca_total DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as ca_cumule,
        ROUND((ps.ca_total / tc.ca_global) * 100, 2) as contribution_pct,
        ROUND((SUM(ps.ca_total) OVER (ORDER BY ps.ca_total DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) / tc.ca_global) * 100, 2) as ca_cumule_pct
    FROM product_sales ps
    CROSS JOIN total_ca tc
),
pareto_classification AS (
    SELECT
        *,
        -- Classification Pareto ABC
        CASE
            WHEN ca_cumule_pct <= 80 THEN 'A'
            WHEN ca_cumule_pct <= 95 THEN 'B'
            ELSE 'C'
        END as classe_pareto,

        -- Rang du produit
        ROW_NUMBER() OVER (ORDER BY ca_total DESC) as rang
    FROM cumulative_ca
)
SELECT * FROM pareto_classification
ORDER BY ca_total DESC;

-- Index pour optimiser les requêtes
CREATE INDEX idx_mv_pareto_classe ON mv_abc_pareto_analysis(classe_pareto);
CREATE INDEX idx_mv_pareto_ca ON mv_abc_pareto_analysis(ca_total DESC);
CREATE INDEX idx_mv_pareto_rang ON mv_abc_pareto_analysis(rang);
CREATE INDEX idx_mv_pareto_categorie ON mv_abc_pareto_analysis(categorie);


-- Vue pour les métriques agrégées de l'analyse Pareto
CREATE MATERIALIZED VIEW mv_pareto_summary AS
SELECT
    COUNT(*) as total_produits,
    SUM(ca_total) as ca_global,

    -- Classe A (80% du CA)
    COUNT(*) FILTER (WHERE classe_pareto = 'A') as nb_produits_a,
    SUM(ca_total) FILTER (WHERE classe_pareto = 'A') as ca_classe_a,
    ROUND((SUM(ca_total) FILTER (WHERE classe_pareto = 'A') / SUM(ca_total)) * 100, 2) as pct_ca_classe_a,

    -- Classe B (15% du CA)
    COUNT(*) FILTER (WHERE classe_pareto = 'B') as nb_produits_b,
    SUM(ca_total) FILTER (WHERE classe_pareto = 'B') as ca_classe_b,
    ROUND((SUM(ca_total) FILTER (WHERE classe_pareto = 'B') / SUM(ca_total)) * 100, 2) as pct_ca_classe_b,

    -- Classe C (5% du CA)
    COUNT(*) FILTER (WHERE classe_pareto = 'C') as nb_produits_c,
    SUM(ca_total) FILTER (WHERE classe_pareto = 'C') as ca_classe_c,
    ROUND((SUM(ca_total) FILTER (WHERE classe_pareto = 'C') / SUM(ca_total)) * 100, 2) as pct_ca_classe_c

FROM mv_abc_pareto_analysis;

-- ===============================================
-- 8. REFRESH ALL UPDATED VIEWS
-- ===============================================

REFRESH MATERIALIZED VIEW mv_daily_sales_summary;
REFRESH MATERIALIZED VIEW mv_monthly_top_products;
REFRESH MATERIALIZED VIEW mv_stock_rotation;
REFRESH MATERIALIZED VIEW mv_customer_rfm;
REFRESH MATERIALIZED VIEW mv_abc_pareto_analysis;
REFRESH MATERIALIZED VIEW mv_pareto_summary;

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
