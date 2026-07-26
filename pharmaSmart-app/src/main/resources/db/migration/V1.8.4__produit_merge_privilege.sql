-- Privilège ACTION pour la fusion de produits en doublon du catalogue.
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-fusion-produit',
       'Fusionner des produits en doublon',
       NULL,
       NULL,
       id,
       0,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'catalogue' ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       'ROLE_ADMIN',
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item
WHERE code = 'pr-fusion-produit' ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_execute = TRUE;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       'ROLE_RESPONSABLE_COMMANDE',
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item
WHERE code = 'pr-fusion-produit' ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_execute = TRUE;

ALTER TABLE logs
  DROP CONSTRAINT logs_transaction_type_check;

ALTER TABLE logs
  ADD CONSTRAINT logs_transaction_type_check
    CHECK ((transaction_type)::text = ANY
  ((ARRAY ['SALE'::character varying, 'DELETE_SALE'::character varying, 'CANCEL_SALE'::character varying, 'REAPPRO'::character varying, 'AJUSTEMENT_IN'::character varying, 'AJUSTEMENT_OUT'::character varying, 'INVENTAIRE'::character varying, 'SUPPRESSION'::character varying, 'COMMANDE'::character varying, 'DECONDTION_IN'::character varying, 'DECONDTION_OUT'::character varying, 'CREATE_PRODUCT'::character varying, 'UPDATE_PRODUCT'::character varying, 'DELETE_PRODUCT'::character varying, 'DISABLE_PRODUCT'::character varying, 'ENABLE_PRODUCT'::character varying, 'MODIFICATION_PRIX_PRODUCT'::character varying, 'MOUVEMENT_STOCK_IN'::character varying, 'MOUVEMENT_STOCK_OUT'::character varying, 'FORCE_STOCK'::character varying, 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE'::character varying, 'ENTREE_STOCK'::character varying, 'ACTIVATION_PRIVILEGE'::character varying, 'RETRAIT_PERIME'::character varying, 'MODIFICATION_DATE_DE_VENTE'::character varying, 'MODIFICATION_INFO_CLIENT'::character varying, 'MERGE_PRODUCT'::character varying])::text[]));

ALTER TABLE logs
  ALTER COLUMN comments TYPE text;
