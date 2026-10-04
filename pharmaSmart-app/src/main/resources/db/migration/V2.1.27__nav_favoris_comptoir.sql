-- Épingler un produit aux favoris du comptoir depuis la vente : un droit propre, distinct du catalogue que le caissier
-- et le vendeur n'ont pas. Le catalogue reste accepté par l'endpoint (RequiresNavAccess listant les deux codes).
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.favoris.gerer',
       'Gérer les favoris du comptoir',
       'pi pi-star',
       NULL,
       id,
       90,
       3,
       'ACTION',
       TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code = 'ventes.favoris.gerer'
ON CONFLICT DO NOTHING;

-- Par défaut, tout rôle qui peut vendre peut épingler (écriture et retrait) ; l'administration des droits permet de le retirer.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT f.id, r.role_name, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE
FROM nav_item f
         JOIN nav_item v ON v.code = 'nouvelle-vente'
         JOIN nav_item_role r ON r.nav_item_id = v.id AND r.can_access
WHERE f.code = 'ventes.favoris.gerer'
  AND r.role_name <> 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
