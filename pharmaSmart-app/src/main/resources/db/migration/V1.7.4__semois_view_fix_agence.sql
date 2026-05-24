-- Fix régression SEMOIS post-intégration fournisseur/agence

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
           pf.delai_livraison_jours,
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
  sc.exclusion_date,
  sc.exclusion_duree_jours,
  sc.exclusion_motif
FROM produit p
       LEFT JOIN fournisseur_produit fp   ON fp.id  = p.fournisseur_produit_principal_id
       LEFT JOIN fournisseur f            ON f.id   = fp.fournisseur_id
       LEFT JOIN fournisseur pf           ON pf.id  = f.parent_id
       LEFT JOIN semois_configuration sc  ON sc.produit_id = p.id
       LEFT JOIN semois_classe_config scc ON scc.classe_criticite = p.classe_criticite
       LEFT JOIN stock_produit sp         ON sp.produit_id = p.id
WHERE p.status::text       = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  AND COALESCE(sc.vmm_calcule, 0) > 0
  AND (
      sc.exclusion_date IS NULL
      OR NOW() > sc.exclusion_date + (COALESCE(sc.exclusion_duree_jours, 30) || ' days')::INTERVAL
  )
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
  f.delai_livraison_jours, pf.delai_livraison_jours,
  p.classe_criticite, scc.coefficient_securite,
  sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
  sc.stock_objectif_calcule, sc.date_dernier_calcul,
  sc.exclusion_date, sc.exclusion_duree_jours, sc.exclusion_motif;

COMMENT ON VIEW v_semois_suggestion IS
    'Vue SEMOIS v4 — fix post-intégration agence : GROUP BY nettoyé, filtre fournisseurId géré côté Java.';
