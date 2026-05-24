
UPDATE store_inventory_line sil
SET storage_id = si.storage_id
FROM store_inventory si
WHERE si.id = sil.store_inventory_id
  AND sil.storage_id IS NULL
  AND si.storage_id IS NOT NULL;

-- Cas 2 : le parent n'a pas de storage_id non plus
-- → utiliser le storage PRINCIPAL du magasin de l'utilisateur
UPDATE store_inventory_line sil
SET storage_id = (
    SELECT s.id
    FROM storage s
    JOIN app_user u ON u.magasin_id = s.magasin_id
    JOIN store_inventory si ON si.user_id = u.id
    WHERE si.id = sil.store_inventory_id
      AND s.storage_type = 'PRINCIPAL'
    LIMIT 1
)
WHERE sil.storage_id IS NULL;

-- ── 2. Contrainte NOT NULL ─────────────────────────────────────────────────────
ALTER TABLE store_inventory_line
    ALTER COLUMN storage_id SET NOT NULL;

-- ── 3. proc_close_inventory_v2 ─────────────────────────────────────────────────
CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
    IN  p_store_inventory_id BIGINT,
    IN  p_gestion_lot        BOOLEAN,
    INOUT p_nombre_ligne      INT
)
LANGUAGE plpgsql AS $$
DECLARE
    v_user_id            INT;
    v_magasin_id         INT;
    v_inventory_category TEXT;
BEGIN
    -- Récupérer user, magasin et catégorie d'inventaire
    SELECT s.user_id, u.magasin_id, s.inventory_category
    INTO v_user_id, v_magasin_id, v_inventory_category
    FROM store_inventory s
    JOIN app_user u ON s.user_id = u.id
    WHERE s.id = p_store_inventory_id;

    -- ── STEP 1 : Mettre à jour le stock du storage inventorié ──────────────
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

    -- ── STEP 2 : Réserve → 0 (tous types SAUF STORAGE) ────────────────────
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

    -- ── STEP 3 : Lots (si gestion lot activée) ────────────────────────────
    IF p_gestion_lot THEN
        UPDATE lot l
        SET current_quantity = COALESCE(il.quantity_on_hand, il.quantity_init)
        FROM inventory_lot il
        JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
        WHERE sil.store_inventory_id = p_store_inventory_id
          AND l.id = il.lot_id;
    END IF;

    -- ── STEP 4 : Transactions d'inventaire (traçabilité) ──────────────────
    INSERT INTO inventory_transaction
        (cost_amount, created_at, entity_id, mouvement_type, quantity,
         quantity_after, quantity_befor, regular_unit_price, magasin_id,
         produit_id, user_id)
    SELECT sil.inventory_value_cost,
           sil.updated_at,
           sil.id,
           'INVENTAIRE',
           sil.quantity_on_hand,
           sil.quantity_on_hand,
           sil.quantity_init,
           sil.last_unit_price,
           v_magasin_id,
           sil.produit_id,
           v_user_id
    FROM store_inventory_line sil
    WHERE sil.store_inventory_id = p_store_inventory_id;

END;
$$;
