-- Fiche client, lot 5 : qualité du fichier et documents (docs/PLAN-FICHE-CLIENT.md).

-- Journal : fusion de deux fiches et anonymisation d'un client.
ALTER TABLE logs
  DROP CONSTRAINT logs_transaction_type_check;

ALTER TABLE logs
  ADD CONSTRAINT logs_transaction_type_check
    CHECK ((transaction_type)::text = ANY
  ((ARRAY ['SALE'::character varying, 'DELETE_SALE'::character varying, 'CANCEL_SALE'::character varying, 'REAPPRO'::character varying, 'AJUSTEMENT_IN'::character varying, 'AJUSTEMENT_OUT'::character varying, 'INVENTAIRE'::character varying, 'SUPPRESSION'::character varying, 'COMMANDE'::character varying, 'DECONDTION_IN'::character varying, 'DECONDTION_OUT'::character varying, 'CREATE_PRODUCT'::character varying, 'UPDATE_PRODUCT'::character varying, 'DELETE_PRODUCT'::character varying, 'DISABLE_PRODUCT'::character varying, 'ENABLE_PRODUCT'::character varying, 'MODIFICATION_PRIX_PRODUCT'::character varying, 'MOUVEMENT_STOCK_IN'::character varying, 'MOUVEMENT_STOCK_OUT'::character varying, 'FORCE_STOCK'::character varying, 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE'::character varying, 'ENTREE_STOCK'::character varying, 'ACTIVATION_PRIVILEGE'::character varying, 'RETRAIT_PERIME'::character varying, 'MODIFICATION_DATE_DE_VENTE'::character varying, 'MODIFICATION_INFO_CLIENT'::character varying, 'MERGE_PRODUCT'::character varying, 'AVOIR_SOLDE_SANS_PRODUIT'::character varying, 'MERGE_CUSTOMER'::character varying, 'ANONYMISATION_CLIENT'::character varying])::text[]));

-- Fusion des doublons, export et effacement des données : droits du pharmacien. Le caissier a
-- l'export sur « customer », qui ne doit pas lui ouvrir le dossier santé d'un client.
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT v.code, v.libelle, NULL, NULL, n.id, 0, 3, 'ACTION', TRUE
FROM nav_item n
       CROSS JOIN (VALUES ('pr-fusion-client', 'Fusionner des clients en doublon'),
                          ('pr-donnees-personnelles-client', 'Exporter ou effacer les données d''un client')) AS v(code, libelle)
WHERE n.code = 'customer'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT n.id, r.role_name, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE
FROM nav_item n
       CROSS JOIN (VALUES ('ROLE_ADMIN'), ('ROLE_PHARMACIEN')) AS r(role_name)
WHERE n.code IN ('pr-fusion-client', 'pr-donnees-personnelles-client')
  AND EXISTS (SELECT 1 FROM authority a WHERE a.name = r.role_name)
ON CONFLICT (nav_item_id, role_name) DO UPDATE SET can_execute = TRUE;
