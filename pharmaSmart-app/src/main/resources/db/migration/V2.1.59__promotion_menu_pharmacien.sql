
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT i.id, a.name, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
  FROM nav_item i
 CROSS JOIN authority a
 WHERE i.code = 'promotions'
   AND a.name = 'ROLE_PHARMACIEN'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
