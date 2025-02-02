INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (27, 'gestion-courant', 'Menu Gestion courante', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (28, 'gestion-stock', 'Menu Gestion stock', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (29, 'referentiel', 'Menu referentiel', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (30, 'gestion-tiers-payant', 'Menu gestion tiers-payant', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (31, 'gestion-facturation', 'Menu gestion de la facturation', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (32, 'administration', 'Menu administration', true, null, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (33, 'menu', 'Gestion des menus', false, 32, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (34, 'privilege', 'Gestion des privilège', false, 32, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (1, 'admin', 'Menu administration', false, 32, 01, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (2, 'inventory-transaction', 'Menu suivi produit', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (3, 'store-inventory', 'Menu inventaire', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (4, 'tableaux', 'Menu gestion des tableau', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (35, 'gestion-entree', 'Menu gestion des entrées en stock', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (5, 'tiers-payant', 'Menu gestion des tiers-payants', false, 30, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (6, 'groupe-tiers-payant', 'Menu gestion des groupes de  tiers-payants', false, 30, 1, true,
        'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (7, 'motif-ajustement', 'Menu motifs ajustement', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (8, 'type-etiquette', 'Menu type étiquette', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (9, 'gamme-produit', 'Menu gamme de produits', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (10, 'laboratoire', 'Menu laboratoire', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (11, 'groupe-fournisseur', 'Menu groupe de fournisseurs', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (12, 'fournisseur', 'Menu fournisseurs', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (13, 'rayon', 'Menu rayons', false, 29, 0, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (14, 'famille-produit', 'Menu de famille de produits', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (15, 'forme-produit', 'Menu forme de produits', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (16, 'tva', 'Menu tva', false, 29, 0, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (17, 'ajustement', 'Menu ajustement', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (18, 'decondition', 'Menu déconditionnement de produits', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (19, 'magasin', 'Menu magasin', false, 32, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (20, 'commande', 'Menu gestion de commande', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (21, 'payment', 'Menu paiements', false, 31, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (22, 'presale', 'Menu préventes', false, 27, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (23, 'sales-line', 'Menu details de vente', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (24, 'customer', 'Menu des clients', false, 31, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (25, 'produit', 'Menu produits', false, 28, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (26, 'categorie', 'Menu catégorie', false, 29, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (36, 'ventes-en-cours', 'Menu ventes en cours', false, 27, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (37, 'sales', 'Menu journal des ventes', false, 27, 1, true, 'ALL');
INSERT IGNORE INTO menu (id, name, libelle, racine, parent_id, enable, `ordre`, `type_menu`)
values (38, 'user-management', 'Menu utilisateurs', false, 32, 1, true, 'ALL');


