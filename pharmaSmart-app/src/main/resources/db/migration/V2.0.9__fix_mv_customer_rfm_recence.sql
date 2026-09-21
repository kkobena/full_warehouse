-- ─────────────────────────────────────────────────────────────
-- Correctif : mv_customer_rfm ne mesurait pas la récence
--
-- La colonne days_since_last_purchase était calculée ainsi :
--
--     EXTRACT(day FROM age(CURRENT_DATE, max(s.sale_date)))
--
-- `age()` ne rend pas un nombre de jours mais un intervalle décomposé en
-- années, mois et jours — « 6 mons 18 days » pour une absence de deux cents
-- jours. `EXTRACT(day FROM …)` n'en prélève que la composante jours : 18. La
-- valeur ne dépassait donc jamais 30, quelle que soit la durée réelle.
--
-- Conséquence : recency_score valait 5 pour presque tout le monde, et les
-- classes qui dépendent d'une récence dégradée — AT_RISK, NEED_ATTENTION,
-- INACTIVE — étaient pratiquement inatteignables. Un client parti depuis sept
-- mois s'affichait « ACTIVE ». La segmentation, dont l'objet même est de
-- repérer les clients qui s'éloignent, ne repérait personne : elle rangeait
-- toute la clientèle de l'année écoulée parmi les actifs, les fidèles, les gros
-- paniers et les champions.
--
-- La soustraction de deux dates rend directement un entier en jours, ce que
-- ClientRetentionReportService fait déjà correctement de son côté. On aligne
-- donc la vue sur cette forme. Le reste de la définition — filtres, seuils des
-- trois notes, classification — est inchangé.
--
-- Le type de la colonne passe de numeric à integer : les services la lisent via
-- Number.intValue(), la lecture Java n'est pas affectée.
-- ─────────────────────────────────────────────────────────────

DROP MATERIALIZED VIEW IF EXISTS mv_customer_rfm CASCADE;

CREATE MATERIALIZED VIEW mv_customer_rfm AS
WITH customer_metrics AS (SELECT c.id                                                   AS customer_id,
                                 (c.first_name::text || ' '::text) || c.last_name::text AS customer_name,
                                 c.phone,
                                 max(s.sale_date)                                       AS last_purchase_date,
                                 (CURRENT_DATE - max(s.sale_date))                      AS days_since_last_purchase,
                                 count(DISTINCT s.id)                                   AS nb_purchases_last_year,
                                 sum(s.sales_amount)                                    AS total_spent_last_year,
                                 avg(s.sales_amount)                                    AS avg_basket_value
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
                             WHEN customer_metrics.days_since_last_purchase <= 30 THEN 5
                             WHEN customer_metrics.days_since_last_purchase <= 60 THEN 4
                             WHEN customer_metrics.days_since_last_purchase <= 90 THEN 3
                             WHEN customer_metrics.days_since_last_purchase <= 180 THEN 2
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

COMMENT ON MATERIALIZED VIEW mv_customer_rfm IS
  'Customer RFM segmentation with filters: statut=CLOSED, canceled=false, ca=CA. '
  'days_since_last_purchase est une soustraction de dates (entier de jours) : '
  'EXTRACT(day FROM age(…)) n''en rendait que la composante jours et plafonnait la '
  'récence à 30 (corrigé en V2.0.9).';

CREATE UNIQUE INDEX idx_mv_customer_rfm_unique
  ON mv_customer_rfm (customer_id);

CREATE INDEX idx_mv_customer_rfm_classification
  ON mv_customer_rfm (customer_classification);

CREATE INDEX idx_mv_customer_rfm_segment
  ON mv_customer_rfm (rfm_segment DESC);

CREATE INDEX idx_mv_customer_rfm_recency
  ON mv_customer_rfm (recency_score DESC);

CREATE INDEX idx_mv_customer_rfm_frequency
  ON mv_customer_rfm (frequency_score DESC);

CREATE INDEX idx_mv_customer_rfm_monetary
  ON mv_customer_rfm (monetary_score DESC);

REFRESH MATERIALIZED VIEW mv_customer_rfm;
