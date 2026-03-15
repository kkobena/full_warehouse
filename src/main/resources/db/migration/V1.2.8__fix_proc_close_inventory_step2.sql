-- ══════════════════════════════════════════════════════════════════════════
-- MIGRATION V1.2.8 — Correction proc_close_inventory_v2 : STEP 2 restreint
-- ══════════════════════════════════════════════════════════════════════════
--
-- Problème corrigé :
--   STEP 2 remettait la réserve (SAFETY_STOCK) à 0 pour TOUS les types
--   d'inventaire sauf STORAGE. Incorrect pour les inventaires partiels
--   et thématiques (RAYON, FAMILLY, PERIME, ALERTE_PEREMPTION, VENDU,
--   INVENDU, SOUS_SEUIL, EN_RUPTURE, ABC, SELECTION_PRODUIT) car la
--   réserve n'a pas été physiquement comptée dans ces cas.
--
-- Solution (Option A — comptage consolidé) :
--   STEP 2 s'exécute uniquement pour inventory_category = 'MAGASIN'.
--   Cohérence : pour un inventaire MAGASIN, le pharmacien saisit le stock
--   total (rayon + réserve consolidés) sur une seule ligne (storage PRINCIPAL).
--   Après clôture, STEP 1 met à jour le PRINCIPAL et STEP 2 remet la
--   réserve à 0 — le total physique est conservé sur le PRINCIPAL.
--   Pour tous les autres types, la réserve n'est plus touchée.
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
  IN p_store_inventory_id BIGINT,
  IN p_gestion_lot        BOOLEAN,
  INOUT p_nombre_ligne    INT
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

  -- STEP 2 : Réserve → 0 (MAGASIN uniquement)
  --   Pour un inventaire MAGASIN, le pharmacien saisit le stock total consolidé
  --   (rayon + réserve) sur une seule ligne PRINCIPAL. La réserve est remise à 0
  --   car son contenu est inclus dans le chiffre PRINCIPAL compté.
  --   Pour tous les autres types (RAYON, FAMILLY, thématiques), la réserve n'est
  --   pas concernée par l'inventaire et ne doit pas être modifiée.
  IF v_inventory_category = 'MAGASIN' THEN
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
         COALESCE(sil.quantity_on_hand, sil.quantity_init) - sil.quantity_init,
         COALESCE(sil.quantity_on_hand, sil.quantity_init),
         sil.quantity_init,
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
