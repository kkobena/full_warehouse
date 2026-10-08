-- Contrôle d'ordonnance : un paramètre par niveau d'alerte.
-- 1 = l'alerte BLOQUE (modale, motif exigé, droit pr-forcer-alerte-sante ou clé d'un collègue) ;
-- 0 = simple avertissement. Par défaut CI, AD et PE bloquent ; l'APEC (dont les redondances, fréquentes
-- et informatives) n'est qu'un avertissement.
INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_CONTROLE_ORDONNANCE_BLOQUANT_CI', '1', 'Contrôle d''ordonnance : une contre-indication (CI) bloque la vente (1) ou avertit seulement (0)', 'NUMBER'),
       ('APP_CONTROLE_ORDONNANCE_BLOQUANT_AD', '1', 'Contrôle d''ordonnance : une association déconseillée (AD) bloque la vente (1) ou avertit seulement (0)', 'NUMBER'),
       ('APP_CONTROLE_ORDONNANCE_BLOQUANT_PE', '1', 'Contrôle d''ordonnance : une précaution d''emploi (PE) bloque la vente (1) ou avertit seulement (0)', 'NUMBER'),
       ('APP_CONTROLE_ORDONNANCE_BLOQUANT_APEC', '0', 'Contrôle d''ordonnance : une alerte à prendre en compte (APEC, redondances comprises) bloque la vente (1) ou avertit seulement (0)', 'NUMBER')
ON CONFLICT (name) DO NOTHING;
