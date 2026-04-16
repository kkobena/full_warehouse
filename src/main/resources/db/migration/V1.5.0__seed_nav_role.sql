INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN ('commande.dashboard',
               'commande.suggestions',
               'commande.repartition-stock',
               'commande.retour-fournisseur',
               'commande.suggestions',
               'peremptions.lot-perimes',
               'peremptions.lot-a-detruire',
               'rapport-stock.stock-alerts',
               'rapport-stock.stock-valuation',
               'rapport-stock.recap-produit-vendu',
               'rapport-stock.stock-rotation',
               'rapport-stock.abc-pareto'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       'ROLE_RESPONSABLE_COMMANDE',
       TRUE,
       TRUE,
       TRUE,
       TRUE,
       FALSE,
       TRUE,
       TRUE
FROM nav_item
WHERE code IN ( 'catalogue'
  ) ON CONFLICT (nav_item_id, role_name) DO NOTHING;

ALTER TABLE planification_facturation DROP COLUMN  IF EXISTS categorie_tiers;
ALTER TABLE utilisation_cle_securite DROP COLUMN  IF EXISTS privilege_name;
ALTER TABLE utilisation_cle_securite ADD COLUMN IF NOT EXISTS navitem_id integer CONSTRAINT fktnulh7o6ryhwdl4c6v4edison REFERENCES nav_item;

DELETE  FROM user_authority WHERE user_id=3 AND authority_name<> 'ROLE_ADMIN';

DELETE  FROM user_authority WHERE  authority_name = 'ROLE_USER';
DROP  TABLE IF EXISTS  authority_privilege;
DROP  TABLE IF EXISTS   authority_menu;
DROP  TABLE IF EXISTS   privilege;
