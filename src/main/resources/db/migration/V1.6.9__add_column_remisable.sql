
ALTER TABLE produit
    ADD COLUMN IF NOT EXISTS remisable BOOLEAN  NOT NULL DEFAULT FALSE;
ALTER TABLE produit
  ADD COLUMN IF NOT EXISTS nom_commercial varchar(255)     ;




