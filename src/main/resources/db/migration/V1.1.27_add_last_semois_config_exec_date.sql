ALTER TABLE app_configuration
  ADD COLUMN IF NOT EXISTS options jsonb;

INSERT INTO app_configuration(name, description, value, other_value,
                              value_type)
values ('APP_LAST_DAY_SEMOIS_CALCULATION',
        'Clé de configuration pour la date du dernier calcul SEMOIS.', '2000-01-01', null, 'DATE')
ON CONFLICT (name) DO NOTHING;
