-- Drop the existing materialized view with incorrect column names
DROP MATERIALIZED VIEW IF EXISTS mv_product_profitability CASCADE;

-- Recreate with correct column references
CREATE MATERIALIZED VIEW mv_product_profitability AS
WITH product_margins AS (
    SELECT
        p.id as produit_id,
        p.libelle,
        fp.code_cip,
        f.libelle as categorie,

        -- Quantités vendues
        COUNT(DISTINCT sl.sales_id) as nb_ventes,
        SUM(sl.quantity_requested) as qte_vendue,

        -- Chiffre d'affaires
        SUM(sl.sales_amount) as ca_total,

        -- Coûts d'achat
        SUM(sl.cost_amount*sl.quantity_requested) as cout_achat_total,

        -- Marges
        SUM(sl.sales_amount - (sl.cost_amount*sl.quantity_requested)) as marge_brute,

        -- Taux de marge (%)
        CASE
            WHEN SUM(sl.sales_amount) > 0
            THEN ROUND((SUM(sl.sales_amount - (sl.cost_amount*sl.quantity_requested)) / SUM(sl.sales_amount)) * 100, 2)
            ELSE 0
        END as taux_marge_pct,

        -- Prix moyens
        CASE
            WHEN SUM(sl.quantity_requested) > 0
            THEN ROUND(SUM(sl.sales_amount) / SUM(sl.quantity_requested), 0)
            ELSE 0
        END as prix_vente_moyen,

        CASE
            WHEN SUM(sl.quantity_requested) > 0
            THEN ROUND(SUM((sl.cost_amount*sl.quantity_requested)) / SUM(sl.quantity_requested), 0)
            ELSE 0
        END as prix_achat_moyen,

        -- Stock actuel (from stock_produit - qty_stock)
        COALESCE(SUM(sp.qty_stock), 0) as stock_quantity,
        -- Prix unitaires (from fournisseur_produit)
        COALESCE(fp.prix_achat, 0) as prix_achat_unitaire,
        COALESCE(fp.prix_uni, 0) as prix_vente_unitaire,

        -- Taux de rotation (12 derniers mois)
        CASE
            WHEN COALESCE(SUM(sp.qty_stock) * fp.prix_achat, 0) > 0
            THEN ROUND((SUM(sl.sales_amount) / (SUM(sp.qty_stock) * fp.prix_achat)) * 12, 2)
            ELSE 0
        END as taux_rotation_annuel

    FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN famille_produit f ON p.famille_id = f.id
    LEFT JOIN sales_line sl ON p.id = sl.produit_id
    LEFT JOIN sales s ON sl.sales_id = s.id
    LEFT JOIN stock_produit sp ON p.id = sp.produit_id

    WHERE s.statut = 'CLOSED'
      AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
      AND p.status = 'ENABLE'
      AND s.canceled = false
      AND s.ca = 'CA'

    GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat, fp.prix_uni
),
bcg_classification AS (
    SELECT
        *,
        -- Classification BCG Matrix
        CASE
            WHEN taux_marge_pct >= 20 AND taux_rotation_annuel >= 6 THEN 'STAR'
            WHEN taux_marge_pct >= 20 AND taux_rotation_annuel < 6 THEN 'CASH_COW'
            WHEN taux_marge_pct < 20 AND taux_rotation_annuel >= 6 THEN 'QUESTION_MARK'
            WHEN taux_marge_pct < 20 AND taux_rotation_annuel < 6 THEN 'DOG'
            ELSE 'UNDEFINED'
        END as bcg_category
    FROM product_margins
)
SELECT * FROM bcg_classification
WHERE ca_total > 0  -- Exclure les produits sans ventes
ORDER BY marge_brute DESC;



DROP MATERIALIZED VIEW IF EXISTS mv_monthly_top_products CASCADE;

CREATE MATERIALIZED VIEW mv_monthly_top_products AS
SELECT
  TO_CHAR(DATE_TRUNC('month', s.sale_date), 'YYYY-MM-DD')as mois,
  p.id as produit_id,
  p.libelle,
  fp.code_cip,
  COUNT(DISTINCT s.id) as nb_ventes,
  SUM(sl.quantity_requested) as qte_vendue,
  SUM(sl.sales_amount) as ca_genere,
  AVG(sl.sales_amount / NULLIF(sl.quantity_requested, 0)) as prix_moyen,
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
      SUM(sl.quantity_requested) as qty_sold,
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
      SUM(sl.quantity_requested) as qty_sold
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
    SUM(sl.quantity_requested) as qte_vendue,

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
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_abc_pareto_unique
  ON mv_abc_pareto_analysis(produit_id);

-- Add comment
COMMENT ON INDEX idx_mv_abc_pareto_unique IS 'Unique index required for concurrent refresh of mv_abc_pareto_analysis';


REFRESH MATERIALIZED VIEW mv_daily_sales_summary;
REFRESH MATERIALIZED VIEW mv_monthly_top_products;
REFRESH MATERIALIZED VIEW mv_stock_rotation;
REFRESH MATERIALIZED VIEW mv_abc_pareto_analysis;
REFRESH MATERIALIZED VIEW mv_pareto_summary;






















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


