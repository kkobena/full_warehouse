-- ============================================================================
-- 00_reset.sql — Purge des données métier
--
-- DESTRUCTEUR. Vide toutes les tables métier du schéma et remet les compteurs
-- à zéro, en préservant les référentiels posés par Flyway (magasin, comptes,
-- TVA, familles, menus…).
--
-- Ne s'exécute que sur confirmation explicite :
--     psql -v ON_ERROR_STOP=1 -v confirm_reset=1 -f 00_reset.sql
--
-- Voir docs/PLAN-GENERATION-DONNEES-DEMO.md §7.
-- ============================================================================

\if :{?confirm_reset}
\else
  \echo ''
  \echo 'ABANDON : 00_reset.sql efface TOUTES les données métier du schéma.'
  \echo 'Relancer avec  -v confirm_reset=1  si la base est bien une base de démo.'
  \echo ''
  \quit
\endif

\i _header.sql

\echo '>> 00_reset : purge des données métier'

-- ---------------------------------------------------------------------------
-- Vidage : liste blanche des tables à CONSERVER, tout le reste est tronqué.
--
-- On énumère ce qu'on garde plutôt que ce qu'on vide : la liste des tables
-- métier dépasse la centaine et se périmerait à la première migration, alors
-- que la liste des référentiels est courte et stable.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_keep CONSTANT text[] := ARRAY[
        -- Structure de l'officine
        'magasin', 'storage', 'rayon', 'tableau',
        -- Comptes et droits
        'app_user', 'authority', 'user_authority',
        -- Référentiels produit
        'tva', 'categorie', 'famille_produit', 'form_produit',
        -- Paramétrage
        'app_configuration', 'payment_mode', 'groupe_fournisseur',
        'semois_configuration', 'semois_classe_config', 'classification_config',
        -- Navigation et tableaux de bord
        'nav_item', 'nav_item_role', 'nav_permission', 'nav_item_user_order',
        'dashboard_layout', 'dashboard_layout_authority',
        -- Planification et licence
        'scheduled_report', 'license_state', 'license_audit'
    ];
    v_tables text;
    v_count  int;
BEGIN
    SELECT string_agg(format('%I.%I', t.schemaname, t.tablename), ', '), count(*)
      INTO v_tables, v_count
      FROM pg_tables t
     WHERE t.schemaname = current_schema()
       AND t.tablename <> ALL (v_keep)
       -- La table d'historique Flyway se reconnaît à sa colonne installed_rank,
       -- ce qui évite de coder en dur un nom qui dépend de la configuration.
       AND NOT EXISTS (
           SELECT 1
             FROM pg_attribute a
             JOIN pg_class c ON c.oid = a.attrelid
             JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = t.tablename
              AND n.nspname = t.schemaname
              AND a.attname = 'installed_rank'
       );

    IF v_tables IS NULL THEN
        RAISE NOTICE 'Aucune table à vider.';
    ELSE
        -- Un seul TRUNCATE pour toutes les tables : PostgreSQL accepte alors
        -- les cycles de clés étrangères entre elles, qu'un DELETE table par
        -- table obligerait à ordonner à la main.
        EXECUTE 'TRUNCATE TABLE ' || v_tables || ' RESTART IDENTITY CASCADE';
        RAISE NOTICE '% table(s) vidée(s).', v_count;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- Séquences autonomes : RESTART IDENTITY ne les touche pas.
--
-- Ces séquences alimentent les identifiants assignés à la main des entités à
-- clé composite (sales, commande, order_line, payment_transaction…).
-- Voir V1.0.4__id_generator.sql et le §3.4 du plan.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_seq  text;
    v_seqs CONSTANT text[] := ARRAY[
        'id_sale_seq', 'id_sale_item_seq', 'id_sale_assurance_item_seq',
        'id_commande_seq', 'id_order_line_seq',
        'id_transaction_seq', 'id_transaction_item_seq',
        'id_facture_seq', 'id_facture_item_seq',
        'id_mvt_produit_seq', 'invoice_generation_code_seq'
    ];
BEGIN
    FOREACH v_seq IN ARRAY v_seqs LOOP
        IF EXISTS (
            SELECT 1 FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE c.relkind = 'S' AND c.relname = v_seq
               AND n.nspname = current_schema()
        ) THEN
            EXECUTE format('ALTER SEQUENCE %I RESTART WITH 1', v_seq);
        ELSE
            RAISE NOTICE 'Séquence % absente, ignorée.', v_seq;
        END IF;
    END LOOP;
END $$;

\echo '<< 00_reset : terminé'
