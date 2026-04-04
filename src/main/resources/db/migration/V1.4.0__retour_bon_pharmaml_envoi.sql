ALTER TABLE retour_bon
    ADD COLUMN IF NOT EXISTS pharmaml_envoi_id INTEGER;

ALTER TABLE retour_bon
    ADD CONSTRAINT fk_retour_bon_pharmaml_envoi
        FOREIGN KEY (pharmaml_envoi_id) REFERENCES pharmaml_envoi (id);

CREATE INDEX IF NOT EXISTS idx_retour_bon_pharmaml_envoi ON retour_bon (pharmaml_envoi_id);

ALTER TABLE tiers_payant
  ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;

ALTER TABLE groupe_tiers_payant
  ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;
ALTER TABLE groupe_tiers_payant
  ADD COLUMN IF NOT EXISTS email VARCHAR(100);
