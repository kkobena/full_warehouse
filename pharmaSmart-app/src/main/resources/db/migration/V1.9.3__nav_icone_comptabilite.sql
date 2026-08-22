UPDATE nav_item
SET icon = 'pi pi-calculator'
WHERE code = 'comptabilite'
  AND icon = 'pi pi-book';

INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_DEVISE', 'FCFA',
        'Devise affichée à la suite des montants (libellé libre, ex. FCFA,F)', 'STRING')
ON CONFLICT (name) DO NOTHING;
