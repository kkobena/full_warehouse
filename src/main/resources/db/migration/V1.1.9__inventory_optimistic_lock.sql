-- Ajout du verrou optimiste sur store_inventory
-- Protège contre les fermetures concurrentes (close() race condition)
ALTER TABLE store_inventory
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
