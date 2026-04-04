-- Add delai_reglement (payment delay in days) to tiers_payant and groupe_tiers_payant
ALTER TABLE tiers_payant
    ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;

ALTER TABLE groupe_tiers_payant
    ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;
