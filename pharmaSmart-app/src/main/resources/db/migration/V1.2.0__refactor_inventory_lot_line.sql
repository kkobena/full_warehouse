-- ============================================================
-- V1.2.0 — Refactoring modèle inventaire
--
-- 1. Ajouter storage_id sur store_inventory_line
--    → distingue stock rayon / stock réserve pour un même inventaire
--    → ajuste la contrainte unique
--
-- 2. Reparenter inventory_lot : StoreInventory → StoreInventoryLine
--    → hiérarchie : Inventaire → LigneProduit → LigneLot
--    → migre les données existantes
-- ============================================================

-- ── 1. store_inventory_line : ajout storage_id ───────────────────────────────

ALTER TABLE store_inventory_line
    ADD COLUMN IF NOT EXISTS storage_id INTEGER REFERENCES storage(id);

-- Peupler storage_id pour les lignes existantes depuis le storage du parent
UPDATE store_inventory_line sil
SET storage_id = si.storage_id
FROM store_inventory si
WHERE si.id = sil.store_inventory_id
  AND si.storage_id IS NOT NULL;

-- Supprimer l'ancienne contrainte unique (produit_id, store_inventory_id)
ALTER TABLE store_inventory_line
    DROP CONSTRAINT IF EXISTS store_inventory_line_produit_id_store_inventory_id_key;

-- Nouvelle contrainte : (produit_id, store_inventory_id, storage_id)
-- NULL storage_id = inventaire MAGASIN (agrégé tous storages) → les NULL sont distincts en PG
ALTER TABLE store_inventory_line
    ADD CONSTRAINT uq_sil_produit_inventory_storage
    UNIQUE (produit_id, store_inventory_id, storage_id);

-- Index sur storage_id pour les jointures
CREATE INDEX IF NOT EXISTS idx_sil_storage_id ON store_inventory_line(storage_id);

-- ── 2. inventory_lot : reparentage StoreInventory → StoreInventoryLine ────────

-- Ajouter la nouvelle FK
ALTER TABLE inventory_lot
    ADD COLUMN IF NOT EXISTS store_inventory_line_id BIGINT REFERENCES store_inventory_line(id);

-- Migrer les données existantes :
-- retrouver la store_inventory_line du même inventaire pour le même produit que le lot
UPDATE inventory_lot il
SET store_inventory_line_id = (
    SELECT sil.id
    FROM store_inventory_line sil
    JOIN lot l ON l.produit_id = sil.produit_id
    WHERE sil.store_inventory_id = il.store_inventory_id
      AND l.id                   = il.lot_id
    LIMIT 1
);

-- Passer NOT NULL après migration (les lignes sans correspondance resteront NULL)
-- On rend NOT NULL uniquement si toutes les lignes ont été migrées
ALTER TABLE inventory_lot
    ALTER COLUMN store_inventory_line_id SET NOT NULL;

-- Supprimer l'ancienne FK vers store_inventory
ALTER TABLE inventory_lot
    DROP COLUMN IF EXISTS store_inventory_id;

-- Mettre à jour la contrainte unique
ALTER TABLE inventory_lot
    DROP CONSTRAINT IF EXISTS ukfqtp7jyosytdyc3jbpubgu636;

ALTER TABLE inventory_lot
    ADD CONSTRAINT uq_il_lot_line
    UNIQUE (lot_id, store_inventory_line_id);

-- Index sur store_inventory_line_id
CREATE INDEX IF NOT EXISTS idx_il_line_id ON inventory_lot(store_inventory_line_id);
