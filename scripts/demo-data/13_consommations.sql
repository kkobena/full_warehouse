-- ============================================================================
-- 13_consommations.sql — Consommations mensuelles des tiers-payants
--
-- consommation_json sérialise des Consommation(id, month, year, consommation) :
--     month        short
--     consommation long
--
-- ATTENTION AUX TYPES DE COLONNE, vérifiés sur la base :
--     client_tiers_payant.consommation_json  est de type  json
--     tiers_payant.consommation_json         est de type  jsonb
-- Sans conséquence à l'insertion, mais les casts explicites doivent suivre.
--
-- Ces cumuls DOIVENT concorder avec les ventes réellement générées, sinon
-- l'écran de suivi de consommation contredit le journal des ventes — et c'est
-- le premier recoupement que fait un gestionnaire quand un plafond est atteint.
--
-- Consommation.equals ne compare que « id » : il doit donc être unique dans
-- l'ensemble. On utilise année × 100 + mois, qui l'est par construction.
-- ============================================================================

\i _header.sql

\echo '>> 13_consommations : cumuls mensuels tiers-payants'

-- ---------------------------------------------------------------------------
-- 1. Consommation par contrat et par mois
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_conso_ctp AS
SELECT
    t.client_tiers_payant_id,
    extract(year  FROM t.sale_date)::int   AS annee,
    extract(month FROM t.sale_date)::int   AS mois,
    sum(t.montant)::bigint                 AS montant
FROM third_party_sale_line t
GROUP BY t.client_tiers_payant_id,
         extract(year FROM t.sale_date),
         extract(month FROM t.sale_date);

UPDATE client_tiers_payant ctp
   SET consommation_json = c.conso,
       -- conso_mensuelle : le cumul du mois courant, celui que compare
       -- applyMonthlyCeiling au plafond de l'organisme.
       conso_mensuelle = COALESCE(c.mois_courant, 0),
       updated = NOW()
  FROM (
      SELECT
          client_tiers_payant_id,
          json_agg(json_build_object(
              'id',           annee * 100 + mois,
              'month',        mois,
              'year',         annee,
              'consommation', montant
          ) ORDER BY annee, mois) AS conso,
          sum(montant) FILTER (
              WHERE annee = extract(year FROM CURRENT_DATE)::int
                AND mois  = extract(month FROM CURRENT_DATE)::int
          ) AS mois_courant
        FROM tmp_conso_ctp
       GROUP BY client_tiers_payant_id
  ) c
 WHERE ctp.id = c.client_tiers_payant_id;

-- Les contrats sans aucune vente gardent un tableau vide plutôt que NULL :
-- l'application itère dessus sans garde.
UPDATE client_tiers_payant
   SET consommation_json = '[]'::json, conso_mensuelle = 0
 WHERE consommation_json IS NULL;

-- ---------------------------------------------------------------------------
-- 2. Consommation par organisme
--
-- Agrégation des contrats qui lui sont rattachés. Noter le type jsonb ici,
-- alors que la table précédente est en json.
-- ---------------------------------------------------------------------------
UPDATE tiers_payant tp
   SET consommation_json = c.conso,
       conso_mensuelle = COALESCE(c.mois_courant, 0),
       updated = NOW()
  FROM (
      SELECT
          ctp.tierspayant_id,
          jsonb_agg(jsonb_build_object(
              'id',           x.annee * 100 + x.mois,
              'month',        x.mois,
              'year',         x.annee,
              'consommation', x.montant
          ) ORDER BY x.annee, x.mois) AS conso,
          sum(x.montant) FILTER (
              WHERE x.annee = extract(year FROM CURRENT_DATE)::int
                AND x.mois  = extract(month FROM CURRENT_DATE)::int
          ) AS mois_courant
        FROM (
            SELECT ctp2.tierspayant_id, cc.annee, cc.mois, sum(cc.montant)::bigint AS montant
              FROM tmp_conso_ctp cc
              JOIN client_tiers_payant ctp2 ON ctp2.id = cc.client_tiers_payant_id
             GROUP BY ctp2.tierspayant_id, cc.annee, cc.mois
        ) x
        JOIN client_tiers_payant ctp ON ctp.tierspayant_id = x.tierspayant_id
       GROUP BY ctp.tierspayant_id
  ) c
 WHERE tp.id = c.tierspayant_id;

UPDATE tiers_payant
   SET consommation_json = '[]'::jsonb, conso_mensuelle = 0
 WHERE consommation_json IS NULL;

DROP TABLE tmp_conso_ctp;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_vides int;
    v_ecart int;
    v_plaf  int;
BEGIN
    SELECT count(*) INTO v_vides FROM client_tiers_payant
     WHERE consommation_json IS NULL;

    -- Le cumul déclaré doit correspondre aux ventilations du mois.
    SELECT count(*) INTO v_ecart FROM client_tiers_payant ctp
     CROSS JOIN LATERAL json_array_elements(ctp.consommation_json) e
     WHERE (e->>'consommation')::bigint <> COALESCE((
         SELECT sum(t.montant) FROM third_party_sale_line t
          WHERE t.client_tiers_payant_id = ctp.id
            AND extract(year  FROM t.sale_date)::int = (e->>'year')::int
            AND extract(month FROM t.sale_date)::int = (e->>'month')::int), 0);

    -- Au moins un organisme plafonné doit avoir de la consommation, sinon le
    -- suivi de plafond n'a rien à afficher.
    SELECT count(*) INTO v_plaf FROM tiers_payant
     WHERE plafond_conso IS NOT NULL AND COALESCE(conso_mensuelle, 0) > 0;

    IF v_vides > 0 THEN RAISE EXCEPTION '% contrat(s) sans consommation initialisée', v_vides; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% cumul(s) contredisant les ventilations', v_ecart; END IF;
    IF v_plaf = 0 THEN RAISE WARNING 'Aucun organisme plafonné n''a de consommation ce mois-ci'; END IF;

    RAISE NOTICE 'Consommations mensuelles calculées.';
END $$;

\echo '<< 13_consommations : terminé'
