
-- Recreate profitability summary view
DROP MATERIALIZED VIEW IF EXISTS mv_profitability_summary;

CREATE MATERIALIZED VIEW mv_profitability_summary AS
SELECT
    SUM(ca_total) as ca_total_global,
    SUM(cout_achat_total) as cout_achat_global,
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

REFRESH MATERIALIZED VIEW mv_profitability_summary;
