-- V2.1.56 — La clé d'unicité du journal inclut le stockage
--


ALTER TABLE inventory_transaction
  DROP CONSTRAINT ukapurp4ipjg65w9gpeqb5xmswd;

ALTER TABLE inventory_transaction
  ADD CONSTRAINT uk_inventory_transaction_piece_storage
    UNIQUE (entity_id, produit_id, mouvement_type, storage_id, transaction_date);

COMMENT ON COLUMN inventory_transaction.storage_id IS
  'Emplacement reellement mouvemente. Fait partie de la cle d''unicite : les deux cotes d''un transfert inter-magasins ne different que par lui.';
