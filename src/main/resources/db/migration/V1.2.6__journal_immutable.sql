-- ═══════════════════════════════════════════════════════════════════════════
-- MIGRATION V1.2.6 — Journal immuable (Principe 1)
-- ═══════════════════════════════════════════════════════════════════════════
-- Contenu :
--   1.  Ajout storage_id à inventory_transaction + backfill
--   2.  Normalisation quantity INVENTAIRE → delta signé
--   3.  Contrainte unique corrigée (created_at au lieu de transaction_date)
--   4.  Table stock_produit_snapshot + index
--   5.  Snapshot initial depuis stock_produit
--   6.  Mise à jour proc_close_inventory_v2 (storage_id + delta + snapshot)
--   7.  Mise à jour proc_close_inventory legacy (storage_id + delta)
--   8.  Fonction fn_stock_at_time
--   9.  Vue v_stock_ecart_journal
--   10. Backfill repartition_stock_produit → journal
--   11. Fonctions de valorisation (fn_stock_valuation_at_time,
--       fn_stock_valuation_bulk, fn_stock_bilan_periode)
-- ═══════════════════════════════════════════════════════════════════════════


-- ══════════════════════════════════════════════════════════════════════════
-- 1. storage_id dans inventory_transaction
-- ══════════════════════════════════════════════════════════════════════════

ALTER TABLE inventory_transaction
  ADD COLUMN IF NOT EXISTS storage_id INT REFERENCES storage (id);

-- Backfill INVENTAIRE (via store_inventory_line.storage_id)
UPDATE inventory_transaction it
SET storage_id = sil.storage_id
FROM store_inventory_line sil
WHERE it.mouvement_type = 'INVENTAIRE'
  AND it.entity_id = sil.id
  AND it.storage_id IS NULL;

-- Backfill AJUSTEMENT (via ajustement → stock_produit → storage)
UPDATE inventory_transaction it
SET storage_id = sp.storage_id
FROM ajustement a
JOIN stock_produit sp ON sp.id = a.stock_produit_id
WHERE it.mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')
  AND it.entity_id = a.id
  AND it.storage_id IS NULL;

-- Backfill RETRAIT_PERIME (via products_to_destroy → magasin → PRINCIPAL)
UPDATE inventory_transaction it
SET storage_id = s.id
FROM products_to_destroy ptd
JOIN storage s ON s.magasin_id = ptd.magasin_id AND s.storage_type = 'PRINCIPAL'
WHERE it.mouvement_type = 'RETRAIT_PERIME'
  AND it.entity_id = ptd.id
  AND it.storage_id IS NULL;

-- Backfill restant → storage PRINCIPAL du magasin (fallback)
UPDATE inventory_transaction it
SET storage_id = s.id
FROM storage s
WHERE it.storage_id IS NULL
  AND s.magasin_id = it.magasin_id
  AND s.storage_type = 'PRINCIPAL';

ALTER TABLE inventory_transaction
  ALTER COLUMN storage_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inv_tx_storage_produit_date
  ON inventory_transaction (storage_id, produit_id, created_at);

CREATE INDEX IF NOT EXISTS idx_inv_tx_produit_date
  ON inventory_transaction (produit_id, created_at);


-- ══════════════════════════════════════════════════════════════════════════
-- 2. Normalisation quantity INVENTAIRE → delta signé
-- ══════════════════════════════════════════════════════════════════════════

-- Avant : quantity = quantity_after (valeur absolue)
-- Après  : quantity = quantity_after - quantity_befor (delta signé)
-- Guard : ne toucher que les lignes où quantity = quantity_after (ancienne valeur)
UPDATE inventory_transaction
SET quantity = quantity_after - quantity_befor
WHERE mouvement_type = 'INVENTAIRE'
  AND quantity = quantity_after
  AND quantity <> (quantity_after - quantity_befor);


-- ══════════════════════════════════════════════════════════════════════════
-- 3. Contrainte unique corrigée
-- ══════════════════════════════════════════════════════════════════════════

DO
$$
  DECLARE
    v_constraint_name TEXT;
  BEGIN
    SELECT conname
    INTO v_constraint_name
    FROM pg_constraint c
           JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'inventory_transaction'
      AND c.contype = 'u'
      AND c.conkey @> ARRAY (
      SELECT attnum
      FROM pg_attribute
      WHERE attrelid = t.oid
        AND attname IN ('entity_id', 'produit_id', 'mouvement_type', 'transaction_date')
                       )
    LIMIT 1;

    IF v_constraint_name IS NOT NULL THEN
      EXECUTE format('ALTER TABLE inventory_transaction DROP CONSTRAINT %I', v_constraint_name);
      RAISE NOTICE 'Contrainte % supprimée', v_constraint_name;
    ELSE
      RAISE NOTICE 'Aucune contrainte à supprimer';
    END IF;
  END;
$$;

ALTER TABLE inventory_transaction
  ADD CONSTRAINT uq_inv_tx_entity_produit_type_storage_ts
    UNIQUE (entity_id, produit_id, mouvement_type, storage_id, created_at);


-- ══════════════════════════════════════════════════════════════════════════
-- 4. Table stock_produit_snapshot
-- ══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS stock_produit_snapshot
(
  id                  BIGINT PRIMARY KEY,
  produit_id          INT         NOT NULL REFERENCES produit (id),
  storage_id          INT         NOT NULL REFERENCES storage (id),
  snapshot_date       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  qty_stock           INT         NOT NULL,
  source_type         VARCHAR(30) NOT NULL, -- 'INVENTAIRE_CLOTURE' | 'BATCH_QUOTIDIEN' | 'INITIAL'
  source_inventory_id BIGINT REFERENCES store_inventory (id),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE stock_produit_snapshot
  ADD CONSTRAINT uq_snapshot_produit_storage_date
    UNIQUE (produit_id, storage_id, snapshot_date);

CREATE INDEX IF NOT EXISTS idx_snapshot_storage_produit
  ON stock_produit_snapshot (storage_id, produit_id, snapshot_date DESC);


-- ══════════════════════════════════════════════════════════════════════════
-- 5. Snapshot initial depuis l'état courant de stock_produit
-- ══════════════════════════════════════════════════════════════════════════

INSERT INTO stock_produit_snapshot (id, produit_id, storage_id, snapshot_date, qty_stock, source_type)
SELECT nextval('mvt_produit_id_seq'), produit_id, storage_id, NOW(), qty_stock, 'INITIAL'
FROM stock_produit
ON CONFLICT DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════════
-- 6. proc_close_inventory_v2 (storage_id + delta signé + snapshot)
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
  IN p_store_inventory_id BIGINT,
  IN p_gestion_lot BOOLEAN,
  INOUT p_nombre_ligne INT
)
  LANGUAGE plpgsql AS $$
DECLARE
  v_user_id            INT;
  v_magasin_id         INT;
  v_inventory_category TEXT;
BEGIN
  SELECT s.user_id, u.magasin_id, s.inventory_category
  INTO v_user_id, v_magasin_id, v_inventory_category
  FROM store_inventory s
         JOIN app_user u ON s.user_id = u.id
  WHERE s.id = p_store_inventory_id;

  -- STEP 1 : Mettre à jour le stock du storage inventorié
  UPDATE stock_produit sp
  SET qty_stock   = COALESCE(sil.quantity_on_hand, sil.quantity_init),
      qty_ug      = 0,
      qty_virtual = COALESCE(sil.quantity_on_hand, sil.quantity_init),
      updated_at  = NOW()
  FROM store_inventory_line sil
  WHERE sil.store_inventory_id = p_store_inventory_id
    AND sp.produit_id = sil.produit_id
    AND sp.storage_id = sil.storage_id;

  GET DIAGNOSTICS p_nombre_ligne = ROW_COUNT;

  -- STEP 2 : Réserve → 0 (tous types SAUF STORAGE)
  IF v_inventory_category <> 'STORAGE' THEN
    UPDATE stock_produit sp
    SET qty_stock   = 0,
        qty_ug      = 0,
        qty_virtual = 0,
        updated_at  = NOW()
    FROM store_inventory_line sil
           JOIN storage s_inventoried ON s_inventoried.id = sil.storage_id
    WHERE sil.store_inventory_id = p_store_inventory_id
      AND sp.produit_id = sil.produit_id
      AND sp.storage_id <> sil.storage_id
      AND sp.storage_id IN (
      SELECT s2.id FROM storage s2
      WHERE s2.magasin_id = s_inventoried.magasin_id
        AND s2.storage_type = 'SAFETY_STOCK'
    );
  END IF;

  -- STEP 3 : Lots (si gestion lot activée)
  IF p_gestion_lot THEN
    UPDATE lot l
    SET current_quantity = COALESCE(il.quantity_on_hand, il.quantity_init)
    FROM inventory_lot il
           JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
    WHERE sil.store_inventory_id = p_store_inventory_id
      AND l.id = il.lot_id;
  END IF;

  -- STEP 4 : Journal — delta signé + storage_id
  INSERT INTO inventory_transaction
    (cost_amount, created_at, entity_id, mouvement_type,
     quantity, quantity_after, quantity_befor, regular_unit_price,
     magasin_id, storage_id, produit_id, user_id, transaction_date)
  SELECT sil.inventory_value_cost,
         sil.updated_at,
         sil.id,
         'INVENTAIRE',
         COALESCE(sil.quantity_on_hand, sil.quantity_init) - sil.quantity_init, -- delta signé
         COALESCE(sil.quantity_on_hand, sil.quantity_init),                     -- after
         sil.quantity_init,                                                     -- before
         sil.last_unit_price,
         v_magasin_id,
         sil.storage_id,
         sil.produit_id,
         v_user_id,
         sil.updated_at::date
  FROM store_inventory_line sil
  WHERE sil.store_inventory_id = p_store_inventory_id;

  -- STEP 5 : Snapshot checkpoint
  INSERT INTO stock_produit_snapshot
    (id, produit_id, storage_id, snapshot_date, qty_stock, source_type, source_inventory_id)
  SELECT nextval('mvt_produit_id_seq'),
         sil.produit_id,
         sil.storage_id,
         NOW(),
         COALESCE(sil.quantity_on_hand, sil.quantity_init),
         'INVENTAIRE_CLOTURE',
         p_store_inventory_id
  FROM store_inventory_line sil
  WHERE sil.store_inventory_id = p_store_inventory_id
  ON CONFLICT ON CONSTRAINT uq_snapshot_produit_storage_date DO NOTHING;

END;
$$;


-- ══════════════════════════════════════════════════════════════════════════
-- 7. proc_close_inventory legacy (storage_id + delta signé)
-- ══════════════════════════════════════════════════════════════════════════
-- Procédure non appelée en production (remplacée par v2) mais présente en DB.
-- Mise à jour pour éviter une violation NOT NULL sur storage_id.

CREATE OR REPLACE PROCEDURE proc_close_inventory(
  IN  p_store_inventory_id BIGINT,
  INOUT p_nombreligne INTEGER
)
  LANGUAGE plpgsql AS
$$
DECLARE
  v_produit_id           INT;
  v_storage_id           INT;
  v_entity_id            BIGINT;
  v_quantity_on_hand     INT;
  v_quantity_init        INT;
  v_inventory_value_cost INT;
  v_last_unit_price      INT;
  v_user_id              INT;
  v_magasin_id           INT;
  v_updated_at           TIMESTAMP;

  curbl CURSOR FOR
    SELECT a.quantity_on_hand,
           a.storage_id,
           a.produit_id,
           s.user_id,
           u.magasin_id,
           a.id,
           a.inventory_value_cost,
           a.last_unit_price,
           COALESCE(a.quantity_init, 0),
           a.updated_at
    FROM store_inventory_line a
           JOIN store_inventory s ON s.id = a.store_inventory_id
           JOIN app_user u ON s.user_id = u.id
    WHERE s.id = p_store_inventory_id;
BEGIN
  p_nombreligne := 0;
  OPEN curbl;
  LOOP
    FETCH curbl INTO v_quantity_on_hand, v_storage_id, v_produit_id, v_user_id,
                     v_magasin_id, v_entity_id, v_inventory_value_cost,
                     v_last_unit_price, v_quantity_init, v_updated_at;
    EXIT WHEN NOT FOUND;

    UPDATE stock_produit st
    SET updated_at  = NOW(),
        qty_ug      = 0,
        qty_stock   = v_quantity_on_hand,
        qty_virtual = v_quantity_on_hand
    WHERE st.produit_id = v_produit_id
      AND st.storage_id = v_storage_id;

    INSERT INTO inventory_transaction
      (cost_amount, created_at, entity_id, mouvement_type,
       quantity, quantity_after, quantity_befor, regular_unit_price,
       magasin_id, storage_id, produit_id, user_id)
    VALUES (v_inventory_value_cost,
            v_updated_at,
            v_entity_id,
            'INVENTAIRE',
            v_quantity_on_hand - v_quantity_init, -- delta signé
            v_quantity_on_hand,
            v_quantity_init,
            v_last_unit_price,
            v_magasin_id,
            v_storage_id,                         -- NOT NULL requis depuis V1.2.6
            v_produit_id,
            v_user_id);

    p_nombreligne := p_nombreligne + 1;
  END LOOP;
  CLOSE curbl;
END;
$$;


-- ══════════════════════════════════════════════════════════════════════════
-- 8. fn_stock_at_time
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION fn_stock_at_time(
  p_produit_id INT,
  p_storage_id INT,
  p_at         TIMESTAMPTZ DEFAULT NOW()
)
  RETURNS INT
  LANGUAGE sql STABLE AS $$
WITH last_snapshot AS (
  SELECT qty_stock, snapshot_date
  FROM stock_produit_snapshot
  WHERE produit_id    = p_produit_id
    AND storage_id    = p_storage_id
    AND snapshot_date <= p_at
  ORDER BY snapshot_date DESC
  LIMIT 1
),
deltas AS (
  SELECT COALESCE(SUM(quantity_after - quantity_befor), 0) AS total_delta
  FROM inventory_transaction
  WHERE produit_id      = p_produit_id
    AND storage_id      = p_storage_id
    AND mouvement_type <> 'INVENTAIRE'
    AND created_at      > (SELECT COALESCE(snapshot_date, '-infinity'::timestamptz) FROM last_snapshot)
    AND created_at     <= p_at
)
SELECT GREATEST(
         COALESCE((SELECT qty_stock FROM last_snapshot), 0) + (SELECT total_delta FROM deltas),
         0
       )::INT;
$$;


-- ══════════════════════════════════════════════════════════════════════════
-- 9. Vue v_stock_ecart_journal (contrôle technique)
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_stock_ecart_journal AS
SELECT sp.produit_id,
       sp.storage_id,
       sp.qty_stock                                                         AS qty_stock_actuel,
       fn_stock_at_time(sp.produit_id, sp.storage_id, NOW())                AS qty_stock_journal,
       sp.qty_stock - fn_stock_at_time(sp.produit_id, sp.storage_id, NOW()) AS ecart
FROM stock_produit sp;


-- ══════════════════════════════════════════════════════════════════════════
-- 10. Backfill repartition_stock_produit → journal
-- ══════════════════════════════════════════════════════════════════════════

-- MOUVEMENT_STOCK_OUT (source)
INSERT INTO inventory_transaction
  (id, transaction_date, cost_amount, created_at, entity_id, mouvement_type,
   quantity, quantity_after, quantity_befor, regular_unit_price,
   magasin_id, storage_id, produit_id, user_id)
SELECT nextval('mvt_produit_id_seq'),
       r.created_at::date,
       COALESCE(fp.prix_achat, 0),
       r.created_at,
       r.id,
       'MOUVEMENT_STOCK_OUT',
       -r.qty_mvt,
       r.source_final_stock,
       r.source_init_stock,
       COALESCE(fp.prix_uni, 0),
       s.magasin_id,
       sp_src.storage_id,
       sp_src.produit_id,
       r.user_id
FROM repartition_stock_produit r
JOIN stock_produit sp_src ON sp_src.id = r.stock_produit_source_id
JOIN storage s             ON s.id = sp_src.storage_id
LEFT JOIN fournisseur_produit fp
       ON fp.produit_id = sp_src.produit_id AND fp.principal = 'true'
WHERE r.stock_produit_source_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM inventory_transaction it
    WHERE it.entity_id      = r.id
      AND it.mouvement_type = 'MOUVEMENT_STOCK_OUT'
      AND it.produit_id     = sp_src.produit_id
  );

-- MOUVEMENT_STOCK_IN (destination)
INSERT INTO inventory_transaction
  (id, transaction_date, cost_amount, created_at, entity_id, mouvement_type,
   quantity, quantity_after, quantity_befor, regular_unit_price,
   magasin_id, storage_id, produit_id, user_id)
SELECT nextval('mvt_produit_id_seq'),
       r.created_at::date,
       COALESCE(fp.prix_achat, 0),
       r.created_at,
       r.id,
       'MOUVEMENT_STOCK_IN',
       r.qty_mvt,
       r.dest_final_stock,
       r.dest_init_stock,
       COALESCE(fp.prix_uni, 0),
       s.magasin_id,
       sp_dst.storage_id,
       sp_dst.produit_id,
       r.user_id
FROM repartition_stock_produit r
JOIN stock_produit sp_dst ON sp_dst.id = r.stock_produit_destination_id
JOIN storage s             ON s.id = sp_dst.storage_id
LEFT JOIN fournisseur_produit fp
       ON fp.produit_id = sp_dst.produit_id AND fp.principal = 'true'
WHERE NOT EXISTS (
  SELECT 1 FROM inventory_transaction it
  WHERE it.entity_id      = r.id
    AND it.mouvement_type = 'MOUVEMENT_STOCK_IN'
    AND it.produit_id     = sp_dst.produit_id
);


-- ══════════════════════════════════════════════════════════════════════════
-- 11. Fonctions de valorisation
-- ══════════════════════════════════════════════════════════════════════════

-- ── 11a. Scalaire : un produit/storage à l'instant T ──────────────────────
CREATE OR REPLACE FUNCTION fn_stock_valuation_at_time(
  p_produit_id INT,
  p_storage_id INT,
  p_at         TIMESTAMPTZ DEFAULT NOW()
)
RETURNS TABLE (
  qty_stock    INT,
  prix_achat   INT,
  prix_vente   INT,
  valeur_achat BIGINT,
  valeur_vente BIGINT
)
LANGUAGE sql STABLE AS $$
  WITH qty AS (
    SELECT fn_stock_at_time(p_produit_id, p_storage_id, p_at) AS q
  ),
  last_tx AS (
    SELECT cost_amount, regular_unit_price
    FROM inventory_transaction
    WHERE produit_id  = p_produit_id
      AND storage_id  = p_storage_id
      AND created_at <= p_at
    ORDER BY created_at DESC
    LIMIT 1
  )
  SELECT qty.q,
         lt.cost_amount,
         lt.regular_unit_price,
         qty.q * lt.cost_amount,
         qty.q * lt.regular_unit_price
  FROM qty, last_tx lt;
$$;

-- ── 11b. Bulk : un magasin entier (ou filtre produits) à l'instant T ──────
CREATE OR REPLACE FUNCTION fn_stock_valuation_bulk(
  p_magasin_id  INT,
  p_produit_ids INT[]       DEFAULT NULL,
  p_at          TIMESTAMPTZ DEFAULT NOW()
)
RETURNS TABLE (
  produit_id   INT,
  storage_id   INT,
  qty_stock    INT,
  prix_achat   INT,
  prix_vente   INT,
  valeur_achat BIGINT,
  valeur_vente BIGINT
)
LANGUAGE sql STABLE AS $$
  WITH
  scope AS (
    SELECT sp.produit_id, sp.storage_id
    FROM stock_produit sp
    JOIN storage s ON s.id = sp.storage_id
    WHERE s.magasin_id = p_magasin_id
      AND (p_produit_ids IS NULL OR sp.produit_id = ANY(p_produit_ids))
  ),
  last_snapshot AS (
    SELECT DISTINCT ON (sn.produit_id, sn.storage_id)
           sn.produit_id, sn.storage_id,
           sn.qty_stock      AS snap_qty,
           sn.snapshot_date  AS snap_date
    FROM stock_produit_snapshot sn
    JOIN scope sc ON sc.produit_id = sn.produit_id AND sc.storage_id = sn.storage_id
    WHERE sn.snapshot_date <= p_at
    ORDER BY sn.produit_id, sn.storage_id, sn.snapshot_date DESC
  ),
  deltas AS (
    SELECT it.produit_id, it.storage_id,
           COALESCE(SUM(it.quantity_after - it.quantity_befor), 0) AS total_delta
    FROM inventory_transaction it
    JOIN scope sc ON sc.produit_id = it.produit_id AND sc.storage_id = it.storage_id
    LEFT JOIN last_snapshot ls ON ls.produit_id = it.produit_id AND ls.storage_id = it.storage_id
    WHERE it.mouvement_type <> 'INVENTAIRE'
      AND it.created_at >  COALESCE(ls.snap_date, '-infinity'::timestamptz)
      AND it.created_at <= p_at
    GROUP BY it.produit_id, it.storage_id
  ),
  qty AS (
    SELECT sc.produit_id, sc.storage_id,
           GREATEST(COALESCE(ls.snap_qty, 0) + COALESCE(d.total_delta, 0), 0)::INT AS q
    FROM scope sc
    LEFT JOIN last_snapshot ls ON ls.produit_id = sc.produit_id AND ls.storage_id = sc.storage_id
    LEFT JOIN deltas d         ON d.produit_id  = sc.produit_id AND d.storage_id  = sc.storage_id
  ),
  last_price AS (
    SELECT DISTINCT ON (it.produit_id, it.storage_id)
           it.produit_id, it.storage_id,
           it.cost_amount        AS prix_achat,
           it.regular_unit_price AS prix_vente
    FROM inventory_transaction it
    JOIN scope sc ON sc.produit_id = it.produit_id AND sc.storage_id = it.storage_id
    WHERE it.created_at <= p_at
    ORDER BY it.produit_id, it.storage_id, it.created_at DESC
  )
  SELECT q.produit_id, q.storage_id, q.q,
         lp.prix_achat, lp.prix_vente,
         (q.q * lp.prix_achat)::BIGINT,
         (q.q * lp.prix_vente)::BIGINT
  FROM qty q
  JOIN last_price lp ON lp.produit_id = q.produit_id AND lp.storage_id = q.storage_id
  WHERE q.q > 0;
$$;

-- ── 11c. Bilan sur une période ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_stock_bilan_periode(
  p_magasin_id  INT,
  p_date_debut  TIMESTAMPTZ,
  p_date_fin    TIMESTAMPTZ,
  p_produit_ids INT[] DEFAULT NULL
)
RETURNS TABLE (
  produit_id         INT,
  storage_id         INT,
  qty_debut          INT,
  prix_achat_debut   INT,
  valeur_achat_debut BIGINT,
  valeur_vente_debut BIGINT,
  entrees            BIGINT,
  sorties            BIGINT,
  ajustements        BIGINT,
  qty_fin            INT,
  prix_achat_fin     INT,
  valeur_achat_fin   BIGINT,
  valeur_vente_fin   BIGINT
)
LANGUAGE sql STABLE AS $$
  WITH
  scope AS (
    SELECT sp.produit_id, sp.storage_id
    FROM stock_produit sp
    JOIN storage s ON s.id = sp.storage_id
    WHERE s.magasin_id = p_magasin_id
      AND (p_produit_ids IS NULL OR sp.produit_id = ANY(p_produit_ids))
  ),
  val_debut AS (
    SELECT v.produit_id, v.storage_id,
           v.qty_stock    AS qty_debut,
           v.prix_achat   AS prix_achat_debut,
           v.valeur_achat AS valeur_achat_debut,
           v.valeur_vente AS valeur_vente_debut
    FROM fn_stock_valuation_bulk(p_magasin_id, p_produit_ids, p_date_debut) v
  ),
  val_fin AS (
    SELECT v.produit_id, v.storage_id,
           v.qty_stock    AS qty_fin,
           v.prix_achat   AS prix_achat_fin,
           v.valeur_achat AS valeur_achat_fin,
           v.valeur_vente AS valeur_vente_fin
    FROM fn_stock_valuation_bulk(p_magasin_id, p_produit_ids, p_date_fin) v
  ),
  mvt AS (
    SELECT it.produit_id, it.storage_id,
           COALESCE(SUM(CASE
             WHEN (it.quantity_after - it.quantity_befor) > 0
              AND it.mouvement_type NOT IN ('AJUSTEMENT_IN','AJUSTEMENT_OUT')
             THEN (it.quantity_after - it.quantity_befor) END), 0) AS entrees,
           COALESCE(SUM(CASE
             WHEN (it.quantity_after - it.quantity_befor) < 0
              AND it.mouvement_type NOT IN ('AJUSTEMENT_IN','AJUSTEMENT_OUT')
             THEN ABS(it.quantity_after - it.quantity_befor) END), 0) AS sorties,
           COALESCE(SUM(CASE
             WHEN it.mouvement_type IN ('AJUSTEMENT_IN','AJUSTEMENT_OUT')
             THEN (it.quantity_after - it.quantity_befor) END), 0) AS ajustements
    FROM inventory_transaction it
    JOIN scope sc ON sc.produit_id = it.produit_id AND sc.storage_id = it.storage_id
    WHERE it.mouvement_type <> 'INVENTAIRE'
      AND it.created_at >  p_date_debut
      AND it.created_at <= p_date_fin
    GROUP BY it.produit_id, it.storage_id
  )
  SELECT sc.produit_id, sc.storage_id,
         COALESCE(vd.qty_debut,           0),
         COALESCE(vd.prix_achat_debut,    0),
         COALESCE(vd.valeur_achat_debut,  0),
         COALESCE(vd.valeur_vente_debut,  0),
         COALESCE(m.entrees,              0),
         COALESCE(m.sorties,              0),
         COALESCE(m.ajustements,          0),
         COALESCE(vf.qty_fin,             0),
         COALESCE(vf.prix_achat_fin,      0),
         COALESCE(vf.valeur_achat_fin,    0),
         COALESCE(vf.valeur_vente_fin,    0)
  FROM scope sc
  LEFT JOIN val_debut vd ON vd.produit_id = sc.produit_id AND vd.storage_id = sc.storage_id
  LEFT JOIN val_fin   vf ON vf.produit_id = sc.produit_id AND vf.storage_id = sc.storage_id
  LEFT JOIN mvt       m  ON m.produit_id  = sc.produit_id AND m.storage_id  = sc.storage_id
  WHERE COALESCE(vd.qty_debut, 0) > 0
     OR COALESCE(vf.qty_fin,   0) > 0
     OR COALESCE(m.entrees,    0) > 0
     OR COALESCE(m.sorties,    0) > 0;
$$;
