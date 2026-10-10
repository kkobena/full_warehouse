-- ============================================================================
-- 25_objectifs_pilotage.sql — Objectifs mensuels du pilotage, années N et N-1
--
-- Sans objectif, l'onglet « Objectifs » s'ouvre sur une grille vide et le tableau de bord n'a ni « Objectif » ni
-- « fin de mois projetée » à montrer. Le jeu pose les quatre indicateurs que la grille propose — chiffre
-- d'affaires TTC, marge brute, taux de marge, taux de remise (un plafond) — pour l'année en cours et la précédente.
--
-- Chaque objectif part du même mois de l'année d'avant, comme le bouton « Proposer » de l'écran :
--   * un montant (CA, marge) : + 5 %, plus une variation propre au mois (de -6 à +6 points) ;
--   * le taux de marge : celui de l'an passé + 0,5 point, à ± 0,6 point près ;
--   * le taux de remise : celui de l'an passé + 10 %, à ± 6 % près — un plafond, tenu tant qu'on reste dessous.
-- La variation donne des mois tenus ET des mois manqués : le suivi a de quoi se lire.
--
-- Les mesures suivent les formules du pilotage (CalculateurIndicateurs) : ventes clôturées, non annulées, comptées
-- dans le chiffre d'affaires ; marge = HT des lignes - coût HT ; taux de marge = marge / HT des lignes ; taux de
-- remise = remises / CA TTC. Un mois sans vente l'année d'avant reste sans objectif, comme dans l'application.
-- ============================================================================

\i _header.sql

\echo '>> 25_objectifs_pilotage : objectifs mensuels N et N-1'

SELECT to_regclass('pilotage_objectif') IS NOT NULL AS table_objectifs \gset
\if :table_objectifs

TRUNCATE TABLE pilotage_objectif;

CREATE TEMP TABLE tmp_obj_mois AS
WITH entetes AS (
    SELECT date_part('year', s.sale_date)::int AS annee, date_part('month', s.sale_date)::int AS mois,
           sum(s.sales_amount)::numeric AS ca_ttc, sum(COALESCE(s.discount_amount, 0))::numeric AS remises
      FROM sales s
     WHERE s.statut = 'CLOSED' AND NOT s.canceled AND s.ca = 'CA'
     GROUP BY 1, 2
),
lignes AS (
    SELECT date_part('year', s.sale_date)::int AS annee, date_part('month', s.sale_date)::int AS mois,
           sum(round(sl.sales_amount * 100.0 / (100 + COALESCE(sl.tax_value, 0))))::numeric AS ca_ht,
           sum(round(COALESCE(sl.cost_amount, 0)::numeric * sl.quantity_requested * 100.0
                     / (100 + COALESCE(sl.tax_value, 0))))::numeric AS cout_ht
      FROM sales s
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
     WHERE s.statut = 'CLOSED' AND NOT s.canceled AND s.ca = 'CA'
     GROUP BY 1, 2
)
SELECT e.annee, e.mois, e.ca_ttc, e.remises, l.ca_ht, l.ca_ht - l.cout_ht AS marge
  FROM entetes e
  JOIN lignes l ON l.annee = e.annee AND l.mois = e.mois
 WHERE e.ca_ttc > 0 AND l.ca_ht > 0;

-- L'objectif de l'année A se fixe à la mi-décembre de A-1, d'après les mois de A-1.
INSERT INTO pilotage_objectif (indicateur, annee, mois, valeur, modifie_par, modifie_le)
SELECT o.indicateur, a.annee, m.mois, o.valeur,
       (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1),
       make_date(a.annee - 1, 12, 15) + TIME '10:00:00'
  FROM (VALUES (date_part('year', CURRENT_DATE)::int - 1), (date_part('year', CURRENT_DATE)::int)) a (annee)
  JOIN tmp_obj_mois m ON m.annee = a.annee - 1
 CROSS JOIN LATERAL (SELECT (((m.mois * 37 + a.annee) % 13) - 6) / 100.0 AS variation) v
 CROSS JOIN LATERAL (VALUES
     ('CA_TTC',      round(m.ca_ttc * (1.05 + v.variation) / 1000) * 1000),
     ('MARGE_BRUTE', round(m.marge * (1.05 + v.variation) / 1000) * 1000),
     ('TAUX_MARGE',  round(m.marge * 100.0 / m.ca_ht + 0.5 + v.variation * 10, 1)),
     ('TAUX_REMISE', round(m.remises * 100.0 / m.ca_ttc * (1.10 + v.variation), 1))
 ) o (indicateur, valeur)
 WHERE o.valeur > 0;

DROP TABLE tmp_obj_mois;

DO $$
DECLARE v_n int; v_annees int; v_indicateurs int;
BEGIN
    SELECT count(*), count(DISTINCT annee), count(DISTINCT indicateur) INTO v_n, v_annees, v_indicateurs FROM pilotage_objectif;
    IF v_annees < 2 OR v_indicateurs < 4 THEN
        RAISE EXCEPTION 'Objectifs : % ligne(s), % année(s), % indicateur(s) (attendu 2 années, 4 indicateurs)', v_n, v_annees, v_indicateurs;
    END IF;
    RAISE NOTICE '% objectif(s) mensuel(s) posé(s).', v_n;
END $$;

\else
\echo '   (table pilotage_objectif absente : migration V2.1.38 non appliquée, étape ignorée)'
\endif

\echo '<< 25_objectifs_pilotage : terminé'
