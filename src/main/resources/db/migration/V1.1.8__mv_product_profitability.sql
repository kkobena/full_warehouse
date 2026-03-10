DROP MATERIALIZED VIEW IF EXISTS mv_product_profitability CASCADE;

CREATE MATERIALIZED VIEW mv_product_profitability AS
WITH

stock_par_produit AS (
    SELECT sp.produit_id,
           sum(sp.qty_stock) AS qty_stock_total
    FROM stock_produit sp
             INNER JOIN storage st ON sp.storage_id = st.id
             INNER JOIN magasin m ON st.magasin_id = m.id
    WHERE m.id = 1
    GROUP BY sp.produit_id
),
-- CTE 2 : agrégats de ventes sur 1 an
product_margins AS (
    SELECT p.id                                                                  AS produit_id,
           p.libelle,
           fp.code_cip,
           f.libelle                                                             AS categorie,
           count(DISTINCT sl.sales_id)                                          AS nb_ventes,
           sum(sl.quantity_requested)                                           AS qte_vendue,
           sum(sl.sales_amount)                                                 AS ca_total,
           sum(sl.cost_amount * sl.quantity_requested)                         AS cout_achat_total,
           sum(sl.sales_amount - sl.cost_amount * sl.quantity_requested)       AS marge_brute,
           -- taux de marge : cast AVANT la division pour éviter la troncature entière
           CASE
               WHEN sum(sl.sales_amount) > 0 THEN
                   round(
                       (sum(sl.sales_amount - sl.cost_amount * sl.quantity_requested)::numeric
                        / sum(sl.sales_amount)::numeric * 100), 2)
               ELSE 0::numeric
           END                                                                  AS taux_marge_pct,
           CASE
               WHEN sum(sl.quantity_requested) > 0 THEN
                   round((sum(sl.sales_amount)::numeric / sum(sl.quantity_requested)::numeric), 0)
               ELSE 0::numeric
           END                                                                  AS prix_vente_moyen,
           CASE
               WHEN sum(sl.quantity_requested) > 0 THEN
                   round((sum(sl.cost_amount * sl.quantity_requested)::numeric
                          / sum(sl.quantity_requested)::numeric), 0)
               ELSE 0::numeric
           END                                                                  AS prix_achat_moyen,
           -- stock issu du CTE isolé : valeur exacte, non multipliée par les jointures
           COALESCE(sp.qty_stock_total, 0)                                     AS stock_quantity,
           COALESCE(fp.prix_achat, 0)                                          AS prix_achat_unitaire,
           COALESCE(fp.prix_uni, 0)                                            AS prix_vente_unitaire,
           -- taux de rotation basé sur le stock réel (non corrompu)
           CASE
               WHEN COALESCE(sp.qty_stock_total, 0) > 0 AND COALESCE(fp.prix_achat, 0) > 0 THEN
                   round(
                       (sum(sl.sales_amount)::numeric
                        / (sp.qty_stock_total * fp.prix_achat)::numeric * 12), 2)
               ELSE 0::numeric
           END                                                                  AS taux_rotation_annuel
    FROM produit p
             LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
             LEFT JOIN famille_produit f ON p.famille_id = f.id
             LEFT JOIN sales_line sl ON p.id = sl.produit_id
             LEFT JOIN sales s ON sl.sales_id = s.id
             -- jointure sur le CTE isolé, pas directement sur stock_produit
             LEFT JOIN stock_par_produit sp ON p.id = sp.produit_id
    WHERE s.statut::text = 'CLOSED'::text
      AND s.sale_date >= (CURRENT_DATE - '1 year'::interval)
      AND p.status::text = 'ENABLE'::text
      AND s.canceled = false
      AND s.ca::text = 'CA'::text
    GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat, fp.prix_uni,
             sp.qty_stock_total
),
bcg_classification AS (
    SELECT pm.produit_id,
           pm.libelle,
           pm.code_cip,
           pm.categorie,
           pm.nb_ventes,
           pm.qte_vendue,
           pm.ca_total,
           pm.cout_achat_total,
           pm.marge_brute,
           pm.taux_marge_pct,
           pm.prix_vente_moyen,
           pm.prix_achat_moyen,
           pm.stock_quantity,
           pm.prix_achat_unitaire,
           pm.prix_vente_unitaire,
           pm.taux_rotation_annuel,
           CASE
               WHEN pm.taux_marge_pct >= 20 AND pm.taux_rotation_annuel >= 6 THEN 'STAR'
               WHEN pm.taux_marge_pct >= 20 AND pm.taux_rotation_annuel < 6  THEN 'CASH_COW'
               WHEN pm.taux_marge_pct < 20  AND pm.taux_rotation_annuel >= 6 THEN 'QUESTION_MARK'
               WHEN pm.taux_marge_pct < 20  AND pm.taux_rotation_annuel < 6  THEN 'DOG'
               ELSE 'UNDEFINED'
           END AS bcg_category
    FROM product_margins pm
)
SELECT produit_id,
       libelle,
       code_cip,
       categorie,
       nb_ventes,
       qte_vendue,
       ca_total,
       cout_achat_total,
       marge_brute,
       taux_marge_pct,
       prix_vente_moyen,
       prix_achat_moyen,
       stock_quantity,
       prix_achat_unitaire,
       prix_vente_unitaire,
       taux_rotation_annuel,
       bcg_category
FROM bcg_classification
WHERE ca_total > 0
ORDER BY marge_brute DESC;


CREATE UNIQUE INDEX idx_mv_product_profitability_produit_id
  ON mv_product_profitability (produit_id);




CREATE MATERIALIZED VIEW mv_marge_produit AS
WITH
-- CTE 1 : stock total par produit, restreint au magasin principal (id = 1)
stock_par_produit AS (
    SELECT sp.produit_id,
           sum(sp.qty_stock)::bigint AS qty_stock_total
    FROM stock_produit sp
             INNER JOIN storage st ON sp.storage_id = st.id
             INNER JOIN magasin m  ON st.magasin_id  = m.id
    WHERE m.id = 1
    GROUP BY sp.produit_id
),
-- CTE 2 : agrégats de ventes sur 1 an
ventes AS (
    SELECT sl.produit_id,
           count(DISTINCT sl.sales_id)                                          AS nb_ventes,
           sum(sl.quantity_requested)                                           AS qte_vendue,
           sum(sl.sales_amount)::bigint                                        AS ca_total,
           sum(sl.cost_amount * sl.quantity_requested)::bigint                 AS cout_achat_total,
           sum(sl.sales_amount - sl.cost_amount * sl.quantity_requested)::bigint AS marge_brute
    FROM sales_line sl
             INNER JOIN sales s ON sl.sales_id = s.id
    WHERE s.statut       = 'CLOSED'
      AND s.canceled     = false
      AND s.ca           = 'CA'
      AND s.sale_date   >= (CURRENT_DATE - INTERVAL '1 year')
    GROUP BY sl.produit_id
)
SELECT
  p.id                                                                          AS produit_id,
  p.libelle,
  fp.code_cip,
  f.libelle                                                                     AS categorie,
  f.id                                                                          AS famille_produit_id,
  COALESCE(v.nb_ventes,        0)                                              AS nb_ventes,
  COALESCE(v.qte_vendue,       0)                                              AS qte_vendue,
  COALESCE(v.ca_total,         0)::bigint                                      AS ca_total,
  COALESCE(v.cout_achat_total, 0)::bigint                                      AS cout_achat_total,
  COALESCE(v.marge_brute,      0)::bigint                                      AS marge_brute,
    -- taux de marge : cast ::numeric sur les opérandes avant division
  CASE
    WHEN COALESCE(v.ca_total, 0) > 0 THEN
      round(
        COALESCE(v.marge_brute, 0)::numeric
                / v.ca_total::numeric * 100, 2)
    ELSE 0::numeric
END                                                                           AS taux_marge_pct,
    CASE
        WHEN COALESCE(v.qte_vendue, 0) > 0 THEN
            round(v.ca_total::numeric / v.qte_vendue::numeric, 0)
        ELSE 0::numeric
END                                                                           AS prix_vente_moyen,
    CASE
        WHEN COALESCE(v.qte_vendue, 0) > 0 THEN
            round(v.cout_achat_total::numeric / v.qte_vendue::numeric, 0)
        ELSE 0::numeric
END                                                                           AS prix_achat_moyen,
    COALESCE(sp.qty_stock_total, 0)                                              AS stock_quantity,
    COALESCE(fp.prix_achat, 0)                                                   AS prix_achat_unitaire,
    COALESCE(fp.prix_uni,   0)                                                   AS prix_vente_unitaire,
    -- taux de rotation annuel basé sur le stock réel (non corrompu par les jointures)
    CASE
        WHEN COALESCE(sp.qty_stock_total, 0) > 0
         AND COALESCE(fp.prix_achat, 0) > 0 THEN
            round(
                v.ca_total::numeric
                / (sp.qty_stock_total * fp.prix_achat)::numeric * 12, 2)
        ELSE 0::numeric
END                                                                           AS taux_rotation_annuel
FROM produit p
         LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
         LEFT JOIN famille_produit f      ON p.famille_id                       = f.id
         LEFT JOIN ventes v               ON p.id                               = v.produit_id
         LEFT JOIN stock_par_produit sp   ON p.id                               = sp.produit_id
WHERE p.status = 'ENABLE'
  AND COALESCE(v.ca_total, 0) > 0
ORDER BY marge_brute DESC;




CREATE UNIQUE INDEX idx_mv_marge_produit_produit_id
  ON mv_marge_produit (produit_id);


CREATE INDEX idx_mv_marge_produit_famille_id
  ON mv_marge_produit (famille_produit_id);


CREATE INDEX idx_mv_marge_produit_taux_marge
  ON mv_marge_produit (taux_marge_pct);


