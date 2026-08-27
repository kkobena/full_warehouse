-- ============================================================================
-- run_all.sql — Enchaînement des scripts de données de démonstration
--
--     psql -U pharma_smart -d pharma_smart_demo \
--          -v ON_ERROR_STOP=1 -v confirm_reset=1 -f run_all.sql
--
-- ON_ERROR_STOP=1 est indispensable : sans lui, psql poursuit après une erreur
-- et laisse une base à moitié chargée, que la vérification finale signalerait
-- trop tard.
--
-- La création de la base elle-même n'est PAS ici : voir create_database.sql,
-- qui se connecte à une autre base.
--
-- Se lance depuis ce répertoire (les \i sont relatifs) :
--     cd scripts/demo-data
-- ============================================================================

\timing on

\echo ''
\echo '=========================================='
\echo ' Données de démonstration — Pharma-Smart'
\echo '=========================================='

\i 00_reset.sql
\i 01_config.sql
\i 02_fournisseurs.sql
\i 02b_dci.sql
\i 03_produits.sql
\i 03b_substituts.sql
\i 04_clients.sql
\i 05_commandes.sql
\i 06_lots.sql
\i 07_stock.sql
\i 08_caisses.sql
\i 09_ventes.sql
\i 10_repartitions.sql
\i 10b_ventes_depot.sql
\i 12_destruction.sql
\i 13_consommations.sql
\i 14_facturation.sql
\i 15_reference.sql
\i 16_mouvements.sql

\i 99_verification.sql

\echo ''
\echo '=========================================='
\echo ' Chargement terminé.'
\echo '=========================================='
\echo ''
