ALTER TABLE sales
  DROP CONSTRAINT sales_statut_check;

ALTER TABLE sales
  ADD CONSTRAINT sales_statut_check
    CHECK (
      statut IN (
                 'PROCESSING',
                 'PENDING',
                 'CLOSED',
                 'ACTIVE',
                 'CANCELED',
                 'REMOVED',
                 'DEVIS'
        )
      );

DROP FUNCTION IF EXISTS search_produits_by_storage_json(text, integer, integer);
DROP FUNCTION IF EXISTS search_produits_json(text, integer, integer);

create function search_produits_json(qtext text, magasin integer DEFAULT 1,
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
                          AND s.magasin_id = magasin)     AS stocks
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



create function search_produits_by_storage_json(qtext text, p_storage_id integer,
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
                          AND sp.storage_id = p_storage_id) AS stocks
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

