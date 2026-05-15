ALTER TABLE storage DROP CONSTRAINT IF EXISTS storage_storage_type_check;
ALTER TABLE storage ADD CONSTRAINT storage_storage_type_check
  CHECK (storage_type IN ('PRINCIPAL','SAFETY_STOCK', 'TOXIQUE', 'QUARANTAINE'));
INSERT INTO storage (name, storage_type, magasin_id)
SELECT 'Toxiques', 'TOXIQUE', id
FROM magasin
ON CONFLICT (storage_type, magasin_id) DO NOTHING;

INSERT INTO storage (name, storage_type, magasin_id)
SELECT 'Quarantaine', 'QUARANTAINE', id
FROM magasin
ON CONFLICT (storage_type, magasin_id) DO NOTHING;
