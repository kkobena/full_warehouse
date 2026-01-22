ALTER TABLE facture_tiers_payant
  ADD COLUMN montant_ttc numeric(15,2) NOT NULL ;
ALTER TABLE facture_tiers_payant
  ADD COLUMN montant_tva numeric(15,2) NOT NULL ;
ALTER TABLE facture_tiers_payant
  ADD COLUMN montant_net numeric(15,2) NOT NULL ;
ALTER TABLE facture_tiers_payant
  ADD COLUMN montant_ht  numeric(15,2) NOT NULL ;
