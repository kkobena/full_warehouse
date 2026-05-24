
-- ─────────────────────────────────────────────────────────────
-- 1. Suppression des vues matérialisées
-- ─────────────────────────────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_pareto_summary       CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_abc_pareto_analysis  CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_stock_rotation       CASCADE;

-- ─────────────────────────────────────────────────────────────
-- 2. v_abc_pareto_analysis — vue ordinaire, 5 classes, tous produits actifs
--    Source principale pour ClassificationCriticiteService (chargement Pareto en 1 requête).
--    Inclut les produits sans ventes (LEFT JOIN) → classe D visible.
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
        COALESCE(SUM(sl.sales_amount),        0)                           AS ca_total,
        COALESCE(SUM(sl.quantity_requested),  0)                           AS qte_vendue,
        COUNT(DISTINCT sl.sales_id)                                         AS nb_ventes,
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
    'ca_cumule_pct = % du CA total cumulé (faible = produit important). '
    'Seuils fixes dans la vue, valeurs de référence dans classification_config.';

-- ─────────────────────────────────────────────────────────────
-- 3. v_stock_rotation — vue ordinaire, reporting pur
--    Suppression de categorie_abc (Z-score incorrect).
--    Sous-requête sales unifiée (une seule passe sur sales_line).
--    rotation_annuelle_qte en quantités (plus pertinente que CA/valeur stock).
-- ─────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_stock_rotation AS
WITH sales_agregees AS (
    SELECT
        sl.produit_id,
        SUM(sl.sales_amount)                                                             AS ca_12m,
        SUM(sl.quantity_requested)                                                        AS qte_12m,
        SUM(CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN sl.sales_amount       ELSE 0 END) AS ca_30j,
        SUM(CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN sl.quantity_requested ELSE 0 END) AS qte_30j,
        COUNT(DISTINCT CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN s.id END)              AS nb_ventes_30j
    FROM sales_line sl
    JOIN sales s ON s.id = sl.sales_id
    WHERE s.statut   = 'CLOSED'
      AND s.canceled = false
      AND s.ca       = 'CA'
      AND s.sale_date >= CURRENT_DATE - INTERVAL '12 months'
    GROUP BY sl.produit_id
)
SELECT
  p.id                                                                    AS produit_id,
  p.libelle,
  fp.code_cip,
  fam.libelle                                                             AS famille,
  COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)                             AS stock_quantite,
  fp.prix_achat                                                           AS cout_unitaire,
  COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)::numeric * fp.prix_achat    AS valeur_stock,
  COALESCE(sa.ca_30j,  0)                                                 AS ca_30_jours,
  COALESCE(sa.qte_30j, 0)                                                 AS qte_vendue_30_jours,
  COALESCE(sa.nb_ventes_30j, 0)                                           AS nb_ventes_30_jours,
  COALESCE(sa.ca_12m,  0)                                                 AS ca_12_mois,
  COALESCE(sa.qte_12m, 0)                                                 AS qte_vendue_12_mois,
  -- CMM (consommation mensuelle moyenne sur 12 mois)
  ROUND(COALESCE(sa.qte_12m, 0) / 12.0, 2)                               AS cmm,
  -- Rotation en quantités : CMM / stock actuel (nombre de rotations par mois)
  CASE
    WHEN COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) = 0 THEN NULL
    ELSE ROUND(
      COALESCE(sa.qte_12m, 0)::numeric
            / NULLIF(SUM(sp.qty_stock + sp.qty_ug), 0),
      2)
    END                                                                     AS rotation_annuelle_qte,
  -- Couverture stock en jours : stock / CMM * 30
  CASE
    WHEN COALESCE(sa.qte_12m, 0) = 0 THEN NULL
    ELSE ROUND(
      COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) * 30.0
        / NULLIF(COALESCE(sa.qte_12m, 0) / 12.0, 0),
      0)
    END                                                                     AS couverture_stock_jours
FROM produit p
       LEFT JOIN fournisseur_produit fp  ON fp.id  = p.fournisseur_produit_principal_id
       LEFT JOIN famille_produit fam     ON fam.id = p.famille_id
       LEFT JOIN stock_produit sp        ON sp.produit_id = p.id
       LEFT JOIN sales_agregees sa       ON sa.produit_id = p.id
WHERE p.status       = 'ENABLE'
  AND p.type_produit <> 'DETAIL'
GROUP BY p.id, p.libelle, fp.code_cip, fam.libelle, fp.prix_achat,
         sa.ca_30j, sa.qte_30j, sa.nb_ventes_30j, sa.ca_12m, sa.qte_12m;

COMMENT ON VIEW v_stock_rotation IS
    'Vue de reporting stock/rotation — données en temps réel, aucune classification ABC. '
    'La classification ABC vient de v_abc_pareto_analysis. '
    'rotation_annuelle_qte = qte_12m / stock_actuel. '
    'couverture_stock_jours = stock_actuel / CMM * 30.';

-- ─────────────────────────────────────────────────────────────
-- 4. Mise à jour classification_config
--    Suppression : poids_ca, poids_rotation, seuils score 0-100, seuils rotation
--    Ajout       : seuils Pareto, seuils CMM, paramètres officine
-- ─────────────────────────────────────────────────────────────

-- 4a. Supprimer les colonnes du scoring pondéré (désormais inutiles)
ALTER TABLE classification_config
DROP COLUMN IF EXISTS poids_ca,
    DROP COLUMN IF EXISTS poids_rotation,
    DROP COLUMN IF EXISTS seuil_a_plus,
    DROP COLUMN IF EXISTS seuil_a,
    DROP COLUMN IF EXISTS seuil_b,
    DROP COLUMN IF EXISTS seuil_c,
    DROP COLUMN IF EXISTS rotation_a_plus,
    DROP COLUMN IF EXISTS rotation_a,
    DROP COLUMN IF EXISTS rotation_b,
    DROP COLUMN IF EXISTS rotation_c,
    DROP COLUMN IF EXISTS changement_min_score;

-- 4b. Ajouter les nouvelles colonnes Pareto + CMM
ALTER TABLE classification_config
  ADD COLUMN IF NOT EXISTS seuil_pareto_a_plus          INTEGER NOT NULL DEFAULT 60,
  ADD COLUMN IF NOT EXISTS seuil_pareto_a               INTEGER NOT NULL DEFAULT 80,
  ADD COLUMN IF NOT EXISTS seuil_pareto_b               INTEGER NOT NULL DEFAULT 95,
  ADD COLUMN IF NOT EXISTS seuil_pareto_c               INTEGER NOT NULL DEFAULT 99,
  ADD COLUMN IF NOT EXISTS seuil_frequence_min_mois     INTEGER NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS cmm_seuil_a_plus             INTEGER NOT NULL DEFAULT 50,
  ADD COLUMN IF NOT EXISTS cmm_seuil_a                  INTEGER NOT NULL DEFAULT 20,
  ADD COLUMN IF NOT EXISTS cmm_seuil_b                  INTEGER NOT NULL DEFAULT 5,
  ADD COLUMN IF NOT EXISTS cmm_seuil_c                  INTEGER NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS changement_min_pourcentage   INTEGER NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS activer_classification_ordo  BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS activer_correction_saisonniere BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE produit
  ADD COLUMN IF NOT EXISTS est_medicament_essentiel BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS est_produit_garde         BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN produit.est_medicament_essentiel IS
    'Médicament essentiel (liste OMS/LGO) : protégé contre la descente sous la classe B. '
    'Si activer_classification_ordo=true dans classification_config, la classe peut monter '
    'selon la CMM (cmm_seuil_a_plus/a/b/c).';

COMMENT ON COLUMN produit.est_produit_garde IS
    'Produit de garde officine : classé automatiquement A_PLUS, '
    'quelle que soit la performance de vente.';
ALTER TABLE classification_config
  ADD COLUMN IF NOT EXISTS indice_saisonnalite_min  INTEGER NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS nb_mois_saison_analyse   INTEGER NOT NULL DEFAULT 3;

COMMENT ON COLUMN classification_config.indice_saisonnalite_min IS
    'Ratio max_mensuel / VMM à partir duquel un produit est considéré saisonnier '
    '(ex. 3 → pic ≥ 3× la moyenne). Utilisé si activer_correction_saisonniere = true.';

COMMENT ON COLUMN classification_config.nb_mois_saison_analyse IS
    'Fenêtre glissante en mois pour le recalcul du score sur le pic saisonnier '
    '(ex. 3 → les 3 meilleurs mois consécutifs). Utilisé si activer_correction_saisonniere = true.';
