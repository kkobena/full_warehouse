-- V2.1.53 — Le réassort devient propre à chaque magasin
--


ALTER TABLE semois_configuration
  ADD COLUMN magasin_id INTEGER;

-- L'existant appartient à l'officine : la vue de réassort ne lisait que son stock, ces calculs sont
-- donc les siens et ceux de personne d'autre.
UPDATE semois_configuration
   SET magasin_id = (SELECT m.id FROM magasin m WHERE m.type_magasin = 'OFFICINE' ORDER BY m.id LIMIT 1)
 WHERE magasin_id IS NULL;

-- Filet : une installation sans officine déclarée rattache à son premier magasin plutôt que de
-- laisser des lignes orphelines qui bloqueraient le NOT NULL.
UPDATE semois_configuration
   SET magasin_id = (SELECT m.id FROM magasin m ORDER BY m.id LIMIT 1)
 WHERE magasin_id IS NULL;

DELETE FROM semois_configuration WHERE magasin_id IS NULL;

ALTER TABLE semois_configuration
  ALTER COLUMN magasin_id SET NOT NULL;

ALTER TABLE semois_configuration
  ADD CONSTRAINT fk_semois_config_magasin
    FOREIGN KEY (magasin_id) REFERENCES magasin (id);

-- La clé d'unicité porte désormais sur le couple.
ALTER TABLE semois_configuration
  DROP CONSTRAINT IF EXISTS uq_semois_config_produit;

ALTER TABLE semois_configuration
  ADD CONSTRAINT uq_semois_config_produit_magasin UNIQUE (produit_id, magasin_id);

CREATE INDEX IF NOT EXISTS idx_semois_config_magasin ON semois_configuration (magasin_id);

-- Duplication vers les autres magasins commandeurs : les paramètres, jamais les calculs.
INSERT INTO semois_configuration (
    produit_id, magasin_id, classe_criticite,
    delai_livraison_jours, frequence_commande_jours, marge_securite, limite_peremption,
    facteur_saisonnier_manuel, exclusion_date, exclusion_duree_jours, exclusion_motif,
    created_at, updated_at
)
SELECT sc.produit_id, m.id, sc.classe_criticite,
       sc.delai_livraison_jours, sc.frequence_commande_jours, sc.marge_securite, sc.limite_peremption,
       sc.facteur_saisonnier_manuel, sc.exclusion_date, sc.exclusion_duree_jours, sc.exclusion_motif,
       now(), now()
  FROM semois_configuration sc
 CROSS JOIN magasin m
 WHERE m.commande_en_propre
   AND m.id <> sc.magasin_id
ON CONFLICT (produit_id, magasin_id) DO NOTHING;

COMMENT ON COLUMN semois_configuration.magasin_id IS
  'Magasin dont ce reassort decrit les parametres. Un magasin qui ne commande pas en propre n''en a pas.';
