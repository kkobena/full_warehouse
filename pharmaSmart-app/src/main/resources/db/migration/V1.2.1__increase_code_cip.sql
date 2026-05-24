
DROP MATERIALIZED VIEW IF EXISTS mv_product_profitability CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_abc_pareto_analysis CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_marge_produit CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_monthly_top_products CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_semois_suggestion CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_stock_alerts CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_stock_rotation CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_stock_valuation_by_rayon CASCADE;


alter table fournisseur_produit
  alter column code_cip type varchar(20) ;

alter table fournisseur_produit
  alter column code_ean type varchar(20) ;

create materialized view mv_abc_pareto_analysis as
WITH product_sales AS (SELECT p.id                        AS produit_id,
                              p.libelle,
                              fp.code_cip,
                              f.libelle                   AS categorie,
                              sum(sl.sales_amount)        AS ca_total,
                              sum(sl.quantity_requested)  AS qte_vendue,
                              count(DISTINCT sl.sales_id) AS nb_ventes
                       FROM produit p
                                LEFT JOIN fournisseur_produit fp
                                          ON p.fournisseur_produit_principal_id = fp.id
                                LEFT JOIN famille_produit f ON p.famille_id = f.id
                                JOIN sales_line sl ON p.id = sl.produit_id
                                JOIN sales s ON sl.sales_id = s.id
                       WHERE s.statut::text = 'CLOSED'::text
                         AND s.canceled = false
                         AND s.ca::text = 'CA'::text
                         AND s.sale_date >= (CURRENT_DATE - '1 year'::interval)
                         AND p.status::text = 'ENABLE'::text
                       GROUP BY p.id, p.libelle, fp.code_cip, f.libelle
                       HAVING sum(sl.sales_amount) > 0),
     total_ca AS (SELECT sum(product_sales.ca_total) AS ca_global
                  FROM product_sales),
     cumulative_ca AS (SELECT ps.produit_id,
                              ps.libelle,
                              ps.code_cip,
                              ps.categorie,
                              ps.ca_total,
                              ps.qte_vendue,
                              ps.nb_ventes,
                              tc.ca_global,
                              sum(ps.ca_total)
                              OVER (ORDER BY ps.ca_total DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ca_cumule,
                              round(ps.ca_total::numeric / tc.ca_global * 100::numeric,
                                    2)                                                                          AS contribution_pct,
                              round(sum(ps.ca_total)
                                    OVER (ORDER BY ps.ca_total DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) /
                                    tc.ca_global * 100::numeric,
                                    2)                                                                          AS ca_cumule_pct
                       FROM product_sales ps
                                CROSS JOIN total_ca tc),
     pareto_classification AS (SELECT cumulative_ca.produit_id,
                                      cumulative_ca.libelle,
                                      cumulative_ca.code_cip,
                                      cumulative_ca.categorie,
                                      cumulative_ca.ca_total,
                                      cumulative_ca.qte_vendue,
                                      cumulative_ca.nb_ventes,
                                      cumulative_ca.ca_global,
                                      cumulative_ca.ca_cumule,
                                      cumulative_ca.contribution_pct,
                                      cumulative_ca.ca_cumule_pct,
                                      CASE
                                          WHEN cumulative_ca.ca_cumule_pct <= 80::numeric THEN 'A'::text
                                          WHEN cumulative_ca.ca_cumule_pct <= 95::numeric THEN 'B'::text
                                          ELSE 'C'::text
                                          END                                                  AS classe_pareto,
                                      row_number() OVER (ORDER BY cumulative_ca.ca_total DESC) AS rang
                               FROM cumulative_ca)
SELECT produit_id,
       libelle,
       code_cip,
       categorie,
       ca_total,
       qte_vendue,
       nb_ventes,
       ca_global,
       ca_cumule,
       contribution_pct,
       ca_cumule_pct,
       classe_pareto,
       rang
FROM pareto_classification
ORDER BY ca_total DESC;



create index idx_mv_pareto_classe
  on mv_abc_pareto_analysis (classe_pareto);

create index idx_mv_pareto_ca
  on mv_abc_pareto_analysis (ca_total desc);

create index idx_mv_pareto_rang
  on mv_abc_pareto_analysis (rang);

create index idx_mv_pareto_categorie
  on mv_abc_pareto_analysis (categorie);

create unique index idx_mv_abc_pareto_unique
  on mv_abc_pareto_analysis (produit_id);

CREATE MATERIALIZED VIEW mv_marge_produit AS
WITH

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


create materialized view mv_monthly_top_products as
SELECT to_char(date_trunc('month'::text, s.sale_date::timestamp with time zone),
               'YYYY-MM-DD'::text)                             AS mois,
       p.id                                                    AS produit_id,
       p.libelle,
       fp.code_cip,
       count(DISTINCT s.id)                                    AS nb_ventes,
       sum(sl.quantity_requested)                              AS qte_vendue,
       sum(sl.sales_amount)                                    AS ca_genere,
       avg(sl.sales_amount / NULLIF(sl.quantity_requested, 0)) AS prix_moyen,
       now()                                                   AS last_updated
FROM sales_line sl
       JOIN sales s ON sl.sales_id = s.id
       JOIN produit p ON sl.produit_id = p.id
       LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
WHERE s.statut::text = 'CLOSED'::text
  AND s.canceled = false
  AND s.ca::text = 'CA'::text
  AND s.sale_date >=
      (date_trunc('month'::text, CURRENT_DATE::timestamp with time zone) - '6 mons'::interval)
GROUP BY (to_char(date_trunc('month'::text, s.sale_date::timestamp with time zone),
  'YYYY-MM-DD'::text)), p.id, p.libelle, fp.code_cip;



create unique index idx_mv_monthly_top_products_unique
  on mv_monthly_top_products (mois, produit_id);

create index idx_mv_monthly_top_products_ca
  on mv_monthly_top_products (mois asc, ca_genere desc);

create index idx_mv_monthly_top_products_qte
  on mv_monthly_top_products (mois asc, qte_vendue desc);


create materialized view mv_semois_suggestion as
SELECT p.id AS produit_id,
       p.libelle,
       fp.code_cip,
       sc.classe_criticite,
       sc.coefficient_securite,
       sc.delai_livraison_jours,
       COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                        NULLIF(sum(7 - vma.row_num), 0::numeric)
                 FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                              row_number()
                                OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                       FROM ventes_mensuelles_agregees
                       WHERE ventes_mensuelles_agregees.produit_id = p.id
                         AND ventes_mensuelles_agregees.annee_mois::text >=
                             to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
                 WHERE vma.row_num <= 6), 0::numeric)::integer AS vmm,
  (COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                    NULLIF(sum(7 - vma.row_num), 0::numeric)
             FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                          row_number()
                            OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                   FROM ventes_mensuelles_agregees
                   WHERE ventes_mensuelles_agregees.produit_id = p.id
                     AND ventes_mensuelles_agregees.annee_mois::text >=
                              to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
             WHERE vma.row_num <= 6), 0::numeric) *
   (sc.delai_livraison_jours::numeric * sc.coefficient_securite /
         30.0))::integer AS marge_securite,
  (COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                    NULLIF(sum(7 - vma.row_num), 0::numeric)
             FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                          row_number()
                            OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                   FROM ventes_mensuelles_agregees
                   WHERE ventes_mensuelles_agregees.produit_id = p.id
                     AND ventes_mensuelles_agregees.annee_mois::text >=
                              to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
             WHERE vma.row_num <= 6), 0::numeric) * (1::numeric +
                                                          sc.delai_livraison_jours::numeric *
                                                          sc.coefficient_securite /
                                                          30.0))::integer AS stock_objectif,
  COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) AS stock_actuel,
       GREATEST(0::bigint, (COALESCE(
                              (SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                                      NULLIF(sum(7 - vma.row_num), 0::numeric)
                               FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                                            row_number()
                                              OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                                     FROM ventes_mensuelles_agregees
                                     WHERE ventes_mensuelles_agregees.produit_id = p.id
                                       AND ventes_mensuelles_agregees.annee_mois::text >=
                                                 to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
                               WHERE vma.row_num <= 6), 0::numeric) * (1::numeric +
                                                                             sc.delai_livraison_jours::numeric *
                                                                             sc.coefficient_securite /
                                                                             30.0))::integer -
                                                                                    COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)) AS quantite_a_commander,
       sc.date_dernier_calcul,
       now() AS vue_refresh_date
FROM produit p
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
       LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
       LEFT JOIN stock_produit sp ON sp.produit_id = p.id
WHERE p.status::text = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  AND sc.id IS NOT NULL
GROUP BY p.id, p.libelle, fp.code_cip, sc.classe_criticite, sc.coefficient_securite,
  sc.delai_livraison_jours, sc.date_dernier_calcul;

comment on materialized view mv_semois_suggestion is 'Vue matérialisée des suggestions SEMOIS (refresh quotidien)';



create index idx_mv_semois_produit
  on mv_semois_suggestion (produit_id);

create index idx_mv_semois_classe
  on mv_semois_suggestion (classe_criticite);

create index idx_mv_semois_qte_commander
  on mv_semois_suggestion (quantite_a_commander)
  where (quantite_a_commander > 0);


create materialized view mv_stock_alerts as
WITH lot_peremption AS (SELECT l.produit_id,
                               min(l.expiry_date) AS nearest_expiry_date
                        FROM lot l
                        WHERE l.statut::text = 'AVAILABLE'::text
                          AND l.current_quantity > 0
                          AND l.expiry_date IS NOT NULL
                        GROUP BY l.produit_id),
     stock AS (SELECT sp.produit_id,
                      COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) AS stock_quantity
               FROM stock_produit sp
                        JOIN storage s_1 ON sp.storage_id = s_1.id
                        JOIN magasin m ON s_1.magasin_id = m.id
               WHERE m.id = 1
               GROUP BY sp.produit_id)
SELECT p.id                                  AS produit_id,
       p.libelle,
       fp.code_cip,
       COALESCE(s.stock_quantity, 0::bigint) AS stock_quantity,
       p.qty_seuil_mini                      AS seuil_min,
       lp.nearest_expiry_date                AS expiry_date,
       CASE
         WHEN COALESCE(s.stock_quantity, 0::bigint) <= 0 THEN 'RUPTURE'::text
         WHEN COALESCE(s.stock_quantity, 0::bigint) < p.qty_seuil_mini THEN 'ALERTE'::text
         WHEN lp.nearest_expiry_date IS NOT NULL AND
              lp.nearest_expiry_date < (CURRENT_DATE + '3 mons'::interval) THEN 'PEREMPTION'::text
         ELSE NULL::text
         END                               AS alert_type,
       now()                                 AS last_updated
FROM produit p
       LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
       LEFT JOIN stock s ON p.id = s.produit_id
       LEFT JOIN lot_peremption lp ON p.id = lp.produit_id
WHERE p.status::text = 'ENABLE'::text
  AND (COALESCE(s.stock_quantity, 0::bigint) <= 0 OR
       COALESCE(s.stock_quantity, 0::bigint) < p.qty_seuil_mini OR
       lp.nearest_expiry_date IS NOT NULL AND
       lp.nearest_expiry_date < (CURRENT_DATE + '3 mons'::interval));

comment on materialized view mv_stock_alerts is 'Stock alerts: ruptures, low stock, and near expiration based on lot nearest expiry date';



create unique index idx_mv_stock_alerts_unique
  on mv_stock_alerts (produit_id);

create index idx_mv_stock_alerts_type
  on mv_stock_alerts (alert_type);

create index idx_mv_stock_alerts_expiry
  on mv_stock_alerts (expiry_date)
  where (expiry_date IS NOT NULL);

create materialized view mv_stock_rotation as
WITH product_sales AS (SELECT p.id                                                               AS produit_id,
                              p.libelle,
                              fp.code_cip,
                              f.libelle                                                          AS categorie,
                              COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)                 AS stock_quantity,
                              fp.prix_achat                                                      AS unit_cost,
                              COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                              fp.prix_achat                                                      AS stock_value,
                              COALESCE(sales_30d.ca, 0::bigint)                                  AS ca_last_30_days,
                              COALESCE(sales_30d.qty_sold, 0::bigint)                            AS qty_sold_last_30_days,
                              COALESCE(sales_30d.nb_sales, 0::bigint)                            AS nb_sales_last_30_days,
                              COALESCE(sales_12m.ca, 0::bigint)                                  AS ca_last_12_months,
                              COALESCE(sales_12m.qty_sold, 0::bigint)                            AS qty_sold_last_12_months,
                              CASE
                                  WHEN (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                        fp.prix_achat) > 0 THEN round(
                                          (COALESCE(sales_12m.ca, 0::bigint) /
                                           (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                            fp.prix_achat))::numeric, 2)
                                  ELSE 0::numeric
                                  END                                                            AS rotation_rate_annual,
                              CASE
                                  WHEN COALESCE(sales_12m.ca, 0::bigint) > 0 AND
                                       (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                        fp.prix_achat) > 0 THEN round((365 / NULLIF(
                                          COALESCE(sales_12m.ca, 0::bigint) /
                                          (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                           fp.prix_achat), 0))::numeric, 0)
                                  ELSE 999::numeric
                                  END                                                            AS avg_days_in_stock
                       FROM produit p
                                LEFT JOIN fournisseur_produit fp
                                          ON p.fournisseur_produit_principal_id = fp.id
                                LEFT JOIN stock_produit sp ON p.id = sp.produit_id
                                LEFT JOIN famille_produit f ON p.famille_id = f.id
                                LEFT JOIN (SELECT sl.produit_id,
                                                  sum(sl.sales_amount)       AS ca,
                                                  sum(sl.quantity_requested) AS qty_sold,
                                                  count(DISTINCT s.id)       AS nb_sales
                                           FROM sales_line sl
                                                    JOIN sales s ON sl.sales_id = s.id
                                           WHERE s.statut::text = 'CLOSED'::text
                                             AND s.canceled = false
                                             AND s.ca::text = 'CA'::text
                                             AND s.sale_date >= (CURRENT_DATE - '30 days'::interval)
                                           GROUP BY sl.produit_id) sales_30d
                                          ON p.id = sales_30d.produit_id
                                LEFT JOIN (SELECT sl.produit_id,
                                                  sum(sl.sales_amount)       AS ca,
                                                  sum(sl.quantity_requested) AS qty_sold
                                           FROM sales_line sl
                                                    JOIN sales s ON sl.sales_id = s.id
                                           WHERE s.statut::text = 'CLOSED'::text
                                             AND s.canceled = false
                                             AND s.ca::text = 'CA'::text
                                             AND s.sale_date >= (CURRENT_DATE - '1 year'::interval)
                                           GROUP BY sl.produit_id) sales_12m
                                          ON p.id = sales_12m.produit_id
                       WHERE p.status::text = 'ENABLE'::text
                       GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat,
                                sales_30d.ca, sales_30d.qty_sold, sales_30d.nb_sales, sales_12m.ca,
                                sales_12m.qty_sold),
     sales_stats AS (SELECT avg(product_sales.ca_last_12_months)        AS avg_ca,
                            stddev_pop(product_sales.ca_last_12_months) AS stddev_ca
                     FROM product_sales
                     WHERE product_sales.ca_last_12_months > 0)
SELECT ps.produit_id,
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
       CASE
         WHEN ps.ca_last_12_months = 0 THEN 'C'::text
         WHEN ss.stddev_ca = 0::numeric OR ss.stddev_ca IS NULL THEN 'B'::text
           WHEN ((ps.ca_last_12_months::numeric - ss.avg_ca) / NULLIF(ss.stddev_ca, 0::numeric)) >=
                1.96 THEN 'A'::text
           WHEN ((ps.ca_last_12_months::numeric - ss.avg_ca) / NULLIF(ss.stddev_ca, 0::numeric)) >=
                1.65 THEN 'B'::text
           ELSE 'C'::text
           END AS categorie_abc,
       now()   AS last_updated
FROM product_sales ps
  CROSS JOIN sales_stats ss;



create unique index idx_mv_stock_rotation_unique
  on mv_stock_rotation (produit_id);

create index idx_mv_stock_rotation_category
  on mv_stock_rotation (categorie);

create index idx_mv_stock_rotation_abc
  on mv_stock_rotation (categorie_abc);

create index idx_mv_stock_rotation_rate
  on mv_stock_rotation (rotation_rate_annual desc);


create materialized view mv_stock_valuation as
SELECT p.id                                                               AS produit_id,
       p.libelle,
       fp.code_cip,
       f.libelle                                                          AS categorie,
       f.id                                                               AS categorie_id,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)                 AS stock_quantity,
       fp.prix_achat                                                      AS purchase_price,
       fp.prix_uni                                                        AS sales_price,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS total_purchase_value,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni   AS total_sales_value,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni -
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS potential_margin,
       CASE
         WHEN fp.prix_uni > 0 THEN round(
           (fp.prix_uni - fp.prix_achat)::numeric / fp.prix_uni::numeric * 100::numeric, 2)
         ELSE 0::numeric
           END                                                            AS margin_percentage,
       now()                                                              AS last_updated
FROM produit p
  LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
  LEFT JOIN stock_produit sp ON p.id = sp.produit_id
  LEFT JOIN famille_produit f ON p.famille_id = f.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, f.id, fp.prix_achat, fp.prix_uni
HAVING COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

comment on materialized view mv_stock_valuation is 'Stock valuation aggregated across all storage locations – margin_percentage uses numeric cast to avoid integer division';



create unique index idx_mv_stock_valuation_unique
  on mv_stock_valuation (produit_id);

create index idx_mv_stock_valuation_category
  on mv_stock_valuation (categorie);

create index idx_mv_stock_valuation_categorie_id
  on mv_stock_valuation (categorie_id);

create index idx_mv_stock_valuation_value
  on mv_stock_valuation (total_sales_value desc);

create materialized view mv_stock_valuation_by_rayon as
SELECT p.id                                         AS produit_id,
       p.libelle,
       fp.code_cip,
       f.libelle                                    AS categorie,
       f.id                                         AS categorieid,
       r.libelle                                    AS rayon,
       r.id                                         AS rayon_id,
       sp.total_qty                                 AS stock_quantity,
       fp.prix_achat                                AS purchase_price,
       fp.prix_uni                                  AS sales_price,
       sp.total_qty * fp.prix_achat                 AS total_purchase_value,
       sp.total_qty * fp.prix_uni                   AS total_sales_value,
       sp.total_qty * (fp.prix_uni - fp.prix_achat) AS potential_margin,
       CASE
         WHEN fp.prix_uni > 0 THEN round(
           (fp.prix_uni - fp.prix_achat)::numeric / fp.prix_uni::numeric * 100::numeric, 2)
         ELSE 0::numeric
           END                                      AS margin_percentage,
       now()                                        AS last_updated
FROM produit p
  JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
  JOIN (SELECT stock_produit.produit_id,
  sum(stock_produit.qty_stock + stock_produit.qty_ug) AS total_qty
  FROM stock_produit
  GROUP BY stock_produit.produit_id) sp ON p.id = sp.produit_id
  LEFT JOIN famille_produit f ON p.famille_id = f.id
  LEFT JOIN rayon_produit rp ON p.id = rp.produit_id
  LEFT JOIN rayon r ON rp.rayon_id = r.id
WHERE p.status::text = 'ENABLE'::text
  AND sp.total_qty > 0
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, f.id, fp.prix_achat, fp.prix_uni, r.libelle, r.id,
  sp.total_qty;



create unique index idx_mv_stock_valuation_by_rayon_unique
  on mv_stock_valuation_by_rayon (produit_id, rayon_id);

create index idx_mv_stock_valuation_by_rayon_category
  on mv_stock_valuation_by_rayon (categorieid);

create index idx_mv_stock_valuation_by_rayon_value
  on mv_stock_valuation_by_rayon (total_sales_value desc);



create materialized view mv_pareto_summary as
SELECT count(*)                                               AS total_produits,
       sum(ca_total)                                          AS ca_global,
       count(*) FILTER (WHERE classe_pareto = 'A'::text)      AS nb_produits_a,
  sum(ca_total) FILTER (WHERE classe_pareto = 'A'::text) AS ca_classe_a,
  round(sum(ca_total) FILTER (WHERE classe_pareto = 'A'::text) / sum(ca_total) * 100::numeric,
        2)                                               AS pct_ca_classe_a,
       count(*) FILTER (WHERE classe_pareto = 'B'::text)      AS nb_produits_b,
  sum(ca_total) FILTER (WHERE classe_pareto = 'B'::text) AS ca_classe_b,
  round(sum(ca_total) FILTER (WHERE classe_pareto = 'B'::text) / sum(ca_total) * 100::numeric,
        2)                                               AS pct_ca_classe_b,
       count(*) FILTER (WHERE classe_pareto = 'C'::text)      AS nb_produits_c,
  sum(ca_total) FILTER (WHERE classe_pareto = 'C'::text) AS ca_classe_c,
  round(sum(ca_total) FILTER (WHERE classe_pareto = 'C'::text) / sum(ca_total) * 100::numeric,
        2)                                               AS pct_ca_classe_c
FROM mv_abc_pareto_analysis;




ALTER TABLE store_inventory
  DROP CONSTRAINT store_inventory_inventory_category_check;

ALTER TABLE store_inventory
  ADD CONSTRAINT store_inventory_inventory_category_check
    CHECK (
      inventory_category IN (
                 'MAGASIN',
                 'STORAGE',
                 'RAYON',
                 'FAMILLY',
                 'PERIME',
                 'ALERTE_PEREMPTION',
                 'VENDU',
                 'INVENDU',
                 'SOUS_SEUIL',
                 'EN_RUPTURE',
                 'GROSSISTE',
                 'SELECTION_PRODUIT'
        )
      );


