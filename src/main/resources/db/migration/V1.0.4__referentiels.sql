INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_VENDEUR', 'Vendeur');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_CAISSE', 'Caisse');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_RESPOSSABLE_COMMANDE', 'Responsable de commande');

INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_FORCE_STOCK', 'Privilège de Forçage du stock à la vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFIER_PRIX', 'Privège de modification du prix de vente du produit à la vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFICATION_VENTE', 'Privège de modification de vente', 37);

INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_ANNULATION_VENTE', 'Privège annulation se vente', 37);


INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_FORCE_STOCK');
INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFIER_PRIX');

INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFICATION_VENTE');
INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_ANNULATION_VENTE');


INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_VOIR_STOCK_INVENTAIRE', 'Privège affichage du stock des produits inventoriés', 3);

INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_VOIR_STOCK_INVENTAIRE');
