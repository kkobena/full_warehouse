
create procedure proc_close_inventory(IN p_store_inventory_id bigint, INOUT p_nombreligne integer)
  language plpgsql
as
$$
DECLARE
  v_produit_id INT;
  v_storage_id INT;
  v_entity_id bigint;
  v_quantity_on_hand INT;
  v_quantity_init INT;
  v_inventory_value_cost INT;
  v_last_unit_price INT;
  v_user_id INT;
  v_magasin_id INT;
  v_updated_at TIMESTAMP;
  curbl CURSOR FOR
    SELECT a.quantity_on_hand,
           s.storage_id,
           a.produit_id,
           s.user_id,
           u.magasin_id,
           a.id,
           a.inventory_value_cost,
           a.last_unit_price,
           a.quantity_init,
           a.updated_at
    FROM store_inventory_line a
           JOIN store_inventory s ON s.id = a.store_inventory_id
           JOIN app_user u ON s.user_id = u.id
    WHERE s.id = p_store_inventory_id;
BEGIN
  p_nombreLigne := 0;
  OPEN curbl;
  LOOP
    FETCH curbl INTO v_quantity_on_hand, v_storage_id, v_produit_id, v_user_id, v_magasin_id, v_entity_id, v_inventory_value_cost, v_last_unit_price, v_quantity_init, v_updated_at;
    EXIT WHEN NOT FOUND;

    UPDATE stock_produit st
    SET updated_at = now(),
        qty_ug = 0,
        qty_stock = v_quantity_on_hand,
        qty_virtual = v_quantity_on_hand
    WHERE st.produit_id = v_produit_id
      AND st.storage_id = v_storage_id;

    INSERT INTO inventory_transaction(cost_amount, created_at, entity_id, mouvement_type, quantity,
                                      quantity_after, quantity_befor, regular_unit_price,
                                      magasin_id, produit_id, user_id)
    VALUES (v_inventory_value_cost, v_updated_at, v_entity_id, 'INVENTAIRE', v_quantity_on_hand,
            v_quantity_on_hand, v_quantity_init, v_last_unit_price, v_magasin_id, v_produit_id, v_user_id);

    p_nombreLigne := p_nombreLigne + 1;
  END LOOP;
  CLOSE curbl;
END;
$$;



create function gettopqty80percentproducts(startdate date, enddate date, calist character varying, statutlist character varying)
  returns TABLE(libelle character varying, code_cip character varying, qte_totale bigint, total_global bigint, pourcentage numeric)
  language sql
as
$$
WITH ventes_par_produit AS (
  SELECT sl.produit_id,
         SUM(sl.quantity_requested) AS qte_totale
  FROM sales_line sl
         JOIN sales s ON s.id = sl.sales_id
  WHERE s.sale_date BETWEEN startDate AND endDate
    AND s.ca = ANY(string_to_array(caList, ','))
    AND s.statut = ANY(string_to_array(statutList, ','))
    AND s.canceled = false
  GROUP BY sl.produit_id
),
     total_global AS (
       SELECT SUM(qte_totale) AS total_global
       FROM ventes_par_produit
     ),
     classement AS (
       SELECT vp.produit_id,
              vp.qte_totale,
              SUM(vp.qte_totale) OVER (ORDER BY vp.qte_totale DESC) AS cumul,
              tg.total_global
       FROM ventes_par_produit vp
              CROSS JOIN total_global tg
     )
SELECT p.libelle,
       MAX(fp.code_cip)::VARCHAR AS code_cip,
       c.qte_totale::BIGINT,
       c.total_global::BIGINT,
       ROUND((c.qte_totale / c.total_global) * 100, 2) AS pourcentage
FROM classement c
       JOIN produit p ON p.id = c.produit_id
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
WHERE c.cumul <= 0.8 * c.total_global
GROUP BY p.id, p.libelle, c.qte_totale, c.total_global
ORDER BY c.qte_totale DESC;
$$;



create function gettopamount80percentproducts(startdate date, enddate date, calist character varying, statutlist character varying)
  returns TABLE(libelle character varying, code_cip character varying, total_global numeric, sales_amount numeric, pourcentage numeric)
  language sql
as
$$
WITH ventes_par_produit AS (
  SELECT sl.produit_id,
         SUM(sl.sales_amount) AS sales_amount
  FROM sales_line sl
         JOIN sales s ON s.id = sl.sales_id
  WHERE s.sale_date BETWEEN startDate AND endDate
    AND s.ca = ANY(string_to_array(caList, ','))
    AND s.statut = ANY(string_to_array(statutList, ','))
    AND s.canceled = false
  GROUP BY sl.produit_id
),
     total_global AS (
       SELECT SUM(sales_amount) AS total_global
       FROM ventes_par_produit
     ),
     classement AS (
       SELECT vp.produit_id,
              vp.sales_amount,
              SUM(vp.sales_amount) OVER (ORDER BY vp.sales_amount DESC) AS cumul,
              tg.total_global
       FROM ventes_par_produit vp
              CROSS JOIN total_global tg
     )
SELECT p.libelle,
       MAX(fp.code_cip)::VARCHAR AS code_cip,
       c.total_global,
       c.sales_amount,
       ROUND((c.sales_amount / c.total_global) * 100, 2) AS pourcentage
FROM classement c
       JOIN produit p ON p.id = c.produit_id
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
WHERE c.cumul <= 0.8 * c.total_global
GROUP BY p.id, p.libelle, c.total_global, c.sales_amount
ORDER BY c.sales_amount DESC;
$$;


