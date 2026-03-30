-- FMD (Falsified Medicines Directive) — Stockage du numéro de série (AI 21 du DataMatrix GS1)
-- Permet la traçabilité unitaire et la détection des boîtes contrefaites (numéro en double)

ALTER TABLE lot
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(50) NULL;

COMMENT ON COLUMN lot.serial_number IS 'Numéro de série FMD (AI 21 GS1 DataMatrix). NULL = non présent ou scan 1D.';

CREATE UNIQUE INDEX IF NOT EXISTS lot_serial_number_produit_idx
    ON lot (serial_number, produit_id)
    WHERE serial_number IS NOT NULL;
