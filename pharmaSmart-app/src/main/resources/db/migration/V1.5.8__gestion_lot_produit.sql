-- Ajout du flag gestion_lot sur la table produit
-- Permet d'activer/désactiver le contrôle de lot par produit (indépendant du param global APP_GESTION_LOT)
ALTER TABLE produit
  ADD COLUMN IF NOT EXISTS gestion_lot BOOLEAN NOT NULL DEFAULT TRUE;
drop table IF exists produit_aud;
drop sequence IF exists revinfo_seq;


ALTER TABLE lot DROP CONSTRAINT IF EXISTS lot_statut_check;
ALTER TABLE lot ADD CONSTRAINT lot_statut_check
  CHECK (statut IN ('IN_PROGRESS','AVAILABLE', 'SOLD', 'EXPIRED', 'DESTROYED'));
