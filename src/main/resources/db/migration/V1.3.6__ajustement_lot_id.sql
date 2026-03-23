
ALTER TABLE ajustement
    ADD COLUMN IF NOT EXISTS lot_id INTEGER REFERENCES lot(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ajustement_lot_id ON ajustement(lot_id);
