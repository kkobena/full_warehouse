-- ============================================================================
-- Synthèse des ventes : ventiler TOUS les types de vente
--
-- `mv_daily_sales_summary` groupe par `dtype` — CashSale, ThirdPartySales,
-- VenteDepot — mais ne retenait que les ventes de catégorie `CA`. Les ventes
-- dépôt, qui portent `CA_DEPOT` (elles sont un transfert, hors chiffre
-- d'affaires déclaré), n'y entraient donc jamais : l'écran « Synthèse des
-- ventes » offre pourtant « Vente aux dépôts » dans son filtre, et ce filtre
-- ne rendait rien. Le lecteur en concluait qu'aucune vente dépôt n'existe.
--
-- La vue les inclut désormais, SANS les confondre : la colonne `type_vente`
-- les distingue ligne à ligne, et la colonne `categorie_ca` ajoutée ici permet
-- à un écran de les additionner ou de les écarter en connaissance de cause.
-- Le chiffre d'affaires déclaré reste donc calculable — il suffit de filtrer
-- sur `categorie_ca = 'CA'`, ce que la vue rendait implicite et invisible.
--
-- Les ventes IMPORTÉES (reprises d'un autre système) sont écartées, comme dans
-- les autres synthèses : elles n'ont pas été faites ici.
-- ============================================================================

DROP MATERIALIZED VIEW IF EXISTS mv_daily_sales_summary CASCADE;

CREATE MATERIALIZED VIEW mv_daily_sales_summary AS
SELECT sale_date,
       dtype                               AS type_vente,
       ca::text                            AS categorie_ca,
       count(*)                            AS nb_ventes,
       sum(sales_amount)                   AS ca_total,
       sum(sales_amount - discount_amount) AS ca_net,
       avg(sales_amount)                   AS panier_moyen,
       sum(discount_amount)                AS total_remises,
       now()                               AS last_updated
FROM sales s
WHERE statut::text = 'CLOSED'::text
  AND canceled = false
  AND imported = false
  AND ca::text IN ('CA', 'CA_DEPOT')
GROUP BY sale_date, dtype, ca;

CREATE UNIQUE INDEX idx_mv_daily_sales_unique
  ON mv_daily_sales_summary (sale_date, type_vente, categorie_ca);

CREATE INDEX idx_mv_daily_sales_date
  ON mv_daily_sales_summary (sale_date);

REFRESH MATERIALIZED VIEW mv_daily_sales_summary;
