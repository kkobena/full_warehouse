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
\i 03_parapharmacie.sql
-- Les médicaments (vrais noms, CIP et EAN13 de la BDPM) avant la substitution, qui s'appuie sur leurs groupes génériques.
\i 03c_produits_bdpm.sql
\i 03b_substituts.sql
\i 04_clients.sql
\i 04b_remises_plafonds_tarifs.sql
\i 05_commandes.sql
\i 05b_historique_prix.sql
\i 06_lots.sql
\i 07_stock.sql
\i 08_caisses.sql
\i 09_ventes.sql
-- Après 09 : les achats de l'historique se déduisent des ventes qui viennent d'être posées.
\i 09b_histo_achats.sql
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
-- Après 14b : le fond de tiroir se recale sur les lignes de ticket Z, règlements compris.
\i 14d_mouvements_caisse.sql
\i 15_reference.sql
-- Les bons d'ajustement décrivent leur mouvement ; le journal (24) leur donne leur stock avant / après.
\i 16b_ajustements.sql
-- Les propositions d'achat s'appuient sur le catalogue et les
-- fournisseurs déjà chargés.
\i 17_suggestions.sql
-- Après 16b : les favoris du comptoir se choisissent dans les ventes et dans le stock stabilisé (la rupture affichée doit le rester).
\i 17b_favoris.sql
-- Après 17 : la trace des mouvements rayon / réserve, que 07 déplace sans
-- l'historiser.
\i 18_repartitions_stock.sql
-- Après 18 : les favoris reçoivent leur stock de réserve, et les suggestions de réassort s'appuient sur les deux stocks.
\i 17c_reassort.sql
\i 19_declaration_ca.sql
\i 20_referentiel_bdpm.sql
-- Après 19 : agrégats, ruptures et classes s'appuient sur les ventes et les achats définitifs.
\i 21_referentiels_produit.sql
-- Après 21 : le suivi du client s'appuie sur des ventes, des différés et des caisses définitifs.
\i 22_clients_suivi.sql
-- Après 22 : le billetage reprend le fond de tiroir définitif de chaque caisse.
\i 23_exploitation.sql
-- En dernier : le journal des mouvements dérive chaque mouvement de sa pièce (réceptions, ventes, retours,
-- ajustements, répartitions, déconditionnements, inventaires), qui doivent donc toutes exister.
\i 24_mouvements.sql
-- Après 24 : les objectifs se fixent sur les ventes définitives.
\i 25_objectifs_pilotage.sql

\i 99_verification.sql

\echo ''
\echo '=========================================='
\echo ' Chargement terminé.'
\echo '=========================================='
\echo ''
