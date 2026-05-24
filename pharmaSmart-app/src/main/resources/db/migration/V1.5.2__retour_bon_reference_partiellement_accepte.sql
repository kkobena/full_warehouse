-- Ajout colonne reference (ex: RET-2026-0042) sur les bons de retour fournisseur
ALTER TABLE retour_bon
    ADD COLUMN IF NOT EXISTS reference VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_retour_bon_reference ON retour_bon (reference);

ALTER TABLE retour_bon
DROP CONSTRAINT retour_bon_statut_check;
ALTER TABLE retour_bon
ALTER COLUMN statut TYPE VARCHAR(20);

ALTER TABLE retour_bon
  ADD CONSTRAINT retour_bon_statut_check
    CHECK (
      statut IN (
                 'PROCESSING',
                 'VALIDATED',
                 'CLOSED',
                 'PARTIALLY_ACCEPTED'
        )
      );

-- Paramètre configurable : délai max (jours) avant avertissement de retour tardif (défaut 365)
INSERT INTO app_configuration (name, description, value, created, updated,value_type)
VALUES (
  'APP_DELAI_RETOUR_FOURNISSEUR',
  'Délai maximum (en jours) entre la réception d''un bon et le retour fournisseur avant avertissement',
  '365',
  NOW(),
  NOW(),'NUMBER'
) ON CONFLICT (name) DO NOTHING;

