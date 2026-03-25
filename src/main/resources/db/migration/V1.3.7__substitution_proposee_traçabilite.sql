ALTER TABLE substitution_proposee
  ADD COLUMN IF NOT EXISTS code_reponse      VARCHAR(10),
  ADD COLUMN IF NOT EXISTS additif           VARCHAR(500),
  ADD COLUMN IF NOT EXISTS type_remplacement VARCHAR(3);

