-- Paramètre : mode d'acceptation des substitutions PharmaML EP
-- AUTO   : acceptation implicite + mémorisation dans la table substitut
-- MANUEL : validation manuelle par le pharmacien (indicateur visuel si paire connue)
INSERT INTO warehouse.app_configuration (name, description, value, created, updated)
VALUES (
    'APP_ACCEPTATION_SUBSTITUTION',
    'Mode d''acceptation des substitutions PharmaML (EP) : AUTO = acceptation implicite et mémorisation, MANUEL = validation pharmacien',
    'MANUEL',
    NOW(),
    NOW()
)
ON CONFLICT (name) DO NOTHING;
