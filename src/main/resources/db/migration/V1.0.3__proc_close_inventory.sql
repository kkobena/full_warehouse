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
        SELECT a.quantity_on_hand, s.storage_id, a.produit_id,s.user_id,u.magasin_id
        ,a.id,a.inventory_value_cost,a.last_unit_price,a.quantity_init
        ,a.updated_at
        FROM store_inventory_line a,
             store_inventory s,user u
        where s.id = a.store_inventory_id
          AND s.user_id= u.id
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

        INSERT INTO  inventory_transaction(cost_amount, created_at, entity_id, mouvemen_type, quantity, quantity_after, quantity_befor, regular_unit_price, magasin_id, produit_id, user_id)
          VALUE (inventory_value_cost,updated_at,entityId,'INVENTAIRE',quantity_on_hand,quantity_on_hand,quantity_init,last_unit_price,magasin_id,user_id);

        SET nombreLigne = nombreLigne + 1;
    END LOOP bl_loop;
    CLOSE curbl;
    COMMIT;

END @@
DELIMITER ;
