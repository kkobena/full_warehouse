
ALTER TABLE classification_config
    DROP COLUMN IF EXISTS nb_mois_analyse,
    DROP COLUMN IF EXISTS poids_frequence;
