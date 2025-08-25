DELIMITER @@
DROP PROCEDURE IF EXISTS proc_close_inventory @@
create procedure proc_close_inventory(IN store_inventory_id bigint, OUT nombreLigne int)
BEGIN
  DECLARE produitId bigint;
  DECLARE storageId bigint;
  DECLARE entityId bigint;
  DECLARE quantity_on_hand INT(8);
  DECLARE quantity_init INT(8);
  DECLARE inventory_value_cost INT;
  DECLARE last_unit_price INT;
  DECLARE userId bigint;
  DECLARE magasinId bigint;
  DECLARE updated_at DATETIME;
  DECLARE done INT DEFAULT 0;

  DECLARE curbl CURSOR FOR
    SELECT a.quantity_on_hand
         , s.storage_id
         , a.produit_id
         , s.user_id
         , u.magasin_id
         , a.id
         , a.inventory_value_cost
         , a.last_unit_price
         , a.quantity_init
         , a.updated_at
    FROM store_inventory_line a,
         store_inventory s,
         user u
    where s.id = a.store_inventory_id
      AND s.user_id = u.id
      AND s.id = store_inventory_id;


  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  SET nombreLigne = 0;
  OPEN curbl;
  bl_loop:
  LOOP
    FETCH curbl INTO quantity_on_hand,storageId,produitId,userId,magasinId,entityId,inventory_value_cost,last_unit_price,quantity_init,updated_at;
    IF done = 1 THEN
      LEAVE bl_loop;
    END IF;

    UPDATE stock_produit st
    SET st.updated_at=now(),
        st.qty_ug=0,
        st.qty_stock=quantity_on_hand,
        st.qty_virtual=quantity_on_hand
    WHERE st.produit_id = produitId
      AND st.storage_id = storageId;

    INSERT INTO inventory_transaction(cost_amount, created_at, entity_id, mouvemen_type, quantity,
                                      quantity_after, quantity_befor, regular_unit_price,
                                      magasin_id, produit_id, user_id)
      VALUE (inventory_value_cost, updated_at, entityId, 'INVENTAIRE', quantity_on_hand,
             quantity_on_hand, quantity_init, last_unit_price, magasin_id, user_id);

    SET nombreLigne = nombreLigne + 1;
  END LOOP bl_loop;
  CLOSE curbl;
  COMMIT;

END @@
DELIMITER ;


DELIMITER //
DROP PROCEDURE IF EXISTS GetTopQty80PercentProducts //
CREATE PROCEDURE GetTopQty80PercentProducts(
  IN startDate DATE,
  IN endDate DATE,
  IN caList VARCHAR(255),
  IN statutList VARCHAR(255)
)
BEGIN
  -- Cette procédure stockée calcule les 80% des produits les plus vendus par quantité
  -- pour une période, une catégorie et un statut donnés.

  WITH ventes_par_produit AS (SELECT sl.produit_id,
                                     SUM(sl.quantity_requested) AS qte_totale

                              FROM sales_line sl
                                     JOIN sales s ON s.id = sl.sales_id
                              WHERE DATE(s.updated_at) BETWEEN startDate AND endDate
                                AND FIND_IN_SET(s.ca, caList) > 0
                                AND FIND_IN_SET(s.statut, statutList) > 0
                                AND s.canceled = false
                                AND s.imported = 0
                              GROUP BY sl.produit_id),
       total_global AS (SELECT SUM(qte_totale) AS total_global
                        FROM ventes_par_produit),
       classement AS (SELECT vp.produit_id,

                             vp.qte_totale,
                             SUM(vp.qte_totale) OVER (ORDER BY vp.qte_totale DESC) AS cumul,
                             tg.total_global
                      FROM ventes_par_produit vp
                             CROSS JOIN total_global tg)
  SELECT p.libelle,
         MAX(fp.code_cip)                                AS code_cip, -- Code fournisseur principal
         c.qte_totale,
         c.total_global,
         ROUND((c.qte_totale / c.total_global) * 100, 2) AS pourcentage
  FROM classement c
         JOIN produit p ON p.id = c.produit_id
         LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id AND fp.principal = true
  WHERE c.cumul <= 0.8 * c.total_global
  GROUP BY p.id, p.libelle, c.qte_totale, c.total_global
  ORDER BY c.qte_totale DESC;

END //

DELIMITER ;


DELIMITER //
DROP PROCEDURE IF EXISTS GetTopAmount80PercentProducts //
CREATE PROCEDURE GetTopAmount80PercentProducts(
  IN startDate DATE,
  IN endDate DATE,
  IN caList VARCHAR(255),
  IN statutList VARCHAR(255)
)
BEGIN


  WITH ventes_par_produit AS (SELECT sl.produit_id,

                                     SUM(sl.sales_amount) AS sales_amount
                              FROM sales_line sl
                                     JOIN sales s ON s.id = sl.sales_id
                              WHERE DATE(s.updated_at) BETWEEN startDate AND endDate
                                AND FIND_IN_SET(s.ca, caList) > 0
                                AND FIND_IN_SET(s.statut, statutList) > 0
                                AND s.canceled = false
                                AND s.imported = 0
                              GROUP BY sl.produit_id),
       total_global AS (SELECT SUM(sales_amount) AS total_global
                        FROM ventes_par_produit),
       classement AS (SELECT vp.produit_id,
                             vp.sales_amount,

                             SUM(vp.sales_amount) OVER (ORDER BY vp.sales_amount DESC) AS cumul,
                             tg.total_global
                      FROM ventes_par_produit vp
                             CROSS JOIN total_global tg)
  SELECT p.libelle,
         MAX(fp.code_cip)                                  AS code_cip, -- Code fournisseur principal
         c.total_global,
         c.sales_amount,
         ROUND((c.sales_amount / c.total_global) * 100, 2) AS pourcentage
  FROM classement c
         JOIN produit p ON p.id = c.produit_id
         LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id AND fp.principal = true
  WHERE c.cumul <= 0.8 * c.total_global
  GROUP BY p.id, p.libelle, c.total_global, c.sales_amount

  ORDER BY c.sales_amount DESC;

END //

DELIMITER ;
