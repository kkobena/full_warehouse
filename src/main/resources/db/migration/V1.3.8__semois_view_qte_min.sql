-- ============================================================
-- V1.3.8 — Correction vue v_semois_suggestion
--
-- Problème identifié : la vue retournait quantite_a_commander = 0 pour
-- des produits dont le batch n'a pas encore été exécuté (stock_objectif_calcule
-- = 0 ou NULL) mais qui sont en rupture (stock_actuel < marge_securite).
--
-- Règle métier (alignée Winpharma / Pharmagest / LGPI) :
--   1. Les produits sans VMM calculé (vmm_calcule IS NULL ou = 0) sont exclus —
--      aucune suggestion n'est possible sans historique de ventes.
--   2. La quantité à commander = MAX(besoin_objectif, besoin_securite_mini)
--      pour ne jamais suggérer 0 à un produit en rupture.
--      - besoin_objectif   = stock_objectif - stock_actuel
--      - besoin_securite   = marge_securite - stock_actuel (si en rupture)
--   3. Le résultat final reste >= 0 (GREATEST(0,...)).
-- ============================================================

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

    -- Quantité à commander :
    --   MAX(0,
    --     MAX( objectif - actuel,            ← besoin pour atteindre l'objectif
    --          marge_securite - actuel ))    ← besoin minimal pour sortir de rupture
    -- Les deux termes peuvent être négatifs → GREATEST(0,...) garantit >= 0.
    GREATEST(
        0,
        GREATEST(
            COALESCE(sc.stock_objectif_calcule, 0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0),
            COALESCE(sc.marge_securite,          0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
        )
    )                                                                              AS quantite_a_commander,

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
  -- Exclure les produits sans VMM calculé (aligné avec Winpharma / Pharmagest / LGPI)
  -- Sans historique de ventes, aucune suggestion SEMOIS n'est possible.
  AND COALESCE(sc.vmm_calcule, 0) > 0
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
         f.delai_livraison_jours, gf.delai_livraison_jours,
         p.classe_criticite, scc.coefficient_securite,
         sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
         sc.stock_objectif_calcule, sc.date_dernier_calcul;

COMMENT ON VIEW v_semois_suggestion IS
    'Vue SEMOIS v2 — quantite_a_commander = MAX(besoin_objectif, besoin_securite_mini).
     Produits sans VMM exclus. Stock_actuel et quantite_a_commander en temps réel.
     vmm/marge/stock_objectif pré-calculés par le batch SEMOIS.';

