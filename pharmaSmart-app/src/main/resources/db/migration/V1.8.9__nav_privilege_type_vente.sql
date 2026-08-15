

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-sale-assurance',
       'Effectuer une vente assurance',
       NULL,
       NULL,
       id,
       8,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'ventes' ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-sale-carnet',
       'Effectuer une vente carnet (à crédit)',
       NULL,
       NULL,
       id,
       9,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'ventes' ON CONFLICT (code) DO NOTHING;

-- ── Rôles autorisés : ADMIN, PHARMACIEN, CAISSIER, VENDEUR ───────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT ni.id,
       r.role_name,
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item ni
       CROSS JOIN (VALUES ('ROLE_ADMIN'),
                          ('ROLE_PHARMACIEN'),
                          ('ROLE_CAISSIER'),
                          ('ROLE_VENDEUR')) AS r(role_name)
WHERE ni.code IN ('pr-sale-assurance', 'pr-sale-carnet') ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_display = TRUE,
  can_access = TRUE,
  can_execute = TRUE;
