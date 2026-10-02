\encoding UTF8
-- ============================================================================
-- run_all.sql — Charge le référentiel médicament (tables ref_*, migration V2.1.19)
--
--     cd scripts/sql
--     psql -U pharma_smart -d <base> -v ON_ERROR_STOP=1 -f run_all.sql
--
-- Prérequis : l'application a démarré une fois (Flyway a créé les tables ref_*).
-- Schéma : pharma_smart par défaut ; autre schéma : -v schema=<schema>.
-- Rejouable : upsert pour les référentiels, remplacement pour la composition.
-- Les fichiers 01 à 06 sont générés par ../generer_sql.py à partir de
-- ../sortie-gpc/referentiel_bdpm.csv ; 07 et 99 sont écrits à la main.
-- ============================================================================
\timing on

\i 01_ref_groupe_generique.sql
\i 02_ref_substance.sql
\i 03_ref_dci.sql
\i 04_ref_specialite.sql
\i 05_ref_specialite_composition.sql
\i 06_ref_specialite_rcp.sql
\i 07_lier_dci.sql
\i 08_rapprocher_produits.sql
\i 99_verification.sql
