DROP FUNCTION IF EXISTS get_product_sales_summary(date, date, text[], text[], integer, integer);



create function get_product_sales_summary(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                                          p_produit_id integer, p_group_by integer DEFAULT 0) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'groupType', p_group_by,
                     'groupBy', grp.mvt_date,
                     'montantHt', grp.montant_ht,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat,
                     'montantTtc', grp.montant_ttc,
                     'montantRemise', grp.montant_remise
                   )
                   ORDER BY grp.mvt_date
                 )
          FROM (SELECT CASE p_group_by
                         WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                         WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                         WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                         WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                     CASE
                                       WHEN extract(quarter from s.sale_date) <= 2
                                         THEN 'S1'
                                       ELSE 'S2' END
                         WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                         END                                            AS mvt_date,
                       CEIL(SUM((o.quantity_requested * o.regular_unit_price) /
                                NULLIF(1 + (o.tax_value / 100), 0)))    AS montant_ht,
                       SUM(o.quantity_requested)                        AS quantite,
                       SUM(o.quantity_requested * o.cost_amount)        AS montant_achat,
                       SUM(o.quantity_requested * o.regular_unit_price) AS montant_ttc,
                       SUM(o.discount_amount)                           AS montant_remise
                FROM sales_line o
                       JOIN sales s ON o.sales_id = s.id
                WHERE o.sales_sale_date = s.sale_date
                  AND s.sale_date BETWEEN p_start_date AND p_end_date
                  AND o.produit_id = p_produit_id
                  AND s.statut = ANY (p_statuts)
                  AND s.ca = ANY (p_cas)
                GROUP BY CASE p_group_by
                           WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                           WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                           WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                           WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                       CASE
                                         WHEN extract(quarter from s.sale_date) <= 2
                                           THEN 'S1'
                                         ELSE 'S2' END
                           WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                           END) grp);
END;
$$;

DROP FUNCTION IF EXISTS search_produits_json( text,  integer , integer);



create function search_produits_json(qtext text, magasin integer DEFAULT 1, limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id ,
                       p.fournisseur_produit_principal_id AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo AS codeeanlabo,
                       p.parent_id                       AS parentid,
                       p.item_qty                        AS itemqty,
                       p.deconditionnable,
                       t.taux AS vatrate,
                       p.regular_unit_price AS regularunitprice ,
                       p.cost_amount  AS costamount ,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                 AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN q.query ~ '^[0-9]+$' AND
                                (pf.code_cip = q.query OR pf.code_ean = q.query OR
                                 p.code_ean_labo = q.query) THEN 1000
                           WHEN q.query ~ '^[0-9]+$' AND (pf.code_cip LIKE q.query || '%' OR
                                                          pf.code_ean LIKE q.query || '%' OR
                                                          p.code_ean_labo LIKE q.query || '%')
                             THEN 500
                           ELSE 0
                           END
                       )                                 AS score,
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
                        WHERE rp.produit_id = p.id)      AS rayons,
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
                          AND s.magasin_id = magasin)    AS stocks
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                   -- recherche par code numérique (exact / préfixe)
                  (q.query ~ '^[0-9]+$' AND
                   (pf.code_cip = q.query OR pf.code_ean = q.query OR p.code_ean_labo = q.query
                     OR pf.code_cip LIKE q.query || '%' OR pf.code_ean LIKE q.query || '%' OR
                    p.code_ean_labo LIKE q.query || '%'))
                   -- recherche préfixe stricte sur le libellé
                   OR (lower(p.libelle) LIKE lower(q.query || '%'))
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,t.taux,p.regular_unit_price,p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;

-- Version de la fonction qui filtre par storage_id
DROP FUNCTION IF EXISTS search_produits_by_storage_json(text, integer, integer);

create function search_produits_by_storage_json(qtext text, p_storage_id integer, limit_result integer DEFAULT 10) returns jsonb
  language plpgsql
as
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id ,
                       p.fournisseur_produit_principal_id AS codecipprincipalid,
                       p.libelle,
                       p.code_ean_labo AS codeeanlabo,
                       p.parent_id                       AS parentid,
                       p.item_qty                        AS itemqty,
                       p.deconditionnable,
                       t.taux AS vatrate,
                       p.regular_unit_price AS regularunitprice ,
                       p.cost_amount  AS costamount ,
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codeCip', pf.code_cip,
                           'codeEan', pf.code_ean,
                           'prixUni', pf.prix_uni,
                           'prixAchat', pf.prix_achat
                         )
                       )                                 AS fournisseurs,
                       -- score composite basé uniquement sur les codes
                       MAX(
                         CASE
                           WHEN q.query ~ '^[0-9]+$' AND
                                (pf.code_cip = q.query OR pf.code_ean = q.query OR
                                 p.code_ean_labo = q.query) THEN 1000
                           WHEN q.query ~ '^[0-9]+$' AND (pf.code_cip LIKE q.query || '%' OR
                                                          pf.code_ean LIKE q.query || '%' OR
                                                          p.code_ean_labo LIKE q.query || '%')
                             THEN 500
                           ELSE 0
                           END
                       )                                 AS score,
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
                        WHERE rp.produit_id = p.id)      AS rayons,
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
                          AND sp.storage_id = p_storage_id)    AS stocks
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       LEFT JOIN tva t ON p.tva_id = t.id
                       CROSS JOIN q
                WHERE
                   -- Vérifier que le produit a un stock dans le storage spécifié
                  EXISTS (
                    SELECT 1
                    FROM stock_produit sp
                    WHERE sp.produit_id = p.id
                      AND sp.storage_id = p_storage_id
                  )
                  AND (
                   -- recherche par code numérique (exact / préfixe)
                  (q.query ~ '^[0-9]+$' AND
                   (pf.code_cip = q.query OR pf.code_ean = q.query OR p.code_ean_labo = q.query
                     OR pf.code_cip LIKE q.query || '%' OR pf.code_ean LIKE q.query || '%' OR
                    p.code_ean_labo LIKE q.query || '%'))
                   -- recherche préfixe stricte sur le libellé
                   OR (lower(p.libelle) LIKE lower(q.query || '%'))
                  )
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_principal_id,t.taux,p.regular_unit_price,p.cost_amount
                ORDER BY p.libelle
                LIMIT limit_result) result);
END;
$$;



