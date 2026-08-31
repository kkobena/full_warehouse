
\i _header.sql

\echo '>> 00b_partitions : partitions annuelles'

DO $$
DECLARE
    -- Reprise à l'identique de la liste de FlywayConfig. Toute table
    -- partitionnée ajoutée là doit l'être ici aussi, sans quoi le chargement
    -- échouera sur la première ligne antérieure à l'année courante.
    v_tables CONSTANT text[] := ARRAY[
        'sales', 'sales_line', 'third_party_sale_line',
        'commande', 'order_line',
        'facture_tiers_payant', 'payment_transaction', 'invoice_payment_item',
        'inventory_transaction'
    ];

    -- Deux ans en arrière : le jeu couvre environ dix-huit mois, et les
    -- périmés détruits remontent plus loin que les ventes. Une année de marge
    -- coûte une table vide, un trou coûte un chargement interrompu.
    v_debut CONSTANT int := EXTRACT(YEAR FROM CURRENT_DATE)::int - 2;
    v_fin   CONSTANT int := EXTRACT(YEAR FROM CURRENT_DATE)::int + 1;

    v_table text;
    v_annee int;
    v_nom   text;
    v_cree  int := 0;
    v_ignore int := 0;
BEGIN
    FOREACH v_table IN ARRAY v_tables LOOP
        -- La table peut ne pas être partitionnée sur une installation
        -- ancienne : on la saute plutôt que d'échouer sur un CREATE TABLE
        -- ... PARTITION OF qui n'aurait aucun sens.
        IF NOT EXISTS (
            SELECT 1 FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE c.relname = v_table
               AND n.nspname = current_schema()
               AND c.relkind = 'p'
        ) THEN
            RAISE NOTICE 'Table % absente ou non partitionnée, ignorée.', v_table;
            CONTINUE;
        END IF;

        FOR v_annee IN v_debut..v_fin LOOP
            v_nom := v_table || '_' || v_annee;

            IF EXISTS (
                SELECT 1 FROM pg_class c
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE c.relname = v_nom AND n.nspname = current_schema()
            ) THEN
                v_ignore := v_ignore + 1;
                CONTINUE;
            END IF;

            EXECUTE format(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                v_nom, v_table, make_date(v_annee, 1, 1), make_date(v_annee + 1, 1, 1)
            );
            v_cree := v_cree + 1;
        END LOOP;
    END LOOP;

    RAISE NOTICE '% partition(s) créée(s), % déjà présente(s) — années % à %.',
                 v_cree, v_ignore, v_debut, v_fin;
END $$;

-- ---------------------------------------------------------------------------
-- Contrôle : chaque table partitionnée doit couvrir toute la plage, sans trou.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_manquantes text;
BEGIN
    SELECT string_agg(t.relname || '_' || a.annee, ', ' ORDER BY t.relname, a.annee)
      INTO v_manquantes
      FROM pg_class t
      JOIN pg_namespace n ON n.oid = t.relnamespace
     CROSS JOIN generate_series(
         EXTRACT(YEAR FROM CURRENT_DATE)::int - 2,
         EXTRACT(YEAR FROM CURRENT_DATE)::int + 1
     ) AS a(annee)
     WHERE n.nspname = current_schema()
       AND t.relkind = 'p'
       AND NOT EXISTS (
           SELECT 1 FROM pg_class c
             JOIN pg_namespace n2 ON n2.oid = c.relnamespace
            WHERE c.relname = t.relname || '_' || a.annee
              AND n2.nspname = current_schema()
       );

    IF v_manquantes IS NOT NULL THEN
        RAISE EXCEPTION 'Partition(s) manquante(s) : %', v_manquantes;
    END IF;
END $$;

\echo '<< 00b_partitions : terminé'
