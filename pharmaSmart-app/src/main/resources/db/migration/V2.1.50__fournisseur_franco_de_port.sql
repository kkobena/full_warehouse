-- Franco de port : le montant au-delà duquel le grossiste ne facture plus la livraison.



ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS franco_de_port bigint;

COMMENT ON COLUMN fournisseur.franco_de_port IS
  'Montant de commande à partir duquel les frais de port ne sont plus facturés. NULL = pas de franco négocié, ou hérité du grossiste parent.';
