-- ─────────────────────────────────────────────────────────────
-- V1.3.5 — Correction v_stock_rotation : valeurs négatives et aberrantes
--
-- Problèmes corrigés :
--   1. Stock négatif (qty_stock + qty_ug < 0) → division par négatif
--      → rotation et couverture retournaient des valeurs négatives impossibles
--   2. CMM quasi-nulle avec stock résiduel → couverture de 10 000+ jours
--      → bornée à 9999 jours max (cohérent avec affichage frontend)
--
-- Fix :
--   • GREATEST(SUM(stock), 0) : plancher à 0 pour tout calcul de dénominateur
--   • NULLIF(..., 0) après GREATEST : retourne NULL quand stock = 0 (pas ∞)
--   • LEAST(..., 9999) : plafonne la couverture à 9999 jours
-- ─────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW v_stock_rotation AS
WITH sales_agregees AS (
    SELECT
        sl.produit_id,
        SUM(sl.sales_amount)                                                              AS ca_12m,
        SUM(sl.quantity_requested)                                                        AS qte_12m,
        SUM(CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN sl.sales_amount       ELSE 0 END) AS ca_30j,
        SUM(CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN sl.quantity_requested ELSE 0 END) AS qte_30j,
        COUNT(DISTINCT CASE WHEN s.sale_date >= CURRENT_DATE - 30 THEN s.id END)               AS nb_ventes_30j
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
  GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0)                AS stock_quantite,
  fp.prix_achat                                                           AS cout_unitaire,
  GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0)::numeric
    * COALESCE(fp.prix_achat, 0)                                          AS valeur_stock,
  COALESCE(sa.ca_30j,  0)                                                 AS ca_30_jours,
  COALESCE(sa.qte_30j, 0)                                                 AS qte_vendue_30_jours,
  COALESCE(sa.nb_ventes_30j, 0)                                           AS nb_ventes_30_jours,
  COALESCE(sa.ca_12m,  0)                                                 AS ca_12_mois,
  COALESCE(sa.qte_12m, 0)                                                 AS qte_vendue_12_mois,
  -- CMM (consommation mensuelle moyenne sur 12 mois)
  ROUND(COALESCE(sa.qte_12m, 0) / 12.0, 2)                               AS cmm,
  -- Rotation annuelle en quantités : qte_12m / stock
  -- NULL si stock <= 0 (stock négatif ou rupture totale → ratio sans sens)
  CASE
    WHEN GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0) = 0 THEN NULL
    ELSE ROUND(
      COALESCE(sa.qte_12m, 0)::numeric
        / NULLIF(GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0), 0),
      2)
    END                                                                     AS rotation_annuelle_qte,
  -- Couverture stock en jours : stock / CMM * 30
  -- NULL si CMM = 0 (produit jamais vendu → couverture infinie non affichable)
  -- NULL si stock <= 0 (stock négatif → couverture sans sens)
  -- Plafonnée à 9999 jours (≈ 27 ans) pour éviter les valeurs aberrantes
  CASE
    WHEN COALESCE(sa.qte_12m, 0) = 0 THEN NULL
    WHEN GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0) = 0 THEN NULL
    ELSE LEAST(
      ROUND(
        GREATEST(COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0), 0) * 30.0
          / NULLIF(COALESCE(sa.qte_12m, 0) / 12.0, 0),
        0),
      9999)
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


