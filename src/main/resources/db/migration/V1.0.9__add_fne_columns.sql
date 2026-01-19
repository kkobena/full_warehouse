ALTER TABLE facture_tiers_payant
  ADD COLUMN fne_response jsonb;
ALTER TABLE facture_tiers_payant
  ADD COLUMN repartitions jsonb;


ALTER TABLE third_party_sale_line
  ADD COLUMN repartitions jsonb ;

ALTER TABLE magasin
  ADD COLUMN fne_point_of_sale varchar(255);
ALTER TABLE magasin
  ADD COLUMN fne_secret_key varchar(255);
