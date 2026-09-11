

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE rel.relname = 'store_inventory_line'
          AND nsp.nspname = current_schema()
          AND con.contype = 'u'
          AND (
                  SELECT array_agg(att.attname::text ORDER BY att.attname)
                  FROM unnest(con.conkey) AS k(attnum)
                           JOIN pg_attribute att
                                ON att.attrelid = con.conrelid AND att.attnum = k.attnum
              ) = ARRAY ['produit_id', 'store_inventory_id']
        LOOP
            RAISE NOTICE 'store_inventory_line : suppression de la contrainte unique obsolete %', r.conname;
            EXECUTE format('ALTER TABLE store_inventory_line DROP CONSTRAINT %I', r.conname);
        END LOOP;
END
$$;


DO $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1
                   FROM pg_constraint con
                            JOIN pg_class rel ON rel.oid = con.conrelid
                            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                   WHERE rel.relname = 'store_inventory_line'
                     AND nsp.nspname = current_schema()
                     AND con.contype = 'u'
                     AND (
                             SELECT array_agg(att.attname::text ORDER BY att.attname)
                             FROM unnest(con.conkey) AS k(attnum)
                                      JOIN pg_attribute att
                                           ON att.attrelid = con.conrelid AND att.attnum = k.attnum
                         ) = ARRAY ['produit_id', 'storage_id', 'store_inventory_id'])
    INTO v_existe;

    IF NOT v_existe THEN
        RAISE NOTICE 'store_inventory_line : creation de l unicite (produit_id, store_inventory_id, storage_id)';
        ALTER TABLE store_inventory_line
            ADD CONSTRAINT uq_sil_produit_inventory_storage
                UNIQUE (produit_id, store_inventory_id, storage_id);
    END IF;
END
$$;
