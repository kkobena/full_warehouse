-- Recherche produit au comptoir : forme, DCI, dosage et statut générique dans chaque résultat
-- Les deux fonctions reprennent à l'identique celles de V1.1.5 ; seules quatre colonnes s'ajoutent :
--   forme, dci, dosage, typegenerique, statutlegal, peremptionlot, peremptiondate.
-- Aucune ne filtre ni ne trie : les résultats et leur ordre sont inchangés. Les sous-requêtes ne sont
-- évaluées que pour les lignes retenues (LIMIT), et reposent sur les index de produit_id.
-- typegenerique vient du rapprochement avec le référentiel médicament (V2.1.19 et V2.1.20) : il n'est
-- renseigné que pour un rapprochement accepté d'office ou validé.

create or replace function search_produits_json(qtext text, magasin integer DEFAULT 1,
                                     limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id,
                       p.fournisseur_produit_principal_id AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo                    AS codeeanlabo,
                       p.parent_id                        AS parentid,
                       p.item_qty                         AS itemqty,
                       p.deconditionnable,
                       t.taux                             AS vatrate,
                       p.regular_unit_price               AS regularunitprice,
                       p.cost_amount                      AS costamount,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                  AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN left(q.query, 1) ~ '[0-9]' AND
                                (upper(pf.code_cip) = upper(q.query) OR
                                 upper(pf.code_ean) = upper(q.query) OR
                                 upper(p.code_ean_labo) = upper(q.query)) THEN 1000
                           WHEN left(q.query, 1) ~ '[0-9]' AND
                                (upper(pf.code_cip) LIKE upper(q.query) || '%' OR
                                 upper(pf.code_ean) LIKE upper(q.query) || '%' OR
                                 upper(p.code_ean_labo) LIKE upper(q.query) || '%')
                             THEN 500
                           ELSE 0
                           END
                       )                                  AS score,
                       -- rayons
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'code', r.code,
                                   'libelle', r.libelle
                                 )
                                 ORDER BY r.libelle
                               )
                        FROM rayon_produit rp
                               JOIN rayon r ON rp.rayon_id = r.id
                        WHERE rp.produit_id = p.id)       AS rayons,
                       -- stocks
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'quantite', sp.qty_stock,
                                   'qteUg', sp.qty_ug,
                                   'storage', sp.storage_id,
                                   'storageType', s.storage_type,
                                   'stockReassort', sp.stock_reassort,
                                   'seuilMini', sp.seuil_mini
                                 )
                                 ORDER BY sp.id
                               )
                        FROM stock_produit sp
                               join storage s on sp.storage_id = s.id
                        WHERE sp.produit_id = p.id
                          AND s.magasin_id = magasin)     AS stocks,
                       -- forme galénique, molécules (DCI) et dosages : de quoi reconnaître le bon produit sans le connaître par cœur
                       (SELECT fm.libelle FROM form_produit fm WHERE fm.id = p.forme_id)                       AS forme,
                       (SELECT string_agg(d.libelle, ' + ' ORDER BY pd.rang)
                        FROM produit_dci pd
                               JOIN dci d ON d.id = pd.dci_id
                        WHERE pd.produit_id = p.id)                                                          AS dci,
                       (SELECT string_agg(trim_scale(pd.dosage_valeur)::text || ' ' || pd.dosage_unite, ' + ' ORDER BY pd.rang)
                        FROM produit_dci pd
                        WHERE pd.produit_id = p.id
                          AND pd.dosage_valeur IS NOT NULL)                                                  AS dosage,
                       -- princeps ou générique, d'après le rapprochement avec le référentiel (accepté d'office ou validé)
                       (SELECT s.type_generique
                        FROM produit_ref_specialite r
                               JOIN ref_specialite s ON s.cis = r.cis
                        WHERE r.produit_id = p.id
                          AND r.decision IN ('AUTO', 'VALIDE')
                          AND s.type_generique IS NOT NULL)                                                  AS typegenerique,
                       p.statut_legal                                                                        AS statutlegal,
                       -- lot en stock le plus proche de sa péremption, s'il est périmé ou proche de l'être
                       -- (seuil : APP_EXPIRY_ALERT_DAYS_BEFORE, 90 jours par défaut) : c'est le lot qui porte la date
                       (SELECT l.num_lot
                        FROM lot l
                        WHERE l.produit_id = p.id
                          AND l.current_quantity > 0
                          AND l.statut = 'AVAILABLE'
                          AND l.expiry_date <= CURRENT_DATE + COALESCE(
                            (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                             WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                        ORDER BY l.expiry_date, l.id
                        LIMIT 1)                                          AS peremptionlot,
                       (SELECT to_char(l.expiry_date, 'YYYY-MM-DD')
                        FROM lot l
                        WHERE l.produit_id = p.id
                          AND l.current_quantity > 0
                          AND l.statut = 'AVAILABLE'
                          AND l.expiry_date <= CURRENT_DATE + COALESCE(
                            (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                             WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                        ORDER BY l.expiry_date, l.id
                        LIMIT 1)               AS peremptiondate
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                   -- Commence par un chiffre → recherche par code
                  (left(q.query, 1) ~ '[0-9]' AND
                   (upper(pf.code_cip) LIKE upper(q.query) || '%' OR
                    upper(pf.code_ean) LIKE upper(q.query) || '%' OR
                    upper(p.code_ean_labo) LIKE upper(q.query) || '%'))
                   -- Commence par une lettre → recherche par libellé
                   OR (left(q.query, 1) ~ '[A-Za-z]' AND
                       lower(p.libelle) LIKE lower(q.query) || '%')
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,
                         t.taux, p.regular_unit_price, p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;

create or replace function search_produits_by_storage_json(qtext text, p_storage_id integer,
                                                limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id,
                       p.fournisseur_produit_principal_id   AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo                      AS codeeanlabo,
                       p.parent_id                          AS parentid,
                       p.item_qty                           AS itemqty,
                       p.deconditionnable,
                       t.taux                               AS vatrate,
                       p.regular_unit_price                 AS regularunitprice,
                       p.cost_amount                        AS costamount,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                    AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN left(q.query, 1) ~ '[0-9]' AND
                                (upper(pf.code_cip) = upper(q.query) OR
                                 upper(pf.code_ean) = upper(q.query) OR
                                 upper(p.code_ean_labo) = upper(q.query)) THEN 1000
                           WHEN left(q.query, 1) ~ '[0-9]' AND
                                (upper(pf.code_cip) LIKE upper(q.query) || '%' OR
                                 upper(pf.code_ean) LIKE upper(q.query) || '%' OR
                                 upper(p.code_ean_labo) LIKE upper(q.query) || '%')
                             THEN 500
                           ELSE 0
                           END
                       )                                    AS score,
                       -- rayons
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'code', r.code,
                                   'libelle', r.libelle
                                 )
                                 ORDER BY r.libelle
                               )
                        FROM rayon_produit rp
                               JOIN rayon r ON rp.rayon_id = r.id
                        WHERE rp.produit_id = p.id)         AS rayons,
                       -- stocks filtrés par storage_id
                       (SELECT jsonb_agg(
                                 jsonb_build_object(
                                   'quantite', sp.qty_stock,
                                   'qteUg', sp.qty_ug,
                                   'storage', sp.storage_id,
                                   'storageType', s.storage_type,
                                   'stockReassort', sp.stock_reassort,
                                   'seuilMini', sp.seuil_mini
                                 )
                                 ORDER BY sp.id
                               )
                        FROM stock_produit sp
                               join storage s on sp.storage_id = s.id
                        WHERE sp.produit_id = p.id
                          AND sp.storage_id = p_storage_id) AS stocks,
                       -- forme galénique, molécules (DCI) et dosages : de quoi reconnaître le bon produit sans le connaître par cœur
                       (SELECT fm.libelle FROM form_produit fm WHERE fm.id = p.forme_id)                       AS forme,
                       (SELECT string_agg(d.libelle, ' + ' ORDER BY pd.rang)
                        FROM produit_dci pd
                               JOIN dci d ON d.id = pd.dci_id
                        WHERE pd.produit_id = p.id)                                                          AS dci,
                       (SELECT string_agg(trim_scale(pd.dosage_valeur)::text || ' ' || pd.dosage_unite, ' + ' ORDER BY pd.rang)
                        FROM produit_dci pd
                        WHERE pd.produit_id = p.id
                          AND pd.dosage_valeur IS NOT NULL)                                                  AS dosage,
                       -- princeps ou générique, d'après le rapprochement avec le référentiel (accepté d'office ou validé)
                       (SELECT s.type_generique
                        FROM produit_ref_specialite r
                               JOIN ref_specialite s ON s.cis = r.cis
                        WHERE r.produit_id = p.id
                          AND r.decision IN ('AUTO', 'VALIDE')
                          AND s.type_generique IS NOT NULL)                                                  AS typegenerique,
                       p.statut_legal                                                                        AS statutlegal,
                       -- lot en stock le plus proche de sa péremption, s'il est périmé ou proche de l'être
                       -- (seuil : APP_EXPIRY_ALERT_DAYS_BEFORE, 90 jours par défaut) : c'est le lot qui porte la date
                       (SELECT l.num_lot
                        FROM lot l
                        WHERE l.produit_id = p.id
                          AND l.current_quantity > 0
                          AND l.statut = 'AVAILABLE'
                          AND l.expiry_date <= CURRENT_DATE + COALESCE(
                            (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                             WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                        ORDER BY l.expiry_date, l.id
                        LIMIT 1)                                          AS peremptionlot,
                       (SELECT to_char(l.expiry_date, 'YYYY-MM-DD')
                        FROM lot l
                        WHERE l.produit_id = p.id
                          AND l.current_quantity > 0
                          AND l.statut = 'AVAILABLE'
                          AND l.expiry_date <= CURRENT_DATE + COALESCE(
                            (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                             WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                        ORDER BY l.expiry_date, l.id
                        LIMIT 1)               AS peremptiondate
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                  -- Vérifier que le produit a un stock dans le storage spécifié
                  EXISTS (SELECT 1
                          FROM stock_produit sp
                          WHERE sp.produit_id = p.id
                            AND sp.storage_id = p_storage_id)
                  AND (
                  -- Commence par un chiffre → recherche par code
                  (left(q.query, 1) ~ '[0-9]' AND
                   (upper(pf.code_cip) LIKE upper(q.query) || '%' OR
                    upper(pf.code_ean) LIKE upper(q.query) || '%' OR
                    upper(p.code_ean_labo) LIKE upper(q.query) || '%'))
                    -- Commence par une lettre → recherche par libellé
                    OR (left(q.query, 1) ~ '[A-Za-z]' AND
                        lower(p.libelle) LIKE lower(q.query) || '%')
                  )
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,
                         t.taux, p.regular_unit_price, p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;
