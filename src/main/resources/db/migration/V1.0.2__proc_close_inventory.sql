DELIMITER @@
DROP PROCEDURE IF EXISTS proc_close_inventory @@
create procedure proc_close_inventory(IN store_inventory_id int, OUT nombreLigne int)
BEGIN
    DECLARE produitId INT;
    DECLARE storageId INT;
    DECLARE quantity_on_hand INT(8);
    DECLARE done INT DEFAULT 0;

    DECLARE curbl CURSOR FOR
        SELECT a.quantity_on_hand, s.storage_id, a.produit_id
        FROM store_inventory_line a,
             store_inventory s
        where s.id = a.store_inventory_id
          AND s.id = store_inventory_id;


    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    SET nombreLigne = 0;
    OPEN curbl;
    bl_loop:
    LOOP
        FETCH curbl INTO quantity_on_hand,storageId,produitId;
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
        SET nombreLigne = nombreLigne + 1;
    END LOOP bl_loop;
    CLOSE curbl;
    COMMIT;

END @@
DELIMITER ;
