
ALTER TABLE produit
  ADD COLUMN IF NOT EXISTS thermosensible BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN produit.thermosensible IS
  'Indique si le produit est thermosensible et doit être conservé entre 2°C et 8°C (chaîne du froid). '
  'Utilisé en réception pour afficher le badge "2–8°C" dans le mode séquentiel';
