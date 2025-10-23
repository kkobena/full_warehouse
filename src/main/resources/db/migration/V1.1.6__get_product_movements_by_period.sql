CREATE OR REPLACE FUNCTION get_product_movements_by_period(
  p_produit_id BIGINT,
  p_magasin_id BIGINT,
  p_date_debut DATE,
  p_date_fin DATE
)
  RETURNS JSONB
  LANGUAGE plpgsql
AS $$
DECLARE
  result JSONB;
BEGIN
  WITH mouvements_ordonnes AS (
    SELECT *,
           ROW_NUMBER() OVER (
             PARTITION BY transaction_date, produit_id, magasin_id
             ORDER BY id ASC
             ) AS rn_debut,
           ROW_NUMBER() OVER (
             PARTITION BY transaction_date, produit_id, magasin_id
             ORDER BY id DESC
             ) AS rn_fin
    FROM inventory_transaction
    WHERE produit_id = p_produit_id
      AND magasin_id = p_magasin_id
      AND transaction_date BETWEEN p_date_debut AND p_date_fin
  ),
       mouvements_detail AS (
         SELECT
           transaction_date,
           magasin_id,
           produit_id,
           mouvement_type,
           SUM(quantity) AS quantite,
           MIN(CASE WHEN rn_debut = 1 THEN quantity_befor END) AS quantite_debut,
           MAX(CASE WHEN rn_fin = 1 THEN quantity_after END) AS quantite_fin
         FROM mouvements_ordonnes
         GROUP BY transaction_date, magasin_id, produit_id, mouvement_type
       ),
       mouvements_agg AS (
         SELECT
           transaction_date,
           magasin_id,
           produit_id,
           MAX(quantite_debut) AS quantite_debut,
           MAX(quantite_fin) AS quantite_fin,
           jsonb_object_agg(mouvement_type, quantite) AS mouvements
         FROM mouvements_detail
         GROUP BY transaction_date, magasin_id, produit_id
         ORDER BY transaction_date ASC
       )
  SELECT jsonb_agg(
           jsonb_build_object(
             'mvtDate', transaction_date,
             'initStock', quantite_debut,
             'afterStock', quantite_fin,
             'mouvements', mouvements
           )
           ORDER BY transaction_date ASC
         )
  INTO result
  FROM mouvements_agg;

  RETURN COALESCE(result, '[]'::jsonb);
END;
$$;


alter table produit
  rename column fournisseur_produit_princial_id to fournisseur_produit_principal_id;
