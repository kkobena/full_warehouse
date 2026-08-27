

INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, actif, created, updated)
SELECT
    'dci',
    'DCI',
    'Dénominations Communes Internationales',
    'pi pi-bookmark',
    '/dci',
    p.id,
    -- Se place après le dernier enfant existant du module.
    COALESCE((SELECT MAX(c.ordre) FROM nav_item c WHERE c.parent_id = p.id), 0) + 1,
    2,
    TRUE,
    NOW(),
    NOW()
FROM nav_item p
WHERE p.code = 'referentiel'
  AND NOT EXISTS (SELECT 1 FROM nav_item x WHERE x.code = 'dci');

-- Droits : alignés sur les autres référentiels produit du module.
INSERT INTO nav_item_role (
    nav_item_id, role_name,
    can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute
)
SELECT n.id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item n
WHERE n.code = 'dci'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
