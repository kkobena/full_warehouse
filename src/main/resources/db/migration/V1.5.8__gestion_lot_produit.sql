-- Ajout du flag gestion_lot sur la table produit
-- Permet d'activer/désactiver le contrôle de lot par produit (indépendant du param global APP_GESTION_LOT)
ALTER TABLE produit
    ADD COLUMN IF NOT EXISTS gestion_lot BOOLEAN NOT NULL DEFAULT TRUE;
