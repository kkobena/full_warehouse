ALTER TABLE retour_bon
    ADD COLUMN IF NOT EXISTS pharmaml_envoi_id INTEGER;

ALTER TABLE retour_bon
    ADD CONSTRAINT fk_retour_bon_pharmaml_envoi
        FOREIGN KEY (pharmaml_envoi_id) REFERENCES pharmaml_envoi (id);

CREATE INDEX IF NOT EXISTS idx_retour_bon_pharmaml_envoi ON retour_bon (pharmaml_envoi_id);
