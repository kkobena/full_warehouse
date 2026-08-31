

drop materialized view if exists mv_supplier_performance cascade;
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
                         WHERE c.order_status::text IN ('RECEIVED'::text, 'CLOSED'::text)
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

comment on materialized view mv_supplier_performance is 'Supplier performance analysis with delivery metrics, purchase volumes, and conformity rates (réceptions RECEIVED et CLOSED)';

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

refresh materialized view mv_supplier_performance;
