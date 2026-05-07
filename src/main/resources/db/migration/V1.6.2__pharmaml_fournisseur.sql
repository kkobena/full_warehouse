ALTER TABLE fournisseur
    ADD COLUMN IF NOT EXISTS url_pharma_ml          VARCHAR(150),
    ADD COLUMN IF NOT EXISTS code_office_pharma_ml  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS code_recepteur_pharma_ml VARCHAR(50),
    ADD COLUMN IF NOT EXISTS id_recepteur_pharma_ml VARCHAR(50);
