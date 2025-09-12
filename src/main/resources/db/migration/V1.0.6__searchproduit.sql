CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX libelle_index_trg ON produit USING gin (libelle gin_trgm_ops);

CREATE INDEX code_ean_labo_index_trg ON produit USING gin (code_ean_labo gin_trgm_ops);
CREATE INDEX code_cipo_index_trg ON fournisseur_produit USING gin (code_cip gin_trgm_ops);


CREATE INDEX libelle_search_tsv_gin ON produit USING GIN (libelle gin_trgm_ops);



CREATE OR REPLACE FUNCTION search_produits_json(qtext text, magasin integer DEFAULT 1,
                                                limit_result int DEFAULT 10)
  RETURNS JSONB
  LANGUAGE plpgsql
AS
$$
BEGIN
  RETURN (WITH q AS (SELECT unaccent(qtext)::text AS query)
          SELECT jsonb_agg(result)
          FROM (SELECT p.id                              AS produit_id,
                       p.fournisseur_produit_princial_id AS code_cip_principal_id,
                       p.libelle,
                       p.code_ean_labo,
                       -- regrouper les codes CIP/EAN des différents fournisseurs
                       jsonb_agg(
                         jsonb_build_object(
                           'id', pf.id,
                           'codecip', pf.code_cip,
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
                                   'storageType', s.storage_type
                                 )
                                 ORDER BY sp.id
                               )
                        FROM stock_produit sp
                               join storage s on sp.storage_id = s.id
                        WHERE sp.produit_id = p.id
                          AND s.magasin_id = magasin)    AS stocks
                FROM produit p
                       LEFT JOIN fournisseur_produit pf ON pf.produit_id = p.id
                       CROSS JOIN q
                WHERE
                   -- recherche par code numérique (exact / préfixe)
                  (q.query ~ '^[0-9]+$' AND
                   (pf.code_cip = q.query OR pf.code_ean = q.query OR p.code_ean_labo = q.query
                     OR pf.code_cip LIKE q.query || '%' OR pf.code_ean LIKE q.query || '%' OR
                    p.code_ean_labo LIKE q.query || '%'))
                   -- recherche préfixe stricte sur le libellé
                   OR (lower(p.libelle) LIKE lower(q.query || '%'))
                GROUP BY p.id, p.libelle, p.code_ean_labo, p.fournisseur_produit_princial_id
                ORDER BY p.libelle ASC
                LIMIT limit_result) result);
END;
$$;




