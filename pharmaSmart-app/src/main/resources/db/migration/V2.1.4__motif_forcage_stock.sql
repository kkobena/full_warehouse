
ALTER TABLE sales_line
  ADD COLUMN motif_forcage varchar(20)
    CONSTRAINT sales_line_motif_forcage_check CHECK (motif_forcage IN ('RUPTURE_AVOIR', 'ECART_INVENTAIRE'));

-- Toute ligne passée en avoir avant cette version était une rupture : c'est le seul cas que le code savait traiter.
UPDATE sales_line sl
SET motif_forcage = 'RUPTURE_AVOIR'
WHERE sl.quantity_avoir > 0
   OR EXISTS (SELECT 1
              FROM avoir_client ac
              WHERE ac.sales_line_id = sl.id
                AND ac.sales_line_date = sl.sale_date);

INSERT INTO motif_ajustement (libelle)
VALUES ('Régularisation constatée à la vente')
ON CONFLICT (libelle) DO NOTHING;

-- Privilège distinct du forçage en avoir, accordé au départ aux mêmes profils que lui (admin et caissier).
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-regulariser-stock-vente',
       'Régulariser le stock à la vente (écart d''inventaire)',
       NULL,
       NULL,
       id,
       0,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT n.id, r.role_name, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE
FROM nav_item n
       CROSS JOIN (VALUES ('ROLE_ADMIN'), ('ROLE_CAISSIER')) AS r(role_name)
WHERE n.code = 'pr-regulariser-stock-vente'
ON CONFLICT (nav_item_id, role_name) DO UPDATE SET can_execute = TRUE;
