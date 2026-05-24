



create materialized view mv_stock_alerts as
SELECT p.id                                               AS produit_id,
       p.libelle,
       fp.code_cip,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) AS stock_quantity,
       p.qty_seuil_mini                                   AS seuil_min,
       l.expiry_date,
       CASE
         WHEN COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) = 0 THEN 'RUPTURE'::text
         WHEN COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) < p.qty_seuil_mini THEN 'ALERTE'::text
         WHEN l.expiry_date < (CURRENT_DATE + '3 mons'::interval) THEN 'PEREMPTION'::text
         ELSE NULL::text
         END                                              AS alert_type,
       now()                                              AS last_updated
FROM produit p
       LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
       JOIN fournisseur_produit fpp ON p.id = fpp.produit_id
       LEFT JOIN stock_produit sp ON p.id = sp.produit_id
       LEFT JOIN order_line ol ON fpp.id = ol.fournisseur_produit_id
       LEFT JOIN lot l ON ol.id = l.order_line_id AND
                          l.expiry_date < (CURRENT_DATE + '3 mons'::interval)
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, p.qty_seuil_mini, l.expiry_date, l.statut
HAVING COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) = 0
    OR COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) < p.qty_seuil_mini
    OR l.expiry_date IS NOT NULL AND l.expiry_date < (CURRENT_DATE + '3 mons'::interval) AND
  l.statut::text = 'AVAILABLE'::text;

comment on materialized view mv_stock_alerts is 'Stock alerts for ruptures, low stock, and near expiration products';



create unique index idx_mv_stock_alerts_unique
  on mv_stock_alerts (produit_id, expiry_date);

create index idx_mv_stock_alerts_type
  on mv_stock_alerts (alert_type);

create index idx_mv_stock_alerts_expiry
  on mv_stock_alerts (expiry_date)
  where (expiry_date IS NOT NULL);

create materialized view mv_stock_valuation as
SELECT p.id                                                               AS produit_id,
       p.libelle,
       fp.code_cip,
       f.libelle                                                          AS categorie,
       s.name                                                             AS storage_location,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)                 AS stock_quantity,
       fp.prix_achat                                                      AS purchase_price,
       fp.prix_uni                                                        AS sales_price,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS total_purchase_value,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni   AS total_sales_value,
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_uni -
       COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) * fp.prix_achat AS potential_margin,
       CASE
         WHEN fp.prix_uni > 0 THEN round(
           ((fp.prix_uni - fp.prix_achat) / fp.prix_uni * 100)::numeric, 2)
         ELSE 0::numeric
         END                                                              AS margin_percentage,
       now()                                                              AS last_updated
FROM produit p
  LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
  LEFT JOIN stock_produit sp ON p.id = sp.produit_id
  LEFT JOIN famille_produit f ON p.famille_id = f.id
  LEFT JOIN storage s ON sp.storage_id = s.id
WHERE p.status::text = 'ENABLE'::text
GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, s.name, fp.prix_achat, fp.prix_uni
HAVING COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) > 0;

comment on materialized view mv_stock_valuation is 'Stock valuation with purchase and sales prices for financial reporting';



create unique index idx_mv_stock_valuation_unique
  on mv_stock_valuation (produit_id);

create index idx_mv_stock_valuation_category
  on mv_stock_valuation (categorie);

create index idx_mv_stock_valuation_storage
  on mv_stock_valuation (storage_location);

create index idx_mv_stock_valuation_value
  on mv_stock_valuation (total_sales_value desc);

create materialized view mv_customer_rfm as
WITH customer_metrics AS (SELECT c.id                                                              AS customer_id,
                                 (c.first_name::text || ' '::text) || c.last_name::text            AS customer_name,
                                 c.phone,
                                 max(s.sale_date)                                                  AS last_purchase_date,
                                 EXTRACT(day FROM age(CURRENT_DATE::timestamp with time zone,
                                                      max(s.sale_date)::timestamp with time zone)) AS days_since_last_purchase,
                                 count(DISTINCT s.id)                                              AS nb_purchases_last_year,
                                 sum(s.sales_amount)                                               AS total_spent_last_year,
                                 avg(s.sales_amount)                                               AS avg_basket_value
                          FROM customer c
                                 LEFT JOIN sales s ON c.id = s.customer_id
                          WHERE s.sale_date >= (CURRENT_DATE - '1 year'::interval)
                            AND s.statut::text = 'CLOSED'::text
                            AND s.canceled = false
                            AND s.ca::text = 'CA'::text
                          GROUP BY c.id, c.first_name, c.last_name, c.phone),
     rfm_scores AS (SELECT customer_metrics.customer_id,
                           customer_metrics.customer_name,
                           customer_metrics.phone,
                           customer_metrics.last_purchase_date,
                           customer_metrics.days_since_last_purchase,
                           customer_metrics.nb_purchases_last_year,
                           customer_metrics.total_spent_last_year,
                           customer_metrics.avg_basket_value,
                           CASE
                             WHEN customer_metrics.days_since_last_purchase <= 30::numeric THEN 5
                             WHEN customer_metrics.days_since_last_purchase <= 60::numeric THEN 4
                             WHEN customer_metrics.days_since_last_purchase <= 90::numeric THEN 3
                             WHEN customer_metrics.days_since_last_purchase <= 180::numeric THEN 2
                             ELSE 1
                             END AS recency_score,
                           CASE
                             WHEN customer_metrics.nb_purchases_last_year >= 20 THEN 5
                             WHEN customer_metrics.nb_purchases_last_year >= 10 THEN 4
                             WHEN customer_metrics.nb_purchases_last_year >= 5 THEN 3
                             WHEN customer_metrics.nb_purchases_last_year >= 2 THEN 2
                             ELSE 1
                             END AS frequency_score,
                           CASE
                             WHEN customer_metrics.total_spent_last_year >= 500000 THEN 5
                             WHEN customer_metrics.total_spent_last_year >= 200000 THEN 4
                             WHEN customer_metrics.total_spent_last_year >= 100000 THEN 3
                             WHEN customer_metrics.total_spent_last_year >= 50000 THEN 2
                             ELSE 1
                             END AS monetary_score
                    FROM customer_metrics)
SELECT customer_id,
       customer_name,
       phone,
       last_purchase_date,
       days_since_last_purchase,
       nb_purchases_last_year,
       total_spent_last_year,
       avg_basket_value,
       recency_score,
       frequency_score,
       monetary_score,
       recency_score * 100 + frequency_score * 10 + monetary_score AS rfm_segment,
       CASE
         WHEN recency_score >= 4 AND frequency_score >= 4 AND monetary_score >= 4 THEN 'CHAMPION'::text
         WHEN recency_score >= 4 AND frequency_score >= 3 THEN 'LOYAL'::text
         WHEN recency_score >= 4 AND monetary_score >= 4 THEN 'BIG_SPENDER'::text
         WHEN recency_score >= 4 THEN 'ACTIVE'::text
         WHEN recency_score = 3 THEN 'AT_RISK'::text
         WHEN recency_score <= 2 AND frequency_score >= 3 THEN 'NEED_ATTENTION'::text
         ELSE 'INACTIVE'::text
         END                                                       AS customer_classification,
       now()                                                       AS last_updated
FROM rfm_scores;

comment on materialized view mv_customer_rfm is 'Customer RFM segmentation with filters: statut=CLOSED, canceled=false, ca=CA';



create unique index idx_mv_customer_rfm_unique
  on mv_customer_rfm (customer_id);

create index idx_mv_customer_rfm_classification
  on mv_customer_rfm (customer_classification);

create index idx_mv_customer_rfm_segment
  on mv_customer_rfm (rfm_segment desc);

create index idx_mv_customer_rfm_recency
  on mv_customer_rfm (recency_score desc);

create index idx_mv_customer_rfm_frequency
  on mv_customer_rfm (frequency_score desc);

create index idx_mv_customer_rfm_monetary
  on mv_customer_rfm (monetary_score desc);

create materialized view mv_supplier_performance as
WITH supplier_orders AS (SELECT f_1.id                                          AS fournisseur_id,
                                f_1.libelle                                     AS fournisseur_name,
                                f_1.code                                        AS fournisseur_code,
                                c.id                                            AS commande_id,
                                c.order_date,
                                c.receipt_date,
                                c.final_amount,
                                c.order_status,
                                CASE
                                  WHEN c.receipt_date IS NOT NULL AND c.order_date IS NOT NULL
                                    THEN EXTRACT(day FROM
                                                 age(c.receipt_date::timestamp with time zone,
                                                     c.order_date::timestamp with time zone))
                                  ELSE NULL::numeric
                                  END                                           AS delivery_days,
                                COALESCE(sum(ol.quantity_received), 0::bigint)  AS total_received,
                                COALESCE(sum(ol.quantity_requested), 0::bigint) AS total_requested
                         FROM fournisseur f_1
                                LEFT JOIN commande c ON f_1.id = c.fournisseur_id
                                LEFT JOIN order_line ol
                                          ON c.id = ol.commande_id AND c.order_date = ol.order_date
                         WHERE c.order_status::text = 'RECEIVED'::text
                         GROUP BY f_1.id, f_1.libelle, f_1.code, c.id, c.order_date, c.receipt_date,
                                  c.final_amount, c.order_status),
     recent_30d AS (SELECT supplier_orders.fournisseur_id,
                           count(DISTINCT supplier_orders.commande_id) AS nb_orders_30d,
                           sum(supplier_orders.final_amount)           AS purchase_amount_30d
                    FROM supplier_orders
                    WHERE supplier_orders.order_date >= (CURRENT_DATE - '30 days'::interval)
                    GROUP BY supplier_orders.fournisseur_id),
     recent_12m AS (SELECT supplier_orders.fournisseur_id,
                           count(DISTINCT supplier_orders.commande_id) AS nb_orders_12m,
                           sum(supplier_orders.final_amount)           AS purchase_amount_12m
                    FROM supplier_orders
                    WHERE supplier_orders.order_date >= (CURRENT_DATE - '1 year'::interval)
                    GROUP BY supplier_orders.fournisseur_id),
     delivery_metrics AS (SELECT supplier_orders.fournisseur_id,
                                 round(avg(supplier_orders.delivery_days), 0) AS avg_delivery_days,
                                 min(supplier_orders.delivery_days)           AS min_delivery_days,
                                 max(supplier_orders.delivery_days)           AS max_delivery_days,
                                 CASE
                                   WHEN sum(supplier_orders.total_requested) > 0::numeric
                                     THEN round(sum(supplier_orders.total_received) /
                                                sum(supplier_orders.total_requested) *
                                                100::numeric, 2)
                                   ELSE 0::numeric
                                   END                                        AS conformity_rate_pct
                          FROM supplier_orders
                          WHERE supplier_orders.delivery_days IS NOT NULL
                            AND supplier_orders.order_date >= (CURRENT_DATE - '1 year'::interval)
                          GROUP BY supplier_orders.fournisseur_id)
SELECT f.id                                         AS fournisseur_id,
       f.libelle                                    AS fournisseur_name,
       f.code                                       AS fournisseur_code,
       f.phone,
       f.mobile,
       COALESCE(r30.nb_orders_30d, 0::bigint)       AS nb_orders_last_30_days,
       COALESCE(r30.purchase_amount_30d, 0::bigint) AS purchase_amount_last_30_days,
       COALESCE(r12.nb_orders_12m, 0::bigint)       AS nb_orders_last_12_months,
       COALESCE(r12.purchase_amount_12m, 0::bigint) AS purchase_amount_last_12_months,
       COALESCE(dm.avg_delivery_days, 0::numeric)   AS avg_delivery_days,
       COALESCE(dm.min_delivery_days, 0::numeric)   AS min_delivery_days,
       COALESCE(dm.max_delivery_days, 0::numeric)   AS max_delivery_days,
       COALESCE(dm.conformity_rate_pct, 0::numeric) AS conformity_rate_pct,
       CASE
         WHEN r12.purchase_amount_12m > 0 THEN round(
           LEAST(r12.purchase_amount_12m::numeric / 10000000.0, 1::numeric) * 40::numeric +
                                                                                 GREATEST(1::numeric - COALESCE(dm.avg_delivery_days, 30::numeric) / 30.0,
                                                                                          0::numeric) * 30::numeric +
                                                                                                           COALESCE(dm.conformity_rate_pct, 0::numeric) * 0.3, 2)
         ELSE 0::numeric
         END                                        AS performance_score,
       now()                                        AS last_updated
FROM fournisseur f
  LEFT JOIN recent_30d r30 ON f.id = r30.fournisseur_id
  LEFT JOIN recent_12m r12 ON f.id = r12.fournisseur_id
  LEFT JOIN delivery_metrics dm ON f.id = dm.fournisseur_id
WHERE COALESCE(r12.nb_orders_12m, 0::bigint) > 0;

comment on materialized view mv_supplier_performance is 'Supplier performance analysis with delivery metrics, purchase volumes, and conformity rates';



create unique index idx_mv_supplier_performance_unique
  on mv_supplier_performance (fournisseur_id);

create index idx_mv_supplier_performance_name
  on mv_supplier_performance (fournisseur_name);

create index idx_mv_supplier_performance_purchase
  on mv_supplier_performance (purchase_amount_last_12_months desc);

create index idx_mv_supplier_performance_score
  on mv_supplier_performance (performance_score desc);

create index idx_mv_supplier_performance_delivery
  on mv_supplier_performance (avg_delivery_days);

create materialized view mv_semois_suggestion as
SELECT p.id                                                                    AS produit_id,
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
                 WHERE vma.row_num <= 6), 0::numeric)::integer                 AS vmm,
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
         30.0))::integer                                                       AS marge_securite,
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
                                                          30.0))::integer      AS stock_objectif,
  COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)                      AS stock_actuel,
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
       now()                                                                   AS vue_refresh_date
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

create materialized view mv_product_profitability as
WITH product_margins AS (SELECT p.id                                                          AS produit_id,
                                p.libelle,
                                fp.code_cip,
                                f.libelle                                                     AS categorie,
                                count(DISTINCT sl.sales_id)                                   AS nb_ventes,
                                sum(sl.quantity_requested)                                    AS qte_vendue,
                                sum(sl.sales_amount)                                          AS ca_total,
                                sum(sl.cost_amount * sl.quantity_requested)                   AS cout_achat_total,
                                sum(sl.sales_amount - sl.cost_amount * sl.quantity_requested) AS marge_brute,
                                CASE
                                  WHEN sum(sl.sales_amount) > 0 THEN round(
                                    (sum(sl.sales_amount - sl.cost_amount * sl.quantity_requested) /
                                     sum(sl.sales_amount) * 100)::numeric, 2)
                                  ELSE 0::numeric
                                  END                                                         AS taux_marge_pct,
                                CASE
                                  WHEN sum(sl.quantity_requested) > 0 THEN round(
                                    (sum(sl.sales_amount) / sum(sl.quantity_requested))::numeric,
                                    0)
                                  ELSE 0::numeric
                                  END                                                         AS prix_vente_moyen,
                                CASE
                                  WHEN sum(sl.quantity_requested) > 0 THEN round(
                                    (sum(sl.cost_amount * sl.quantity_requested) /
                                     sum(sl.quantity_requested))::numeric, 0)
                                  ELSE 0::numeric
                                  END                                                         AS prix_achat_moyen,
                                COALESCE(sum(sp.qty_stock), 0::bigint)                        AS stock_quantity,
                                COALESCE(fp.prix_achat, 0)                                    AS prix_achat_unitaire,
                                COALESCE(fp.prix_uni, 0)                                      AS prix_vente_unitaire,
                                CASE
                                  WHEN COALESCE(sum(sp.qty_stock) * fp.prix_achat, 0::bigint) > 0
                                    THEN round((sum(sl.sales_amount) /
                                                (sum(sp.qty_stock) * fp.prix_achat) *
                                                12)::numeric, 2)
                                  ELSE 0::numeric
                                  END                                                         AS taux_rotation_annuel
                         FROM produit p
                                LEFT JOIN fournisseur_produit fp
                                          ON p.fournisseur_produit_principal_id = fp.id
                                LEFT JOIN famille_produit f ON p.famille_id = f.id
                                LEFT JOIN sales_line sl ON p.id = sl.produit_id
                                LEFT JOIN sales s ON sl.sales_id = s.id
                                LEFT JOIN stock_produit sp ON p.id = sp.produit_id
                         WHERE s.statut::text = 'CLOSED'::text
                           AND s.sale_date >= (CURRENT_DATE - '1 year'::interval)
                           AND p.status::text = 'ENABLE'::text
                           AND s.canceled = false
                           AND s.ca::text = 'CA'::text
                         GROUP BY p.id, p.libelle, fp.code_cip, f.libelle, fp.prix_achat,
                                  fp.prix_uni),
     bcg_classification AS (SELECT product_margins.produit_id,
                                   product_margins.libelle,
                                   product_margins.code_cip,
                                   product_margins.categorie,
                                   product_margins.nb_ventes,
                                   product_margins.qte_vendue,
                                   product_margins.ca_total,
                                   product_margins.cout_achat_total,
                                   product_margins.marge_brute,
                                   product_margins.taux_marge_pct,
                                   product_margins.prix_vente_moyen,
                                   product_margins.prix_achat_moyen,
                                   product_margins.stock_quantity,
                                   product_margins.prix_achat_unitaire,
                                   product_margins.prix_vente_unitaire,
                                   product_margins.taux_rotation_annuel,
                                   CASE
                                     WHEN product_margins.taux_marge_pct >= 20::numeric AND
                                          product_margins.taux_rotation_annuel >= 6::numeric
                                       THEN 'STAR'::text
                                     WHEN product_margins.taux_marge_pct >= 20::numeric AND
                                          product_margins.taux_rotation_annuel < 6::numeric
                                       THEN 'CASH_COW'::text
                                     WHEN product_margins.taux_marge_pct < 20::numeric AND
                                          product_margins.taux_rotation_annuel >= 6::numeric
                                       THEN 'QUESTION_MARK'::text
                                     WHEN product_margins.taux_marge_pct < 20::numeric AND
                                          product_margins.taux_rotation_annuel < 6::numeric
                                       THEN 'DOG'::text
                                     ELSE 'UNDEFINED'::text
                                     END AS bcg_category
                            FROM product_margins)
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

create materialized view mv_daily_sales_summary as
SELECT sale_date,
       dtype                               AS type_vente,
       count(*)                            AS nb_ventes,
       sum(sales_amount)                   AS ca_total,
       sum(sales_amount - discount_amount) AS ca_net,
       avg(sales_amount)                   AS panier_moyen,
       sum(discount_amount)                AS total_remises,
       now()                               AS last_updated
FROM sales s
WHERE statut::text = 'CLOSED'::text
  AND canceled = false
  AND ca::text = 'CA'::text
GROUP BY sale_date, dtype;



create unique index idx_mv_daily_sales_unique
  on mv_daily_sales_summary (sale_date, type_vente);

create index idx_mv_daily_sales_date
  on mv_daily_sales_summary (sale_date);

create index idx_mv_daily_sales_type
  on mv_daily_sales_summary (type_vente);

create materialized view mv_stock_rotation as
WITH product_sales AS (SELECT p.id                                               AS produit_id,
                              p.libelle,
                              fp.code_cip,
                              f.libelle                                          AS categorie,
                              COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) AS stock_quantity,
                              fp.prix_achat                                      AS unit_cost,
                              COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                              fp.prix_achat                                      AS stock_value,
                              COALESCE(sales_30d.ca, 0::bigint)                  AS ca_last_30_days,
                              COALESCE(sales_30d.qty_sold, 0::bigint)            AS qty_sold_last_30_days,
                              COALESCE(sales_30d.nb_sales, 0::bigint)            AS nb_sales_last_30_days,
                              COALESCE(sales_12m.ca, 0::bigint)                  AS ca_last_12_months,
                              COALESCE(sales_12m.qty_sold, 0::bigint)            AS qty_sold_last_12_months,
                              CASE
                                WHEN (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                      fp.prix_achat) > 0 THEN round(
                                  (COALESCE(sales_12m.ca, 0::bigint) /
                                   (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                    fp.prix_achat))::numeric, 2)
                                ELSE 0::numeric
                                END                                              AS rotation_rate_annual,
                              CASE
                                WHEN COALESCE(sales_12m.ca, 0::bigint) > 0 AND
                                     (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                      fp.prix_achat) > 0 THEN round((365 / NULLIF(
                                  COALESCE(sales_12m.ca, 0::bigint) /
                                  (COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint) *
                                   fp.prix_achat), 0))::numeric, 0)
                                ELSE 999::numeric
                                END                                              AS avg_days_in_stock
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
       now() AS last_updated
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
                                        END                                                    AS classe_pareto,
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

comment on index idx_mv_abc_pareto_unique is 'Unique index required for concurrent refresh of mv_abc_pareto_analysis';


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



create materialized view mv_dashboard_ca_daily as
WITH daily_sales AS (SELECT s.sale_date,
                            s.id                                            AS sale_id,
                            s.sales_amount,
                            s.discount_amount,
                            COALESCE((SELECT sum(sl.cost_amount * sl.quantity_requested) AS sum
                                      FROM sales_line sl
                                      WHERE sl.sales_id = s.id), 0::bigint) AS cost_amount,
                            s.customer_id,
                            s.rest_to_pay + s.part_tiers_payant             AS montant_restant,
                            COALESCE((SELECT sum(pt.paid_amount) AS sum
                                      FROM payment_transaction pt
                                      WHERE pt.sale_id = s.id), 0::bigint)  AS montant_encaisse
                     FROM sales s
                     WHERE s.statut::text = 'CLOSED'::text
                       AND s.canceled = false
                       AND s.ca::text = 'CA'::text)
SELECT sale_date,
       count(DISTINCT sale_id)             AS nb_transactions,
       sum(sales_amount)                   AS ca_total,
       sum(sales_amount - discount_amount) AS ca_net,
       avg(sales_amount)                   AS panier_moyen,
       sum(cost_amount)                    AS cout_total,
       sum(sales_amount - cost_amount)     AS marge_brute,
       CASE
         WHEN sum(sales_amount) > 0 THEN round(
           sum(sales_amount - cost_amount) * 100.0 / sum(sales_amount)::numeric, 2)
         ELSE 0::numeric
         END                               AS taux_marge_pct,
       count(DISTINCT customer_id)         AS nb_clients,
       sum(montant_encaisse)               AS montant_encaisse,
       sum(montant_restant)                AS montant_credit
FROM daily_sales
GROUP BY sale_date;



create unique index idx_mv_dashboard_ca_daily_unique
  on mv_dashboard_ca_daily (sale_date desc);

create materialized view mv_dashboard_ca_payment_methods as
SELECT pt.transaction_date   AS payment_date,
       pm.libelle            AS payment_method,
       pm.code               AS payment_code,
       count(DISTINCT pt.id) AS nb_payments,
       sum(pt.paid_amount)   AS montant_total,
       avg(pt.paid_amount)   AS montant_moyen
FROM payment_transaction pt
       JOIN payment_mode pm ON pt.payment_mode_code::text = pm.code::text
       JOIN sales s ON pt.sale_id = s.id
WHERE s.statut::text = 'CLOSED'::text
  AND s.canceled = false
  AND s.ca::text = 'CA'::text
GROUP BY pt.transaction_date, pm.libelle, pm.code;



create index idx_mv_dashboard_ca_payment_date
  on mv_dashboard_ca_payment_methods (payment_date desc);

create index idx_mv_dashboard_ca_payment_code
  on mv_dashboard_ca_payment_methods (payment_code);

create unique index idx_mv_dashboard_ca_payment_methods_unique
  on mv_dashboard_ca_payment_methods (payment_date, payment_code);

create materialized view mv_dashboard_ca_product_families as
WITH product_sales AS (SELECT s.sale_date,
                              COALESCE(fp.libelle, 'Non classé'::character varying)    AS famille,
                              sl.quantity_requested,
                              sl.sales_amount,
                              sl.cost_amount * sl.quantity_requested                   AS cost_amount,
                              sl.sales_amount - sl.cost_amount * sl.quantity_requested AS marge
                       FROM sales s
                              JOIN sales_line sl ON s.id = sl.sales_id
                              JOIN produit p ON sl.produit_id = p.id
                              LEFT JOIN famille_produit fp ON p.famille_id = fp.id
                       WHERE s.statut::text = 'CLOSED'::text
                         AND s.canceled = false
                         AND s.ca::text = 'CA'::text)
SELECT sale_date,
       famille,
       sum(quantity_requested) AS quantite_vendue,
       sum(sales_amount)       AS ca_total,
       sum(cost_amount)        AS cout_total,
       sum(marge)              AS marge_brute,
       CASE
         WHEN sum(sales_amount) > 0 THEN round(
           sum(marge)::numeric * 100.0 / sum(sales_amount)::numeric, 2)
         ELSE 0::numeric
         END                   AS taux_marge_pct,
       count(*)                AS nb_lignes_vente
FROM product_sales
GROUP BY sale_date, famille;



create index idx_mv_dashboard_ca_families_date
  on mv_dashboard_ca_product_families (sale_date desc);

create index idx_mv_dashboard_ca_families_name
  on mv_dashboard_ca_product_families (famille);

create unique index idx_mv_dashboard_ca_families_unique
  on mv_dashboard_ca_product_families (sale_date, famille);

create materialized view mv_profitability_summary as
SELECT sum(ca_total)                                           AS ca_total_global,
       sum(cout_achat_total)                                   AS cout_achat_global,
       sum(marge_brute)                                        AS marge_brute_globale,
       CASE
         WHEN sum(ca_total) > 0::numeric THEN round(
           sum(marge_brute) / sum(ca_total) * 100::numeric, 2)
         ELSE 0::numeric
         END                                                   AS taux_marge_moyen,
       count(*)                                                AS total_produits,
       count(*) FILTER (WHERE bcg_category = 'STAR'::text)     AS nb_stars,
       COALESCE(sum(ca_total) FILTER (WHERE bcg_category = 'STAR'::text),
                0::numeric)                                    AS ca_stars,
       COALESCE(sum(marge_brute) FILTER (WHERE bcg_category = 'STAR'::text),
                0::numeric)                                    AS marge_stars,
       count(*) FILTER (WHERE bcg_category = 'CASH_COW'::text) AS nb_cash_cows,
       COALESCE(sum(ca_total) FILTER (WHERE bcg_category = 'CASH_COW'::text),
                0::numeric)                                    AS ca_cash_cows,
       COALESCE(sum(marge_brute) FILTER (WHERE bcg_category = 'CASH_COW'::text),
                0::numeric)                                    AS marge_cash_cows,
       count(*)
       FILTER (WHERE bcg_category = 'QUESTION_MARK'::text)     AS nb_question_marks,
       COALESCE(sum(ca_total) FILTER (WHERE bcg_category = 'QUESTION_MARK'::text),
                0::numeric)                                    AS ca_question_marks,
       COALESCE(sum(marge_brute) FILTER (WHERE bcg_category = 'QUESTION_MARK'::text),
                0::numeric)                                    AS marge_question_marks,
       count(*) FILTER (WHERE bcg_category = 'DOG'::text)      AS nb_dogs,
       COALESCE(sum(ca_total) FILTER (WHERE bcg_category = 'DOG'::text),
                0::numeric)                                    AS ca_dogs,
       COALESCE(sum(marge_brute) FILTER (WHERE bcg_category = 'DOG'::text),
                0::numeric)                                    AS marge_dogs,
       now()                                                   AS last_updated
FROM mv_product_profitability;



create view v_produit_metriques_classification
    (produit_id, libelle, classe_actuelle, vmm_12_mois, ca_12_mois, qte_vendue_12_mois,
     frequence_vente_mois,
     stock_actuel, rotation_annuelle, anciennete_mois, est_nouveau_produit, date_creation_produit)
as
SELECT id                                                                   AS produit_id,
       libelle,
       classe_criticite                                                     AS classe_actuelle,
       COALESCE((SELECT avg(vma.quantite_vendue) AS avg
                 FROM ventes_mensuelles_agregees vma
                 WHERE vma.produit_id = p.id
                   AND vma.annee_mois::text >= to_char(now() - '1 year'::interval, 'YYYY-MM'::text)
                   AND vma.is_frozen = true),
                0::numeric)::integer                                        AS vmm_12_mois,
  COALESCE((SELECT sum(vma.montant_ca) AS sum
            FROM ventes_mensuelles_agregees vma
            WHERE vma.produit_id = p.id
              AND vma.annee_mois::text >= to_char(now() - '1 year'::interval, 'YYYY-MM'::text)
                   AND vma.is_frozen = true),
           0::bigint)                                                  AS ca_12_mois,
       COALESCE((SELECT sum(vma.quantite_vendue) AS sum
                 FROM ventes_mensuelles_agregees vma
                 WHERE vma.produit_id = p.id
                   AND vma.annee_mois::text >= to_char(now() - '1 year'::interval, 'YYYY-MM'::text)
                   AND vma.is_frozen = true),
                0::bigint)::integer                                         AS qte_vendue_12_mois,
  COALESCE((SELECT count(*) AS count
            FROM ventes_mensuelles_agregees vma
            WHERE vma.produit_id = p.id
              AND vma.annee_mois::text >= to_char(now() - '1 year'::interval, 'YYYY-MM'::text)
                   AND vma.is_frozen = true
                   AND vma.quantite_vendue > 0),
           0::bigint)::integer                                         AS frequence_vente_mois,
  COALESCE((SELECT sum(sp.qty_stock + sp.qty_ug) AS sum
            FROM stock_produit sp
                   JOIN storage s ON sp.storage_id = s.id
            WHERE sp.produit_id = p.id
              AND s.magasin_id = 1),
           0::bigint)::integer                                         AS stock_actuel,
  CASE
    WHEN COALESCE((SELECT sum(sp.qty_stock + sp.qty_ug) AS sum
                   FROM stock_produit sp
                          JOIN storage s ON sp.storage_id = s.id
                   WHERE sp.produit_id = p.id
                     AND s.magasin_id = 1), 0::bigint) > 0 THEN
      COALESCE((SELECT sum(vma.quantite_vendue) AS sum
                FROM ventes_mensuelles_agregees vma
                WHERE vma.produit_id = p.id
                  AND vma.annee_mois::text >=
                           to_char(now() - '1 year'::interval, 'YYYY-MM'::text)
                       AND vma.is_frozen = true), 0::bigint)::numeric /
           COALESCE((SELECT sum(sp.qty_stock + sp.qty_ug) AS sum
                     FROM stock_produit sp
                            JOIN storage s ON sp.storage_id = s.id
                     WHERE sp.produit_id = p.id
                       AND s.magasin_id = 1), 1::bigint)::numeric
         ELSE 0::numeric
         END                                                                AS rotation_annuelle,
       (EXTRACT(year FROM age(now(), created_at::timestamp with time zone)) * 12::numeric +
        EXTRACT(month FROM
                age(now(), created_at::timestamp with time zone)))::integer AS anciennete_mois,
       CASE
         WHEN (EXTRACT(year FROM age(now(), created_at::timestamp with time zone)) * 12::numeric +
               EXTRACT(month FROM age(now(), created_at::timestamp with time zone))) < 6::numeric
           THEN true
         ELSE false
         END                                                                AS est_nouveau_produit,
       created_at                                                           AS date_creation_produit
FROM produit p
WHERE status::text = 'ENABLE'::text
  AND type_produit::text <> 'DETAIL'::text;

comment on view v_produit_metriques_classification is 'Vue des métriques de classification pour chaque produit actif (stock filtré sur magasin par défaut id=1)';



create procedure proc_close_inventory(IN p_store_inventory_id bigint, INOUT p_nombreligne integer)
  language plpgsql
as
$$
DECLARE
v_produit_id           INT;
v_storage_id           INT;
  v_entity_id            bigint;
  v_quantity_on_hand     INT;
  v_quantity_init        INT;
  v_inventory_value_cost INT;
  v_last_unit_price      INT;
  v_user_id              INT;
  v_magasin_id           INT;
  v_updated_at           TIMESTAMP;
  curbl CURSOR FOR
SELECT a.quantity_on_hand,
       s.storage_id,
       a.produit_id,
       s.user_id,
       u.magasin_id,
       a.id,
       a.inventory_value_cost,
       a.last_unit_price,
       a.quantity_init,
       a.updated_at
FROM store_inventory_line a
       JOIN store_inventory s ON s.id = a.store_inventory_id
       JOIN app_user u ON s.user_id = u.id
WHERE s.id = p_store_inventory_id;
BEGIN
  p_nombreLigne := 0;
  OPEN curbl;
  LOOP
    FETCH curbl INTO v_quantity_on_hand, v_storage_id, v_produit_id, v_user_id, v_magasin_id, v_entity_id, v_inventory_value_cost, v_last_unit_price, v_quantity_init, v_updated_at;
    EXIT WHEN NOT FOUND;

    UPDATE stock_produit st
    SET updated_at  = now(),
        qty_ug      = 0,
        qty_stock   = v_quantity_on_hand,
        qty_virtual = v_quantity_on_hand
    WHERE st.produit_id = v_produit_id
      AND st.storage_id = v_storage_id;

    INSERT INTO inventory_transaction(cost_amount, created_at, entity_id, mouvement_type, quantity,
                                      quantity_after, quantity_befor, regular_unit_price,
                                      magasin_id, produit_id, user_id)
    VALUES (v_inventory_value_cost, v_updated_at, v_entity_id, 'INVENTAIRE', v_quantity_on_hand,
            v_quantity_on_hand, v_quantity_init, v_last_unit_price, v_magasin_id, v_produit_id,
            v_user_id);

    p_nombreLigne := p_nombreLigne + 1;
  END LOOP;
  CLOSE curbl;
END;
$$;



create function gettopqty80percentproducts(startdate date, enddate date, calist character varying,
                                           statutlist character varying)
  returns TABLE
          (
  libelle      character varying,
  code_cip     character varying,
  qte_totale   bigint,
  total_global bigint,
  pourcentage  numeric
          )
  language sql
as
$$
WITH ventes_par_produit AS (SELECT sl.produit_id,
                                   SUM(sl.quantity_requested) AS qte_totale
                            FROM sales_line sl
                                   JOIN sales s ON s.id = sl.sales_id
                            WHERE s.sale_date BETWEEN startDate AND endDate
                              AND s.ca = ANY (string_to_array(caList, ','))
                              AND s.statut = ANY (string_to_array(statutList, ','))
                              AND s.canceled = false
                            GROUP BY sl.produit_id),
     total_global AS (SELECT SUM(qte_totale) AS total_global
                      FROM ventes_par_produit),
     classement AS (SELECT vp.produit_id,
                           vp.qte_totale,
                           SUM(vp.qte_totale) OVER (ORDER BY vp.qte_totale DESC) AS cumul,
                           tg.total_global
                    FROM ventes_par_produit vp
                           CROSS JOIN total_global tg)
SELECT p.libelle,
       MAX(fp.code_cip)::VARCHAR                       AS code_cip,
  c.qte_totale::BIGINT,
  c.total_global::BIGINT,
  ROUND((c.qte_totale / c.total_global) * 100, 2) AS pourcentage
FROM classement c
       JOIN produit p ON p.id = c.produit_id
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
WHERE c.cumul <= 0.8 * c.total_global
GROUP BY p.id, p.libelle, c.qte_totale, c.total_global
ORDER BY c.qte_totale DESC;
$$;



create function gettopamount80percentproducts(startdate date, enddate date,
                                              calist character varying,
                                              statutlist character varying)
  returns TABLE
          (
  libelle      character varying,
  code_cip     character varying,
  total_global numeric,
  sales_amount numeric,
  pourcentage  numeric
          )
  language sql
as
$$
WITH ventes_par_produit AS (SELECT sl.produit_id,
                                   SUM(sl.sales_amount) AS sales_amount
                            FROM sales_line sl
                                   JOIN sales s ON s.id = sl.sales_id
                            WHERE s.sale_date BETWEEN startDate AND endDate
                              AND s.ca = ANY (string_to_array(caList, ','))
                              AND s.statut = ANY (string_to_array(statutList, ','))
                              AND s.canceled = false
                            GROUP BY sl.produit_id),
     total_global AS (SELECT SUM(sales_amount) AS total_global
                      FROM ventes_par_produit),
     classement AS (SELECT vp.produit_id,
                           vp.sales_amount,
                           SUM(vp.sales_amount) OVER (ORDER BY vp.sales_amount DESC) AS cumul,
                           tg.total_global
                    FROM ventes_par_produit vp
                           CROSS JOIN total_global tg)
SELECT p.libelle,
       MAX(fp.code_cip)::VARCHAR                         AS code_cip,
  c.total_global,
       c.sales_amount,
       ROUND((c.sales_amount / c.total_global) * 100, 2) AS pourcentage
FROM classement c
       JOIN produit p ON p.id = c.produit_id
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
WHERE c.cumul <= 0.8 * c.total_global
GROUP BY p.id, p.libelle, c.total_global, c.sales_amount
ORDER BY c.sales_amount DESC;
$$;


create function sales_summary_json(p_start_date date, p_end_date date, p_statuts text[],
                                   p_cas text[], p_canceled boolean) returns jsonb
  language sql
as
$$
with
-- Filtrer les ventes selon les paramètres
filtered_sales as (select id, sale_date
                   from sales
                   where sale_date between p_start_date and p_end_date
                     and imported = false
                     and statut = any (p_statuts)
                     and ca = any (p_cas)
                     and canceled = p_canceled),

-- Agrégats des lignes de vente
sales_line_agg as (select sum(sl.quantity_requested * sl.cost_amount)        as cost_amount,
                          sum(sl.quantity_requested * sl.regular_unit_price) as sales_amount,
                          ceiling(sum((sl.quantity_requested * sl.regular_unit_price) /
                                      (1 + (sl.tax_value / 100))))           as total_sales_excl_tax

                   from sales_line sl
                          join filtered_sales fs on fs.id = sl.sales_id
                   WHERE sl.sale_date = fs.sale_date
                     AND sl.to_ignore = false),
-- Agrégats des paiements
payment_agg as (select sum(p.paid_amount) as total_paid_amount,
                       sum(p.reel_amount) as total_reel_amount
                from payment_transaction p
                       join filtered_sales fs on fs.id = p.sale_id

                where p.dtype = 'SalePayment'
                  AND p.sale_date = fs.sale_date),
-- Agrégats de la table sales
sales_agg as (select sum(s.amount_to_be_paid)               as total_amount_to_be_paid,
                     sum(s.discount_amount)                 as total_discount_amount,
                     sum(s.amount_to_be_taken_into_account) as total_amount_to_account,

                     sum(s.part_assure)                     as total_part_assure,
                     sum(s.part_tiers_payant)               as total_part_tiers_payant,
                     sum(s.rest_to_pay)                     as total_rest_to_pay,
                     sum(s.payroll_amount)                  as total_payroll_amount,
                     count(distinct s.id)                   as distinct_sales_count
              from sales s
                     join filtered_sales fs on fs.id = s.id
              WHERE s.sale_date = fs.sale_date)
-- Construction finale du JSONB
select jsonb_build_object(
         'salesAmount', coalesce(sla.sales_amount, 0),
         'amountToBePaid', sa.total_amount_to_be_paid,
         'discountAmount', sa.total_discount_amount,
         'amountToBeTakenIntoAccount', sa.total_amount_to_account,
         'netAmount', coalesce(sla.sales_amount, 0) - coalesce(sa.total_discount_amount, 0),
         'partAssure', coalesce(sa.total_part_assure, 0),
         'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
         'restToPay', sa.total_rest_to_pay,
         'payrollAmount', sa.total_payroll_amount,
         'saleCount', sa.distinct_sales_count,
         'costAmount', coalesce(sla.cost_amount, 0),
         'montantHt', coalesce(sla.total_sales_excl_tax, 0),
         'paidAmount', coalesce(pa.total_paid_amount, 0),
         'realNetAmount', coalesce(pa.total_reel_amount, 0)
       )
from sales_agg sa
       cross join sales_line_agg sla
       cross join payment_agg pa;
$$;



create function sales_balance(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                              p_exclude_free_qty boolean DEFAULT false,
                              p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date, dtype
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.dtype,
                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.cost_amount
                               )                                           as cost_amount,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND sl.sale_date = fs.sale_date
                        group by fs.dtype),
     payment_agg as (select dtype,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', code,
                                'libelle', libelle,
                                'paidAmount', total_paid_amount,
                                'realAmount', total_real_amount
                              )
                            ) as payments
                     from (select fs.dtype,
                                  pm.code,
                                  pm.libelle,
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND p.sale_date = fs.sale_date
                           group by fs.dtype, pm.code, pm.libelle) t
                     group by dtype),
     sales_agg as (select fs.dtype,
                          sum(s.amount_to_be_paid)                                   as total_amount_to_be_paid,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_assure)                                         as total_part_assure,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and s.sale_date = fs.sale_date
                   group by fs.dtype)
select jsonb_agg(
         jsonb_build_object(
           'typeSale', sa.dtype,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'discountAmount', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'partAssure', coalesce(sa.total_part_assure, 0),
           'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
           'montantDiffere', sa.total_rest_to_pay,
           'count', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


create function sales_summary_by_type_json(p_start_date date, p_end_date date, p_statuts text[],
                                           p_cas text[],
                                           p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date, dtype
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)
                          and canceled = false),
     sales_line_agg as (select fs.dtype,
                               sum(sl.quantity_requested * sl.cost_amount)        as cost_amount,
                               sum(sl.quantity_requested * sl.regular_unit_price) as sales_amount,
                               ceiling(sum((sl.quantity_requested * sl.regular_unit_price) /
                                           nullif(1 + (sl.tax_value / 100), 0)))  as total_sales_excl_tax
                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND fs.sale_date = sl.sale_date
                        group by fs.dtype),
     payment_agg as (select fs.dtype,
                            sum(p.paid_amount) as total_paid_amount,
                            sum(p.reel_amount) as total_reel_amount
                     from payment_transaction p
                            join filtered_sales fs on fs.id = p.sale_id
                     where p.dtype = 'SalePayment'
                       AND fs.sale_date = p.sale_date
                     group by fs.dtype),
     sales_agg as (select fs.dtype,
                          sum(s.amount_to_be_paid)               as total_amount_to_be_paid,
                          sum(s.discount_amount)                 as total_discount_amount,
                          sum(s.amount_to_be_taken_into_account) as total_amount_to_account,
                          sum(s.part_assure)                     as total_part_assure,
                          sum(s.part_tiers_payant)               as total_part_tiers_payant,
                          sum(s.rest_to_pay)                     as total_rest_to_pay,
                          sum(s.payroll_amount)                  as total_payroll_amount,
                          count(distinct s.id)                   as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and fs.sale_date = s.sale_date
                   group by fs.dtype)
select jsonb_agg(
         jsonb_build_object(
           'type', sa.dtype,
           'salesAmount', coalesce(sla.sales_amount, 0),
           'amountToBePaid', sa.total_amount_to_be_paid,
           'discountAmount', sa.total_discount_amount,
           'amountToBeTakenIntoAccount', sa.total_amount_to_account,
           'netAmount', coalesce(sla.sales_amount, 0) - coalesce(sa.total_discount_amount, 0),
           'partAssure', coalesce(sa.total_part_assure, 0),
           'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
           'restToPay', sa.total_rest_to_pay,
           'payrollAmount', sa.total_payroll_amount,
           'saleCount', sa.distinct_sales_count,
           'costAmount', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'paidAmount', coalesce(pa.total_paid_amount, 0),
           'realNetAmount', coalesce(pa.total_reel_amount, 0)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


create function sales_tva_report(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                                 p_exclude_free_qty boolean DEFAULT false,
                                 p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select sl.tax_value,
                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND sl.sale_date = fs.sale_date
                        group by sl.tax_value)
select jsonb_agg(
         jsonb_build_object(
           'codeTva', sla.tax_value,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', coalesce(sla.remise_produit, 0),
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.tax_value
       )
from sales_line_agg sla;
$$;


create function sales_tva_report_journalier(p_start_date date, p_end_date date, p_statuts text[],
                                            p_cas text[], p_exclude_free_qty boolean DEFAULT false,
                                            p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.sale_date,
                               sl.tax_value,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND sl.sale_date = fs.sale_date
                        group by fs.sale_date, sl.tax_value)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', sla.sale_date,
           'codeTva', sla.tax_value,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', coalesce(sla.remise_produit, 0),
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.sale_date, sla.tax_value
       )
from sales_line_agg sla;
$$;


create function tableau_pharmacien_report(p_start_date date, p_end_date date, p_statuts text[],
                                          p_cas text[], p_exclude_free_qty boolean DEFAULT false,
                                          p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.sale_date,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.cost_amount
                               )                                           as cost_amount,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND sl.sale_date = fs.sale_date
                        group by fs.sale_date),
     payment_agg as (select t.sale_date,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select fs.sale_date,
                                  pm.code,
                                  pm.libelle,
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND p.sale_date = fs.sale_date
                           group by fs.sale_date, pm.code, pm.libelle) t
                     group by t.sale_date),
     sales_agg as (select fs.sale_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and s.sale_date = fs.sale_date
                   group by fs.sale_date)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', sa.sale_date,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantCredit', coalesce(sa.total_part_tiers_payant, 0) + sa.total_rest_to_pay,
           'montantDiffere', sa.total_rest_to_pay,
           'nombreVente', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
           order by sa.sale_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.sale_date = sla.sale_date
       left join payment_agg pa on sa.sale_date = pa.sale_date;
$$;



create function tableau_pharmacien_month_report(p_start_date date, p_end_date date,
                                                p_statuts text[], p_cas text[],
                                                p_exclude_free_qty boolean DEFAULT false,
                                                p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, date_trunc('month', sale_date)::date as month_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.month_date,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.cost_amount
                               )                                           as cost_amount,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND date_trunc('month', sl.sale_date)::date = fs.month_date
                        group by fs.month_date),
     payment_agg as (select t.month_date,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select date_trunc('month', fs.month_date)::date as month_date,
                                  pm.code,
                                  pm.libelle,
                                  sum(p.paid_amount)                       as total_paid_amount,
                                  sum(p.reel_amount)                       as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND date_trunc('month', p.sale_date)::date = fs.month_date
                           group by date_trunc('month', fs.month_date), pm.code, pm.libelle) t
                     group by t.month_date),
     sales_agg as (select fs.month_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id
                   WHERE date_trunc('month', s.sale_date)::date = fs.month_date
                   group by fs.month_date)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', to_char(sa.month_date, 'YYYY-MM-DD'),
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantCredit', coalesce(sa.total_part_tiers_payant, 0) + sa.total_rest_to_pay,
           'montantDiffere', sa.total_rest_to_pay,
           'nombreVente', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         ) order by sa.month_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.month_date = sla.month_date
       left join payment_agg pa on sa.month_date = pa.month_date;
$$;


create function tableau_pharmacien_commandes_report(p_start_date date, p_end_date date, p_order_status text) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'mvtDate', mvtDate,
                     'montantNet', net_amount,
                     'montantTaxe', tax_amount,
                     'montantTtc', gross_amount,
                     'montantRemise', discount_amount,
                     'groupeGrossisteId', group_id,
                     'groupeGrossiste', group_libelle,
                     'ordreAffichage', group_order
                   )
                 )
          FROM (SELECT c.order_date                       AS mvtDate,
                       SUM(c.gross_amount - c.tax_amount) AS net_amount,
                       SUM(c.tax_amount)                  AS tax_amount,
                       SUM(c.gross_amount)                AS gross_amount,
                       SUM(c.discount_amount)             AS discount_amount,
                       g.id                               AS group_id,
                       g.libelle                          AS group_libelle,
                       g.odre                             AS group_order
                FROM commande c
                       JOIN fournisseur f ON f.id = c.fournisseur_id
                       JOIN groupe_fournisseur g ON g.id = f.groupe_pournisseur_id
                WHERE c.order_date BETWEEN p_start_date AND p_end_date

                  AND c.order_status = p_order_status
                GROUP BY mvtDate, g.id, g.libelle, g.odre
                ORDER BY mvtDate) sub);
END;
$$;


create function tableau_pharmacien_commandes_mois_report(p_start_date date, p_end_date date, p_order_status text) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'mvtDate', to_char(month_date, 'YYYY-MM-DD'),
                     'montantNet', net_amount,
                     'montantTaxe', tax_amount,
                     'montantTtc', gross_amount,
                     'montantRemise', discount_amount,
                     'groupeGrossisteId', group_id,
                     'groupeGrossiste', group_libelle,
                     'ordreAffichage', group_order
                   )
                 )
          FROM (SELECT date_trunc('month', c.updated_at)  AS month_date,
                       SUM(c.gross_amount - c.tax_amount) AS net_amount,
                       SUM(c.tax_amount)                  AS tax_amount,
                       SUM(c.gross_amount)                AS gross_amount,
                       SUM(c.discount_amount)             AS discount_amount,
                       g.id                               AS group_id,
                       g.libelle                          AS group_libelle,
                       g.odre                             AS group_order
                FROM commande c
                       JOIN fournisseur f ON f.id = c.fournisseur_id
                       JOIN groupe_fournisseur g ON g.id = f.groupe_pournisseur_id
                WHERE c.order_date BETWEEN p_start_date AND p_end_date

                  AND c.order_status = p_order_status
                GROUP BY month_date, g.id, g.libelle, g.odre
                ORDER BY month_date) sub);
END;
$$;


create function rapport_activite_vente_report(p_start_date date, p_end_date date, p_statuts text[],
                                              p_cas text[],
                                              p_exclude_free_qty boolean DEFAULT false,
                                              p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.cost_amount
                               )                                           as cost_amount,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore),
     payment_agg as (select jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select pm.code,
                                  pm.libelle,
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                           group by pm.code, pm.libelle) t),
     sales_agg as (select sum(s.discount_amount)   as total_discount_amount,
                          sum(s.part_tiers_payant) as total_part_tiers_payant,
                          sum(s.rest_to_pay)       as total_rest_to_pay,
                          count(distinct s.id)     as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id)
select jsonb_build_object(
         'montantTtc', coalesce(sla.sales_amount, 0),
         'montantRemise', sa.total_discount_amount,
         'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
         'montantTp', coalesce(sa.total_part_tiers_payant, 0),
         'montantDiffere', sa.total_rest_to_pay,
         'nombreVente', sa.distinct_sales_count,
         'montantTtcUg', sla.sales_ug_amount,
         'montantHtUg', sla.total_sales_ug_tax,
         'montantAchat', coalesce(sla.cost_amount, 0),
         'montantHt', coalesce(sla.total_sales_excl_tax, 0),
         'payments', coalesce(pa.payments, '[]'::jsonb)
       )
from sales_agg sa
       cross join sales_line_agg sla
       left join payment_agg pa on true;
$$;


create function find_reglement_tierspayant(p_from_date date, p_to_date date,
                                           p_search text DEFAULT NULL::text,
                                           p_offset integer DEFAULT 0,
                                           p_limit integer DEFAULT 20) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (WITH data AS (SELECT tp.full_name             AS libelle,
                               tp.categorie             AS type,
                               f.num_facture            AS num_facture,
                               SUM(it.montantReglement) AS montant_reglement,
                               SUM(it.montantFacture)   AS montant_facture
                        FROM payment_transaction p
                               JOIN facture_tiers_payant f ON p.facture_tierspayant_id = f.id
                               JOIN tiers_payant tp ON f.tiers_payant_id = tp.id
                               JOIN (SELECT s.facture_tiers_payant_id,
                                            SUM(s.montant_regle) AS montantReglement,
                                            SUM(s.montant)       AS montantFacture
                                     FROM third_party_sale_line s
                                     GROUP BY s.facture_tiers_payant_id, s.sale_date) it
                                    ON it.facture_tiers_payant_id = f.id

                        WHERE p.transaction_date BETWEEN p_from_date AND p_to_date
                          AND (
                          p_search IS NULL
                            OR tp.name ILIKE p_search
                            OR tp.full_name ILIKE p_search
                          )
                        GROUP BY tp.id, tp.full_name, tp.categorie, f.num_facture
                        ORDER BY tp.full_name
                        LIMIT p_limit OFFSET p_offset)
          SELECT jsonb_build_object(
                   'totalElements', (SELECT COUNT(*)
                                     FROM (SELECT 1
                                           FROM payment_transaction p
                                                  JOIN facture_tiers_payant f ON p.facture_tierspayant_id = f.id
                                                  JOIN tiers_payant tp ON f.tiers_payant_id = tp.id
                                           WHERE p.transaction_date BETWEEN p_from_date AND p_to_date
                                             AND (
                                             p_search IS NULL
                                               OR tp.name ILIKE p_search
                                               OR tp.full_name ILIKE p_search
                                             )
                                           GROUP BY tp.id, tp.full_name, tp.categorie,
                                                    f.num_facture) sub),
                   'content', jsonb_agg(
                     jsonb_build_object(
                       'libelle', data.libelle,
                       'type', data.type,
                       'factureNumber', data.num_facture,
                       'montantReglement', data.montant_reglement,
                       'montantFacture', data.montant_facture
                     )
                              )
                 )
          FROM data);
END;
$$;




create function get_product_order_summary(p_start_date date, p_end_date date, p_statut text,
                                          p_produit_id integer,
                                          p_group_by integer DEFAULT 0) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'groupType', p_group_by,
                     'groupBy', grp.mvt_date,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat
                   )
                     ORDER BY grp.min_date
                 )
          FROM (SELECT CASE p_group_by
                         WHEN 0 THEN to_char(d.order_date, 'YYYY-MM-DD') -- jour
                         WHEN 1 THEN to_char(d.order_date, 'YYYY-MM') -- mois
                         WHEN 2 THEN to_char(d.order_date, 'YYYY-Q') -- trimestre
                         WHEN 3 THEN to_char(d.order_date, 'YYYY') ||
                                     CASE
                                       WHEN extract(quarter from d.order_date) <= 2
                                         THEN 'S1'
                                       ELSE 'S2' END -- semestre
                         WHEN 4 THEN to_char(d.order_date, 'YYYY') -- année
                         END                                          AS mvt_date,
                       MIN(d.order_date)                              AS min_date,
                       SUM(o.quantity_received)                       AS quantite,
                       SUM(o.order_cost_amount * o.quantity_received) AS montant_achat
                FROM order_line o
                       JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id
                       JOIN commande d ON o.commande_id = d.id
                WHERE o.commande_order_date = d.order_date
                  AND fp.produit_id = p_produit_id
                  AND d.order_status = p_statut
                  AND o.order_date BETWEEN p_start_date AND p_end_date
                GROUP BY CASE p_group_by
                           WHEN 0 THEN to_char(d.order_date, 'YYYY-MM-DD')
                           WHEN 1 THEN to_char(d.order_date, 'YYYY-MM')
                           WHEN 2 THEN to_char(d.order_date, 'YYYY-Q')
                           WHEN 3 THEN to_char(d.order_date, 'YYYY') ||
                                       CASE
                                         WHEN extract(quarter from d.order_date) <= 2
                                           THEN 'S1'
                                         ELSE 'S2' END
                           WHEN 4 THEN to_char(d.order_date, 'YYYY')
                           END) grp);
END;
$$;


create function get_product_order_summary_monthly(p_start_date date, p_end_date date, p_statut text,
                                                  p_produit_id integer) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'annee', grp.annee,
                     'mois', grp.mois,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat
                   )
                     ORDER BY grp.annee DESC, grp.mois DESC
                 )
          FROM (SELECT EXTRACT(YEAR FROM o.order_date)::int           AS annee,
                  EXTRACT(MONTH FROM o.order_date)::int          AS mois,
                  SUM(o.quantity_received)                       AS quantite,
                       SUM(o.quantity_received * o.order_cost_amount) AS montant_achat
                FROM order_line o
                       JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id
                       JOIN commande d ON o.commande_id = d.id
                WHERE fp.produit_id = p_produit_id
                  AND d.order_status = p_statut
                  AND o.order_date BETWEEN p_start_date AND p_end_date
                  AND o.commande_order_date = d.order_date
                GROUP BY EXTRACT(YEAR FROM o.order_date), EXTRACT(MONTH FROM o.order_date)) grp);
END;
$$;


create function get_product_sales_summary_monthly(p_start_date date, p_end_date date,
                                                  p_statuts text[], p_cas text[],
                                                  p_produit_id integer) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'annee', grp.annee,
                     'mois', grp.mois,
                     'quantite', grp.quantite,
                     'montantTtc', grp.sales_amount
                   )
                     ORDER BY grp.annee DESC, grp.mois DESC
                 )
          FROM (SELECT EXTRACT(YEAR FROM s.sale_date)::int  AS annee,
                  EXTRACT(MONTH FROM s.sale_date)::int AS mois,
                  SUM(o.quantity_requested)            AS quantite,
                       SUM(o.sales_amount)                  AS sales_amount
                FROM sales_line o
                       JOIN sales s ON o.sales_id = s.id
                WHERE o.produit_id = p_produit_id
                  AND s.statut = ANY (p_statuts)
                  AND s.ca = ANY (p_cas)
                  AND s.sale_date BETWEEN p_start_date AND p_end_date
                  AND o.sales_sale_date = s.sale_date
                GROUP BY EXTRACT(YEAR FROM s.sale_date), EXTRACT(MONTH FROM s.sale_date)) grp);
END;
$$;


create function get_historique_vente(p_produit_id integer, p_start_date date, p_end_date date,
                                     p_statuts text[], p_cas text[], p_offset integer DEFAULT 0,
                                     p_limit integer DEFAULT 20) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (WITH data AS (SELECT s.updated_at,
                               s.number_transaction,
                               o.quantity_requested,
                               o.regular_unit_price,
                               CEIL((o.quantity_requested * o.regular_unit_price) /
                                    NULLIF(1 + (o.tax_value / 100), 0)) AS ht_amount,
                               o.sales_amount,
                               o.discount_amount,
                               u.first_name,
                               u.last_name
                        FROM sales_line o
                               JOIN sales s ON o.sales_id = s.id
                               JOIN app_user u ON s.caissier_id = u.id
                        WHERE o.produit_id = p_produit_id
                          AND s.statut = ANY (p_statuts)
                          AND s.ca = ANY (p_cas)
                          AND o.sales_sale_date = s.sale_date
                          AND s.sale_date BETWEEN p_start_date AND p_end_date
                        ORDER BY s.updated_at DESC
                        LIMIT p_limit OFFSET p_offset)
          SELECT jsonb_build_object(
                   'totalElements', (SELECT COUNT(*)
                                     FROM sales_line o
                                            JOIN sales s ON o.sales_id = s.id
                                     WHERE o.produit_id = p_produit_id
                                       AND s.statut = ANY (p_statuts)
                                       AND s.ca = ANY (p_cas)
                                       AND o.sales_sale_date = s.sale_date
                                       AND s.sale_date BETWEEN p_start_date AND p_end_date),
                   'content', jsonb_agg(
                     jsonb_build_object(
                       'mvtDate', data.updated_at,
                       'reference', data.number_transaction,
                       'quantite', data.quantity_requested,
                       'prixUnitaire', data.regular_unit_price,
                       'montantHt', data.ht_amount,
                       'montantNet', data.sales_amount - data.discount_amount,
                       'montantTtc', data.sales_amount,
                       'montantRemise', data.discount_amount,
                       'montantTva', data.sales_amount - data.ht_amount,
                       'firstName', data.first_name,
                       'lastName', data.last_name
                     )
                              )
                 )
          FROM data);
END;
$$;


create function get_product_movements_by_period(p_produit_id integer, p_magasin_id integer,
                                                p_date_debut date, p_date_fin date) returns jsonb
  language plpgsql
as
$$
DECLARE
  result JSONB;
BEGIN
  WITH mouvements_ordonnes AS (SELECT *,
                                      ROW_NUMBER() OVER (
                                        PARTITION BY transaction_date, produit_id, magasin_id
                                        ORDER BY id
                                        ) AS rn_debut,
                                      ROW_NUMBER() OVER (
                                        PARTITION BY transaction_date, produit_id, magasin_id
                                        ORDER BY id DESC
                                        ) AS rn_fin
                               FROM inventory_transaction
                               WHERE produit_id = p_produit_id
                                 AND magasin_id = p_magasin_id
                                 AND transaction_date BETWEEN p_date_debut AND p_date_fin),
       mouvements_detail AS (SELECT transaction_date,
                                    magasin_id,
                                    produit_id,
                                    mouvement_type,
                                    SUM(quantity)                                       AS quantite,
                                    MIN(CASE WHEN rn_debut = 1 THEN quantity_befor END) AS quantite_debut,
                                    MAX(CASE WHEN rn_fin = 1 THEN quantity_after END)   AS quantite_fin
                             FROM mouvements_ordonnes
                             GROUP BY transaction_date, magasin_id, produit_id, mouvement_type),
       mouvements_agg AS (SELECT transaction_date,
                                 magasin_id,
                                 produit_id,
                                 MAX(quantite_debut)                        AS quantite_debut,
                                 MAX(quantite_fin)                          AS quantite_fin,
                                 jsonb_object_agg(mouvement_type, quantite) AS mouvements
                          FROM mouvements_detail
                          GROUP BY transaction_date, magasin_id, produit_id
                          ORDER BY transaction_date)
  SELECT jsonb_agg(
           jsonb_build_object(
             'mvtDate', transaction_date,
             'initStock', quantite_debut,
             'afterStock', quantite_fin,
             'mouvements', mouvements
           )
             ORDER BY transaction_date
         )
  INTO result
  FROM mouvements_agg;

  RETURN COALESCE(result, '[]'::jsonb);
END;
$$;


create function refresh_all_report_views() returns void
  language plpgsql
as
$$
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
$$;

comment on function refresh_all_report_views() is 'Refreshes all report materialized views - should be scheduled to run periodically';



create function refresh_phase2_report_views() returns void
  language plpgsql
as
$$
BEGIN
  -- Refresh stock valuation view
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_valuation;
  RAISE NOTICE 'Stock valuation view refreshed at %', NOW();


  -- Refresh customer RFM view
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_rfm;
  RAISE NOTICE 'Customer RFM view refreshed at %', NOW();

  RAISE NOTICE 'All Phase 2 report materialized views refreshed successfully at %', NOW();
END;
$$;

comment on function refresh_phase2_report_views() is 'Refreshes all Phase 2 report materialized views';



create function refresh_supplier_performance_view() returns void
  language plpgsql
as
$$
BEGIN
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supplier_performance;
  RAISE NOTICE 'Supplier performance view refreshed at %', NOW();
END;
$$;

comment on function refresh_supplier_performance_view() is 'Refreshes supplier performance materialized view';




create function refresh_dashboard_ca_views() returns void
  language plpgsql
as
$$
BEGIN
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_daily;
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_payment_methods;
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_ca_product_families;
END;
$$;


create function get_product_sales_summary(p_start_date date, p_end_date date, p_statuts text[],
                                          p_cas text[], p_produit_id integer,
                                          p_group_by integer DEFAULT 0) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'groupType', p_group_by,
                     'groupBy', grp.mvt_date,
                     'montantHt', grp.montant_ht,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat,
                     'montantTtc', grp.montant_ttc,
                     'montantRemise', grp.montant_remise
                   )
                     ORDER BY grp.mvt_date
                 )
          FROM (SELECT CASE p_group_by
                         WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                         WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                         WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                         WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                     CASE
                                       WHEN extract(quarter from s.sale_date) <= 2
                                         THEN 'S1'
                                       ELSE 'S2' END
                         WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                         END                                            AS mvt_date,
                       CEIL(SUM((o.quantity_requested * o.regular_unit_price) /
                                NULLIF(1 + (o.tax_value / 100), 0)))    AS montant_ht,
                       SUM(o.quantity_requested)                        AS quantite,
                       SUM(o.quantity_requested * o.cost_amount)        AS montant_achat,
                       SUM(o.quantity_requested * o.regular_unit_price) AS montant_ttc,
                       SUM(o.discount_amount)                           AS montant_remise
                FROM sales_line o
                       JOIN sales s ON o.sales_id = s.id
                WHERE o.sales_sale_date = s.sale_date
                  AND s.sale_date BETWEEN p_start_date AND p_end_date
                  AND o.produit_id = p_produit_id
                  AND s.statut = ANY (p_statuts)
                  AND s.ca = ANY (p_cas)
                GROUP BY CASE p_group_by
                           WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                           WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                           WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                           WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                       CASE
                                         WHEN extract(quarter from s.sale_date) <= 2
                                           THEN 'S1'
                                         ELSE 'S2' END
                           WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                           END) grp);
END;
$$;


create function search_produits_json(qtext text, magasin integer DEFAULT 1,
                                     limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id,
                       p.fournisseur_produit_principal_id AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo                    AS codeeanlabo,
                       p.parent_id                        AS parentid,
                       p.item_qty                         AS itemqty,
                       p.deconditionnable,
                       t.taux                             AS vatrate,
                       p.regular_unit_price               AS regularunitprice,
                       p.cost_amount                      AS costamount,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                  AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN q.query ~ '^[0-9]+$' AND
                         (pf.code_cip = q.query OR pf.code_ean = q.query OR
                          p.code_ean_labo = q.query) THEN 1000
                         WHEN q.query ~ '^[0-9]+$' AND (pf.code_cip LIKE q.query || '%' OR
                                                        pf.code_ean LIKE q.query || '%' OR
                                                        p.code_ean_labo LIKE q.query || '%')
                         THEN 500
                         ELSE 0
                         END
                       )                                  AS score,
                       -- rayons
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'code', r.code,
                                   'libelle', r.libelle
                                 )
                                   ORDER BY r.libelle
                               )
                        FROM rayon_produit rp
                               JOIN rayon r ON rp.rayon_id = r.id
                        WHERE rp.produit_id = p.id)       AS rayons,
                       -- stocks
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'quantite', sp.qty_stock,
                                   'qteUg', sp.qty_ug,
                                   'storage', sp.storage_id,
                                   'storageType', s.storage_type,
                                   'stockReassort', sp.stock_reassort,
                                   'seuilMini', sp.seuil_mini
                                 )
                                   ORDER BY sp.id
                               )
                        FROM stock_produit sp
                               join storage s on sp.storage_id = s.id
                        WHERE sp.produit_id = p.id
                          AND s.magasin_id = magasin)     AS stocks
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                   -- recherche par code numérique (exact / préfixe)
                  (q.query ~ '^[0-9]+$' AND
                   (pf.code_cip = q.query OR pf.code_ean = q.query OR p.code_ean_labo = q.query
                     OR pf.code_cip LIKE q.query || '%' OR pf.code_ean LIKE q.query || '%' OR
                    p.code_ean_labo LIKE q.query || '%'))
                   -- recherche préfixe stricte sur le libellé
                   OR (lower(p.libelle) LIKE lower(q.query || '%'))
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,
                         t.taux, p.regular_unit_price, p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;



create function search_produits_by_storage_json(qtext text, p_storage_id integer,
                                                limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id,
                       p.fournisseur_produit_principal_id   AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo                      AS codeeanlabo,
                       p.parent_id                          AS parentid,
                       p.item_qty                           AS itemqty,
                       p.deconditionnable,
                       t.taux                               AS vatrate,
                       p.regular_unit_price                 AS regularunitprice,
                       p.cost_amount                        AS costamount,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                    AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN q.query ~ '^[0-9]+$' AND
                         (pf.code_cip = q.query OR pf.code_ean = q.query OR
                          p.code_ean_labo = q.query) THEN 1000
                         WHEN q.query ~ '^[0-9]+$' AND (pf.code_cip LIKE q.query || '%' OR
                                                        pf.code_ean LIKE q.query || '%' OR
                                                        p.code_ean_labo LIKE q.query || '%')
                         THEN 500
                         ELSE 0
                         END
                       )                                    AS score,
                       -- rayons
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'code', r.code,
                                   'libelle', r.libelle
                                 )
                                   ORDER BY r.libelle
                               )
                        FROM rayon_produit rp
                               JOIN rayon r ON rp.rayon_id = r.id
                        WHERE rp.produit_id = p.id)         AS rayons,
                       -- stocks filtrés par storage_id
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'quantite', sp.qty_stock,
                                   'qteUg', sp.qty_ug,
                                   'storage', sp.storage_id,
                                   'storageType', s.storage_type,
                                   'stockReassort', sp.stock_reassort,
                                   'seuilMini', sp.seuil_mini
                                 )
                                   ORDER BY sp.id
                               )
                        FROM stock_produit sp
                               join storage s on sp.storage_id = s.id
                        WHERE sp.produit_id = p.id
                          AND sp.storage_id = p_storage_id) AS stocks
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                  -- Vérifier que le produit a un stock dans le storage spécifié
                  EXISTS (SELECT 1
                          FROM stock_produit sp
                          WHERE sp.produit_id = p.id
                            AND sp.storage_id = p_storage_id)
                  AND (
                  -- recherche par code numérique (exact / préfixe)
                  (q.query ~ '^[0-9]+$' AND
                   (pf.code_cip = q.query OR pf.code_ean = q.query OR p.code_ean_labo = q.query
                     OR pf.code_cip LIKE q.query || '%' OR pf.code_ean LIKE q.query || '%' OR
                    p.code_ean_labo LIKE q.query || '%'))
                    -- recherche préfixe stricte sur le libellé
                    OR (lower(p.libelle) LIKE lower(q.query || '%'))
                  )
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,
                         t.taux, p.regular_unit_price, p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;



create function init_semois_configurations() returns integer
  language plpgsql
as
$$
DECLARE
  nb_created INTEGER := 0;
BEGIN
  -- Créer config par défaut pour tous les produits actifs sans config
  INSERT INTO semois_configuration (produit_id,
                                    classe_criticite,
                                    coefficient_securite,
                                    nb_mois_historique,
                                    delai_livraison_jours,
                                    facteur_saisonnier_actuel,
                                    limite_peremption,
                                    created_at,
                                    updated_at)
  SELECT p.id,
         'B'::VARCHAR(3), -- Classe par défaut: rotation moyenne
    1.0,             -- Coefficient par défaut
         6,               -- 6 mois d'historique
         7,               -- 7 jours de délai livraison
         1.0,             -- Pas d'ajustement saisonnier
         FALSE,
         NOW(),
         NOW()
  FROM produit p
  WHERE p.status = 'ENABLE'
    AND p.type_produit != 'DETAIL'
    AND NOT EXISTS (SELECT 1 FROM semois_configuration sc WHERE sc.produit_id = p.id);

  GET DIAGNOSTICS nb_created = ROW_COUNT;

  RETURN nb_created;
END;
$$;

comment on function init_semois_configurations() is 'Initialise config SEMOIS pour tous produits actifs (classe B par défaut)';



create function update_semois_updated_at() returns trigger
  language plpgsql
as
$$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

create function fetch_product_quantity_sold_json(p_start_date date, p_end_date date) returns jsonb
  language plpgsql
as
$$
DECLARE
result JSONB := '[]'::jsonb;
BEGIN
  WITH sales_base AS (
    -- Total qty sold pour chaque produit "PACKAGE"
    SELECT sl.produit_id, SUM(sl.quantity_requested) AS qty_sold
    FROM sales_line sl
           JOIN sales s ON s.id = sl.sales_id
    WHERE s.statut = 'CLOSED'
      AND s.canceled = FALSE
      AND s.sale_date BETWEEN p_start_date AND p_end_date
    GROUP BY sl.produit_id),
       sales_detail AS (
         -- Total qty sold des produits "DETAIL" enfants pour chaque parent
         SELECT pd.parent_id, SUM(sl.quantity_requested) AS qty_sold
         FROM sales_line sl
                JOIN sales s ON s.id = sl.sales_id
                JOIN produit pd ON pd.id = sl.produit_id
         WHERE s.statut = 'CLOSED'
           AND s.canceled = FALSE
           AND pd.type_produit = 'DETAIL'
           AND s.sale_date BETWEEN p_start_date AND p_end_date
         GROUP BY pd.parent_id),
       computed AS (SELECT p.id,
                           p.item_qty,
                           COALESCE(sb.qty_sold, 0) AS qtySold,
                           COALESCE(sd.qty_sold, 0) AS itemQtySold
                    FROM produit p
                           LEFT JOIN sales_base sb ON sb.produit_id = p.id
                           LEFT JOIN sales_detail sd ON sd.parent_id = p.id
                    WHERE p.status = 'ENABLE'
                      AND p.type_produit = 'PACKAGE')
  SELECT jsonb_agg(
           jsonb_build_object(
             'id', c.id,
             'itemQty', c.item_qty,
             'qtySold', c.qtySold,
             'itemQtySold', c.itemQtySold
           )
         )
  INTO result
  FROM computed c;

  RETURN result;
END;
$$;


create function update_classification_config_updated_at() returns trigger
  language plpgsql
as
$$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;



create trigger trigger_classification_config_updated_at
  before update
  on classification_config
  for each row
  execute procedure update_classification_config_updated_at();

