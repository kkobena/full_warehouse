INSERT INTO app_configuration (name, value, other_value, description, value_type)
VALUES ('APP_SCANNER_MODE', 'TIMING', NULL,
        'Mode de detection scanner: TIMING (vitesse frappe) ou PREFIX_SUFFIX (prefixe/suffixe configure sur le scanner)',
        'STRING');

ALTER TABLE sales
  ADD COLUMN ht_amount integer DEFAULT 0;
ALTER TABLE sales
  ADD COLUMN net_amount integer DEFAULT 0;
ALTER TABLE sales
  ADD COLUMN tax_amount integer DEFAULT 0;
ALTER TABLE sales
  ADD COLUMN cost_amount integer DEFAULT 0;

