-- Première instruction du fichier, avant tout caractère non ASCII : psql déduit
-- son encodage client de la page de code de la console (WIN1252 sous Windows),
-- et la conversion vers UTF8 échoue ou dégrade les accents. Voir
-- demo-data/_header.sql pour le détail.
\encoding UTF8

-- ============================================================================
-- reset-base.sql — Purge sélective d'une base Pharma-Smart
--
-- DESTRUCTEUR. Vide les tables demandées et, par CASCADE, tout ce qui en dépend.
--
--     psql -U pharma_smart -d pharma_smart -v ON_ERROR_STOP=1 \
--          -v cibles=catalogue,referentiels -v confirm_reset=1 -f reset-base.sql
--
-- Sans -v confirm_reset=1, le script affiche seulement le plan — tables ciblées,
-- tables emportées par CASCADE, nombre de lignes — et ne touche à rien.
--
-- Groupes (-v cibles=..., séparés par des virgules) :
--   catalogue      produit et ses satellites (codes-barres, DCI, prix TP…)
--   referentiels   tva, form_produit, famille_produit, categorie, tableau, dci,
--                  magasin, storage, payment_mode, grille_remise, motifs
--                  d'ajustement et de retour
--   bdpm           référentiel médicament importé (ref_*) et interactions
--   socle          comptes, droits, navigation, paramétrage, licence
--   exploitation   tout le reste : ventes, caisses, commandes, stock, factures,
--                  et ce que l'officine saisit elle-même — fournisseurs, rayons,
--                  laboratoires, gammes, banques, tiers payants, remises,
--                  prescripteurs
--   tout           les cinq groupes
--
-- Tables à l'unité, en plus ou à la place des groupes :
--     -v tables=tva,payment_mode
--
-- Schéma : -v schema=<autre> (défaut pharma_smart).
--
-- À savoir avant de lancer :
--   * proche de demo-data/00_reset.sql sans l'égaler : `exploitation,catalogue`
--     emporte aussi fournisseurs, rayons, tiers payants et remises, que 00_reset
--     conserve. Pour recharger la démo, c'est 00_reset qu'il faut, et run_all.sql
--     le rejoue de toute façon.
--   * les lignes posées par Flyway (V1.0.2 : magasin, comptes, tva, familles,
--     modes de règlement, menus…) ne reviennent PAS toutes seules — Flyway ne
--     rejoue pas une migration déjà inscrite dans son historique. Purger
--     `referentiels` ou `socle` suppose de recréer la base ensuite.
--   * app_user référence magasin : purger magasin emporte les comptes, donc la
--     possibilité de se connecter. Le plan le montre dans le collatéral CASCADE.
-- ============================================================================

\if :{?schema}
\else
  \set schema pharma_smart
\endif
\if :{?cibles}
\else
  \set cibles ''
\endif
\if :{?tables}
\else
  \set tables ''
\endif

SET search_path TO :"schema";

-- Les tags de commande du préambule n'apprennent rien ; seuls le plan et les
-- messages comptent.
\set QUIET on

-- Les blocs DO ne voient pas les variables psql : on les leur passe par une
-- table temporaire.
CREATE TEMP TABLE reset_param(cibles text, tables text);
INSERT INTO reset_param VALUES (:'cibles', :'tables');

-- Tables du schéma, hors partitions (le TRUNCATE du parent les emporte) et hors
-- historique Flyway, reconnu à sa colonne installed_rank plutôt qu'à son nom,
-- qui dépend de la configuration.
CREATE TEMP VIEW reset_table_base AS
SELECT c.relname::text AS nom
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = current_schema()
   AND c.relkind IN ('r', 'p')
   AND NOT c.relispartition
   AND NOT EXISTS (
       SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = c.oid AND a.attname = 'installed_rank'
   );

-- ---------------------------------------------------------------------------
-- Composition des groupes. Une table absente de la base est ignorée : le script
-- doit rester lançable sur une base plus ancienne que le code.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE reset_groupe(groupe text, nom text);

INSERT INTO reset_groupe(groupe, nom) VALUES
    ('catalogue', 'produit'),
    ('catalogue', 'produit_aud'),
    ('catalogue', 'produit_code_alias'),
    ('catalogue', 'produit_dci'),
    ('catalogue', 'produit_ref_specialite'),
    ('catalogue', 'produit_tiers_payant_prix'),
    ('catalogue', 'fournisseur_produit'),
    ('catalogue', 'fournisseur_produit_price_history'),
    ('catalogue', 'rayon_produit'),
    ('catalogue', 'substitut'),

    ('referentiels', 'tva'),
    ('referentiels', 'categorie'),
    ('referentiels', 'famille_produit'),
    ('referentiels', 'form_produit'),
    ('referentiels', 'tableau'),
    ('referentiels', 'dci'),
    ('referentiels', 'magasin'),
    ('referentiels', 'storage'),
    ('referentiels', 'payment_mode'),
    ('referentiels', 'motif_ajustement'),
    ('referentiels', 'motif_retour_produit'),
    ('referentiels', 'grille_remise'),

    ('bdpm', 'ref_groupe_generique'),
    ('bdpm', 'ref_substance'),
    ('bdpm', 'ref_dci'),
    ('bdpm', 'ref_specialite'),
    ('bdpm', 'ref_specialite_composition'),
    ('bdpm', 'ref_specialite_rcp'),
    ('bdpm', 'referentiel_interaction_version'),
    ('bdpm', 'interaction'),
    ('bdpm', 'contre_indication'),
    ('bdpm', 'classe_interaction'),
    ('bdpm', 'dci_classe'),

    ('socle', 'app_user'),
    ('socle', 'authority'),
    ('socle', 'user_authority'),
    ('socle', 'app_configuration'),
    ('socle', 'menu'),
    ('socle', 'authority_menu'),
    ('socle', 'authority_privilege'),
    ('socle', 'privilege'),
    ('socle', 'nav_item'),
    ('socle', 'nav_item_role'),
    ('socle', 'nav_permission'),
    ('socle', 'nav_item_user_order'),
    ('socle', 'dashboard_layout'),
    ('socle', 'dashboard_layout_authority'),
    ('socle', 'license_state'),
    ('socle', 'license_audit'),
    ('socle', 'scheduled_report'),
    ('socle', 'planification_facturation'),
    ('socle', 'planification_certification_fne'),
    -- semois_configuration référence produit : elle suit le catalogue, pas le
    -- socle, et tombe donc dans `exploitation`. Seul le paramétrage des classes,
    -- qui ne dépend d'aucun produit, reste ici.
    ('socle', 'semois_classe_config'),
    ('socle', 'classification_config'),
    ('socle', 'poste'),
    ('socle', 'poste_device'),
    ('socle', 'export_modele'),
    ('socle', 'pilotage_vue');

DELETE FROM reset_groupe g WHERE NOT EXISTS (SELECT 1 FROM reset_table_base t WHERE t.nom = g.nom);

-- `exploitation` se définit par soustraction : les données d'activité se
-- comptent par centaines de tables et s'enrichissent à chaque migration, alors
-- que les quatre listes ci-dessus sont courtes et stables.
INSERT INTO reset_groupe(groupe, nom)
SELECT 'exploitation', t.nom
  FROM reset_table_base t
 WHERE NOT EXISTS (SELECT 1 FROM reset_groupe g WHERE g.nom = t.nom);

-- ---------------------------------------------------------------------------
-- Dépouillement des paramètres.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE reset_cible(nom text PRIMARY KEY, groupe text);

DO $$
DECLARE
    v_cibles  text[];
    v_tables  text[];
    v_connus  text[];
    v_inconnu text;
BEGIN
    SELECT coalesce(array_agg(DISTINCT btrim(x)) FILTER (WHERE btrim(x) <> ''), '{}')
      INTO v_cibles
      FROM reset_param p, unnest(string_to_array(lower(p.cibles), ',')) AS x;

    SELECT coalesce(array_agg(DISTINCT btrim(x)) FILTER (WHERE btrim(x) <> ''), '{}')
      INTO v_tables
      FROM reset_param p, unnest(string_to_array(lower(p.tables), ',')) AS x;

    -- Liste figée plutôt que déduite de reset_groupe : un groupe dont aucune
    -- table n'existe sur cette base reste un nom valide, et le script répond
    -- « rien à purger » au lieu de « groupe inconnu ».
    v_connus := ARRAY['catalogue', 'referentiels', 'bdpm', 'socle', 'exploitation', 'tout'];

    SELECT string_agg(g, ', ') INTO v_inconnu
      FROM unnest(v_cibles) AS g WHERE NOT g = ANY (v_connus);
    IF v_inconnu IS NOT NULL THEN
        RAISE EXCEPTION 'Groupe inconnu : %. Attendu : %', v_inconnu, array_to_string(v_connus, ', ');
    END IF;

    SELECT string_agg(t, ', ') INTO v_inconnu
      FROM unnest(v_tables) AS t
     WHERE NOT EXISTS (SELECT 1 FROM reset_table_base b WHERE b.nom = t);
    IF v_inconnu IS NOT NULL THEN
        RAISE EXCEPTION 'Table inconnue dans le schéma % : %', current_schema(), v_inconnu;
    END IF;

    INSERT INTO reset_cible(nom, groupe)
    SELECT g.nom, min(g.groupe)
      FROM reset_groupe g
     WHERE g.groupe = ANY (v_cibles) OR 'tout' = ANY (v_cibles)
     GROUP BY g.nom;

    INSERT INTO reset_cible(nom, groupe)
    SELECT t, 'à l''unité' FROM unnest(v_tables) AS t
    ON CONFLICT (nom) DO NOTHING;

    IF NOT EXISTS (SELECT 1 FROM reset_cible) THEN
        RAISE EXCEPTION 'Rien à purger : préciser -v cibles=<groupes> et/ou -v tables=<tables>. Groupes : %',
            array_to_string(v_connus, ', ');
    END IF;
END $$;

-- Tables qu'aucune cible ne nomme mais que le TRUNCATE ... CASCADE videra, en
-- remontant les clés étrangères de proche en proche.
CREATE TEMP TABLE reset_collateral(nom text PRIMARY KEY);

WITH RECURSIVE dependants AS (
    SELECT c.oid
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      JOIN reset_cible t ON t.nom = c.relname
     WHERE n.nspname = current_schema()
    UNION
    SELECT k.conrelid
      FROM pg_constraint k
      JOIN dependants d ON d.oid = k.confrelid
     WHERE k.contype = 'f' AND k.conrelid <> k.confrelid
)
INSERT INTO reset_collateral(nom)
SELECT DISTINCT c.relname
  FROM dependants d
  JOIN pg_class c ON c.oid = d.oid
 WHERE NOT c.relispartition
   AND NOT EXISTS (SELECT 1 FROM reset_cible t WHERE t.nom = c.relname);

-- ---------------------------------------------------------------------------
-- Plan. Le comptage est exact — un chiffre approché n'aide pas à décider d'une
-- purge — au prix d'un parcours de chaque table concernée.
-- ---------------------------------------------------------------------------
\echo ''
\echo '>> reset-base : plan de purge'

SELECT coalesce(c.groupe, 'CASCADE')                           AS "origine",
       x.nom                                                   AS "table",
       (xpath('/row/n/text()',
              query_to_xml(format('SELECT count(*) AS n FROM %I.%I', current_schema(), x.nom),
                           false, true, '')))[1]::text::bigint AS "lignes"
  FROM (SELECT nom FROM reset_cible UNION ALL SELECT nom FROM reset_collateral) x
  LEFT JOIN reset_cible c ON c.nom = x.nom
 ORDER BY 1, 2;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM reset_cible WHERE groupe IN ('catalogue', 'referentiels'))
       AND NOT EXISTS (SELECT 1 FROM reset_cible WHERE groupe = 'exploitation') THEN
        RAISE WARNING 'Catalogue ou référentiels purgés sans `exploitation` : ventes, commandes et factures survivront sans leurs lignes.';
    END IF;
END $$;

\if :{?confirm_reset}
\else
  \echo ''
  \echo 'SIMULATION : rien n''a été touché.'
  \echo 'Relancer avec  -v confirm_reset=1  si la base est bien celle à purger.'
  \echo ''
  \quit
\endif

-- ---------------------------------------------------------------------------
-- Purge. Un seul TRUNCATE pour toutes les tables : PostgreSQL accepte alors les
-- cycles de clés étrangères entre elles, qu'un DELETE table par table
-- obligerait à ordonner à la main.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_liste text;
    v_count int;
BEGIN
    SELECT string_agg(format('%I.%I', current_schema(), nom), ', '), count(*)
      INTO v_liste, v_count
      FROM reset_cible;

    EXECUTE 'TRUNCATE TABLE ' || v_liste || ' RESTART IDENTITY CASCADE';
    RAISE NOTICE '% table(s) ciblée(s) vidée(s), % emportée(s) par CASCADE.',
        v_count, (SELECT count(*) FROM reset_collateral);
END $$;

-- ---------------------------------------------------------------------------
-- Séquences autonomes : RESTART IDENTITY ne touche que celles attachées à une
-- colonne. Les autres (id_sale_seq, id_commande_seq, avoir_numero_seq…) donnent
-- leur identifiant aux entités à clé assignée ; les remettre à 1 alors qu'il
-- reste des lignes provoquerait des collisions, d'où la condition sur une purge
-- complète de l'exploitation.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_seq   record;
    v_count int := 0;
BEGIN
    IF EXISTS (
        SELECT 1 FROM reset_groupe g
         WHERE g.groupe = 'exploitation'
           AND NOT EXISTS (SELECT 1 FROM reset_cible c WHERE c.nom = g.nom)
    ) THEN
        RAISE NOTICE 'Séquences autonomes laissées en l''état : l''exploitation n''est pas entièrement purgée.';
        RETURN;
    END IF;

    FOR v_seq IN
        SELECT c.relname
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE c.relkind = 'S'
           AND n.nspname = current_schema()
           -- Une séquence attachée à une colonne dépend d'elle (refobjsubid non
           -- nul), que la colonne soit en SERIAL ou en IDENTITY.
           AND NOT EXISTS (
               SELECT 1 FROM pg_depend d
                WHERE d.classid = 'pg_class'::regclass
                  AND d.objid = c.oid
                  AND d.refclassid = 'pg_class'::regclass
                  AND d.refobjsubid > 0
           )
    LOOP
        EXECUTE format('ALTER SEQUENCE %I RESTART WITH 1', v_seq.relname);
        v_count := v_count + 1;
    END LOOP;
    RAISE NOTICE '% séquence(s) autonome(s) remise(s) à 1.', v_count;
END $$;

-- Les vues matérialisées gardent sinon l'image d'avant la purge.
DO $$
DECLARE
    v_mv record;
BEGIN
    FOR v_mv IN SELECT matviewname FROM pg_matviews WHERE schemaname = current_schema()
    LOOP
        BEGIN
            EXECUTE format('REFRESH MATERIALIZED VIEW %I', v_mv.matviewname);
        EXCEPTION WHEN others THEN
            RAISE WARNING 'Vue matérialisée % non rafraîchie : %', v_mv.matviewname, SQLERRM;
        END;
    END LOOP;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM reset_cible WHERE groupe IN ('referentiels', 'socle')) THEN
        RAISE WARNING 'Référentiels ou socle purgés : les lignes posées par Flyway ne reviendront qu''en recréant la base (Flyway ne rejoue pas une migration déjà appliquée).';
    END IF;
END $$;

\echo '<< reset-base : terminé'
