ALTER TABLE
  ligne_reassort
  ADD COLUMN IF NOT EXISTS stock_src_produit_id integer;

ALTER TABLE ligne_reassort
  ADD CONSTRAINT fkfv43qjphxfshassymjb95d551
    FOREIGN KEY (stock_src_produit_id) REFERENCES stock_produit (id);


DELETE FROM ajustement;
DELETE FROM ajust;
ALTER TABLE
  ajust
  DROP COLUMN IF EXISTS storage_id;
ALTER TABLE ajustement
  DROP COLUMN IF EXISTS produit_id;

ALTER TABLE
  ajustement
  ADD COLUMN IF NOT EXISTS stock_produit_id integer;

ALTER TABLE ajustement
  ADD CONSTRAINT fkajfv43qjphxfshassymjb95d5
    FOREIGN KEY (stock_produit_id) REFERENCES stock_produit (id);

