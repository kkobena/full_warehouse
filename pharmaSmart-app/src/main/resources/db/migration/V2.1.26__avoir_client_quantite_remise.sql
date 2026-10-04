-- Unités déjà remises au client quand l'avoir est soldé en « Le produit est remis au client » : la remise peut être partielle,
-- et seules les unités jamais remises reviennent en stock à la clôture.
ALTER TABLE avoir_client ADD COLUMN IF NOT EXISTS quantite_remise INTEGER NOT NULL DEFAULT 0;
