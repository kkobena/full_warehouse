-- V2.1.55 — La vue de reassort, dans sa forme definitive
--


DROP VIEW IF EXISTS v_semois_suggestion;


CREATE VIEW v_semois_suggestion AS
SELECT p.id                                         AS produit_id,
       sc.magasin_id,
       p.libelle,
       fp.code_cip,
       fp.fournisseur_id,
       f.libelle                                    AS fournisseur_libelle,
       p.classe_criticite,
       scc.coefficient_securite,
       COALESCE(sc.delai_livraison_jours,
                f.delai_livraison_jours,
                pf.delai_livraison_jours,
                7)                                  AS delai_livraison_jours,
       COALESCE(sc.vmm_calcule, 0)                  AS vmm,
       COALESCE(sc.marge_securite, 0)               AS marge_securite,
       COALESCE(sc.stock_objectif_calcule, 0)       AS stock_objectif,
       COALESCE(SUM(
         CASE WHEN st.storage_type <> 'QUARANTAINE'
              THEN sp.qty_stock + sp.qty_ug
              ELSE 0 END
       ), 0)                                        AS stock_actuel,
       GREATEST(
         0,
         GREATEST(
           COALESCE(sc.stock_objectif_calcule, 0) - (COALESCE(SUM(
             CASE WHEN st.storage_type <> 'QUARANTAINE'
                  THEN sp.qty_stock + sp.qty_ug
                  ELSE 0 END
           ), 0) - (SELECT COALESCE(SUM(a.quantite - a.quantite_remise), 0)
                      FROM avoir_client a
                     WHERE a.produit_id = p.id
                       AND a.statut = 'OUVERT'
                       AND a.quantite > a.quantite_remise)),
           COALESCE(sc.marge_securite, 0) - (COALESCE(SUM(
             CASE WHEN st.storage_type <> 'QUARANTAINE'
                  THEN sp.qty_stock + sp.qty_ug
                  ELSE 0 END
           ), 0) - (SELECT COALESCE(SUM(a.quantite - a.quantite_remise), 0)
                      FROM avoir_client a
                     WHERE a.produit_id = p.id
                       AND a.statut = 'OUVERT'
                       AND a.quantite > a.quantite_remise))
         )
       )                                            AS quantite_a_commander,
       sc.date_dernier_calcul,
       sc.exclusion_date,
       sc.exclusion_duree_jours,
       sc.exclusion_motif,
       COALESCE(sc.facteur_saisonnier_actuel, 1.00) AS facteur_saisonnier_actuel,
       -- En fin de liste : CREATE OR REPLACE VIEW n'autorise que l'ajout de colonnes APRÈS les
       -- existantes. L'ordre est sans effet, le mapping JPA se fait par nom.
       (SELECT COALESCE(SUM(a.quantite - a.quantite_remise), 0)
          FROM avoir_client a
         WHERE a.produit_id = p.id
           AND a.statut = 'OUVERT'
           AND a.quantite > a.quantite_remise)      AS dette_client
FROM produit p
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
       LEFT JOIN fournisseur f ON f.id = fp.fournisseur_id
       LEFT JOIN fournisseur pf ON pf.id = f.parent_id
       -- Jointure INTERNE, et c'est ce qui donne sa forme a la vue : une ligne par
       -- (produit, magasin) CONFIGURE. Le filtre « vmm > 0 » ci-dessous exigeait deja une
       -- configuration, la jointure externe ne servait donc plus a rien ; la garder
       -- produirait desormais une ligne fantome par produit sans configuration.
       JOIN semois_configuration sc ON sc.produit_id = p.id
       LEFT JOIN semois_classe_config scc ON scc.classe_criticite = p.classe_criticite
       LEFT JOIN stock_produit sp ON sp.produit_id = p.id
       -- Le stock suit LE MAGASIN DE LA CONFIGURATION. Fige sur l'officine jusqu'ici, il
       -- faisait lire a une parapharmacie le stock d'un rayon qui n'est pas le sien.
       LEFT JOIN storage st ON st.id = sp.storage_id
                           AND st.magasin_id = sc.magasin_id
WHERE p.status::text = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  AND COALESCE(sc.vmm_calcule, 0) > 0
  AND (
  sc.exclusion_date IS NULL
    OR NOW() > sc.exclusion_date + (COALESCE(sc.exclusion_duree_jours, 30) || ' days')::INTERVAL
  )
GROUP BY p.id, sc.magasin_id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
         f.delai_livraison_jours, pf.delai_livraison_jours,
         p.classe_criticite, scc.coefficient_securite,
         sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
         sc.stock_objectif_calcule, sc.date_dernier_calcul,
         sc.exclusion_date, sc.exclusion_duree_jours, sc.exclusion_motif,
         sc.facteur_saisonnier_actuel;

COMMENT ON VIEW v_semois_suggestion IS
  'Vue SEMOIS v9 — une ligne par (produit, magasin commandeur) ; stock limite au magasin de la configuration ; quarantaine exclue ; dette client retranchee du stock disponible.';

-- L'écran « Dus aux clients » et ce calcul interrogent la même table avec le même filtre.
CREATE INDEX IF NOT EXISTS avoir_client_ouvert_produit_idx
  ON avoir_client (produit_id) WHERE statut = 'OUVERT';
