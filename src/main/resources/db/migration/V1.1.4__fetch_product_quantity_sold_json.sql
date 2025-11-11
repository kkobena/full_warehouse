CREATE OR REPLACE FUNCTION fetch_product_quantity_sold_json(
  p_start_date date,
  p_end_date date
)
  RETURNS JSONB AS
$$
DECLARE
  result JSONB := '[]'::jsonb;
BEGIN
  WITH sales_base AS (
    -- Total qty sold pour chaque produit "PACKAGE"
    SELECT sl.produit_id, SUM(sl.quantity_sold) AS qty_sold
    FROM sales_line sl
           JOIN sales s ON s.id = sl.sales_id
    WHERE s.statut = 'CLOSED'
      AND s.canceled = FALSE
      AND s.sale_date BETWEEN p_start_date AND p_end_date
    GROUP BY sl.produit_id),
       sales_detail AS (
         -- Total qty sold des produits "DETAIL" enfants pour chaque parent
         SELECT pd.parent_id, SUM(sl.quantity_sold) AS qty_sold
         FROM sales_line sl
                JOIN sales s ON s.id = sl.sales_id
                JOIN produit pd ON pd.id = sl.produit_id
         WHERE s.statut = 'CLOSED'
           AND s.canceled = FALSE
           AND pd.type_produit = 'DETAIL'
           AND s.sale_date BETWEEN p_start_date AND p_end_date
         GROUP BY pd.parent_id),
       computed AS (SELECT p.id,
                           p.item_qty,
                           COALESCE(sb.qty_sold, 0) AS qtySold,
                           COALESCE(sd.qty_sold, 0) AS itemQtySold
                    FROM produit p
                           LEFT JOIN sales_base sb ON sb.produit_id = p.id
                           LEFT JOIN sales_detail sd ON sd.parent_id = p.id
                    WHERE p.status = 'ENABLE'
                      AND p.type_produit = 'PACKAGE')
  SELECT jsonb_agg(
           jsonb_build_object(
             'id', c.id,
             'itemQty', c.item_qty,
             'qtySold', c.qtySold,
             'itemQtySold', c.itemQtySold
           )
         )
  INTO result
  FROM computed c;

  RETURN result;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_product_movements_by_period(
  p_produit_id INT,
  p_magasin_id INT,
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
             ORDER BY id
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
         ORDER BY transaction_date
       )
  SELECT jsonb_agg(
           jsonb_build_object(
             'mvtDate', transaction_date,
             'initStock', quantite_debut,
             'afterStock', quantite_fin,
             'mouvements', mouvements
           )
             ORDER BY transaction_date
         )
  INTO result
  FROM mouvements_agg;

  RETURN COALESCE(result, '[]'::jsonb);
END;
$$;


