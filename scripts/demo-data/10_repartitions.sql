-- ============================================================================
-- 10_repartitions.sql — Ventilation TVA de la part de chaque payeur
--
-- third_party_sale_line.repartitions est un jsonb de
-- RepartitionTiersPayantParTva(montantTtc, montantTva, montantNet, montantHt, tva).
--
-- ATTENTION : ces champs sont des DOUBLE, alors que tous les autres montants du
-- modèle sont des entiers. C'est le seul endroit du jeu de données où l'on
-- manipule des décimales.
--
-- La part d'un payeur se répartit sur les taux de TVA présents dans la vente,
-- au prorata du poids de chaque taux. Le reliquat d'arrondi est absorbé par le
-- taux le plus lourd, pour que la somme retombe EXACTEMENT sur le montant du
-- payeur — sans quoi l'écran de facturation afficherait un total qui ne
-- correspond pas à la ligne dont il est issu.
-- ============================================================================

\i _header.sql

\echo '>> 10_repartitions : ventilation TVA par payeur'

-- ---------------------------------------------------------------------------
-- 1. Poids de chaque taux de TVA dans chaque vente
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_tva_vente AS
SELECT
    sl.sales_id,
    sl.sales_sale_date AS sale_date,
    sl.tax_value,
    sum(sl.sales_amount)::int AS montant_taux,
    row_number() OVER (PARTITION BY sl.sales_id, sl.sales_sale_date
                       ORDER BY sum(sl.sales_amount) DESC, sl.tax_value) AS rang_poids
FROM sales_line sl
JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
WHERE s.dtype = 'ThirdPartySales'
GROUP BY sl.sales_id, sl.sales_sale_date, sl.tax_value;

CREATE INDEX ON tmp_tva_vente (sales_id, sale_date);

-- ---------------------------------------------------------------------------
-- 2. Répartition de la part du payeur, au prorata
--
-- Le reliquat va au taux de rang 1, c'est-à-dire au plus gros montant : c'est
-- là qu'un franc d'écart est le moins visible, et le calcul reste exact.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_repart AS
WITH brut AS (
    SELECT
        t.id AS tpsl_id,
        tv.tax_value,
        tv.rang_poids,
        t.montant,
        s.sales_amount,
        -- Part proportionnelle, tronquée : le reliquat est réattribué ensuite.
        floor(t.montant::numeric * tv.montant_taux / s.sales_amount)::int AS ttc_brut
    FROM third_party_sale_line t
    JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
    JOIN tmp_tva_vente tv ON tv.sales_id = t.sale_id AND tv.sale_date = t.sale_sale_date
    WHERE s.sales_amount > 0
),
avec_reliquat AS (
    SELECT b.*,
           b.montant - sum(b.ttc_brut) OVER (PARTITION BY b.tpsl_id) AS reliquat
    FROM brut b
)
SELECT
    tpsl_id,
    tax_value,
    -- Le taux dominant absorbe le reliquat d'arrondi.
    (ttc_brut + CASE WHEN rang_poids = 1 THEN reliquat ELSE 0 END)::int AS ttc
FROM avec_reliquat;

-- ---------------------------------------------------------------------------
-- 3. Écriture du jsonb
--
-- montantHt = TTC / (1 + taux/100), arrondi au centime ; montantTva est le
-- complément, jamais recalculé indépendamment — c'est ce qui garantit que
-- HT + TVA retombe sur le TTC.
-- ---------------------------------------------------------------------------
UPDATE third_party_sale_line t
   SET repartitions = r.rep
  FROM (
      SELECT
          tpsl_id,
          jsonb_agg(jsonb_build_object(
              'montantTtc', ttc::float8,
              'montantHt',  ht::float8,
              'montantTva', (ttc - ht)::float8,
              'montantNet', ttc::float8,
              'tva',        tax_value
          ) ORDER BY tax_value) AS rep
      FROM (
          SELECT tpsl_id, tax_value, ttc,
                 CASE WHEN tax_value = 0 THEN ttc::numeric
                      ELSE round(ttc::numeric / (1 + tax_value / 100.0), 2) END AS ht
            FROM tmp_repart
           WHERE ttc > 0
      ) x
      GROUP BY tpsl_id
  ) r
 WHERE t.id = r.tpsl_id;

DROP TABLE tmp_repart;
DROP TABLE tmp_tva_vente;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_lignes int;
    v_vides  int;
    v_ecart  int;
BEGIN
    SELECT count(*) INTO v_lignes FROM third_party_sale_line;
    SELECT count(*) INTO v_vides  FROM third_party_sale_line
     WHERE montant > 0 AND jsonb_array_length(repartitions) = 0;

    -- La somme des ventilations doit retomber exactement sur le montant.
    SELECT count(*) INTO v_ecart FROM third_party_sale_line t
     WHERE jsonb_array_length(t.repartitions) > 0
       AND t.montant <> (SELECT sum((e->>'montantTtc')::numeric)::int
                           FROM jsonb_array_elements(t.repartitions) e);

    IF v_lignes = 0 THEN RAISE EXCEPTION 'Aucune ventilation tiers-payant à répartir'; END IF;
    IF v_vides > 0 THEN RAISE EXCEPTION '% ventilation(s) sans répartition TVA', v_vides; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% répartition(s) dont la somme contredit le montant', v_ecart; END IF;

    RAISE NOTICE '% ventilations réparties par taux de TVA.', v_lignes;
END $$;

\echo '<< 10_repartitions : terminé'
