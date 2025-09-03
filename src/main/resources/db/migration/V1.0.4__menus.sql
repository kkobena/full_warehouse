INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (27, 'gestion-courant', 'Menu Gestion courante', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (28, 'gestion-stock', 'Menu Gestion stock', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (29, 'referentiel', 'Menu referentiel', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (30, 'gestion-tiers-payant', 'Menu gestion tiers-payant', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (31, 'gestion-facturation', 'Menu gestion de la facturation', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (32, 'administration', 'Menu administration', true, null, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (33, 'menu', 'Gestion des menus', false, 32, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (34, 'privilege', 'Gestion des privilège', false, 32, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (1, 'admin', 'Menu administration', false, 32, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (2, 'inventory-transaction', 'Menu suivi produit', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (3, 'store-inventory', 'Menu inventaire', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (4, 'tableaux', 'Menu gestion des tableau', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (35, 'gestion-entree', 'Menu gestion des entrées en stock', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (5, 'tiers-payant', 'Menu gestion des tiers-payants', false, 30, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (6, 'groupe-tiers-payant', 'Menu gestion des groupes de  tiers-payants', false, 30, true, 1,
        'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (7, 'motif-ajustement', 'Menu motifs ajustement', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (8, 'type-etiquette', 'Menu type étiquette', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (9, 'gamme-produit', 'Menu gamme de produits', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (10, 'laboratoire', 'Menu laboratoire', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (11, 'groupe-fournisseur', 'Menu groupe de fournisseurs', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (12, 'fournisseur', 'Menu fournisseurs', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (13, 'rayon', 'Menu rayons', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (14, 'famille-produit', 'Menu de famille de produits', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (15, 'forme-produit', 'Menu forme de produits', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (16, 'tva', 'Menu tva', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (17, 'ajustement', 'Menu ajustement', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (18, 'decondition', 'Menu déconditionnement de produits', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (19, 'magasin', 'Menu magasin', false, 32, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (20, 'commande', 'Menu gestion de commande', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (21, 'payment', 'Menu paiements', false, 31, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (22, 'presale', 'Menu préventes', false, 27, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (23, 'sales-line', 'Menu details de vente', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (24, 'customer', 'Menu des clients', false, 31, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (25, 'produit', 'Menu produits', false, 28, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (26, 'categorie', 'Menu catégorie', false, 29, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (36, 'ventes-en-cours', 'Menu ventes en cours', false, 27, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (37, 'sales', 'Menu journal des ventes', false, 27, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (38, 'user-management', 'Menu utilisateurs', false, 32, true, 1, 'ALL') ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (39, 'mobile_admin', 'Privilège pour toutes les fonctionnaliés de l Application mobile',
        true, null, true, 1, 'MOBILE') ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre", "type_menu")
values (40, 'mobile_user', 'Privilège d utilisation l Application pour les utilisateurs',
        true, null, true, 1, 'MOBILE') ON CONFLICT (id) DO NOTHING;




INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_FORCE_STOCK', 'Privilège de Forçage du stock à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFIER_PRIX', 'Privilège de modification du prix de vente du produit à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFICATION_VENTE', 'Privilège de modification de vente', 37) ON CONFLICT (name) DO NOTHING;

INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_ANNULATION_VENTE', 'Privilège annulation se vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_SUPPRIME_PRODUIT_VENTE', 'Privilège suppression d une ligne de produit à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_AJOUTER_REMISE_VENTE', 'Privilège appliquer une remise à une vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_VOIR_STOCK_INVENTAIRE', 'Privilège affichage du stock des produits inventoriés', 3) ON CONFLICT (name) DO NOTHING;


INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_FORCE_STOCK') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFIER_PRIX') ON CONFLICT (authority_name, privilege_name) DO NOTHING;

INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFICATION_VENTE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_ANNULATION_VENTE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;



INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_VOIR_STOCK_INVENTAIRE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO tableau(code, valeur)
VALUES ('A', 0) ON CONFLICT (code) DO NOTHING;
INSERT INTO tableau(code, valeur)
VALUES ('C', 0) ON CONFLICT (code) DO NOTHING;

INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_CAISSIER') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_RESPONSABLE_COMMANDE') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_USER') ON CONFLICT (user_id, authority_name) DO NOTHING;

INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_VENDEUR') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_ADMIN') ON CONFLICT (user_id, authority_name) DO NOTHING;

