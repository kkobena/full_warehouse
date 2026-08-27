-- ============================================================================
-- create_database.sql — Création de la base de démonstration
--
-- PRÉREQUIS, exécuté À PART des autres scripts : il se connecte à la base
-- « postgres », alors que tous les autres visent la base de démo elle-même.
-- Il n'est donc PAS appelé par run_all.sql.
--
--   Rôle    : pharma_smart
--   Base    : pharma_smart_demo
--   Schéma  : pharma_smart
--
-- Utilisation (avec un compte superutilisateur) :
--     psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f create_database.sql
--
-- Surcharges possibles :
--     -v db=<autre_base>  -v owner=<autre_role>  -v pwd=<mot_de_passe>
--
-- CREATE DATABASE ne peut s'exécuter ni dans une transaction ni dans un bloc
-- DO : d'où le recours à \gexec, qui exécute le SQL produit par la requête.
-- ============================================================================

\if :{?db}
\else
  \set db pharma_smart_demo
\endif

\if :{?owner}
\else
  \set owner pharma_smart
\endif

-- Aligné sur la configuration du projet (application-dev.yml). Le rôle existant
-- n'est de toute façon pas modifié : il n'est créé que s'il est absent.
\if :{?pwd}
\else
  \set pwd 2802_pharma_smart
\endif

\echo '>> création du rôle et de la base de démonstration'

-- ---------------------------------------------------------------------------
-- Rôle applicatif
-- ---------------------------------------------------------------------------
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'owner', :'pwd')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'owner')
\gexec

-- ---------------------------------------------------------------------------
-- Base
--
-- TEMPLATE template0 : évite l'échec « new encoding is incompatible » quand la
-- base template1 de l'instance a été créée avec un autre encodage.
-- ---------------------------------------------------------------------------
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0',
              :'db', :'owner')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db')
\gexec

-- ---------------------------------------------------------------------------
-- Schéma applicatif, dans la base fraîchement créée
-- ---------------------------------------------------------------------------
\connect :"db"

CREATE SCHEMA IF NOT EXISTS pharma_smart AUTHORIZATION :"owner";

-- L'application accède au schéma via son search_path ; on le pose aussi comme
-- défaut du rôle pour que les connexions psql manuelles tombent au bon endroit.
SELECT format('ALTER ROLE %I IN DATABASE %I SET search_path TO pharma_smart, public',
              :'owner', :'db')
\gexec

GRANT ALL ON SCHEMA pharma_smart TO :"owner";

\echo ''
\echo 'Base de démonstration prête.'
\echo ''
\echo 'Étape suivante : laisser Flyway créer les tables, en pointant'
\echo 'l''application sur cette base, par exemple :'
\echo ''
\echo '    mvnw.cmd flyway:migrate \'
\echo '      -Dflyway.url=jdbc:postgresql://localhost:5432/pharma_smart_demo \'
\echo '      -Dflyway.user=pharma_smart -Dflyway.password=pharma_smart'
\echo ''
\echo 'Puis charger les données de démonstration :'
\echo ''
\echo '    psql -U pharma_smart -d pharma_smart_demo -v ON_ERROR_STOP=1 \'
\echo '         -v confirm_reset=1 -f run_all.sql'
\echo ''
