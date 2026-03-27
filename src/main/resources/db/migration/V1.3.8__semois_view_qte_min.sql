ALTER TABLE suggestion_line
  ADD COLUMN IF NOT EXISTS quantite_modifiee_manuel boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN suggestion_line.quantite_modifiee_manuel IS
    'true = quantité modifiée manuellement par le pharmacien → le batch ne l écrase pas';

ALTER TABLE semois_configuration
  ADD COLUMN IF NOT EXISTS exclusion_date         TIMESTAMP       DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS exclusion_duree_jours  INTEGER         DEFAULT 30,
  ADD COLUMN IF NOT EXISTS exclusion_motif        VARCHAR(255)    DEFAULT NULL;

COMMENT ON COLUMN semois_configuration.exclusion_date IS
    'Date de début de l exclusion temporaire (NULL = pas d exclusion active)';
COMMENT ON COLUMN semois_configuration.exclusion_duree_jours IS
    'Durée de l exclusion en jours (défaut 30). Réintégration auto après exclusion_date + duree.';
COMMENT ON COLUMN semois_configuration.exclusion_motif IS
    'Motif libre saisi par le pharmacien (surstock promo, rupture fournisseur, etc.)';

-- Index pour les requêtes du batch (filtrage des produits exclus)
CREATE INDEX IF NOT EXISTS idx_semois_config_exclusion
  ON semois_configuration (exclusion_date)
  WHERE exclusion_date IS NOT NULL;

ALTER TABLE semois_configuration
  ADD COLUMN IF NOT EXISTS exclusion_date         TIMESTAMP       DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS exclusion_duree_jours  INTEGER         DEFAULT 30,
  ADD COLUMN IF NOT EXISTS exclusion_motif        VARCHAR(255)    DEFAULT NULL;

COMMENT ON COLUMN semois_configuration.exclusion_date IS
    'Date de début de l exclusion temporaire (NULL = pas d exclusion active)';
COMMENT ON COLUMN semois_configuration.exclusion_duree_jours IS
    'Durée de l exclusion en jours (défaut 30). Réintégration auto après exclusion_date + duree.';
COMMENT ON COLUMN semois_configuration.exclusion_motif IS
    'Motif libre saisi par le pharmacien (surstock promo, rupture fournisseur, etc.)';

-- Index pour les requêtes du batch (filtrage des produits exclus)
CREATE INDEX IF NOT EXISTS idx_semois_config_exclusion
  ON semois_configuration (exclusion_date)
  WHERE exclusion_date IS NOT NULL;

-- Mise à jour de la vue v_semois_suggestion pour exclure les produits temporairement exclus
CREATE OR REPLACE VIEW v_semois_suggestion AS
SELECT
  p.id                                                                           AS produit_id,
  p.libelle,
  fp.code_cip,
  fp.fournisseur_id,
  f.libelle                                                                      AS fournisseur_libelle,
  p.classe_criticite,
  scc.coefficient_securite,
  COALESCE(sc.delai_livraison_jours,
           f.delai_livraison_jours,
           gf.delai_livraison_jours,
           7)                                                                    AS delai_livraison_jours,
  COALESCE(sc.vmm_calcule, 0)                                                   AS vmm,
  COALESCE(sc.marge_securite, 0)                                                AS marge_securite,
  COALESCE(sc.stock_objectif_calcule, 0)                                        AS stock_objectif,
  COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)                                    AS stock_actuel,
  GREATEST(
    0,
    GREATEST(
      COALESCE(sc.stock_objectif_calcule, 0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0),
      COALESCE(sc.marge_securite,          0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
    )
  )                                                                              AS quantite_a_commander,
  sc.date_dernier_calcul,
  -- Informations d'exclusion (pour l'UI analytics)
  sc.exclusion_date,
  sc.exclusion_duree_jours,
  sc.exclusion_motif
FROM produit p
       LEFT JOIN fournisseur_produit fp   ON fp.id = p.fournisseur_produit_principal_id
       LEFT JOIN fournisseur f            ON f.id  = fp.fournisseur_id
       LEFT JOIN groupe_fournisseur gf    ON gf.id = f.groupe_pournisseur_id
       LEFT JOIN semois_configuration sc  ON sc.produit_id = p.id
       LEFT JOIN semois_classe_config scc ON scc.classe_criticite = p.classe_criticite
       LEFT JOIN stock_produit sp         ON sp.produit_id = p.id
WHERE p.status::text       = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  -- Exclure les produits sans VMM calculé
  AND COALESCE(sc.vmm_calcule, 0) > 0
  -- Exclure les produits temporairement exclus (exclusion encore active)
  AND (
      sc.exclusion_date IS NULL
      OR NOW() > sc.exclusion_date + (COALESCE(sc.exclusion_duree_jours, 30) || ' days')::INTERVAL
  )
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
  f.delai_livraison_jours, gf.delai_livraison_jours,
  p.classe_criticite, scc.coefficient_securite,
  sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
  sc.stock_objectif_calcule, sc.date_dernier_calcul,
  sc.exclusion_date, sc.exclusion_duree_jours, sc.exclusion_motif;

COMMENT ON VIEW v_semois_suggestion IS
    'Vue SEMOIS v3 — exclut les produits sans VMM ET les produits temporairement exclus.
     Colonnes d exclusion exposées pour l onglet Analyse des stocks.';

-- Colisage fournisseur : quantité par colis et quantité minimale de commande
-- Ces champs permettent au batch SEMOIS d'arrondir les quantités suggérées
-- au multiple supérieur du conditionnement du fournisseur.

ALTER TABLE fournisseur_produit
  ADD COLUMN IF NOT EXISTS qte_colis              INTEGER DEFAULT 1   CHECK (qte_colis >= 1),
  ADD COLUMN IF NOT EXISTS qte_minimale_commande  INTEGER DEFAULT 0   CHECK (qte_minimale_commande >= 0);

COMMENT ON COLUMN fournisseur_produit.qte_colis
    IS 'Nombre d''unités par colis (conditionnement fournisseur). La quantité commandée sera arrondie au multiple supérieur. Default=1 (pas de contrainte).';

COMMENT ON COLUMN fournisseur_produit.qte_minimale_commande
    IS 'Quantité minimale acceptée par le fournisseur pour ce produit (en unités). 0 = pas de minimum.';

ALTER TABLE suggestion
DROP CONSTRAINT suggestion_type_suggession_check;

ALTER TABLE suggestion
  ADD CONSTRAINT suggestion_type_suggession_check
    CHECK (
      type_suggession IN (
                       'MANUELLE',
                       'SEMOIS',
                       'CLOSED',
                       'AUTO'
        )
      );
