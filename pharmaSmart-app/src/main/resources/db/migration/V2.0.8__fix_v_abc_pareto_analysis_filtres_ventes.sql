-- ─────────────────────────────────────────────────────────────
-- Correctif : v_abc_pareto_analysis comptait toutes les lignes de vente
--
-- Les conditions métier (vente clôturée, non annulée, rangée en chiffre
-- d'affaires, et des douze derniers mois) étaient portées par la clause ON du
-- LEFT JOIN sur `sales`. Un LEFT JOIN ne supprime pas les lignes de gauche :
-- quand la vente ne satisfaisait pas ces conditions, `s` devenait NULL mais la
-- ligne `sales_line` restait, et son montant entrait quand même dans les
-- agrégats.
--
-- Conséquence : ca_total, qte_vendue et nb_ventes intégraient les ventes
-- annulées, les ventes hors chiffre d'affaires et tout l'historique au lieu des
-- douze derniers mois. La classification ABC s'en trouvait faussée — un produit
-- arrêté depuis deux ans pouvait encore ressortir en A_PLUS et continuer d'être
-- géré comme un produit critique.
--
-- Les agrégats sont désormais gardés par FILTER (WHERE s.id IS NOT NULL) : seules
-- les lignes dont la vente a satisfait les conditions du JOIN sont comptées. Les
-- LEFT JOIN sont conservés, de sorte qu'un produit sans vente reste présent dans
-- la vue et se range en classe D, comme avant.
--
-- Le reste de la vue — classement, cumuls, seuils 60/80/95/99 — est inchangé.
-- ─────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW v_abc_pareto_analysis AS
WITH product_sales AS (
    SELECT
        p.id                                                                AS produit_id,
        p.libelle,
        fp.code_cip,
        fam.libelle                                                         AS famille,
        p.classe_criticite                                                  AS classe_actuelle,
        p.is_classification_overridden,
        COALESCE(SUM(sl.sales_amount)       FILTER (WHERE s.id IS NOT NULL), 0) AS ca_total,
        COALESCE(SUM(sl.quantity_requested) FILTER (WHERE s.id IS NOT NULL), 0) AS qte_vendue,
        COUNT(DISTINCT sl.sales_id)         FILTER (WHERE s.id IS NOT NULL)     AS nb_ventes,
        COUNT(DISTINCT DATE_TRUNC('month', s.sale_date))                    AS frequence_mois
    FROM produit p
    LEFT JOIN fournisseur_produit fp  ON fp.id  = p.fournisseur_produit_principal_id
    LEFT JOIN famille_produit fam     ON fam.id = p.famille_id
    LEFT JOIN sales_line sl           ON sl.produit_id = p.id
    LEFT JOIN sales s                 ON s.id = sl.sales_id
                                    AND s.statut    = 'CLOSED'
                                    AND s.canceled  = false
                                    AND s.ca        = 'CA'
                                    AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
    WHERE p.status       = 'ENABLE'
      AND p.type_produit <> 'DETAIL'
    GROUP BY p.id, p.libelle, fp.code_cip, fam.libelle,
             p.classe_criticite, p.is_classification_overridden
),
total_ca AS (
    SELECT NULLIF(SUM(ca_total), 0) AS ca_global
    FROM product_sales
),
ranked AS (
    SELECT
        ps.*,
        tc.ca_global,
        SUM(ps.ca_total) OVER (
            ORDER BY ps.ca_total DESC
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        )                                                                   AS ca_cumule,
        ROUND(ps.ca_total * 100.0 / NULLIF(tc.ca_global, 0), 2)           AS contribution_pct,
        ROUND(
            SUM(ps.ca_total) OVER (
                ORDER BY ps.ca_total DESC
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) * 100.0 / NULLIF(tc.ca_global, 0),
        2)                                                                  AS ca_cumule_pct,
        ROW_NUMBER() OVER (ORDER BY ps.ca_total DESC)                       AS rang
    FROM product_sales ps
    CROSS JOIN total_ca tc
)
SELECT
  produit_id,
  libelle,
  code_cip,
  famille,
  classe_actuelle,
  is_classification_overridden,
  ca_total,
  qte_vendue,
  nb_ventes,
  frequence_mois,
  ca_global,
  ca_cumule,
  contribution_pct,
  ca_cumule_pct,
  rang,
  -- 5 classes alignées avec l'enum ClasseCriticite
  -- seuils par défaut : 60 / 80 / 95 / 99 — paramétrables dans classification_config
  CASE
    WHEN ca_total = 0             THEN 'D'
    WHEN ca_cumule_pct <= 60.00   THEN 'A_PLUS'
    WHEN ca_cumule_pct <= 80.00   THEN 'A'
    WHEN ca_cumule_pct <= 95.00   THEN 'B'
    WHEN ca_cumule_pct <= 99.00   THEN 'C'
    ELSE                               'D'
    END                                                                     AS classe_pareto
FROM ranked
ORDER BY ca_total DESC;

COMMENT ON VIEW v_abc_pareto_analysis IS
    'Classification ABC Pareto 5 classes (A_PLUS/A/B/C/D) sur le CA des 12 derniers mois. '
    'Tous les produits actifs non-DETAIL sont présents (produits sans ventes → classe D). '
    'Les agrégats de vente sont gardés par FILTER (WHERE s.id IS NOT NULL) : sans ce garde, '
    'les conditions du LEFT JOIN sur sales ne filtraient rien (corrigé en V2.0.8).';
