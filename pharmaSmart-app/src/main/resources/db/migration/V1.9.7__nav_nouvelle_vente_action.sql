

UPDATE nav_item
   SET actif       = TRUE,
       target_type = 'ACTION',
       updated     = NOW()
 WHERE code IN ('nouvelle-vente', 'nouvelle-prevente');

UPDATE nav_item_role r
   SET can_display = FALSE
  FROM nav_item n
 WHERE n.id = r.nav_item_id
   AND n.code IN ('nouvelle-vente', 'nouvelle-prevente');
