-- Avant tout caractère non ASCII : les scripts sont en UTF-8, alors que psql
-- déduit son encodage client de la page de code de la console (WIN1252 sous
-- Windows). Chaque script inclus repose son propre \encoding via _header.sql ;
-- celui-ci couvre le présent fichier. Voir _header.sql pour le détail.
\encoding UTF8

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
-- Avant tout chargement : le jeu couvre plus que l'année courante, alors que
-- l'application ne crée les partitions que pour l'année en cours et la suivante.
\i 00b_partitions.sql
\i 01_config.sql
\i 02_fournisseurs.sql
\i 02b_dci.sql
\i 03_produits.sql
\i 03b_substituts.sql
\i 04_clients.sql
\i 04b_remises_plafonds_tarifs.sql
\i 05_commandes.sql
\i 05b_historique_prix.sql
\i 06_lots.sql
\i 07_stock.sql
\i 08_caisses.sql
\i 09_ventes.sql
\i 10_repartitions.sql
\i 10b_ventes_depot.sql
\i 11_inventaires.sql
\i 12_destruction.sql
\i 12b_retours_fournisseurs.sql
\i 13_consommations.sql
-- Après 13 : le plafond se cale sur la consommation qui vient d'être calculée.
\i 13b_plafonds.sql
\i 14_facturation.sql
-- Après 14 : les règlements de factures supposent les factures posées.
\i 14b_reglements.sql
\i 14c_avoirs.sql
\i 15_reference.sql
\i 16_mouvements.sql
-- Après 16 : les bons d'ajustement s'écrivent sur un stock stabilisé, dont ils
-- reprennent l'état à l'instant du mouvement.
\i 16b_ajustements.sql
-- Après 16 : les propositions d'achat s'appuient sur le catalogue et les
-- fournisseurs déjà chargés.
\i 17_suggestions.sql
-- Après 17 : la trace des mouvements rayon / réserve, que 07 déplace sans
-- l'historiser.
\i 18_repartitions_stock.sql
\i 19_declaration_ca.sql

\i 99_verification.sql

\echo ''
\echo '=========================================='
\echo ' Chargement terminé.'
\echo '=========================================='
\echo ''
