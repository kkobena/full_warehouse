-- Les exports de données se filtrent comme les écrans d'où elles viennent : famille, laboratoire, fournisseur,
-- type de vente, vendeur… Les filtres retenus sont gardés avec le fichier (pour dire sur quoi il portait) et avec
-- le modèle (pour les rejouer). Rangés en texte : FAMILLE=12,45;NATURE_VENTE=COMPTANT.

ALTER TABLE export_fichier
  ADD COLUMN IF NOT EXISTS filtres VARCHAR(1000);

ALTER TABLE export_modele
  ADD COLUMN IF NOT EXISTS filtres VARCHAR(1000);
