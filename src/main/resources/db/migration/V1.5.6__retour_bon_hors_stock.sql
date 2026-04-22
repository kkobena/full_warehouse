ALTER TABLE retour_bon ADD COLUMN IF NOT EXISTS hors_stock boolean NOT NULL DEFAULT false;
CREATE INDEX idx_receipt_type
  ON commande (receipt_type);
