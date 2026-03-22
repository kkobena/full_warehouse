
INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_SEUIL_VARIATION_PRIX', '20',
        'Seuil de variation de prix d''achat en (%) déclenchant une alerte à la validation de la commande',
        'NUMBER') ON CONFLICT (name) DO NOTHING;

