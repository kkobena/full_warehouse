-- =====================================================
-- Phase 3: Intelligence Décisionnelle
-- Rapports de Rentabilité et Analyse ABC Pareto
-- =====================================================

-- =====================================================
-- 1. PROFITABILITY ANALYSIS (Analyse de Rentabilité)
-- =====================================================

-- Vue matérialisée pour l'analyse de rentabilité par produit
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_product_profitability AS
WITH product_margins AS (
    SELECT
        p.id as produit_id,
        p.libelle,
        fp.code_cip,
        f.libelle as categorie,

        -- Quantités vendues
        COUNT(DISTINCT sl.sale_id) as nb_ventes,
        SUM(sl.quantity_sold) as qte_vendue,

        -- Chiffre d'affaires
        SUM(sl.sales_amount) as ca_total,

        -- Coûts d'achat
        SUM(sl.cost_amount) as cout_achat_total,

        -- Marges
        SUM(sl.sales_amount - sl.cost_amount) as marge_brute,

        -- Taux de marge (%)
        CASE
            WHEN SUM(sl.sales_amount) > 0
            THEN ROUND((SUM(sl.sales_amount - sl.cost_amount) / SUM(sl.sales_amount)) * 100, 2)
            ELSE 0
        END as taux_marge_pct,

        -- Prix moyens
        CASE
            WHEN SUM(sl.quantity_sold) > 0
            THEN ROUND(SUM(sl.sales_amount) / SUM(sl.quantity_sold), 0)
            ELSE 0
        END as prix_vente_moyen,

        CASE
            WHEN SUM(sl.quantity_sold) > 0
            THEN ROUND(SUM(sl.cost_amount) / SUM(sl.quantity_sold), 0)
            ELSE 0
        END as prix_achat_moyen,

        -- Stock actuel
        COALESCE(sp.stock_quantity, 0) as stock_quantity,
        COALESCE(sp.cost_amount, 0) as prix_achat_unitaire,
        COALESCE(sp.regular_unit_price, 0) as prix_vente_unitaire,

        -- Taux de rotation (12 derniers mois)
        CASE
            WHEN COALESCE(sp.stock_quantity * sp.cost_amount, 0) > 0
            THEN ROUND((SUM(sl.sales_amount) / (sp.stock_quantity * sp.cost_amount)) * 12, 2)
            ELSE 0
        END as taux_rotation_annuel

    FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN famille f ON p.famille_id = f.id
    LEFT JOIN sales_line sl ON p.id = sl.produit_id
    LEFT JOIN sales s ON sl.sale_id = s.id
    LEFT JOIN stock_produit sp ON p.id = sp.produit_id

    WHERE s.statut = 'CLOSED'
      AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
      AND p.status = 'ENABLE'

    GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, sp.stock_quantity, sp.cost_amount, sp.regular_unit_price
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

-- Index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_mv_profitability_marge ON mv_product_profitability(marge_brute DESC);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_taux_marge ON mv_product_profitability(taux_marge_pct);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_bcg ON mv_product_profitability(bcg_category);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_categorie ON mv_product_profitability(categorie);

-- Rafraîchir la vue (peut être appelé manuellement ou via cron)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_product_profitability;

-- =====================================================
-- 2. ABC PARETO ANALYSIS (Analyse ABC selon Pareto 80/20)
-- =====================================================

-- Vue matérialisée pour l'analyse ABC Pareto
-- Différente de l'ABC Z-score de Phase 2 (rotation de stock)
-- Ici: classification selon contribution au CA (règle 80/20)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_abc_pareto_analysis AS
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
        COUNT(DISTINCT sl.sale_id) as nb_ventes

    FROM produit p
    LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
    LEFT JOIN famille f ON p.famille_id = f.id
    INNER JOIN sales_line sl ON p.id = sl.produit_id
    INNER JOIN sales s ON sl.sale_id = s.id

    WHERE s.statut = 'CLOSED'
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
CREATE INDEX IF NOT EXISTS idx_mv_pareto_classe ON mv_abc_pareto_analysis(classe_pareto);
CREATE INDEX IF NOT EXISTS idx_mv_pareto_ca ON mv_abc_pareto_analysis(ca_total DESC);
CREATE INDEX IF NOT EXISTS idx_mv_pareto_rang ON mv_abc_pareto_analysis(rang);
CREATE INDEX IF NOT EXISTS idx_mv_pareto_categorie ON mv_abc_pareto_analysis(categorie);

-- Rafraîchir la vue
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_abc_pareto_analysis;

-- =====================================================
-- 3. VUE AGGREGEE: SUMMARY PROFITABILITY
-- =====================================================

-- Vue pour les métriques agrégées de rentabilité
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_profitability_summary AS
SELECT
    COUNT(*) as total_produits,
    SUM(ca_total) as ca_total_global,
    SUM(cout_achat_total) as cout_achat_global,
    SUM(marge_brute) as marge_brute_globale,
    ROUND(AVG(taux_marge_pct), 2) as taux_marge_moyen,

    -- Par catégorie BCG
    COUNT(*) FILTER (WHERE bcg_category = 'STAR') as nb_stars,
    COUNT(*) FILTER (WHERE bcg_category = 'CASH_COW') as nb_cash_cows,
    COUNT(*) FILTER (WHERE bcg_category = 'QUESTION_MARK') as nb_question_marks,
    COUNT(*) FILTER (WHERE bcg_category = 'DOG') as nb_dogs,

    -- CA par catégorie BCG
    SUM(ca_total) FILTER (WHERE bcg_category = 'STAR') as ca_stars,
    SUM(ca_total) FILTER (WHERE bcg_category = 'CASH_COW') as ca_cash_cows,
    SUM(ca_total) FILTER (WHERE bcg_category = 'QUESTION_MARK') as ca_question_marks,
    SUM(ca_total) FILTER (WHERE bcg_category = 'DOG') as ca_dogs,

    -- Marges par catégorie BCG
    SUM(marge_brute) FILTER (WHERE bcg_category = 'STAR') as marge_stars,
    SUM(marge_brute) FILTER (WHERE bcg_category = 'CASH_COW') as marge_cash_cows,
    SUM(marge_brute) FILTER (WHERE bcg_category = 'QUESTION_MARK') as marge_question_marks,
    SUM(marge_brute) FILTER (WHERE bcg_category = 'DOG') as marge_dogs

FROM mv_product_profitability;

REFRESH MATERIALIZED VIEW mv_profitability_summary;

-- =====================================================
-- 4. VUE AGGREGEE: ABC PARETO SUMMARY
-- =====================================================

-- Vue pour les métriques agrégées de l'analyse Pareto
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_pareto_summary AS
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

REFRESH MATERIALIZED VIEW mv_pareto_summary;

-- =====================================================
-- 5. COMMENTS
-- =====================================================

COMMENT ON MATERIALIZED VIEW mv_product_profitability IS
'Analyse de rentabilité par produit avec classification BCG (STAR, CASH_COW, QUESTION_MARK, DOG)';

COMMENT ON MATERIALIZED VIEW mv_abc_pareto_analysis IS
'Analyse ABC Pareto selon la règle 80/20: Classe A (80% CA), B (15% CA), C (5% CA)';

COMMENT ON MATERIALIZED VIEW mv_profitability_summary IS
'Métriques agrégées de rentabilité et distribution BCG';

COMMENT ON MATERIALIZED VIEW mv_pareto_summary IS
'Métriques agrégées de l''analyse ABC Pareto';

-- =====================================================
-- 6. GRANT PERMISSIONS (si nécessaire)
-- =====================================================

-- Donner les droits de lecture aux utilisateurs de l'application
-- GRANT SELECT ON mv_product_profitability TO warehouse_app;
-- GRANT SELECT ON mv_abc_pareto_analysis TO warehouse_app;
-- GRANT SELECT ON mv_profitability_summary TO warehouse_app;
-- GRANT SELECT ON mv_pareto_summary TO warehouse_app;
