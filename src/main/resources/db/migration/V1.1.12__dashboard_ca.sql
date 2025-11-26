-- Dashboard Chiffre d'Affaires (CA) - Materialized Views
-- Description: Materialized views for sales dashboard metrics (daily, weekly, monthly, yearly)

-- Drop existing views if they exist
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_ca_daily CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_ca_payment_methods CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_ca_product_families CASCADE;

-- ======================================================
-- 1. Daily CA Summary (with period comparisons)
-- ======================================================
CREATE MATERIALIZED VIEW mv_dashboard_ca_daily AS
WITH daily_sales AS (
    SELECT
        DATE(s.updated_at) as sale_date,
        s.id as sale_id,
        s.avoir,
        s.sales_amount,
        s.discount_amount,
        s.cost_amount,
        s.customer_id,
        (s.rest_to_pay + s.part_tiers_payant) as montant_restant,
        COALESCE((
            SELECT SUM(pt.paid_amount)
            FROM payment_transaction pt
            WHERE pt.sale_id = s.id
        ), 0) as montant_encaisse
    FROM sales s
    WHERE s.statut IN ('CLOSED')
      AND s.canceled = false
      AND s.ca = 'CA'
)
SELECT
    sale_date,
    COUNT(DISTINCT sale_id) as nb_transactions,
    COUNT(DISTINCT CASE WHEN avoir THEN sale_id END) as nb_avoirs,
    SUM(sales_amount) as ca_total,
    SUM(CASE WHEN avoir THEN sales_amount ELSE 0 END) as ca_avoirs,
    SUM(sales_amount - discount_amount) as ca_net,
    AVG(sales_amount) as panier_moyen,
    SUM(cost_amount) as cout_total,
    SUM(sales_amount - cost_amount) as marge_brute,
    CASE
        WHEN SUM(sales_amount) > 0
        THEN ROUND((SUM(sales_amount - cost_amount) * 100.0 / SUM(sales_amount))::numeric, 2)
        ELSE 0
    END as taux_marge_pct,
    COUNT(DISTINCT customer_id) as nb_clients,
    SUM(montant_encaisse) as montant_encaisse,
    SUM(montant_restant) as montant_credit
FROM daily_sales
GROUP BY sale_date;

CREATE INDEX idx_mv_dashboard_ca_daily_date ON mv_dashboard_ca_daily(sale_date DESC);

-- ======================================================
-- 2. CA by Payment Method
-- ======================================================
CREATE MATERIALIZED VIEW mv_dashboard_ca_payment_methods AS
SELECT
    DATE(pt.created_at) as payment_date,
    pm.libelle as payment_method,
    pm.code as payment_code,
    COUNT(DISTINCT pt.id) as nb_payments,
    SUM(pt.paid_amount) as montant_total,
    SUM(CASE WHEN s.avoir THEN pt.paid_amount ELSE 0 END) as montant_avoirs,
    AVG(pt.paid_amount) as montant_moyen
FROM payment_transaction pt
INNER JOIN mode_payment pm ON pt.payment_mode_id = pm.id
INNER JOIN sales s ON pt.sale_id = s.id
WHERE s.statut IN ('CLOSED')
  AND s.canceled = false
  AND s.ca = 'CA'
GROUP BY DATE(pt.created_at), pm.libelle, pm.code;

CREATE INDEX idx_mv_dashboard_ca_payment_date ON mv_dashboard_ca_payment_methods(payment_date DESC);
CREATE INDEX idx_mv_dashboard_ca_payment_code ON mv_dashboard_ca_payment_methods(payment_code);

-- ======================================================
-- 3. CA by Product Family
-- ======================================================
CREATE MATERIALIZED VIEW mv_dashboard_ca_product_families AS
WITH product_sales AS (
    SELECT
        DATE(s.updated_at) as sale_date,
        COALESCE(fp.libelle, 'Non classé') as famille,
        sl.quantity_sold,
        sl.sales_amount,
        sl.cost_amount*sl.quantity_sold,
        (sl.sales_amount - (sl.cost_amount*sl.quantity_sold)) as marge
    FROM sales s
    INNER JOIN sales_line sl ON s.id = sl.sales_id
    INNER JOIN produit p ON sl.produit_id = p.id
    LEFT JOIN famille_produit fp ON p.famille_id = fp.id
    WHERE s.statut IN ('CLOSED')
      AND s.canceled = false
      AND s.ca = 'CA'
)
SELECT
    sale_date,
    famille,
    SUM(quantity_sold) as quantite_vendue,
    SUM(sales_amount) as ca_total,
    SUM(cost_amount) as cout_total,
    SUM(marge) as marge_brute,
    CASE
        WHEN SUM(sales_amount) > 0
        THEN ROUND((SUM(marge) * 100.0 / SUM(sales_amount))::numeric, 2)
        ELSE 0
    END as taux_marge_pct,
    COUNT(*) as nb_lignes_vente
FROM product_sales
GROUP BY sale_date, famille;

CREATE INDEX idx_mv_dashboard_ca_families_date ON mv_dashboard_ca_product_families(sale_date DESC);
CREATE INDEX idx_mv_dashboard_ca_families_name ON mv_dashboard_ca_product_families(famille);

-- ======================================================
-- Grant permissions
-- ======================================================
GRANT SELECT ON mv_dashboard_ca_daily TO warehouse;
GRANT SELECT ON mv_dashboard_ca_payment_methods TO warehouse;
GRANT SELECT ON mv_dashboard_ca_product_families TO warehouse;

-- ======================================================
-- Refresh function (to be called by scheduler or manually)
-- ======================================================
CREATE OR REPLACE FUNCTION refresh_dashboard_ca_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_daily;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_payment_methods;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_product_families;
END;
$$ LANGUAGE plpgsql;

-- Initial refresh
SELECT refresh_dashboard_ca_views();
