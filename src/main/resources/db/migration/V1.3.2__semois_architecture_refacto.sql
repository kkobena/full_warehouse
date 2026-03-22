-- ============================================================
-- V1.3.2 — Nouvelle architecture SEMOIS
-- Remplace mv_semois_suggestion (matérialisée) par v_semois_suggestion (vue ordinaire).
-- La vue lit les valeurs pré-calculées de semois_configuration et le stock en temps réel.
-- La classe de criticité vient désormais de produit.classe_criticite (auto-classifié).
-- ============================================================

-- 1. Supprimer l'ancienne vue matérialisée (peut avoir été déjà supprimée par V1.3.1)
DROP MATERIALIZED VIEW IF EXISTS mv_semois_suggestion CASCADE;

-- 2. Ajouter marge_securite dans semois_configuration (stocke la marge pré-calculée)
ALTER TABLE semois_configuration
    ADD COLUMN IF NOT EXISTS marge_securite INTEGER;

-- 3. Créer la vue ordinaire v_semois_suggestion
--    - stock_actuel et quantite_a_commander : temps réel
--    - vmm, marge_securite, stock_objectif   : pré-calculés par le batch
--    - classe_criticite                       : depuis produit.classe_criticite
CREATE OR REPLACE VIEW v_semois_suggestion AS
SELECT
    p.id                                                                         AS produit_id,
    p.libelle,
    fp.code_cip,
    fp.fournisseur_id,
    f.libelle                                                                    AS fournisseur_libelle,
    p.classe_criticite,
    scc.coefficient_securite,
    COALESCE(sc.delai_livraison_jours,
             f.delai_livraison_jours,
             gf.delai_livraison_jours,
             7)                                                                  AS delai_livraison_jours,
    COALESCE(sc.vmm_calcule, 0)                                                 AS vmm,
    COALESCE(sc.marge_securite, 0)                                              AS marge_securite,
    COALESCE(sc.stock_objectif_calcule, 0)                                      AS stock_objectif,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)                                  AS stock_actuel,
    GREATEST(0, COALESCE(sc.stock_objectif_calcule, 0)
                - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0))                   AS quantite_a_commander,
    sc.date_dernier_calcul
FROM produit p
         LEFT JOIN fournisseur_produit fp  ON fp.id = p.fournisseur_produit_principal_id
         LEFT JOIN fournisseur f           ON f.id  = fp.fournisseur_id
         LEFT JOIN groupe_fournisseur gf   ON gf.id = f.groupe_pournisseur_id
         LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
         LEFT JOIN semois_classe_config scc ON scc.classe_criticite = p.classe_criticite
         LEFT JOIN stock_produit sp        ON sp.produit_id = p.id
WHERE p.status::text      = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
         f.delai_livraison_jours, gf.delai_livraison_jours,
         p.classe_criticite, scc.coefficient_securite,
         sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
         sc.stock_objectif_calcule, sc.date_dernier_calcul;

COMMENT ON VIEW v_semois_suggestion IS
    'Vue SEMOIS en temps réel — stock_actuel et quantite_a_commander calculés à la volée, vmm/marge/stock_objectif pré-calculés par le batch. Classe de criticité héritée de produit.classe_criticite (auto-classifié).';

-- 4. S''assurer que APP_LAST_DAY_SEMOIS_CALCULATION existe (idempotent)
INSERT INTO app_configuration (name, value, description, value_type, updated)
VALUES ('APP_LAST_DAY_SEMOIS_CALCULATION', '', 'Date du dernier recalcul SEMOIS (YYYY-MM-DD)', 'STRING', NOW())
ON CONFLICT (name) DO NOTHING;
