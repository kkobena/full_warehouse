-- V2.1.52 — Recaler les séquences d'identité sous leur max(id)

DO $$
DECLARE
    r        record;
    max_id   bigint;
    derniere bigint;
    recalees int := 0;
BEGIN
    FOR r IN
        SELECT c.table_name AS table_nom,
               pg_get_serial_sequence(quote_ident(c.table_schema) || '.' || quote_ident(c.table_name), 'id') AS seq
          FROM information_schema.columns c
          JOIN information_schema.tables t
            ON t.table_schema = c.table_schema AND t.table_name = c.table_name AND t.table_type = 'BASE TABLE'
         WHERE c.table_schema = current_schema()
           AND c.column_name = 'id'
    LOOP
        CONTINUE WHEN r.seq IS NULL;

        EXECUTE format('SELECT max(id) FROM %I', r.table_nom) INTO max_id;
        CONTINUE WHEN max_id IS NULL;

        SELECT s.last_value INTO derniere
          FROM pg_sequences s
         WHERE quote_ident(s.schemaname) || '.' || quote_ident(s.sequencename) = r.seq;

        -- `last_value` est NULL tant que la séquence n'a jamais servi : des lignes posées à la
        -- main sur une séquence intacte sont exactement le cas qu'on répare.
        IF derniere IS NULL OR max_id > derniere THEN
            EXECUTE format('SELECT setval(%L, %s)', r.seq, max_id);
            RAISE NOTICE 'Sequence recalee : % -> %', r.seq, max_id;
            recalees := recalees + 1;
        END IF;
    END LOOP;

    RAISE NOTICE 'Sequences recalees : %', recalees;
END $$;
