-- =====================================================
-- Fix V1.1.8: Correct column references from stock_produit
-- =====================================================

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
        COUNT(DISTINCT sl.sale_id) as nb_ventes,
        SUM(sl.quantity_sold) as qte_vendue,

        -- Chiffre d'affaires
        SUM(sl.sales_amount) as ca_total,

        -- Coûts d'achat
        SUM(sl.cost_amount*sl.quantity_sold) as cout_achat_total,

        -- Marges
        SUM(sl.sales_amount - (sl.cost_amount*sl.quantity_sold)) as marge_brute,

        -- Taux de marge (%)
        CASE
            WHEN SUM(sl.sales_amount) > 0
            THEN ROUND((SUM(sl.sales_amount - (sl.cost_amount*sl.quantity_sold)) / SUM(sl.sales_amount)) * 100, 2)
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
            THEN ROUND(SUM((sl.cost_amount*sl.quantity_sold)) / SUM(sl.quantity_sold), 0)
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
    LEFT JOIN famille f ON p.famille_id = f.id
    LEFT JOIN sales_line sl ON p.id = sl.produit_id
    LEFT JOIN sales s ON sl.sale_id = s.id
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

-- Recreate indexes
CREATE INDEX IF NOT EXISTS idx_mv_profitability_marge ON mv_product_profitability(marge_brute DESC);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_taux_marge ON mv_product_profitability(taux_marge_pct);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_bcg ON mv_product_profitability(bcg_category);
CREATE INDEX IF NOT EXISTS idx_mv_profitability_categorie ON mv_product_profitability(categorie);

-- Recreate profitability summary view
DROP MATERIALIZED VIEW IF EXISTS mv_profitability_summary;

CREATE MATERIALIZED VIEW mv_profitability_summary AS
SELECT
    SUM(ca_total) as ca_total_global,
    SUM(marge_brute) as marge_brute_globale,
    CASE
        WHEN SUM(ca_total) > 0
        THEN ROUND((SUM(marge_brute) / SUM(ca_total)) * 100, 2)
        ELSE 0
    END as taux_marge_moyen,
    COUNT(*) as total_produits,

    -- Stars
    COUNT(*) FILTER (WHERE bcg_category = 'STAR') as nb_stars,
    COALESCE(SUM(ca_total) FILTER (WHERE bcg_category = 'STAR'), 0) as ca_stars,
    COALESCE(SUM(marge_brute) FILTER (WHERE bcg_category = 'STAR'), 0) as marge_stars,

    -- Cash Cows
    COUNT(*) FILTER (WHERE bcg_category = 'CASH_COW') as nb_cash_cows,
    COALESCE(SUM(ca_total) FILTER (WHERE bcg_category = 'CASH_COW'), 0) as ca_cash_cows,
    COALESCE(SUM(marge_brute) FILTER (WHERE bcg_category = 'CASH_COW'), 0) as marge_cash_cows,

    -- Question Marks
    COUNT(*) FILTER (WHERE bcg_category = 'QUESTION_MARK') as nb_question_marks,
    COALESCE(SUM(ca_total) FILTER (WHERE bcg_category = 'QUESTION_MARK'), 0) as ca_question_marks,
    COALESCE(SUM(marge_brute) FILTER (WHERE bcg_category = 'QUESTION_MARK'), 0) as marge_question_marks,

    -- Dogs
    COUNT(*) FILTER (WHERE bcg_category = 'DOG') as nb_dogs,
    COALESCE(SUM(ca_total) FILTER (WHERE bcg_category = 'DOG'), 0) as ca_dogs,
    COALESCE(SUM(marge_brute) FILTER (WHERE bcg_category = 'DOG'), 0) as marge_dogs,

    NOW() as last_updated
FROM mv_product_profitability;

-- Refresh both views
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_product_profitability;
REFRESH MATERIALIZED VIEW mv_profitability_summary;
