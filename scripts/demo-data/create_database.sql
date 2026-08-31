-- Avant tout caractère non ASCII — ce script n'inclut pas _header.sql, qui vise
-- déjà la base de démo. Voir _header.sql pour le détail : psql déduit son
-- encodage client de la page de code de la console (WIN1252 sous Windows) alors
-- que les scripts sont en UTF-8.
\encoding UTF8

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
-- PÉRIMÈTRE : ce script s'arrête au rôle, à la base, au schéma et aux droits.
-- La création des tables par Flyway et le chargement des données de démonstration
-- font chacun l'objet d'une exécution distincte, décrite dans README.md — les
-- enchaîner ici mêlerait trois opérations aux prérequis et aux droits différents.
--
-- Utilisation (avec un compte superutilisateur) :
--     psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f create_database.sql
--
-- Surcharges possibles :
--     -v db=<autre_base>  -v owner=<autre_role>  -v pwd=<mot_de_passe>
--
-- Le script est IDEMPOTENT : il peut être relancé sur une base déjà créée.
-- Rien n'est détruit ; les droits sont simplement réappliqués. C'est ce qui
-- permet de l'utiliser aussi pour réparer une base dont le propriétaire ou
-- les privilèges auraient dérivé.
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

-- Aligné sur la configuration du projet (application-dev.yml). Le mot de passe
-- n'est posé qu'à la création : un rôle existant n'est jamais réinitialisé.
\if :{?pwd}
\else
  \set pwd 2802_pharma_smart
\endif

\echo '>> création du rôle et de la base de démonstration'

-- ---------------------------------------------------------------------------
-- 1. Rôle applicatif
-- ---------------------------------------------------------------------------
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'owner', :'pwd')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'owner')
\gexec

-- CREATEDB : le rôle doit pouvoir recréer lui-même sa base de démonstration.
-- Sans cet attribut, toute remise à zéro repasse par un superutilisateur —
-- c'est précisément ce qui a bloqué le premier chargement des données de démo.
--
-- Conditionnel, et pas seulement par élégance : ALTER ROLE exige CREATEROLE ou
-- un superutilisateur. Inconditionnel, il ferait échouer toute réexécution du
-- script par le rôle applicatif lui-même — alors qu'il détient déjà l'attribut
-- et que le script se veut rejouable.
SELECT format('ALTER ROLE %I CREATEDB', :'owner')
 WHERE NOT EXISTS (
   SELECT 1 FROM pg_roles WHERE rolname = :'owner' AND rolcreatedb
 )
\gexec

-- ---------------------------------------------------------------------------
-- 2. Base
--
-- TEMPLATE template0 : évite l'échec « new encoding is incompatible » quand la
-- base template1 de l'instance a été créée avec un autre encodage.
-- ---------------------------------------------------------------------------
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0',
              :'db', :'owner')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db')
\gexec

-- Base préexistante appartenant à quelqu'un d'autre : on la réaligne. Être
-- propriétaire vaut mieux que cumuler des GRANT — le propriétaire peut
-- modifier et supprimer ce que les seuls privilèges ne permettent pas.
SELECT format('ALTER DATABASE %I OWNER TO %I', :'db', :'owner')
 WHERE EXISTS (
   SELECT 1 FROM pg_database d
     JOIN pg_roles r ON r.oid = d.datdba
    WHERE d.datname = :'db' AND r.rolname <> :'owner'
 )
\gexec

SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', :'db', :'owner')
\gexec

-- ---------------------------------------------------------------------------
-- 3. Schémas, dans la base fraîchement créée
-- ---------------------------------------------------------------------------
\connect :"db"

CREATE SCHEMA IF NOT EXISTS pharma_smart AUTHORIZATION :"owner";

SELECT format('ALTER SCHEMA pharma_smart OWNER TO %I', :'owner')
\gexec

GRANT ALL ON SCHEMA pharma_smart TO :"owner";

-- Depuis PostgreSQL 15, le schéma « public » n'est plus ouvert en écriture au
-- rôle PUBLIC. Certaines extensions et fonctions utilitaires s'y installent
-- encore : on y rend explicitement la main au rôle applicatif.
GRANT ALL ON SCHEMA public TO :"owner";

-- ---------------------------------------------------------------------------
-- 4. Droits sur les objets
--
-- Deux volets nécessaires, et souvent confondus :
--   * les GRANT ci-dessous ne concernent que les objets EXISTANTS — utiles au
--     rejeu, quand Flyway a déjà créé les tables ;
--   * ALTER DEFAULT PRIVILEGES concerne les objets À VENIR. Sans lui, chaque
--     nouvelle migration Flyway recréerait le problème.
--
-- Le rôle étant propriétaire, il détient déjà tout : ces instructions couvrent
-- le cas d'une base reprise, où des objets appartiennent à un autre rôle.
-- ---------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA pharma_smart TO :"owner";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA pharma_smart TO :"owner";
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA pharma_smart TO :"owner";
GRANT ALL PRIVILEGES ON ALL ROUTINES  IN SCHEMA pharma_smart TO :"owner";

ALTER DEFAULT PRIVILEGES IN SCHEMA pharma_smart
  GRANT ALL PRIVILEGES ON TABLES    TO :"owner";
ALTER DEFAULT PRIVILEGES IN SCHEMA pharma_smart
  GRANT ALL PRIVILEGES ON SEQUENCES TO :"owner";
ALTER DEFAULT PRIVILEGES IN SCHEMA pharma_smart
  GRANT ALL PRIVILEGES ON FUNCTIONS TO :"owner";

-- ---------------------------------------------------------------------------
-- 5. search_path du rôle
--
-- L'application le pose via sa configuration ; on le fixe aussi comme défaut du
-- rôle pour que les connexions psql manuelles tombent au bon endroit.
-- ---------------------------------------------------------------------------
SELECT format('ALTER ROLE %I IN DATABASE %I SET search_path TO pharma_smart, public',
              :'owner', :'db')
\gexec

-- ---------------------------------------------------------------------------
-- 6. Contrôle
--
-- Rien n'est plus pénible qu'un script de droits qui se termine sans erreur
-- alors qu'il n'a rien accordé. On vérifie donc le résultat effectif.
-- ---------------------------------------------------------------------------
-- psql ne substitue pas ses variables à l'intérieur d'une chaîne $$…$$ : le nom
-- du rôle transite donc par un paramètre de session, relu par current_setting.
SELECT set_config('demo.owner', :'owner', false);

DO $$
DECLARE
    v_owner   text := current_setting('demo.owner');
    v_proprio text;
BEGIN
    SELECT r.rolname INTO v_proprio
      FROM pg_namespace n JOIN pg_roles r ON r.oid = n.nspowner
     WHERE n.nspname = 'pharma_smart';

    IF v_proprio IS DISTINCT FROM v_owner THEN
        RAISE EXCEPTION 'Le schéma pharma_smart appartient à %, attendu %',
                        v_proprio, v_owner;
    END IF;

    IF NOT has_schema_privilege(v_owner, 'pharma_smart', 'CREATE, USAGE') THEN
        RAISE EXCEPTION '% n''a pas CREATE/USAGE sur le schéma pharma_smart', v_owner;
    END IF;

    IF NOT has_database_privilege(v_owner, current_database(), 'CREATE, CONNECT') THEN
        RAISE EXCEPTION '% n''a pas CREATE/CONNECT sur la base %',
                        v_owner, current_database();
    END IF;

    RAISE NOTICE 'Droits vérifiés : % est propriétaire du schéma et de la base %.',
                 v_owner, current_database();
END $$;

\echo ''
\echo 'Base de démonstration prête : rôle, base, schéma et droits.'
\echo ''
