
-- 1. Ajouter parent_id (self-reference nullable) et odre sur fournisseur
ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS parent_id INTEGER REFERENCES fournisseur(id),
  ADD COLUMN IF NOT EXISTS odre      INTEGER NOT NULL DEFAULT 100,
  ADD COLUMN IF NOT EXISTS email     VARCHAR(255);

-- 2. Rendre groupe_pournisseur_id nullable (transition vers Phase 2)
ALTER TABLE fournisseur
  ALTER COLUMN groupe_pournisseur_id DROP NOT NULL;

-- 3. Copier l'ordre d'affichage depuis groupe_fournisseur vers fournisseur
UPDATE fournisseur f
SET odre = gf.odre
FROM groupe_fournisseur gf
WHERE gf.id = f.groupe_pournisseur_id;

-- 4. Ajouter agence_id sur commande (FK nullable vers le fournisseur-agence)
ALTER TABLE commande
  ADD COLUMN IF NOT EXISTS agence_id INTEGER REFERENCES fournisseur(id);

COMMENT ON COLUMN fournisseur.parent_id IS
  'Référence vers le fournisseur parent. NULL = fournisseur principal (ex-GroupeFournisseur). NOT NULL = agence.';
COMMENT ON COLUMN fournisseur.odre IS
  'Ordre d''affichage (hérité de groupe_fournisseur.odre). Utilisé pour le tableau pharmacien.';
COMMENT ON COLUMN commande.agence_id IS
  'Agence (fournisseur enfant) chez qui la commande est passée. NULL = commande directe au fournisseur principal.';


-- 5. Copier les champs de configuration depuis groupe_fournisseur vers fournisseur
--    (COALESCE : on ne remplace pas ce qui est déjà renseigné sur le fournisseur)
UPDATE fournisseur f
SET
    delai_livraison_jours    = COALESCE(f.delai_livraison_jours,    gf.delai_livraison_jours),
    frequence_commande_jours = COALESCE(f.frequence_commande_jours, gf.frequence_commande_jours),
    jours_credit             = COALESCE(f.jours_credit,             gf.jours_credit),
    jours_critique           = COALESCE(f.jours_critique,           gf.jours_critique),
    url_pharma_ml            = COALESCE(f.url_pharma_ml,            gf.url_pharma_ml),
    code_office_pharma_ml    = COALESCE(f.code_office_pharma_ml,    gf.code_office_pharma_ml),
    code_recepteur_pharma_ml = COALESCE(f.code_recepteur_pharma_ml, gf.code_recepteur_pharma_ml),
    id_recepteur_pharma_ml   = COALESCE(f.id_recepteur_pharma_ml,   gf.id_recepteur_pharma_ml)
FROM groupe_fournisseur gf
WHERE gf.id = f.groupe_pournisseur_id;

-- 6. Recréer les procédures stockées du tableau pharmacien
--    pour utiliser la self-référence parent_id au lieu de groupe_fournisseur

CREATE OR REPLACE FUNCTION tableau_pharmacien_commandes_report(
    p_start_date   date,
    p_end_date     date,
    p_order_status text
) RETURNS jsonb
    LANGUAGE plpgsql AS
$$
BEGIN
    RETURN (
        SELECT jsonb_agg(
                   jsonb_build_object(
                       'mvtDate',          mvtDate,
                       'montantNet',       net_amount,
                       'montantTaxe',      tax_amount,
                       'montantTtc',       gross_amount,
                       'montantRemise',    discount_amount,
                       'groupeGrossisteId', group_id,
                       'groupeGrossiste',  group_libelle,
                       'ordreAffichage',   group_order
                   )
               )
        FROM (
            SELECT c.order_date                            AS mvtDate,
                   SUM(c.gross_amount - c.tax_amount)      AS net_amount,
                   SUM(c.tax_amount)                       AS tax_amount,
                   SUM(c.gross_amount)                     AS gross_amount,
                   SUM(c.discount_amount)                  AS discount_amount,
                   COALESCE(pf.id,      f.id)              AS group_id,
                   COALESCE(pf.libelle, f.libelle)         AS group_libelle,
                   COALESCE(pf.odre,    f.odre,    100)    AS group_order
            FROM commande c
                     JOIN fournisseur f  ON f.id  = c.fournisseur_id
                     LEFT JOIN fournisseur pf ON pf.id = f.parent_id
            WHERE c.order_date BETWEEN p_start_date AND p_end_date
              AND c.order_status = p_order_status
            GROUP BY mvtDate,
                     COALESCE(pf.id,      f.id),
                     COALESCE(pf.libelle, f.libelle),
                     COALESCE(pf.odre,    f.odre, 100)
            ORDER BY mvtDate
        ) sub
    );
END;
$$;

CREATE OR REPLACE FUNCTION tableau_pharmacien_commandes_mois_report(
    p_start_date   date,
    p_end_date     date,
    p_order_status text
) RETURNS jsonb
    LANGUAGE plpgsql AS
$$
BEGIN
    RETURN (
        SELECT jsonb_agg(
                   jsonb_build_object(
                       'mvtDate',          to_char(month_date, 'YYYY-MM-DD'),
                       'montantNet',       net_amount,
                       'montantTaxe',      tax_amount,
                       'montantTtc',       gross_amount,
                       'montantRemise',    discount_amount,
                       'groupeGrossisteId', group_id,
                       'groupeGrossiste',  group_libelle,
                       'ordreAffichage',   group_order
                   )
               )
        FROM (
            SELECT date_trunc('month', c.updated_at)       AS month_date,
                   SUM(c.gross_amount - c.tax_amount)      AS net_amount,
                   SUM(c.tax_amount)                       AS tax_amount,
                   SUM(c.gross_amount)                     AS gross_amount,
                   SUM(c.discount_amount)                  AS discount_amount,
                   COALESCE(pf.id,      f.id)              AS group_id,
                   COALESCE(pf.libelle, f.libelle)         AS group_libelle,
                   COALESCE(pf.odre,    f.odre,    100)    AS group_order
            FROM commande c
                     JOIN fournisseur f  ON f.id  = c.fournisseur_id
                     LEFT JOIN fournisseur pf ON pf.id = f.parent_id
            WHERE c.order_date BETWEEN p_start_date AND p_end_date
              AND c.order_status = p_order_status
            GROUP BY month_date,
                     COALESCE(pf.id,      f.id),
                     COALESCE(pf.libelle, f.libelle),
                     COALESCE(pf.odre,    f.odre, 100)
            ORDER BY month_date
        ) sub
    );
END;
$$;

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


