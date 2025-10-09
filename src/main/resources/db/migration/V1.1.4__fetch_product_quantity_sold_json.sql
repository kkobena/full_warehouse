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
