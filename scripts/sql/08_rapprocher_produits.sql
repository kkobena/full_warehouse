\encoding UTF8
-- ============================================================================
-- 08_rapprocher_produits.sql — Rapproche le catalogue du référentiel (migration V2.1.20)
--
-- Recalcule les propositions EN_ATTENTE de tous les produits actifs : à jouer après chaque
-- rechargement du référentiel. Les décisions VALIDE et REJETE ne sont jamais touchées.
-- Sans effet tant que ref_specialite est vide.
-- ============================================================================
\if :{?schema}
\else
  \set schema pharma_smart
\endif
SET search_path TO :"schema";

SELECT ref_rapprocher_produits(NULL, TRUE) AS propositions_ecrites;

SELECT statut, count(*) AS produits, round(avg(score)) AS score_moyen
FROM produit_ref_specialite
GROUP BY statut
ORDER BY statut;
