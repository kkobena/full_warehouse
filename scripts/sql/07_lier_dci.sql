\encoding UTF8
-- ============================================================================
-- 07_lier_dci.sql — Relie ref_dci aux DCI du catalogue (dci)
--
-- Rapprochement par libellé normalisé (ref_normaliser : sans accent, majuscules,
-- ponctuation réduite). Rejouable : ne touche que les ref_dci non encore liées.
--
-- Un libellé du catalogue porté par plusieurs DCI après normalisation (« ACIDE
-- FOLIQUE » / « Acide-folique ») est AMBIGU : on ne lie pas, la liste est
-- affichée en fin de script pour arbitrage manuel.
-- ============================================================================
\if :{?schema}
\else
  \set schema pharma_smart
\endif
SET search_path TO :"schema";

-- La logique vit dans la fonction ref_lier_dci (migration V2.1.20), aussi appelée à chaque ajout de DCI.
SELECT ref_lier_dci() AS dci_reliees;

\echo 'DCI du référentiel reliées au catalogue :'
SELECT count(*) FILTER (WHERE dci_id IS NOT NULL) AS reliees,
       count(*) FILTER (WHERE dci_id IS NULL)     AS sans_equivalent_catalogue
FROM ref_dci;

\echo 'Libellés ambigus (plusieurs DCI du catalogue pour une DCI du référentiel) :'
SELECT rd.libelle AS ref_dci, string_agg(d.libelle, ' | ' ORDER BY d.id) AS dci_catalogue
FROM ref_dci rd
         JOIN dci d ON ref_normaliser(d.libelle) = rd.libelle_normalise
WHERE rd.dci_id IS NULL
GROUP BY rd.id, rd.libelle
HAVING count(*) > 1;
