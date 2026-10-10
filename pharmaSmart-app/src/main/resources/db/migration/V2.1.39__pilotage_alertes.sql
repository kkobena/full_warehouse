-- Seuils des alertes du pilotage (tableau de bord) ; l'alerte « remises anormales » garde APP_PILOTAGE_ALERTE_REMISE_VENDEUR.
INSERT INTO app_configuration (name, value, description, created, updated, value_type)
VALUES ('APP_PILOTAGE_ALERTE_CHUTE_ACTIVITE', '90',
        'Alerte « chute d''activité » : CA des 7 derniers jours sous ce pourcentage des mêmes jours de l''année précédente (défaut : 90).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_FAMILLE_RECUL', '15',
        'Alerte « famille en recul » : baisse en % du CA d''une famille sur le mois en cours, à date (défaut : 15).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_EROSION_MARGE', '1',
        'Alerte « marge qui s''érode » : baisse en points du taux de marge du mois en cours, à date (défaut : 1).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_OBJECTIF_MENACE', '95',
        'Alerte « objectif menacé » : projection de fin de mois sous ce pourcentage de l''objectif (défaut : 95).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_RATIO_ACHATS', '0.8',
        'Alerte « achats qui dérapent » : ratio ventes / achats des 30 derniers jours sous cette valeur (défaut : 0.8).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_DSO_ORGANISME', '90',
        'Alerte « créances » : organisme dont l''encours dépasse ce nombre de jours de chiffre (défaut : 90).',
        NOW(), NOW(), 'NUMBER')
ON CONFLICT (name) DO NOTHING;
